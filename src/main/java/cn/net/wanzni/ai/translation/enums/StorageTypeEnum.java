package cn.net.wanzni.ai.translation.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 存储类型枚举
 *
 * @date 2024/10/4 上午11:56
 */
@Getter
@AllArgsConstructor
public enum StorageTypeEnum implements BaseEnum<Integer> {

    /**
     * 本地存储
     */
    LOCAL(1, "本地存储"),

    /**
     * 阿里云OSS
     */
    OSS(2, "阿里云OSS"),

    /**
     * Amazon S3
     */
    S3(3, "Amazon S3"),

    /**
     * MinIO
     */
    MINIO(4, "MinIO");

    /**
     * 枚举值
     */
    private Integer value;


    /**
     * 枚举描述
     */
    private String desc;
}