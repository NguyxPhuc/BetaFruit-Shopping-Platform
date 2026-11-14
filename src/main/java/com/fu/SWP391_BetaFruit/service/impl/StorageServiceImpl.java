package com.fu.SWP391_BetaFruit.service.impl;

import com.fu.SWP391_BetaFruit.exception.InvalidUploadImageException;
import com.fu.SWP391_BetaFruit.service.StorageService;
import com.fu.SWP391_BetaFruit.utils.ImageUploadValidator;
import io.minio.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
public class StorageServiceImpl implements StorageService {
    @Autowired
    private MinioClient minioClient;

    @Autowired
    private ImageUploadValidator imageUploadValidator;

    @Value("${minio.bucket-name}")
    private String defaultBucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Override
    public void validateImage(MultipartFile file){
        try{
            imageUploadValidator.validate(file.getBytes());
        }catch (Exception e){
            throw new InvalidUploadImageException("Invalid image content" + e.getMessage());
        }
    }

    private void setBucketPublicPolicy(String bucketName) throws Exception {
        String publicPolicy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """.formatted(bucketName);
        minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                        .bucket(bucketName)
                        .config(publicPolicy)
                        .build()
        );
    }

    private void checkExistBucket() throws Exception {
        boolean isExist = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(defaultBucket).build()
        );
        if (!isExist) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(defaultBucket).build());
            setBucketPublicPolicy(defaultBucket);
        }
    }

    @Override
    public String storeImage(MultipartFile file, String folder) {
        try {
            checkExistBucket();
            String extension = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }
            String fileName = UUID.randomUUID().toString() + extension;
            String cleanFolder = (folder != null && !folder.trim().isEmpty())
                    ? folder.trim().replaceAll("^/+|/+$", "")
                    : "general";
            String objectName = cleanFolder + fileName;
            // Đẩy InputStream lên MinIO
            try (InputStream stream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(defaultBucket)
                                .contentType(file.getContentType())
                                .object(objectName)
                                .stream(stream, file.getSize(), -1)
                                .build()
                );
            }
            String baseUrl = endpoint.endsWith("/") ? endpoint : endpoint + "/";
            return baseUrl + defaultBucket + "/" + objectName;
        } catch (Exception e) {
            throw new InvalidUploadImageException("Lỗi lưu trữ ảnh lên MinIO: " + e.getMessage());
        }
    }

    private void clearUp(String uploadKey) {
        if (uploadKey == null || uploadKey.trim().isEmpty()) {
            return;
        }
        try {
            String objectName = uploadKey;
            String prefix = defaultBucket + "/";
            if (objectName.contains(prefix)) {
                objectName = objectName.substring(objectName.indexOf(prefix) + prefix.length());
            }
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            System.err.println("Cảnh báo dọn dẹp ảnh MinIO thất bại cho key [" + uploadKey + "]: " + e.getMessage());
        }
    }

    @Override
    public void clearUp(List<String> uploadKeys) {
        if (uploadKeys != null) {
            for (String key : uploadKeys) {
                clearUp(key);
            }
        }
    }


}
