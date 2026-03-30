package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.config.DashscopeProperties;
import cn.net.wanzni.ai.translation.dto.QualityAssessmentRequest;
import cn.net.wanzni.ai.translation.dto.QualityAssessmentResponse;
import cn.net.wanzni.ai.translation.entity.QualityAssessment;
import cn.net.wanzni.ai.translation.repository.QualityAssessmentRepository;
import cn.net.wanzni.ai.translation.service.QualityAssessmentService;
import cn.net.wanzni.ai.translation.service.impl.quality.QualityAssessmentLlmPayload;
import cn.net.wanzni.ai.translation.service.impl.quality.QualityAssessmentStructuredParser;
import cn.net.wanzni.ai.translation.service.impl.quality.StructuredOutputMode;
import cn.net.wanzni.ai.translation.service.impl.quality.StructuredOutputPath;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class QualityAssessmentServiceImpl implements QualityAssessmentService {

    private static final int TM_MIN_OVERALL_SCORE = 80;
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:\\.\\d+)?");
    private static final Pattern NUMBER_WITH_UNIT_PATTERN = Pattern.compile("(?i)\\d+(?:\\.\\d+)?\\s*([a-zA-Z%$￥]+)");
    private static final Pattern LATIN_PROPER_NOUN_PATTERN = Pattern.compile("\\b[A-Z][A-Za-z0-9_-]{1,}\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("(\\{\\{[^{}]+}}|\\$\\{[^{}]+}|%s|%d|\\{\\d+})");
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`[^`]+`");
    private static final Set<String> COMMON_UNITS = Set.of(
            "ms", "sec", "seconds", "minute", "minutes", "hour", "hours",
            "kb", "mb", "gb", "tb", "%", "px", "cm", "mm", "kg", "km",
            "usd", "rmb", "cny", "￥", "$"
    );
    private static final Set<String> SENSITIVE_TERMS = Set.of(
            "violence", "weapon", "kill", "bomb", "terror", "drug", "porn",
            "blood", "suicide", "extremist", "hack", "attack",
            "暴力", "血腥", "炸弹", "毒品", "色情", "恐怖", "极端", "自杀", "攻击"
    );
    private static final Set<String> NON_PROPER_NOUN_CAPITALIZED_WORDS = Set.of(
            "The", "This", "That", "These", "Those", "A", "An", "And", "But", "For", "With", "Without"
    );

    private final DashscopeProperties properties;
    private final QualityAssessmentRepository repository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient = WebClient.builder().build();
    private QualityAssessmentStructuredParser structuredParser;

    @Override
    public QualityAssessmentResponse assess(QualityAssessmentRequest request) throws Exception {
        long start = System.currentTimeMillis();

        RuleCheckResult ruleCheckResult = evaluateRules(request);
        ModelEvaluationResult modelResult = resolveModelOrHeuristicResult(request);
        QualityAssessmentLlmPayload payload = modelResult.payload();

        int acc = payload.accuracyScore();
        int flu = payload.fluencyScore();
        int con = payload.consistencyScore();
        int comp = payload.completenessScore();
        int overall = payload.overallScore() != null
                ? payload.overallScore()
                : (int) Math.round(acc * 0.4 + flu * 0.3 + con * 0.2 + comp * 0.1);

        List<String> suggestions = payload.improvementSuggestions();
        List<String> attention = payload.attentionPoints();
        List<String> pros = payload.strengths();

        if (suggestions.isEmpty() && overall < 85) {
            suggestions = generateHeuristicSuggestions(acc, flu, con, comp);
        }
        if (attention.isEmpty()) {
            attention = generateDefaultAttentionPoints();
        }
        if (pros.isEmpty()) {
            pros = generateDefaultStrengths();
        }

        List<String> tmRejectReasons = new ArrayList<>(ruleCheckResult.rejectReasons());
        if (overall < TM_MIN_OVERALL_SCORE) {
            tmRejectReasons.add("LOW_OVERALL_SCORE");
        }

        boolean hardRulePassed = ruleCheckResult.hardRulePassed();
        boolean sensitiveContentDetected = ruleCheckResult.sensitiveContentDetected();
        boolean tmEligible = overall >= TM_MIN_OVERALL_SCORE && hardRulePassed && !sensitiveContentDetected;
        boolean needsHumanReview = sensitiveContentDetected;
        boolean needsRetry = !hardRulePassed && !sensitiveContentDetected;
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> assessmentDetails = new LinkedHashMap<>(payload.assessmentDetails());
        assessmentDetails.put("ruleCheck", ruleCheckResult.details());
        assessmentDetails.put("structuredOutputMode", modelResult.mode().name());
        assessmentDetails.put("structuredOutputPath", modelResult.path().name());
        assessmentDetails.put("structuredOutputValid", modelResult.valid());
        assessmentDetails.put("structuredOutputRawLength", modelResult.rawLength());
        assessmentDetails.put("structuredOutputRepairApplied", modelResult.repairApplied());
        assessmentDetails.put("structuredOutputRetryUsed", modelResult.retryUsed());

        QualityAssessmentResponse response = QualityAssessmentResponse.builder()
                .overallScore(overall)
                .accuracyScore(acc)
                .fluencyScore(flu)
                .consistencyScore(con)
                .completenessScore(comp)
                .improvementSuggestions(suggestions)
                .attentionPoints(attention)
                .strengths(pros)
                .assessmentDetails(assessmentDetails)
                .assessmentTime(cost)
                .assessmentEngine("QWEN")
                .qualityLevel(qualityLevel(overall))
                .numberScore(ruleCheckResult.numberScore())
                .terminologyScore(ruleCheckResult.terminologyScore())
                .formatScore(ruleCheckResult.formatScore())
                .llmJudgeScore(overall)
                .needsHumanReview(needsHumanReview)
                .needsRetry(needsRetry)
                .tmEligible(tmEligible)
                .hardRulePassed(hardRulePassed)
                .sensitiveContentDetected(sensitiveContentDetected)
                .tmRejectReasons(List.copyOf(new LinkedHashSet<>(tmRejectReasons)))
                .build();

        if (request.isSave() && request.getTranslationRecordId() != null) {
            saveQualityAssessmentEntity(request, response, cost);
        }

        return response;
    }

    private ModelEvaluationResult resolveModelOrHeuristicResult(QualityAssessmentRequest request) throws Exception {
        if (!StringUtils.hasText(properties.getApiKey())) {
            return heuristicResult(request.getSourceText(), request.getTargetText(), "API_KEY_MISSING");
        }

        String systemPrompt = buildSystemPrompt(request.getTargetLanguage());
        String userPrompt = "Source Language: " + (StringUtils.hasText(request.getSourceLanguage()) ? request.getSourceLanguage() : "auto") +
                "\nTarget Language: " + request.getTargetLanguage() +
                "\n\nSource Text:\n" + request.getSourceText() +
                "\n\nTarget Text:\n" + request.getTargetText() +
                "\n\nTask: Evaluate the translation quality and respond with ONLY JSON as specified.";

        if (!properties.getQuality().getStructuredOutput().isEnabled()) {
            return resolveWithMode(request, systemPrompt, userPrompt, StructuredOutputMode.PROMPT_ONLY);
        }

        for (StructuredOutputMode mode : preferredModes()) {
            try {
                return resolveWithMode(request, systemPrompt, userPrompt, mode);
            } catch (Exception apiError) {
                if (mode != StructuredOutputMode.PROMPT_ONLY && supportsStructuredFallback(apiError)) {
                    log.warn("Structured output mode {} not supported, fallback to next mode: {}", mode, apiError.getMessage());
                    continue;
                }
                log.warn("Fallback to heuristic quality evaluation under mode {}: {}", mode, apiError.getMessage());
                return heuristicResult(request.getSourceText(), request.getTargetText(), "API_CALL_FAILED");
            }
        }

        return heuristicResult(request.getSourceText(), request.getTargetText(), "STRUCTURED_MODE_UNSUPPORTED");
    }

    private ModelEvaluationResult resolveWithMode(QualityAssessmentRequest request,
                                                  String systemPrompt,
                                                  String userPrompt,
                                                  StructuredOutputMode mode) throws Exception {
        String content = callQualityModel(systemPrompt, userPrompt, mode);
        QualityAssessmentStructuredParser.ParseResult parseResult = parser().parse(
                content,
                properties.getQuality().getStructuredOutput().isRepairEnabled()
        );
        if (parseResult.success()) {
            return new ModelEvaluationResult(
                    parseResult.payload(),
                    mode,
                    parseResult.path(),
                    true,
                    parseResult.normalizedContent() == null ? 0 : parseResult.normalizedContent().length(),
                    parseResult.repairApplied(),
                    false
            );
        }

        int retries = Math.max(0, properties.getQuality().getStructuredOutput().getMaxRetries());
        if (retries > 0) {
            String retryPrompt = buildRepairPrompt(content);
            String repairedContent = callQualityModel(
                    buildRepairSystemPrompt(),
                    retryPrompt,
                    mode == StructuredOutputMode.PROMPT_ONLY ? StructuredOutputMode.PROMPT_ONLY : StructuredOutputMode.JSON_OBJECT
            );
            QualityAssessmentStructuredParser.ParseResult retryResult = parser().parseRetried(repairedContent);
            if (retryResult.success()) {
                return new ModelEvaluationResult(
                        retryResult.payload(),
                        mode,
                        StructuredOutputPath.RETRIED,
                        true,
                        retryResult.normalizedContent() == null ? 0 : retryResult.normalizedContent().length(),
                        parseResult.repairApplied() || retryResult.repairApplied(),
                        true
                );
            }
            log.warn("Quality assessment repair retry failed: initial={}, retry={}", parseResult.error(), retryResult.error());
        } else {
            log.warn("Quality assessment parse failed without retry: {}", parseResult.error());
        }

        return heuristicResult(request.getSourceText(), request.getTargetText(), "PARSE_FAILED");
    }

    private String callQualityModel(String systemPrompt,
                                    String userPrompt,
                                    StructuredOutputMode mode) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (properties.getTemperature() != null) {
            body.put("temperature", properties.getTemperature());
        }
        applyResponseFormat(body, mode);

        Map<String, Object> resp = webClient.post()
                .uri(properties.getBaseUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + properties.getApiKey())
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (ClientResponse r) ->
                        r.bodyToMono(String.class).flatMap(errBody ->
                                Mono.error(new RuntimeException("DashScope 4xx: " + r.statusCode() + " - " + errBody))))
                .onStatus(HttpStatusCode::is5xxServerError, (ClientResponse r) ->
                        r.bodyToMono(String.class).flatMap(errBody ->
                                Mono.error(new RuntimeException("DashScope 5xx: " + r.statusCode() + " - " + errBody))))
                .bodyToMono(Map.class)
                .block();
        return extractContent(resp);
    }

    private void applyResponseFormat(Map<String, Object> body, StructuredOutputMode mode) {
        if (mode == StructuredOutputMode.JSON_SCHEMA) {
            Map<String, Object> jsonSchema = new LinkedHashMap<>();
            jsonSchema.put("name", "quality_assessment_result");
            jsonSchema.put("strict", true);
            jsonSchema.put("schema", QualityAssessmentStructuredParser.buildJsonSchemaDefinition());
            body.put("response_format", Map.of(
                    "type", "json_schema",
                    "json_schema", jsonSchema
            ));
        } else if (mode == StructuredOutputMode.JSON_OBJECT) {
            body.put("response_format", Map.of("type", "json_object"));
        }
    }

    private List<StructuredOutputMode> preferredModes() {
        String prefer = properties.getQuality().getStructuredOutput().getPrefer();
        if ("json_object".equalsIgnoreCase(prefer)) {
            return List.of(StructuredOutputMode.JSON_OBJECT, StructuredOutputMode.PROMPT_ONLY);
        }
        return List.of(StructuredOutputMode.JSON_SCHEMA, StructuredOutputMode.JSON_OBJECT, StructuredOutputMode.PROMPT_ONLY);
    }

    private boolean supportsStructuredFallback(Exception apiError) {
        String message = Objects.toString(apiError.getMessage(), "").toLowerCase(Locale.ROOT);
        return message.contains("response_format")
                || message.contains("json_schema")
                || message.contains("json_object")
                || message.contains("not support")
                || message.contains("unsupported")
                || message.contains("invalid parameter");
    }

    private String buildRepairSystemPrompt() {
        return "You are a JSON repair assistant. Return ONLY valid JSON that matches the requested schema. Do not re-evaluate the translation.";
    }

    private String buildRepairPrompt(String rawContent) {
        return "Fix the following model output into a valid JSON object that matches this schema exactly.\n" +
                "Do not add explanations and do not re-evaluate the translation.\n" +
                "Required fields: accuracyScore, fluencyScore, consistencyScore, completenessScore.\n" +
                "Optional fields: overallScore, improvementSuggestions, attentionPoints, strengths, assessmentDetails.\n" +
                "Rules: all scores must be integers in [0,100]; lists must contain strings; assessmentDetails must be an object.\n" +
                "Return ONLY JSON.\n\n" +
                "[Raw Output]\n" + rawContent;
    }

    private ModelEvaluationResult heuristicResult(String sourceText, String targetText, String reason) {
        QualityAssessmentLlmPayload payload = buildHeuristicEvaluation(sourceText, targetText, reason);
        return new ModelEvaluationResult(
                payload,
                StructuredOutputMode.HEURISTIC,
                StructuredOutputPath.FALLBACK,
                false,
                0,
                false,
                false
        );
    }

    private void saveQualityAssessmentEntity(QualityAssessmentRequest request,
                                             QualityAssessmentResponse response,
                                             long cost) {
        try {
            QualityAssessment entity = QualityAssessment.builder()
                    .translationRecordId(request.getTranslationRecordId())
                    .assessmentMode(QualityAssessment.AssessmentMode.AUTOMATIC)
                    .overallScore(response.getOverallScore())
                    .accuracyScore(response.getAccuracyScore())
                    .fluencyScore(response.getFluencyScore())
                    .consistencyScore(response.getConsistencyScore())
                    .completenessScore(response.getCompletenessScore())
                    .improvementSuggestions(safeToJson(response.getImprovementSuggestions()))
                    .attentionPoints(safeToJson(response.getAttentionPoints()))
                    .strengths(safeToJson(response.getStrengths()))
                    .assessmentDetails(safeToJson(response.getAssessmentDetails()))
                    .assessmentTime(cost)
                    .assessmentEngine(response.getAssessmentEngine())
                    .terminologyScore(response.getTerminologyScore())
                    .numberScore(response.getNumberScore())
                    .formatScore(response.getFormatScore())
                    .llmJudgeScore(response.getLlmJudgeScore())
                    .needsRetry(Boolean.TRUE.equals(response.getNeedsRetry()))
                    .needsHumanReview(Boolean.TRUE.equals(response.getNeedsHumanReview()))
                    .isManualAssessment(false)
                    .assessorId(null)
                    .build();
            repository.save(entity);
        } catch (Exception e) {
            log.error("Save quality assessment failed: {}", e.getMessage(), e);
        }
    }

    private RuleCheckResult evaluateRules(QualityAssessmentRequest request) {
        List<String> rejectReasons = new ArrayList<>();
        Map<String, Object> details = new LinkedHashMap<>();

        int numberScore = evaluateNumberAndUnitScore(request.getSourceText(), request.getTargetText(), details, rejectReasons);
        int terminologyScore = evaluateTerminologyScore(request.getSourceText(), request.getTargetText(), request.getGlossaryMap(), details, rejectReasons);
        int formatScore = evaluateFormatScore(request.getSourceText(), request.getTargetText(), details);
        boolean sensitiveContentDetected = containsSensitiveContent(request.getSourceText()) || containsSensitiveContent(request.getTargetText());
        if (sensitiveContentDetected) {
            rejectReasons.add("SENSITIVE_CONTENT");
        }
        details.put("sensitiveContentDetected", sensitiveContentDetected);

        boolean hardRulePassed = numberScore >= 100 && terminologyScore >= 100;
        return new RuleCheckResult(
                numberScore,
                terminologyScore,
                formatScore,
                hardRulePassed,
                sensitiveContentDetected,
                List.copyOf(new LinkedHashSet<>(rejectReasons)),
                details
        );
    }

    private int evaluateNumberAndUnitScore(String sourceText,
                                           String targetText,
                                           Map<String, Object> details,
                                           List<String> rejectReasons) {
        List<String> sourceNumbers = sortValues(extractMatches(sourceText, NUMBER_PATTERN));
        List<String> targetNumbers = sortValues(extractMatches(targetText, NUMBER_PATTERN));
        List<String> sourceUnits = extractUnits(sourceText);
        List<String> targetUnits = extractUnits(targetText);

        boolean numbersMatch = sourceNumbers.equals(targetNumbers);
        boolean unitsMatch = targetUnits.containsAll(sourceUnits);
        details.put("sourceNumbers", sourceNumbers);
        details.put("targetNumbers", targetNumbers);
        details.put("sourceUnits", sourceUnits);
        details.put("targetUnits", targetUnits);
        details.put("numbersMatch", numbersMatch);
        details.put("unitsMatch", unitsMatch);

        if (!numbersMatch || !unitsMatch) {
            rejectReasons.add("NUMBER_OR_UNIT_MISMATCH");
            return 0;
        }
        return 100;
    }

    private int evaluateFormatScore(String sourceText,
                                    String targetText,
                                    Map<String, Object> details) {
        Map<String, Object> formatCheck = new LinkedHashMap<>();
        List<String> failedChecks = new ArrayList<>();

        comparePatternCounts("url", sourceText, targetText, URL_PATTERN, formatCheck, failedChecks);
        comparePatternCounts("email", sourceText, targetText, EMAIL_PATTERN, formatCheck, failedChecks);
        comparePatternCounts("placeholder", sourceText, targetText, PLACEHOLDER_PATTERN, formatCheck, failedChecks);
        comparePatternCounts("inlineCode", sourceText, targetText, INLINE_CODE_PATTERN, formatCheck, failedChecks);
        compareCount("newline", countNewlines(sourceText), countNewlines(targetText), formatCheck, failedChecks);
        compareBalancedDelimiter("parentheses", sourceText, targetText, '(', ')', formatCheck, failedChecks);
        compareBalancedDelimiter("squareBrackets", sourceText, targetText, '[', ']', formatCheck, failedChecks);
        compareBalancedDelimiter("curlyBraces", sourceText, targetText, '{', '}', formatCheck, failedChecks);
        compareCount("doubleQuotes", countChar(sourceText, '"'), countChar(targetText, '"'), formatCheck, failedChecks);

        formatCheck.put("failedChecks", failedChecks);
        details.put("formatCheck", formatCheck);

        int score = Math.max(0, 100 - failedChecks.size() * 20);
        formatCheck.put("score", score);
        return score;
    }

    private int evaluateTerminologyScore(String sourceText,
                                         String targetText,
                                         Map<String, String> glossaryMap,
                                         Map<String, Object> details,
                                         List<String> rejectReasons) {
        List<String> missingTargets = new ArrayList<>();
        if (glossaryMap != null && !glossaryMap.isEmpty()) {
            glossaryMap.forEach((sourceTerm, targetTerm) -> {
                if (StringUtils.hasText(sourceTerm) && StringUtils.hasText(targetTerm)
                        && sourceText != null && sourceText.contains(sourceTerm)
                        && (targetText == null || !targetText.contains(targetTerm))) {
                    missingTargets.add(targetTerm);
                }
            });
        }

        List<String> sourceProperNouns = extractProperNouns(sourceText);
        List<String> missingProperNouns = sourceProperNouns.stream()
                .filter(properNoun -> !containsIgnoreCase(targetText, properNoun))
                .collect(java.util.stream.Collectors.toList());

        details.put("missingGlossaryTargets", missingTargets);
        details.put("missingProperNouns", missingProperNouns);
        details.put("sourceProperNouns", sourceProperNouns);

        if (!missingTargets.isEmpty() || !missingProperNouns.isEmpty()) {
            rejectReasons.add("TERMINOLOGY_OR_PROPER_NOUN_MISMATCH");
            return 0;
        }
        return 100;
    }

    private boolean containsSensitiveContent(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        return SENSITIVE_TERMS.stream().anyMatch(normalized::contains);
    }

    private void comparePatternCounts(String key,
                                      String sourceText,
                                      String targetText,
                                      Pattern pattern,
                                      Map<String, Object> formatCheck,
                                      List<String> failedChecks) {
        List<String> sourceMatches = extractMatches(sourceText, pattern);
        List<String> targetMatches = extractMatches(targetText, pattern);
        formatCheck.put(key + "Source", sourceMatches);
        formatCheck.put(key + "Target", targetMatches);
        if (sourceMatches.size() != targetMatches.size()) {
            failedChecks.add(key.toUpperCase(Locale.ROOT) + "_COUNT_MISMATCH");
        }
    }

    private void compareCount(String key,
                              int sourceCount,
                              int targetCount,
                              Map<String, Object> formatCheck,
                              List<String> failedChecks) {
        formatCheck.put(key + "SourceCount", sourceCount);
        formatCheck.put(key + "TargetCount", targetCount);
        if (sourceCount != targetCount) {
            failedChecks.add(key.toUpperCase(Locale.ROOT) + "_COUNT_MISMATCH");
        }
    }

    private void compareBalancedDelimiter(String key,
                                          String sourceText,
                                          String targetText,
                                          char open,
                                          char close,
                                          Map<String, Object> formatCheck,
                                          List<String> failedChecks) {
        int sourceBalance = countChar(sourceText, open) - countChar(sourceText, close);
        int targetBalance = countChar(targetText, open) - countChar(targetText, close);
        int sourcePairCount = Math.min(countChar(sourceText, open), countChar(sourceText, close));
        int targetPairCount = Math.min(countChar(targetText, open), countChar(targetText, close));
        formatCheck.put(key + "SourceBalance", sourceBalance);
        formatCheck.put(key + "TargetBalance", targetBalance);
        formatCheck.put(key + "SourcePairCount", sourcePairCount);
        formatCheck.put(key + "TargetPairCount", targetPairCount);
        if (sourceBalance != targetBalance || sourcePairCount != targetPairCount) {
            failedChecks.add(key.toUpperCase(Locale.ROOT) + "_STRUCTURE_MISMATCH");
        }
    }

    private List<String> extractMatches(String text, Pattern pattern) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<String> matches = new ArrayList<>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            matches.add(matcher.group());
        }
        return matches;
    }

    private int countNewlines(String text) {
        return countChar(text, '\n');
    }

    private int countChar(String text, char target) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == target) {
                count++;
            }
        }
        return count;
    }

    private List<String> extractProperNouns(String text) {
        return extractMatches(text, LATIN_PROPER_NOUN_PATTERN).stream()
                .filter(token -> !NON_PROPER_NOUN_CAPITALIZED_WORDS.contains(token))
                .filter(token -> token.equals(token.toUpperCase(Locale.ROOT))
                        || token.chars().anyMatch(Character::isDigit)
                        || hasInnerCapital(token))
                .distinct()
                .toList();
    }

    private List<String> sortValues(List<String> values) {
        return values.stream().sorted().toList();
    }

    private boolean hasInnerCapital(String token) {
        for (int i = 1; i < token.length(); i++) {
            if (Character.isUpperCase(token.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private List<String> extractUnits(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        Set<String> units = new LinkedHashSet<>();
        Matcher matcher = NUMBER_WITH_UNIT_PATTERN.matcher(text);
        while (matcher.find()) {
            String unit = matcher.group(1).toLowerCase(Locale.ROOT);
            if (COMMON_UNITS.contains(unit)) {
                units.add(unit);
            }
        }
        if (text.contains("%")) {
            units.add("%");
        }
        if (text.contains("￥")) {
            units.add("￥");
        }
        if (text.contains("$")) {
            units.add("$");
        }
        return List.copyOf(units);
    }

    private boolean containsIgnoreCase(String text, String target) {
        return StringUtils.hasText(text) && StringUtils.hasText(target)
                && text.toLowerCase(Locale.ROOT).contains(target.toLowerCase(Locale.ROOT));
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> resp) {
        if (resp == null) return "{}";
        Object choicesObj = null;
        Object output = resp.get("output");
        if (output instanceof Map<?, ?> out) {
            choicesObj = out.get("choices");
        }
        if (choicesObj == null) {
            choicesObj = resp.get("choices");
        }
        if (choicesObj instanceof List<?> choices && !choices.isEmpty()) {
            Object first = choices.get(0);
            if (first instanceof Map<?, ?> m) {
                Object message = m.get("message");
                if (message instanceof Map<?, ?> mm) {
                    Object content = mm.get("content");
                    if (content instanceof String cs && StringUtils.hasText(cs)) {
                        return cs.trim();
                    }
                }
            }
        }
        return "{}";
    }

    private String qualityLevel(int overall) {
        if (overall >= 90) return "浼樼";
        if (overall >= 80) return "鑹ソ";
        if (overall >= 70) return "涓瓑";
        if (overall >= 60) return "鍙婃牸";
        return "闇€瑕佹敼杩?";
    }

    private String safeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String buildSystemPrompt(String targetLang) {
        boolean zh = StringUtils.hasText(targetLang) && targetLang.toLowerCase(Locale.ROOT).startsWith("zh");
        String languageDirective = zh
                ? "All textual fields MUST be written in Simplified Chinese, including improvementSuggestions, attentionPoints, strengths, and assessmentDetails."
                : "Use the target language for textual fields. If target language is Chinese, use Simplified Chinese.";

        return "You are a professional translation quality evaluator. " +
                "Assess the translation across dimensions: accuracy, fluency, consistency, completeness. " +
                languageDirective +
                " Return ONLY a compact JSON object with keys: overallScore, accuracyScore, fluencyScore, consistencyScore, completenessScore, " +
                "improvementSuggestions (array of strings), attentionPoints (array of strings), strengths (array of strings), assessmentDetails (object). " +
                "All scores must be integers in [0,100]. Do not include any commentary or markdown.";
    }

    private QualityAssessmentLlmPayload buildHeuristicEvaluation(String src, String tgt, String reason) {
        int srcLen = src != null ? src.length() : 0;
        int tgtLen = tgt != null ? tgt.length() : 0;
        double lenRatio = (srcLen > 0) ? (tgtLen / (double) srcLen) : 1.0;

        int acc = 78;
        int flu = 76;
        int con = 75;
        int comp = 74;

        if (lenRatio < 0.6) comp = Math.max(60, comp - 10);
        if (lenRatio > 1.4) acc = Math.max(60, acc - 8);

        int overall = (int) Math.round(acc * 0.4 + flu * 0.3 + con * 0.2 + comp * 0.1);

        Map<String, Object> details = new HashMap<>();
        details.put("lengthRatio", lenRatio);
        details.put("note", "heuristic-evaluation");
        details.put("fallbackReason", reason);

        return QualityAssessmentLlmPayload.builder()
                .overallScore(overall)
                .accuracyScore(acc)
                .fluencyScore(flu)
                .consistencyScore(con)
                .completenessScore(comp)
                .improvementSuggestions(generateHeuristicSuggestions(acc, flu, con, comp))
                .attentionPoints(generateDefaultAttentionPoints())
                .strengths(generateDefaultStrengths())
                .assessmentDetails(details)
                .build();
    }

    private List<String> generateHeuristicSuggestions(int acc, int flu, int con, int comp) {
        List<String> suggestions = new ArrayList<>();
        if (acc < 85) suggestions.add("提高准确性：核对术语、专有名词和关键信息");
        if (flu < 85) suggestions.add("提升流畅性：优化句式与连接表达");
        if (con < 85) suggestions.add("增强一致性：保持术语和表达风格统一");
        if (comp < 85) suggestions.add("补足完整性：避免漏译与格式信息丢失");
        if (suggestions.isEmpty()) suggestions.add("整体质量较好，可继续按目标语习惯润色");
        return suggestions;
    }

    private List<String> generateDefaultAttentionPoints() {
        return List.of("专有名词核对", "避免逐字直译", "数字与单位格式一致");
    }

    private List<String> generateDefaultStrengths() {
        return List.of("整体可读性良好", "信息基本完整", "语义传达较为清晰");
    }

    private QualityAssessmentStructuredParser parser() {
        if (structuredParser == null) {
            structuredParser = new QualityAssessmentStructuredParser(objectMapper);
        }
        return structuredParser;
    }

    private record RuleCheckResult(int numberScore,
                                   int terminologyScore,
                                   int formatScore,
                                   boolean hardRulePassed,
                                   boolean sensitiveContentDetected,
                                   List<String> rejectReasons,
                                   Map<String, Object> details) {
    }

    private record ModelEvaluationResult(QualityAssessmentLlmPayload payload,
                                         StructuredOutputMode mode,
                                         StructuredOutputPath path,
                                         boolean valid,
                                         int rawLength,
                                         boolean repairApplied,
                                         boolean retryUsed) {
    }
}
