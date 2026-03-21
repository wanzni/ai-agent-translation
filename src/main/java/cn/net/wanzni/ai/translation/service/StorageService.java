package cn.net.wanzni.ai.translation.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 通用存储服务接口
 */
public interface StorageService {

    /**
     * 上传文件到对象存储
     * @param file 文件
     * @param objectName 对象名（路径）
     * @return 存储的对象名
     * @throws Exception 异常
     */
    String upload(MultipartFile file, String objectName) throws Exception;

    /**
     * 上传字节内容到对象存储
     */
    String uploadBytes(byte[] content, String contentType, String objectName) throws Exception;

    /**
     * 获取下载的预签名链接
     */
    String getPresignedGetUrl(String objectName, int expirySeconds) throws Exception;

    /**
     * 读取对象字节
     */
    byte[] downloadBytes(String objectName) throws Exception;
}