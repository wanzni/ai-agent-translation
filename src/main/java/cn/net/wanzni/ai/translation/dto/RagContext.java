package cn.net.wanzni.ai.translation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * RAG 上下文实体（检索增强生成）。
 *
 * 用于承载由术语库与翻译历史检索得到的结构化上下文信息，
 * 包括术语约束映射、历史记忆片段、关键词、上下文片段等。
 * 该类仅作为运行时数据封装，不涉及持久化。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagContext {

    /**
     * 术语约束映射（sourceTerm -> targetTerm）。
     * 当启用术语时，翻译结果可据此进行术语统一或后处理。
     */
    private Map<String, String> glossaryMap;

    /**
     * 历史翻译记忆片段列表，用于为 LLM 或 MT 引擎提供示例上下文。
     */
    private List<HistorySnippet> historySnippets;

    /**
     * 上下文片段（字符串形式），通常由术语说明和历史示例拼接而成，
     * 可直接用于提示词构造。
     */
    private List<String> contextSnippets;

    /**
     * 从源文本中提取的关键词（简单分词或后续替换为更强分词器）。
     */
    private List<String> keywords;

    /**
     * RAG 检索的 TopK 大小（用于控制检索条目数量）。
     */
    private Integer topK;

    /**
     * 构建该上下文所耗费的毫秒数，用于监控与调优。
     */
    private Long buildTimeMs;

    /**
     * 预处理后的源文本（已应用术语前置约束的标记），用于在调用大模型前
     * 将术语目标译名嵌入源文本中，避免事后替换导致的偏差。
     * 示例：将源术语替换为 [[TargetTerm]]，提示词中要求模型输出时去掉方括号。
     */
    private String preprocessedSourceText;

    /**
     * 历史片段实体：包含源文本与目标译文，用于提示示例。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HistorySnippet {
        /** 源文本片段 */
        private String source;
        /** 目标译文片段 */
        private String target;
    }
}