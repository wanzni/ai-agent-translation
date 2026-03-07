package cn.net.susan.ai.translation.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 *
 * <p>该配置类负责初始化和配置与 MinIO 对象存储服务的连接。
 * 它从 `application.yml` 文件中读取 `minio.*` 相关属性，
 * 并创建一个 {@link MinioClient} 的单例 Bean，供整个应用程序使用。
 *
 * <p>在初始化过程中，它还会自动检查配置的存储桶（Bucket）是否存在，
 * 如果不存在，则会尝试创建它，以确保应用程序能够正常读写文件。
 *
 * @author 苏三
 * @version 1.0.0
 * @since 2025-11-21
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /** MinIO 服务端点 URL */
    private String endpoint;

    /** 访问密钥 (Access Key) */
    private String accessKey;

    /** 私有密钥 (Secret Key) */
    private String secretKey;

    /** 默认使用的存储桶名称 */
    private String bucketName;

    /** 是否启用 HTTPS (secure) 连接 */
    private boolean secure = false;

    /**
     * 创建并配置 MinioClient Bean
     *
     * <p>此方法会构建一个 {@link MinioClient} 实例，并检查配置的存储桶是否存在。
     * 如果存储桶不存在，它将自动创建，以简化部署和配置过程。
     *
     * @return 配置完成的 {@link MinioClient} 实例。
     */
    @Bean
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("已创建 MinIO 桶: {}", bucketName);
            }
        } catch (Exception e) {
            log.warn("检查/创建 MinIO 桶失败: {}", e.getMessage());
        }
        return client;
    }
}