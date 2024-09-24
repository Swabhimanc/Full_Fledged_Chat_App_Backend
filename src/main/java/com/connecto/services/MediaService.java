package com.connecto.services;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URISyntaxException;

public interface MediaService {

    String uploadFile(MultipartFile file) throws IOException, URISyntaxException;
    String downloadFile();
}
