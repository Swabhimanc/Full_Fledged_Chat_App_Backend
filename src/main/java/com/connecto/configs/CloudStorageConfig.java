package com.connecto.configs;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class CloudStorageConfig {

    @Autowired
    SecretManager secretManager;

    @Bean
    public Storage googleCloudStorage() throws IOException {

        FileInputStream serviceAccount = new FileInputStream(System.getProperty("user.dir")+"/src/main/resources/Secrets/service-key.json");
        GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

//        String serviceAccountKey = secretManager.getServiceKey();
//        GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(serviceAccountKey.getBytes()));

        return StorageOptions.newBuilder()
                .setCredentials(credentials)
                .build()
                .getService();
    }
}
