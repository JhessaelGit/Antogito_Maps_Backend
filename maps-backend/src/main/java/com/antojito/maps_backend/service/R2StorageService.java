package com.antojito.maps_backend.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@Slf4j
public class R2StorageService {

    private static final String DEFAULT_OBJECT_PREFIX = "restaurantes";

    private final RestClient restClient;
    private final String endpoint;
    private final String bucket;
    private final String publicBaseUrl;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final boolean uploadEnabled;

    private volatile S3Client s3Client;

    public R2StorageService(
            @Value("${app.r2.s3-api-url:}") String s3ApiUrl,
            @Value("${app.r2.endpoint:}") String explicitEndpoint,
            @Value("${app.r2.bucket:}") String explicitBucket,
            @Value("${app.r2.public-base-url:}") String explicitPublicBaseUrl,
            @Value("${app.r2.access-key-id:}") String accessKeyId,
            @Value("${app.r2.secret-access-key:}") String secretAccessKey,
            @Value("${app.r2.upload-enabled:true}") boolean uploadEnabled) {
        ParsedR2Config parsed = parseS3ApiUrl(s3ApiUrl);

        this.restClient = RestClient.create();
        this.endpoint = hasText(explicitEndpoint) ? trimTrailingSlash(explicitEndpoint) : parsed.endpoint();
        this.bucket = hasText(explicitBucket) ? explicitBucket : parsed.bucket();
        this.publicBaseUrl = hasText(explicitPublicBaseUrl)
                ? trimTrailingSlash(explicitPublicBaseUrl)
                : parsed.publicBaseUrl();
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.uploadEnabled = uploadEnabled;
    }

    public String ensureR2ImageUrl(String currentImageUrl, String fallbackSourceUrl, String imageName) {
        if (isAlreadyR2Url(currentImageUrl)) {
            return currentImageUrl;
        }

        String sourceUrl = hasText(currentImageUrl) ? currentImageUrl : fallbackSourceUrl;
        if (!hasText(sourceUrl)) {
            return currentImageUrl;
        }

        if (!uploadEnabled || !isConfiguredForUpload()) {
            return sourceUrl;
        }

        return uploadImageOrFallback(sourceUrl, imageName);
    }

    public String uploadMultipartImage(String imageName, String originalFilename, String contentType, byte[] content) {
        if (!uploadEnabled || !isConfiguredForUpload()) {
            throw new IllegalStateException("R2 no esta configurado para subida de imagenes");
        }
        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("Imagen vacia");
        }

        String fallbackName = hasText(originalFilename) ? originalFilename : "imagen.jpg";
        return uploadBytesOrFallback(content, contentType, imageName, fallbackName);
    }

    private String uploadImageOrFallback(String sourceUrl, String imageName) {
        try {
            ResponseEntity<byte[]> source = restClient.get()
                    .uri(sourceUrl)
                    .retrieve()
                    .toEntity(byte[].class);

            byte[] content = source.getBody();
            if (content == null || content.length == 0) {
                return sourceUrl;
            }

            String contentType = source.getHeaders().getContentType() != null
                    ? source.getHeaders().getContentType().toString()
                    : MediaType.IMAGE_JPEG_VALUE;
            return uploadBytesOrFallback(content, contentType, imageName, sourceUrl);
        } catch (Exception exception) {
            log.warn("No se pudo subir imagen de '{}' a R2: {}", imageName, exception.getMessage());
            return sourceUrl;
        }
    }

    private String uploadBytesOrFallback(byte[] content, String contentType, String imageName, String fallbackValue) {
        if (content == null || content.length == 0) {
            return fallbackValue;
        }

        String extension = resolveExtension(contentType, fallbackValue);
        String objectKey = DEFAULT_OBJECT_PREFIX + "/"
                + toSlug(imageName) + "-"
                + UUID.randomUUID() + extension;

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        getS3Client().putObject(request, RequestBody.fromBytes(content));
        return buildPublicObjectUrl(objectKey);
    }

    private S3Client getS3Client() {
        if (s3Client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    s3Client = S3Client.builder()
                            .endpointOverride(URI.create(endpoint))
                            .region(Region.of("auto"))
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                            .serviceConfiguration(S3Configuration.builder()
                                    .pathStyleAccessEnabled(true)
                                    .build())
                            .build();
                }
            }
        }
        return s3Client;
    }

    private boolean isConfiguredForUpload() {
        return hasText(endpoint)
                && hasText(bucket)
                && hasText(accessKeyId)
                && hasText(secretAccessKey);
    }

    private boolean isAlreadyR2Url(String imageUrl) {
        if (!hasText(imageUrl)) {
            return false;
        }
        return imageUrl.contains(".r2.cloudflarestorage.com")
                || imageUrl.contains(".r2.dev")
                || (hasText(publicBaseUrl) && imageUrl.startsWith(publicBaseUrl));
    }

    private String buildPublicObjectUrl(String objectKey) {
        if (hasText(publicBaseUrl)) {
            return trimTrailingSlash(publicBaseUrl) + "/" + objectKey;
        }
        return trimTrailingSlash(endpoint) + "/" + bucket + "/" + objectKey;
    }

    private String resolveExtension(String contentType, String sourceUrl) {
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);

        if (normalizedContentType.contains("png")) {
            return ".png";
        }
        if (normalizedContentType.contains("webp")) {
            return ".webp";
        }
        if (normalizedContentType.contains("gif")) {
            return ".gif";
        }
        if (normalizedContentType.contains("jpeg") || normalizedContentType.contains("jpg")) {
            return ".jpg";
        }

        int queryIndex = sourceUrl.indexOf('?');
        String urlWithoutQuery = queryIndex >= 0 ? sourceUrl.substring(0, queryIndex) : sourceUrl;
        int lastDot = urlWithoutQuery.lastIndexOf('.');
        int lastSlash = urlWithoutQuery.lastIndexOf('/');
        if (lastDot > lastSlash) {
            String extension = urlWithoutQuery.substring(lastDot).toLowerCase(Locale.ROOT);
            if (extension.length() <= 5) {
                return extension;
            }
        }

        return ".jpg";
    }

    private String toSlug(String value) {
        if (!hasText(value)) {
            return "restaurante";
        }
        String slug = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "restaurante" : slug;
    }

    private ParsedR2Config parseS3ApiUrl(String s3ApiUrl) {
        if (!hasText(s3ApiUrl)) {
            return new ParsedR2Config("", "", "");
        }

        String normalized = trimTrailingSlash(s3ApiUrl);

        try {
            URI uri = new URI(normalized);
            String host = uri.getHost();
            if (!hasText(host)) {
                return new ParsedR2Config("", "", "");
            }

            String endpointValue = uri.getScheme() + "://" + host;
            String path = uri.getPath();
            if (path == null || path.isBlank() || "/".equals(path)) {
                return new ParsedR2Config(endpointValue, "", "");
            }

            String bucketValue = path.startsWith("/") ? path.substring(1) : path;
            int slashPosition = bucketValue.indexOf('/');
            if (slashPosition > 0) {
                bucketValue = bucketValue.substring(0, slashPosition);
            }

            String publicBase = endpointValue + "/" + bucketValue;
            return new ParsedR2Config(endpointValue, bucketValue, publicBase);
        } catch (URISyntaxException exception) {
            log.warn("No se pudo parsear app.r2.s3-api-url: {}", exception.getMessage());
            return new ParsedR2Config("", "", "");
        }
    }

    private String trimTrailingSlash(String value) {
        if (!hasText(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParsedR2Config(String endpoint, String bucket, String publicBaseUrl) {
    }

}
