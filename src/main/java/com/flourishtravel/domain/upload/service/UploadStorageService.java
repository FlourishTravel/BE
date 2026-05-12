package com.flourishtravel.domain.upload.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadStorageService {

    String store(MultipartFile file) throws IOException;
}
