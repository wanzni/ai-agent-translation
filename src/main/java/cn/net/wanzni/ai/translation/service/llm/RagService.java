package cn.net.wanzni.ai.translation.service.llm;

import cn.net.wanzni.ai.translation.dto.RagContext;
import cn.net.wanzni.ai.translation.dto.TranslationMemoryMatch;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import cn.net.wanzni.ai.translation.repository.TranslationRecordRepository;
import cn.net.wanzni.ai.translation.service.TranslationMemoryService;
import com.huaban.analysis.jieba.JiebaSegmenter;
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
public class RagService {

    private static final int HISTORY_CANDIDATE_FETCH_SIZE = 30;

    private final TerminologyEntryRepository terminologyEntryRepository;
    private final TranslationMemoryService translationMemoryService;
    private final TranslationRecordRepository translationRecordRepository;
    /**
     * 可选获取 EmbeddingModel 的 Provider；如未配置，将回退至关键词检索。
     */
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final RagRetrievalDecisionPolicy retrievalDecisionPolicy;
    private final RagFusionSupport fusionSupport;

    /**
     * jieba中文分词器（线程安全，可复用）
     */
    private final JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();

    public RagService(TerminologyEntryRepository terminologyEntryRepository,
                      TranslationMemoryService translationMemoryService,
                      TranslationRecordRepository translationRecordRepository,
                      ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.terminologyEntryRepository = terminologyEntryRepository;
        this.translationMemoryService = translationMemoryService;
        this.translationRecordRepository = translationRecordRepository;
        this.embeddingModelProvider = embeddingModelProvider;
        this.retrievalDecisionPolicy = new RagRetrievalDecisionPolicy();
        this.fusionSupport = new RagFusionSupport(embeddingModelProvider);
    }

