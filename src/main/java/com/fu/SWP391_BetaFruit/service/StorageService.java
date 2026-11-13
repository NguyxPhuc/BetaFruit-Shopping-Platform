package com.fu.SWP391_BetaFruit.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface StorageService {
    void validateImage(MultipartFile file);

    String storeImage(MultipartFile file, String folder);

    void clearUp(List<String> uploadKeys);
}
