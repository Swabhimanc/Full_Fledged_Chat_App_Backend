package com.connecto.configs;

import com.google.cloud.secretmanager.v1beta2.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta2.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta2.SecretVersionName;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecretManager {

    @Bean
    public String getServiceKey() {
        /*String projectId = System.getenv("PROJECT_ID");
        String secretId = System.getenv("FIRESTORE_SECRET");
        String versionId = "1"; // or specific version number

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            // Access the secret version
            SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretId, versionId);
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);

            // Print the secret payload
            return response.getPayload().getData().toStringUtf8();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;*/
        String firestoreSecret = System.getenv("FIRESTORE_SECRET");

        if (firestoreSecret == null) {
            throw new RuntimeException("FIRESTORE_SECRET environment variable is not set.");
        }
        // Return the secret (it is already retrieved and available as an environment variable)
        return firestoreSecret;
    }
}
