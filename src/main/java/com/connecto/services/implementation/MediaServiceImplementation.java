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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;


@Service
public class MediaServiceImplementation implements MediaService {
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "video/mp4", "video/webm",
            "application/pdf", "text/plain"
    );
    private final String bucketName = System.getenv("BUCKET_NAME");
    @Autowired
    Storage storage;

    @Override
    public String uploadFile(MultipartFile file) throws IOException, URISyntaxException {
        if (file.isEmpty() || file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType().toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported or empty file");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = originalName.lastIndexOf('.') >= 0 ? originalName.substring(originalName.lastIndexOf('.')).replaceAll("[^A-Za-z0-9.]", "") : "";
        String fileName = UUID.randomUUID() + extension;
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
