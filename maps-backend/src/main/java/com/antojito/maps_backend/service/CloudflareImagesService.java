package com.antojito.maps_backend.service;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
public class CloudflareImagesService {

    private static final String CLOUDFLARE_DELIVERY_HOST = "imagedelivery.net";

    private final RestClient restClient;
    private final String accountId;
    private final String apiToken;

    public CloudflareImagesService(
            @Value("${app.cloudflare.images.account-id:}") String accountId,
            @Value("${app.cloudflare.images.api-token:}") String apiToken) {
        this.restClient = RestClient.builder().baseUrl("https://api.cloudflare.com/client/v4").build();
        this.accountId = accountId;
        this.apiToken = apiToken;
    }

    public String ensureCloudflareImageUrl(String currentImageUrl, String fallbackSourceUrl, String imageName) {
        if (isCloudflareDeliveryUrl(currentImageUrl)) {
            return currentImageUrl;
        }

        String sourceUrl = hasText(currentImageUrl) ? currentImageUrl : fallbackSourceUrl;
        if (!hasText(sourceUrl)) {
            return currentImageUrl;
        }

        if (!isConfigured()) {
            return sourceUrl;
        }

        return uploadFromUrlOrFallback(sourceUrl, imageName);
    }

    private String uploadFromUrlOrFallback(String sourceUrl, String imageName) {
        try {
            MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
            bodyBuilder.part("url", sourceUrl);
            bodyBuilder.part("requireSignedURLs", "false");

            MultiValueMap<String, HttpEntity<?>> body = bodyBuilder.build();

            CloudflareUploadResponse response = restClient.post()
                    .uri("/accounts/{accountId}/images/v1", accountId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)
                    .body(body)
                    .retrieve()
                    .body(CloudflareUploadResponse.class);

            if (response != null
                    && Boolean.TRUE.equals(response.success())
                    && response.result() != null
                    && response.result().variants() != null
                    && !response.result().variants().isEmpty()) {
                return response.result().variants().get(0);
            }

            log.warn("Cloudflare Images no devolvio variantes para '{}'. Se mantiene URL original.", imageName);
        } catch (Exception exception) {
            log.warn("No se pudo subir imagen de '{}' a Cloudflare Images: {}", imageName, exception.getMessage());
        }

        return sourceUrl;
    }

    private boolean isConfigured() {
        return hasText(accountId) && hasText(apiToken);
    }

    private boolean isCloudflareDeliveryUrl(String imageUrl) {
        return hasText(imageUrl) && imageUrl.contains(CLOUDFLARE_DELIVERY_HOST);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CloudflareUploadResponse(Boolean success, CloudflareImageResult result) {
    }

    private record CloudflareImageResult(List<String> variants) {
    }
}