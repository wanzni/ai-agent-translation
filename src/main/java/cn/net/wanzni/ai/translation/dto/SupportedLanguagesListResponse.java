package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 支持语言列表响应
 *
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportedLanguagesListResponse {
    /**
     * 语言列表
     */
    private List<SupportedLanguageResponse> languages;
    /**
     * 总数
     */
    private int total;
}