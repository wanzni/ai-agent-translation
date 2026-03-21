package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.config.MinioConfig;
import cn.net.wanzni.ai.translation.service.StorageService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * MinIO 存储服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    /**
     * 上传文件到对象存储
     * @param file 文件
     * @param objectName 对象名（路径）
     * @return 存储的对象名
     * @throws Exception 异常
     */
    @Override
    public String upload(MultipartFile file, String objectName) throws Exception {
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .contentType(contentType)
                    .stream(is, file.getSize(), -1)
                    .build());
            log.info("上传到 MinIO 成功: {}", objectName);
        } catch (MinioException e) {
            log.error("MinIO 上传失败: {}", e.getMessage());
            throw e;
        }
        return objectName;
    }

    /**
     * 上传字节内容到对象存储
     */
    @Override
    public String uploadBytes(byte[] content, String contentType, String objectName) throws Exception {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioConfig.getBucketName())
                    .object(objectName)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .stream(bais, content.length, -1)
                    .build());
            log.info("上传字节到 MinIO 成功: {}", objectName);
        }
        return objectName;
    }

    /**
     * 获取下载的预签名链接
     */
    @Override
    public String getPresignedGetUrl(String objectName, int expirySeconds) throws Exception {
        String url = minioClient.getPresignedObjectUrl(io.minio.GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .expiry(expirySeconds)
                .build());
        log.info("生成下载预签名链接: {}", url);
        return url;
    }

    /**
     * 读取对象字节
     */
    @Override
    public byte[] downloadBytes(String objectName) throws Exception {
        try (InputStream is = minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioConfig.getBucketName())
                .object(objectName)
                .build())) {
            return is.readAllBytes();
        }
    }
}