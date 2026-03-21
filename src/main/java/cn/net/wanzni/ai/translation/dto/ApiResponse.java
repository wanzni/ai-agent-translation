package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用API响应包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String error;

    // 静态工厂方法：成功响应（仅数据）
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    // 静态工厂方法：成功响应（带消息与数据）
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    // 静态工厂方法：错误响应（带消息、错误码/类型、数据）
    public static <T> ApiResponse<T> error(String message, String error, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .data(data)
                .build();
    }

    // 静态工厂方法：错误响应（带消息与错误码/类型）
    public static <T> ApiResponse<T> error(String message, String error) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(error)
                .build();
    }
}