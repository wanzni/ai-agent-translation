package cn.net.susan.ai.translation.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 
 * 存储系统级别的配置信息，如翻译引擎配置、系统参数等
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "system_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig {

    /**
     * 配置ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置键
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置类型
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "config_type", nullable = false)
    @Builder.Default
    private ConfigType configType = ConfigType.STRING;

    /**
     * 配置描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 配置分类
     */
    @Column(name = "category", length = 50)
    private String category;

    /**
     * 是否可编辑
     */
    @Column(name = "is_editable")
    @Builder.Default
    private Boolean isEditable = true;

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
     * 配置类型枚举
     */
    public enum ConfigType {
        STRING("字符串"),
        NUMBER("数字"),
        BOOLEAN("布尔值"),
        JSON("JSON对象");

        private final String description;

        ConfigType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 获取字符串类型的配置值
     * 
     * @return 字符串值
     */
    public String getStringValue() {
        return this.configValue;
    }

    /**
     * 获取数字类型的配置值
     * 
     * @return 数字值
     */
    public Integer getNumberValue() {
        try {
            return Integer.parseInt(this.configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取布尔类型的配置值
     * 
     * @return 布尔值
     */
    public Boolean getBooleanValue() {
        return Boolean.parseBoolean(this.configValue);
    }

    /**
     * 设置字符串类型的配置值
     * 
     * @param value 字符串值
     */
    public void setStringValue(String value) {
        this.configType = ConfigType.STRING;
        this.configValue = value;
    }

    /**
     * 设置数字类型的配置值
     * 
     * @param value 数字值
     */
    public void setNumberValue(Integer value) {
        this.configType = ConfigType.NUMBER;
        this.configValue = value != null ? value.toString() : null;
    }

    /**
     * 设置布尔类型的配置值
     * 
     * @param value 布尔值
     */
    public void setBooleanValue(Boolean value) {
        this.configType = ConfigType.BOOLEAN;
        this.configValue = value != null ? value.toString() : null;
    }

    /**
     * 设置JSON类型的配置值
     * 
     * @param value JSON字符串
     */
    public void setJsonValue(String value) {
        this.configType = ConfigType.JSON;
        this.configValue = value;
    }
}

