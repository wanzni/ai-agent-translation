package cn.net.susan.ai.translation.dto;

import cn.net.susan.ai.translation.entity.TerminologyEntry;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 术语列表响应
 *
 * @author 苏三
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TerminologyListResponse {
    /**
     * 数据
     */
    private List<TerminologyEntry> data;
    /**
     * 页码
     */
    private int page;
    /**
     * 每页大小
     */
    private int size;
    /**
     * 总页数
     */
    private int totalPages;
    /**
     * 总元素数
     */
    private long totalElements;
}