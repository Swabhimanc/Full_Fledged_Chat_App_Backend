package com.connecto.configs;

import com.google.cloud.secretmanager.v1beta2.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta2.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta2.SecretVersionName;

public class SecretManager {
    public static String getServiceKey() {
        String projectId = "396305485425";
        String secretId = "Firestore-Secret";
        String versionId = "1"; // or specific version number

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            // Access the secret version
            SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretId, versionId);
            AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);

            // Print the secret payload
            String secret = response.getPayload().getData().toStringUtf8();
            System.out.println("Secret: " + secret);
            return secret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
