package cn.net.wanzni.ai.translation.service.llm;

import cn.net.wanzni.ai.translation.dto.RagContext;
import cn.net.wanzni.ai.translation.dto.TranslationMemoryMatch;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import cn.net.wanzni.ai.translation.service.TranslationMemoryService;
import com.huaban.analysis.jieba.JiebaSegmenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG（检索增强生成）服务实现。
 *
 * 实现基于术语库与历史记录的混合检索（关键词为主，语义可后续拓展），
 * 构建用于生成的上下文与术语约束映射。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final TerminologyEntryRepository terminologyEntryRepository;
    private final TranslationMemoryService translationMemoryService;
    /**
     * 可选获取 EmbeddingModel 的 Provider；如未配置，将回退至关键词检索。
     */
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    /**
     * jieba中文分词器（线程安全，可复用）
     */
    private final JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();

    /**
     * 基于请求参数构建结构化 RAG 上下文。
     * 包含术语映射、历史示例、关键词及便于 LLM/MT 使用的上下文片段。
     */
    public RagContext buildRagContext(TranslationRequest request) throws Exception {
        long start = System.currentTimeMillis();
        try {
            // 提取关键词（简单分词，可替换为更强的分词器）
            List<String> tokens = extractKeywords(request.getSourceText());
            int topK = Optional.ofNullable(request.getRagTopK()).orElse(5);

            // 术语检索（中文等无空格场景增加回退：按源文本包含匹配）
            String normSrcLang = normalizeLanguage(request.getSourceLanguage(), request.getSourceText());
            String normTgtLang = normalizeLanguage(request.getTargetLanguage(), null);
            Map<String, String> glossaryMap = buildGlossaryMap(//术语表
                    tokens,
                    normSrcLang,
                    normTgtLang,
                    request.getDomain(),
                    request.getSourceText()
            );

            // 历史检索（优先用户维度，其次全局维度）
            List<RagContext.HistorySnippet> historySnippets = searchHistorySnippets(
                    request.getSourceText(),
                    normSrcLang,
                    normTgtLang,
                    request.getDomain(),
                    topK
            );

            // 组装上下文片段：术语说明 + 历史示例
            List<String> contextSnippets = new ArrayList<>();
            if (!glossaryMap.isEmpty()) {
                contextSnippets.add("Terminology constraints (source -> target):");
                glossaryMap.forEach((src, tgt) -> contextSnippets.add(src + " => " + tgt));
            }
            if (!historySnippets.isEmpty()) {
                contextSnippets.add("Relevant translation memory examples:");
                for (RagContext.HistorySnippet s : historySnippets) {
                    contextSnippets.add("SRC: " + Optional.ofNullable(s.getSource()).orElse("")
                            + "\nTGT: " + Optional.ofNullable(s.getTarget()).orElse(""));
                }
            }

            // 使用 Spring AI Embedding 做相似度排序（若可用），仅保留 TopK 片段
            List<String> rankedSnippets = rankByEmbedding(request.getSourceText(), contextSnippets, topK);
            // 基于术语映射对源文本进行前置处理，生成预处理文本
            String preprocessed = preprocessSourceWithGlossary(request.getSourceText(), glossaryMap);

            return RagContext.builder()
                    .glossaryMap(glossaryMap)
                    .historySnippets(historySnippets)
                    .contextSnippets(rankedSnippets)
                    .keywords(tokens)
                    .topK(topK)
                    .buildTimeMs(System.currentTimeMillis() - start)
                    .preprocessedSourceText(preprocessed)
                    .build();
        } catch (Exception e) {
            log.error("构建RAG上下文失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Extracts keywords from a text.
     * 从文本中提取关键词，支持中文jieba分词和英文空格分词
     *
     * @param text the text to extract keywords from
     * @return the list of keywords
     */
    private List<String> extractKeywords(String text) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }

        // 根据文本语言选择分词策略
        if (containsChinese(text)) {
            // 中文：使用jieba分词
            return extractKeywordsChinese(text);
        } else {
            // 英文：使用空格分词
            return extractKeywordsEnglish(text);
        }
    }

    /**
     * 中文分词（jieba）
     */
    private List<String> extractKeywordsChinese(String text) {
        List<String> words = jiebaSegmenter.sentenceProcess(text);
        return words.stream()
                .map(String::toLowerCase)
                .filter(word -> word.length() >= 2)
                .filter(word -> !isStopWord(word))
                .distinct()
                .limit(50)
                .collect(Collectors.toList());
    }

    /**
     * 英文分词（空格）
     */
    private List<String> extractKeywordsEnglish(String text) {
        String normalized = text.replaceAll("[\\n\\r]+", " ")
                .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
                .toLowerCase(Locale.ROOT);
        String[] parts = normalized.split("\\s+");
        return Arrays.stream(parts)
                .filter(p -> p.length() >= 2)
                .filter(p -> !isStopWord(p))
                .distinct()
                .limit(50)
                .collect(Collectors.toList());
    }

    /**
     * 停用词过滤
     */
    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
                "的", "了", "在", "是", "我", "有", "和", "就", "不", "人",
                "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去",
                "你", "会", "着", "没有", "看", "好", "自己", "这", "那", "这些",
                "the", "is", "at", "which", "on", "a", "an", "as", "are", "was",
                "were", "been", "be", "have", "has", "had", "do", "does", "did"
        );
        return stopWords.contains(word);
    }

    /**
     * Builds a glossary map.
     * 构建术语表映射
     *
     * @param tokens the list of tokens
     * @param srcLang the source language
     * @param tgtLang the target language
     * @param domain the domain
     * @param sourceText the source text
     * @return the glossary map
     */
    private Map<String, String> buildGlossaryMap(List<String> tokens, String srcLang, String tgtLang, String domain, String sourceText) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            if (!tokens.isEmpty()) {
                List<TerminologyEntry> entries = terminologyEntryRepository
                        .findBySourceTermsAndLanguagePair(tokens, srcLang, tgtLang);

                for (TerminologyEntry e : entries) {
                    if (e.getIsActive() != null && !e.getIsActive()) continue;
                    if (StringUtils.hasText(domain) && e.getDomain() != null && !domain.equalsIgnoreCase(e.getDomain())) {
                        continue;
                    }
                    String sourceTerm = e.getSourceTerm();
                    String targetTerm = e.getTargetTerm();
                    if (StringUtils.hasText(sourceTerm) && StringUtils.hasText(targetTerm)) {
                        map.putIfAbsent(sourceTerm, targetTerm);
                    }
                }
            }

            // 回退：当未命中或中文长文本未被分词时，按源文本包含匹配语言对下的术语
            if (map.isEmpty() && StringUtils.hasText(sourceText)) {
                List<TerminologyEntry> allByLangPair = terminologyEntryRepository
                        .findBySourceLanguageAndTargetLanguageAndIsActiveTrue(srcLang, tgtLang, PageRequest.of(0, 500))
                        .getContent();
                for (TerminologyEntry e : allByLangPair) {
                    if (e.getIsActive() != null && !e.getIsActive()) continue;
                    if (StringUtils.hasText(domain) && e.getDomain() != null && !domain.equalsIgnoreCase(e.getDomain())) {
                        continue;
                    }
                    String sourceTerm = e.getSourceTerm();
                    String targetTerm = e.getTargetTerm();
                    if (StringUtils.hasText(sourceTerm) && StringUtils.hasText(targetTerm)
                            && sourceText.contains(sourceTerm)) {
                        map.putIfAbsent(sourceTerm, targetTerm);
                    }
                }
                // 进一步回退：当源语言为自动检测或未知，尝试按目标语言聚合（忽略源语言），提升命中率
                if (map.isEmpty() && (srcLang == null || "auto".equalsIgnoreCase(srcLang))) {
                    // 简化策略：尝试常用中文代码集
                    for (String zhVariant : new String[]{"zh", "zh-CN", "zh-Hans"}) {
                        List<TerminologyEntry> allByZhPair = terminologyEntryRepository
                                .findBySourceLanguageAndTargetLanguageAndIsActiveTrue(zhVariant, tgtLang, PageRequest.of(0, 500))
                                .getContent();
                        for (TerminologyEntry e : allByZhPair) {
                            if (e.getIsActive() != null && !e.getIsActive()) continue;
                            if (StringUtils.hasText(domain) && e.getDomain() != null && !domain.equalsIgnoreCase(e.getDomain())) continue;
                            String sourceTerm = e.getSourceTerm();
                            String targetTerm = e.getTargetTerm();
                            if (StringUtils.hasText(sourceTerm) && StringUtils.hasText(targetTerm)
                                    && sourceText.contains(sourceTerm)) {
                                map.putIfAbsent(sourceTerm, targetTerm);
                            }
                        }
                        if (!map.isEmpty()) break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("术语检索失败，降级为空映射: {}", e.getMessage());
        }
        return map;
    }

    /**
     * Preprocesses the source text with a glossary.
     * 用术语表预处理源文本
     *
     * @param sourceText the source text
     * @param glossaryMap the glossary map
     * @return the preprocessed source text
     */
    private String preprocessSourceWithGlossary(String sourceText, Map<String, String> glossaryMap) {
        if (!StringUtils.hasText(sourceText) || glossaryMap == null || glossaryMap.isEmpty()) {
            return sourceText;
        }
        String processed = sourceText;
        try {
            for (Map.Entry<String, String> e : glossaryMap.entrySet()) {
                String src = e.getKey();
                String tgt = e.getValue();
                if (StringUtils.hasText(src) && StringUtils.hasText(tgt) && processed.contains(src)) {
                    // 简单替换：将源术语替换为 [[目标术语]]
                    processed = processed.replace(src, "[[" + tgt + "]]" );
                }
            }
        } catch (Exception ex) {
            log.warn("术语前置处理失败，回退为原文本: {}", ex.getMessage());
            return sourceText;
        }
        return processed;
    }

    /**
     * Normalizes a language code.
     * 标准化语言代码
     *
     * @param code the language code
     * @param sourceText the source text
     * @return the normalized language code
     */
    private String normalizeLanguage(String code, String sourceText) {
        if (!StringUtils.hasText(code) || "auto".equalsIgnoreCase(code)) {
            if (StringUtils.hasText(sourceText) && containsChinese(sourceText)) {
                return "zh";
            }
            return code; // 保留 auto 以便后续回退逻辑处理
        }
        String c = code.toLowerCase(Locale.ROOT);
        if (c.startsWith("zh")) {
            // 统一中文简体到 zh
            if (c.equals("zh-cn") || c.equals("zh-hans")) return "zh";
            return c;
        }
        if (c.startsWith("en")) {
            return "en";
        }
        return c;
    }

    /**
     * Checks if a text contains Chinese characters.
     * 检查文本是否包含中文字符
     *
     * @param text the text to check
     * @return true if the text contains Chinese characters, false otherwise
     */
    private boolean containsChinese(String text) {
        if (!StringUtils.hasText(text)) return false;
        // 简单检测汉字范围
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    /**
     * Searches for history snippets.
     * 搜索用户历史记录中的相关片段
     *
     * @param userId the user ID
     * @param sourceText the source text
     * @param topK the number of snippets to return
     * @return the list of history snippets
     */
    private List<RagContext.HistorySnippet> searchHistorySnippets(String sourceText,
                                                                  String sourceLanguage,
                                                                  String targetLanguage,
                                                                  String domain,
                                                                  int topK) {
        List<RagContext.HistorySnippet> list = new ArrayList<>();
        if (!StringUtils.hasText(sourceText)) return list;
        try {
            List<TranslationMemoryMatch> matches = translationMemoryService.searchSimilar(
                    sourceText,
                    sourceLanguage,
                    targetLanguage,
                    domain,
                    topK
            );
            for (TranslationMemoryMatch match : matches) {
                list.add(RagContext.HistorySnippet.builder()
                        .source(safeCut(match.getSourceText(), 300))
                        .target(safeCut(match.getTargetText(), 300))
                        .build());
            }
        } catch (Exception e) {
            log.warn("历史检索失败，降级为空: {}", e.getMessage());
        }
        return list;
    }

    /**
     * Safely cuts a string to a maximum length.
     * 安全地将字符串截断到最大长度
     *
     * @param t the string to cut
     * @param max the maximum length
     * @return the cut string
     */
    private String safeCut(String t, int max) {
        if (t == null) return "";
        return t.length() > max ? t.substring(0, max) : t;
    }
    /**
     * Ranks candidates by embedding similarity.
     * 根据嵌入向量相似度对候选片段进行排序
     *
     * @param sourceText the source text
     * @param candidates the list of candidates
     * @param topK the number of candidates to return
     * @return the ranked list of candidates
     */
    private List<String> rankByEmbedding(String sourceText, List<String> candidates, int topK) {
        try {
            EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
            if (embeddingModel == null || !StringUtils.hasText(sourceText) || candidates == null || candidates.isEmpty()) {
                return candidates;
            }
            // 计算源文本与候选片段向量（M6 返回 List<float[]>）
            List<float[]> srcVecs = embeddingModel.embed(java.util.List.of(sourceText));
            float[] srcVec = srcVecs.get(0);
            List<float[]> candVecs = embeddingModel.embed(candidates);
            // 依据余弦相似度排序
            List<Map.Entry<Integer, Double>> scored = new ArrayList<>();
            for (int i = 0; i < candVecs.size(); i++) {
                float[] v = candVecs.get(i);
                double sim = cosineSimilarity(srcVec, v);
                scored.add(Map.entry(i, sim));
            }
            scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int limit = Math.min(topK, scored.size());
            List<String> ranked = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) {
                ranked.add(candidates.get(scored.get(i).getKey()));
            }
            return ranked;
        } catch (Exception e) {
            log.warn("Embedding 排序失败，回退原候选: {}", e.getMessage());
            return candidates;
        }
    }

    /**
     * Calculates the cosine similarity between two vectors.
     * 计算两个向量的余弦相似度
     *
     * @param a the first vector
     * @param b the second vector
     * @return the cosine similarity
     */
    private double cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0d;
        }
        double dot = 0.0d, na = 0.0d, nb = 0.0d;
        for (int i = 0; i < a.length; i++) {
            double x = a[i];
            double y = b[i];
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        double denom = Math.sqrt(na) * Math.sqrt(nb);
        return denom == 0.0d ? 0.0d : (dot / denom);
    }
}