    /**
     * 基于请求参数构建结构化 RAG 上下文。
     * 包含术语映射、历史示例、关键词及便于 LLM/MT 使用的上下文片段。
     */
    public RagContext buildRagContext(TranslationRequest request) throws Exception {
        long start = System.currentTimeMillis();
        try {
            boolean useTerminology = Boolean.TRUE.equals(request.getUseTerminology());
            boolean useHistory = Boolean.TRUE.equals(request.getUseRag());
            List<String> retrievalReasons = new ArrayList<>();
            if (useTerminology) {
                retrievalReasons.add("TERMINOLOGY_ENABLED");
            }
            if (useHistory) {
                retrievalReasons.add("TRANSLATION_MEMORY_ENABLED");
            }
            if (StringUtils.hasText(request.getDomain())) {
                retrievalReasons.add("DOMAIN_FILTER=" + request.getDomain());
            }

            List<String> tokens = extractKeywords(request.getSourceText());
            int topK = Optional.ofNullable(request.getRagTopK()).orElse(5);
            RagRetrievalPlan retrievalPlan = retrievalDecisionPolicy.decide(request, tokens);
            useTerminology = retrievalPlan.retrieveTerminology();
            useHistory = retrievalPlan.retrieveHistory();
            retrievalReasons = new ArrayList<>(retrievalPlan.reasons());

            String normSrcLang = normalizeLanguage(request.getSourceLanguage(), request.getSourceText());
            String normTgtLang = normalizeLanguage(request.getTargetLanguage(), null);
            Map<String, String> glossaryMap = buildGlossaryMap(
                    tokens,
                    normSrcLang,
                    normTgtLang,
                    request.getDomain(),
                    request.getSourceText(),
                    request.getUserId()
            );
            if (!useTerminology) {
                glossaryMap = new LinkedHashMap<>();
            }
            if (!glossaryMap.isEmpty()) {
                retrievalReasons.add("GLOSSARY_HIT");
            }

            List<RagContext.HistorySnippet> historySnippets = searchHistorySnippets(
                    request.getSourceText(),
                    normSrcLang,
                    normTgtLang,
                    request.getDomain(),
                    topK
            );
            if (!useHistory) {
                historySnippets = new ArrayList<>();
            }
            if (!historySnippets.isEmpty()) {
                retrievalReasons.add("HISTORY_HIT");
            }

            List<String> contextSnippets = new ArrayList<>();
            if (!historySnippets.isEmpty()) {
                contextSnippets.add("Relevant translation memory examples:");
                for (RagContext.HistorySnippet snippet : historySnippets) {
                    contextSnippets.add("SOURCE_TYPE: " + Optional.ofNullable(snippet.getSourceType()).orElse("UNKNOWN")
                            + "\nSRC: " + Optional.ofNullable(snippet.getSource()).orElse("")
                            + "\nTGT: " + Optional.ofNullable(snippet.getTarget()).orElse(""));
                }
            }

            List<String> rankedSnippets = fusionSupport.fuse(request.getSourceText(), contextSnippets, topK);
            String preprocessed = preprocessSourceWithGlossary(request.getSourceText(), glossaryMap);

            return RagContext.builder()
                    .glossaryMap(glossaryMap)
                    .historySnippets(historySnippets)
                    .contextSnippets(rankedSnippets)
                    .keywords(tokens)
                    .topK(topK)
                    .buildTimeMs(System.currentTimeMillis() - start)
                    .retrievalTriggered(useTerminology || useHistory)
                    .terminologyRetrievalTriggered(useTerminology)
                    .historyRetrievalTriggered(useHistory)
                    .retrievalReasons(retrievalReasons)
                    .glossaryHitCount(glossaryMap.size())
                    .historyHitCount(historySnippets.size())
                    .preprocessedSourceText(preprocessed)
                    .build();
        } catch (Exception e) {
            log.error("??RAG?????: {}", e.getMessage(), e);
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
    private Map<String, String> buildGlossaryMap(List<String> tokens,
                                                 String srcLang,
                                                 String tgtLang,
                                                 String domain,
                                                 String sourceText,
                                                 Long userId) {
        Map<String, String> map = new LinkedHashMap<>();
        try {
            List<TerminologyEntry> scopedEntries = loadScopedEntries(srcLang, tgtLang, userId);

            if (!tokens.isEmpty()) {
                for (TerminologyEntry entry : scopedEntries) {
                    if (!isEligibleEntry(entry, domain)) {
                        continue;
                    }
                    String sourceTerm = entry.getSourceTerm();
                    if (tokens.contains(sourceTerm)) {
                        map.putIfAbsent(sourceTerm, entry.getTargetTerm());
                    }
                }
            }

            if (map.isEmpty() && StringUtils.hasText(sourceText)) {
                for (TerminologyEntry entry : scopedEntries) {
                    if (!isEligibleEntry(entry, domain)) {
                        continue;
                    }
                    String sourceTerm = entry.getSourceTerm();
                    if (sourceText.contains(sourceTerm)) {
                        map.putIfAbsent(sourceTerm, entry.getTargetTerm());
                    }
                }

                if (map.isEmpty() && (srcLang == null || "auto".equalsIgnoreCase(srcLang))) {
                    for (String zhVariant : new String[]{"zh", "zh-CN", "zh-Hans"}) {
                        List<TerminologyEntry> allByZhPair = userId != null
                                ? terminologyEntryRepository.findByUserIdAndSourceLanguageAndTargetLanguage(userId, zhVariant, tgtLang)
                                : terminologyEntryRepository.findBySourceLanguageAndTargetLanguageAndIsActiveTrue(zhVariant, tgtLang, PageRequest.of(0, 500)).getContent();
                        for (TerminologyEntry entry : allByZhPair) {
                            if (!isEligibleEntry(entry, domain)) {
                                continue;
                            }
                            String sourceTerm = entry.getSourceTerm();
                            if (sourceText.contains(sourceTerm)) {
                                map.putIfAbsent(sourceTerm, entry.getTargetTerm());
                            }
                        }
                        if (!map.isEmpty()) {
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("????????????????????: {}", e.getMessage());
        }
        return map;
    }

    private List<TerminologyEntry> loadScopedEntries(String srcLang, String tgtLang, Long userId) {
        if (userId != null) {
            return terminologyEntryRepository.findByUserIdAndSourceLanguageAndTargetLanguage(userId, srcLang, tgtLang);
        }
        return terminologyEntryRepository
                .findBySourceLanguageAndTargetLanguageAndIsActiveTrue(srcLang, tgtLang, PageRequest.of(0, 500))
                .getContent();
    }

    private boolean isEligibleEntry(TerminologyEntry entry, String domain) {
        if (entry == null) {
            return false;
        }
        if (entry.getIsActive() != null && !entry.getIsActive()) {
            return false;
        }
        if (StringUtils.hasText(domain) && entry.getDomain() != null && !domain.equalsIgnoreCase(entry.getDomain())) {
            return false;
        }
        return isUsableSourceTerm(entry.getSourceTerm()) && StringUtils.hasText(entry.getTargetTerm());
    }

    private boolean isUsableSourceTerm(String sourceTerm) {
        if (!StringUtils.hasText(sourceTerm)) {
            return false;
        }
        String trimmed = sourceTerm.trim();
        if (trimmed.length() < 2) {
            return false;
        }
        long substantiveChars = trimmed.chars().filter(this::isSubstantiveSourceChar).count();
        return substantiveChars >= 2;
    }

    private boolean isSubstantiveSourceChar(int codePoint) {
        return Character.isLetterOrDigit(codePoint)
                || Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
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
        // ????????????????????????????????????????????????????
        return sourceText;
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
     * @param targetLanguage the user ID
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
                        .sourceType(StringUtils.hasText(match.getSourceType()) ? match.getSourceType() : "TM")
                        .build());
            }
            if (list.size() < topK) {
                int remain = Math.max(0, topK - list.size());
                Set<String> existingSources = list.stream()
                        .map(RagContext.HistorySnippet::getSource)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                List<TranslationRecord> records = translationRecordRepository.findRagFallbackCandidates(
                        sourceLanguage,
                        targetLanguage,
                        normalizeDomain(domain),
                        PageRequest.of(0, HISTORY_CANDIDATE_FETCH_SIZE)
                );
                records.stream()
                        .map(record -> Map.entry(record, calculateHistorySimilarity(sourceText, record.getSourceText())))
                        .filter(entry -> entry.getValue() >= 0.2d)
                        .sorted((left, right) -> {
                            int similarityCompare = Double.compare(right.getValue(), left.getValue());
                            if (similarityCompare != 0) {
                                return similarityCompare;
                            }
                            return Comparator.comparing(TranslationRecord::getCreatedAt,
                                            Comparator.nullsLast(Comparator.reverseOrder()))
                                    .compare(right.getKey(), left.getKey());
                        })
                        .map(Map.Entry::getKey)
                        .filter(record -> !existingSources.contains(safeCut(record.getSourceText(), 300)))
                        .limit(remain)
                        .forEach(record -> list.add(RagContext.HistorySnippet.builder()
                                .source(safeCut(record.getSourceText(), 300))
                                .target(safeCut(record.getTranslatedText(), 300))
                                .sourceType("HISTORY")
                                .build()));
            }
        } catch (Exception e) {
            log.warn("历史检索失败，降级为空: {}", e.getMessage());
        }
        return list;
    }

    private double calculateHistorySimilarity(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0.0d;
        }
        String normalizedLeft = normalizeText(left);
        String normalizedRight = normalizeText(right);
        if (normalizedLeft.equals(normalizedRight)) {
            return 1.0d;
        }
        if (normalizedLeft.contains(normalizedRight) || normalizedRight.contains(normalizedLeft)) {
            return 0.85d;
        }
        Set<String> leftTokens = new LinkedHashSet<>(extractKeywords(left));
        Set<String> rightTokens = new LinkedHashSet<>(extractKeywords(right));
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0.0d;
        }
        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);
        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);
        double coverage = (double) intersection.size() / leftTokens.size();
        double jaccard = union.isEmpty() ? 0.0d : (double) intersection.size() / union.size();
        return coverage * 0.6d + jaccard * 0.4d;
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.trim()
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return domain.trim().toLowerCase(Locale.ROOT);
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
            List<float[]> srcVecs = embeddingModel.embed(List.of(sourceText));
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

    private record RetrievalPlan(
            boolean retrievalTriggered,
            boolean retrieveTerminology,
            boolean retrieveHistory,
            List<String> reasons
    ) {
    }
}
