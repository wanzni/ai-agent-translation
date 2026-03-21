package cn.net.wanzni.ai.translation.entity;

import cn.net.wanzni.ai.translation.enums.StorageTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文件存储实体类
 * 
 * 存储文件的基本信息，包括文件名、路径、大小、类型等
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "file_storage")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileStorage {

    /**
     * 文件ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件名
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 原始文件名
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /**
     * 文件路径
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 文件类型
     */
    @Column(name = "file_type", length = 100)
    private String fileType;

    /**
     * MIME类型
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * 文件哈希值
     */
    @Column(name = "file_hash", length = 64)
    private String fileHash;

    /**
     * 存储类型
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "storage_type", nullable = false)
    @Builder.Default
    private StorageTypeEnum storageType = StorageTypeEnum.LOCAL;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 是否公开
     */
    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * 下载次数
     */
    @Column(name = "download_count")
    @Builder.Default
    private Integer downloadCount = 0;

    /**
     * 过期时间
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 增加下载次数
     */
    public void incrementDownloadCount() {
        this.downloadCount = (this.downloadCount == null ? 0 : this.downloadCount) + 1;
    }

    /**
     * 检查文件是否过期
     * 
     * @return 是否过期
     */
    public boolean isExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 获取文件大小（人类可读格式）
     * 
     * @return 文件大小字符串
     */
    public String getFileSizeFormatted() {
        if (this.fileSize == null) {
            return "0 B";
        }

        long size = this.fileSize;
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return size + " " + units[unitIndex];
    }

    /**
     * 获取文件扩展名
     * 
     * @return 文件扩展名
     */
    public String getFileExtension() {
        if (this.originalName == null || this.originalName.trim().isEmpty()) {
            return "";
        }

        int lastDotIndex = this.originalName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == this.originalName.length() - 1) {
            return "";
        }

        return this.originalName.substring(lastDotIndex + 1).toLowerCase();
    }

    /**
     * 检查是否为图片文件
     * 
     * @return 是否为图片
     */
    public boolean isImage() {
        String extension = getFileExtension();
        return extension.matches("jpg|jpeg|png|gif|bmp|webp|svg");
    }

    /**
     * 检查是否为文档文件
     * 
     * @return 是否为文档
     */
    public boolean isDocument() {
        String extension = getFileExtension();
        return extension.matches("pdf|doc|docx|xls|xlsx|ppt|pptx|txt|rtf");
    }

    /**
     * 检查是否为压缩文件
     * 
     * @return 是否为压缩文件
     */
    public boolean isArchive() {
        String extension = getFileExtension();
        return extension.matches("zip|rar|7z|tar|gz|bz2");
    }
}

