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
 * 用户配置实体类
 * 
 * 存储用户的个性化配置信息，如语言偏好、界面设置等
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Entity
@Table(name = "user_settings", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "setting_key"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSetting {

    /**
     * 配置ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 配置键
     */
    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    /**
     * 配置值
     */
    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    /**
     * 配置类型
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "setting_type", nullable = false)
    @Builder.Default
    private SettingType settingType = SettingType.STRING;

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
    public enum SettingType {
        STRING("字符串"),
        NUMBER("数字"),
        BOOLEAN("布尔值"),
        JSON("JSON对象");

        private final String description;

        SettingType(String description) {
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
        return this.settingValue;
    }

    /**
     * 获取数字类型的配置值
     * 
     * @return 数字值
     */
    public Integer getNumberValue() {
        try {
            return Integer.parseInt(this.settingValue);
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
        return Boolean.parseBoolean(this.settingValue);
    }

    /**
     * 设置字符串类型的配置值
     * 
     * @param value 字符串值
     */
    public void setStringValue(String value) {
        this.settingType = SettingType.STRING;
        this.settingValue = value;
    }

    /**
     * 设置数字类型的配置值
     * 
     * @param value 数字值
     */
    public void setNumberValue(Integer value) {
        this.settingType = SettingType.NUMBER;
        this.settingValue = value != null ? value.toString() : null;
    }

    /**
     * 设置布尔类型的配置值
     * 
     * @param value 布尔值
     */
    public void setBooleanValue(Boolean value) {
        this.settingType = SettingType.BOOLEAN;
        this.settingValue = value != null ? value.toString() : null;
    }

    /**
     * 设置JSON类型的配置值
     * 
     * @param value JSON字符串
     */
    public void setJsonValue(String value) {
        this.settingType = SettingType.JSON;
        this.settingValue = value;
    }
}

