package com.antojito.maps_backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        try {
            InputStream serviceAccount = null;

            // 1. Intentar leer desde variable de entorno (Para Heroku/Produccion)
            String firebaseEnv = System.getenv("FIREBASE_CREDENTIALS_BASE64");
            if (firebaseEnv != null && !firebaseEnv.isBlank()) {
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(firebaseEnv);
                serviceAccount = new java.io.ByteArrayInputStream(decodedBytes);
            } else {
                // 2. Fallback a archivo local (Para desarrollo)
                FileSystemResource resource = new FileSystemResource("antojitos-maps-auth-firebase-adminsdk-fbsvc-2f6d77adc7.json");
                if (resource.exists()) {
                    serviceAccount = resource.getInputStream();
                }
            }

            if (serviceAccount != null) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                    System.out.println("Firebase Admin SDK inicializado correctamente.");
                }
            } else {
                System.err.println("ADVERTENCIA: No se pudo inicializar Firebase. Faltan las credenciales (variable FIREBASE_CREDENTIALS o archivo local).");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
