package com.connecto.services.implementation;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.connecto.services.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;


@Service
public class MediaServiceImplementation implements MediaService {
    private final String bucketName = "connecto-media-storage";
    @Autowired
    Storage storage;

    @Override
    public String uploadFile(MultipartFile file) throws IOException, URISyntaxException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            throw new RuntimeException("Bucket not found: " + bucketName);
        }
        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, fileName)
                .setContentType(file.getContentType())
                .build();

        storage.create(blobInfo, file.getBytes());

//        URL signedUrl = storage.signUrl(blobInfo, 6, TimeUnit.DAYS, Storage.SignUrlOption.withV4Signature());
//        return signedUrl.toString();

        URI publicUri = new URI("https", "storage.googleapis.com", "/" + bucketName + "/" + fileName, null);
        return publicUri.toString();
    }

    @Override
    public String downloadFile() {
        return "";
    }
}
