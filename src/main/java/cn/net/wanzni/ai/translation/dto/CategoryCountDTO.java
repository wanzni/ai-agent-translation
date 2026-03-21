package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类统计条目
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryCountDTO {
    private String category;
    private long count;
}