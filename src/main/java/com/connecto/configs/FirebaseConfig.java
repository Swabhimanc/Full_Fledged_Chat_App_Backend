package com.connecto.configs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Bean
    public Firestore firestore() throws IOException {
        String serviceAccountKey = SecretManager.getServiceKey();

        FileInputStream serviceAccount = new FileInputStream(System.getProperty("user.dir")+"/src/main/resources/Secrets/service-key.json");
        FirebaseOptions options = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountKey.getBytes())))
                .build();

        FirebaseApp.initializeApp(options);
        return FirestoreClient.getFirestore();
    }
}