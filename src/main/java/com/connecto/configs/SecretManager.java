package com.connecto.configs;

import com.google.cloud.secretmanager.v1beta2.AccessSecretVersionResponse;
import com.google.cloud.secretmanager.v1beta2.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1beta2.SecretVersionName;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class SecretManager {

    public String getServiceKey() {
        String projectId = System.getenv("PROJECT_ID");
        String secretId = System.getenv("FIRESTORE_SECRET");
        String versionId = "1"; // or specific version number

        // 1) Preferred for production: fetch from GCP Secret Manager if env vars are provided
        if (hasText(projectId) && hasText(secretId)) {
            try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
                SecretVersionName secretVersionName = SecretVersionName.of(projectId, secretId, versionId);
                AccessSecretVersionResponse response = client.accessSecretVersion(secretVersionName);
                String secret = response.getPayload().getData().toStringUtf8();
                if (hasText(secret)) {
                    return secret;
                }
            } catch (Exception ignored) {
                // Fall back to local key file for dev/local environments
            }
        }

        // 2) Local/dev fallback: service key json file
        String localPath = System.getenv("SERVICE_ACCOUNT_KEY_PATH");
        Path serviceKeyPath = hasText(localPath)
                ? Paths.get(localPath)
                : Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "Secrets", "service-key.json");

        if (Files.exists(serviceKeyPath)) {
            try {
                String json = Files.readString(serviceKeyPath, StandardCharsets.UTF_8);
                if (hasText(json)) {
                    return json;
                }
            } catch (IOException ignored) {
            }
        }

        // 3) Inline JSON (optional) for containerized/dev setups
        String inlineServiceKey = System.getenv("SERVICE_ACCOUNT_KEY");
        if (hasText(inlineServiceKey)) {
            return inlineServiceKey;
        }

        throw new IllegalStateException(
                "Firebase credentials not found. Configure PROJECT_ID/FIRESTORE_SECRET for GCP " +
                        "or provide SERVICE_ACCOUNT_KEY_PATH (or default Secrets/service-key.json)."
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
