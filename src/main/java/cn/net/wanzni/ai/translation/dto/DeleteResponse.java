package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 删除操作的通用响应。
 */
@Data
@AllArgsConstructor
public class DeleteResponse {
    /** 被删除实体的ID */
    private Long id;
    /** 操作是否成功 */
    private boolean success;
}