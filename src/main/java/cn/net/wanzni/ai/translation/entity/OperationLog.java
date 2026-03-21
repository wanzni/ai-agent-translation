package cn.net.wanzni.ai.translation.entity;

import cn.net.wanzni.ai.translation.enums.OperationTypeEnum;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 
 * 存储系统操作日志，包括用户操作、API调用、错误日志等
 * 
 * @version 1.0.0
 */
@Entity
@Table(name = "operation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperationLog {

    /**
     * 日志ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "operation_type", nullable = false, length = 50)
    private OperationTypeEnum operationType;

    /**
     * 操作描述
     */
    @Column(name = "operation_description", length = 500)
    private String operationDescription;

    /**
     * 请求URL
     */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /**
     * 请求方法
     */
    @Column(name = "request_method", length = 10)
    private String requestMethod;

    /**
     * 请求参数
     */
    @Column(name = "request_params", columnDefinition = "TEXT")
    private String requestParams;

    /**
     * 响应状态码
     */
    @Column(name = "response_status")
    private Integer responseStatus;

    /**
     * 响应时间（毫秒）
     */
    @Column(name = "response_time")
    private Long responseTime;

    /**
     * IP地址
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * 用户代理
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 检查是否为成功操作
     * 
     * @return 是否成功
     */
    public boolean isSuccess() {
        return this.responseStatus != null && this.responseStatus >= 200 && this.responseStatus < 300;
    }

    /**
     * 检查是否为错误操作
     * 
     * @return 是否为错误
     */
    public boolean isError() {
        return this.responseStatus != null && this.responseStatus >= 400;
    }

    /**
     * 检查是否为慢操作
     * 
     * @param threshold 阈值（毫秒）
     * @return 是否为慢操作
     */
    public boolean isSlowOperation(long threshold) {
        return this.responseTime != null && this.responseTime > threshold;
    }

    /**
     * 获取操作类型描述
     * 
     * @return 操作类型描述
     */
    public String getOperationTypeDescription() {
        try {
            return this.operationType.getDesc();
        } catch (IllegalArgumentException e) {
            return this.operationType.name();
        }
    }
}

