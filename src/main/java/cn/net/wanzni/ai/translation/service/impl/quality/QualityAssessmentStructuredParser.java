package cn.net.wanzni.ai.translation.service.impl.quality;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QualityAssessmentStructuredParser {

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(.*?)\\s*```");

    private final ObjectMapper objectMapper;

    public QualityAssessmentStructuredParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParseResult parse(String rawContent, boolean repairEnabled) {
        String trimmed = rawContent == null ? "" : rawContent.trim();
        ParseResult direct = tryParse(trimmed, StructuredOutputPath.DIRECT, false, null);
        if (direct.success()) {
            return direct;
        }

        String extracted = extractJsonObject(trimmed);
        if (StringUtils.hasText(extracted) && !extracted.equals(trimmed)) {
            ParseResult extractedResult = tryParse(extracted, StructuredOutputPath.EXTRACTED, false, null);
            if (extractedResult.success()) {
                return extractedResult;
            }
            direct = extractedResult;
        }

        if (!repairEnabled) {
            return direct;
        }

        String candidate = StringUtils.hasText(extracted) ? extracted : trimmed;
        String repaired = JsonRepairSupport.repair(candidate);
        if (StringUtils.hasText(repaired) && !repaired.equals(candidate)) {
            ParseResult repairedResult = tryParse(repaired, StructuredOutputPath.REPAIRED, true, null);
            if (repairedResult.success()) {
                return repairedResult;
            }
            return repairedResult;
        }

        return direct;
    }

    public ParseResult parseRetried(String rawContent) {
        ParseResult retried = parse(rawContent, false);
        if (retried.success()) {
            return new ParseResult(
                    true,
                    retried.payload(),
                    StructuredOutputPath.RETRIED,
                    retried.repairApplied(),
                    null,
                    retried.normalizedContent()
            );
        }
        return new ParseResult(false, null, StructuredOutputPath.RETRIED, false, retried.error(), retried.normalizedContent());
    }

    public static Map<String, Object> buildJsonSchemaDefinition() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("overallScore", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        properties.put("accuracyScore", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        properties.put("fluencyScore", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        properties.put("consistencyScore", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        properties.put("completenessScore", Map.of("type", "integer", "minimum", 0, "maximum", 100));
        properties.put("improvementSuggestions", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("attentionPoints", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("strengths", Map.of("type", "array", "items", Map.of("type", "string")));
        properties.put("assessmentDetails", Map.of("type", "object"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of(
                "accuracyScore",
                "fluencyScore",
                "consistencyScore",
                "completenessScore"
        ));
        schema.put("additionalProperties", false);
        return schema;
    }

    private ParseResult tryParse(String content,
                                 StructuredOutputPath path,
                                 boolean repairApplied,
                                 String previousError) {
        if (!StringUtils.hasText(content)) {
            return new ParseResult(false, null, path, repairApplied, "EMPTY_CONTENT", null);
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            QualityAssessmentLlmPayload payload = toPayload(root);
            return new ParseResult(true, payload, path, repairApplied, null, content);
        } catch (Exception e) {
            return new ParseResult(false, null, path, repairApplied, previousError == null ? e.getMessage() : previousError, content);
        }
    }

    private QualityAssessmentLlmPayload toPayload(JsonNode root) throws JsonProcessingException {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("LLM payload must be a JSON object");
        }

        Integer accuracyScore = readRequiredScore(root, "accuracyScore");
        Integer fluencyScore = readRequiredScore(root, "fluencyScore");
        Integer consistencyScore = readRequiredScore(root, "consistencyScore");
        Integer completenessScore = readRequiredScore(root, "completenessScore");
        Integer overallScore = readOptionalScore(root, "overallScore");

        List<String> suggestions = readStringArray(root, "improvementSuggestions");
        List<String> attentionPoints = readStringArray(root, "attentionPoints");
        List<String> strengths = readStringArray(root, "strengths");
        Map<String, Object> assessmentDetails = readObject(root, "assessmentDetails");

        return QualityAssessmentLlmPayload.builder()
                .overallScore(overallScore)
                .accuracyScore(accuracyScore)
                .fluencyScore(fluencyScore)
                .consistencyScore(consistencyScore)
                .completenessScore(completenessScore)
                .improvementSuggestions(suggestions)
                .attentionPoints(attentionPoints)
                .strengths(strengths)
                .assessmentDetails(assessmentDetails)
                .build();
    }

    private Integer readRequiredScore(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing required score: " + fieldName);
        }
        return readScore(fieldName, node);
    }

    private Integer readOptionalScore(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return null;
        }
        return readScore(fieldName, node);
    }

    private Integer readScore(String fieldName, JsonNode node) {
        if (!node.isIntegralNumber()) {
            throw new IllegalArgumentException("Score field must be an integer: " + fieldName);
        }
        int value = node.intValue();
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("Score field out of range: " + fieldName);
        }
        return value;
    }

    private List<String> readStringArray(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new IllegalArgumentException("Field must be an array of strings: " + fieldName);
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Array item must be a string: " + fieldName);
            }
            String value = item.asText().trim();
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
        }
        return List.copyOf(result);
    }

    private Map<String, Object> readObject(JsonNode root, String fieldName) throws JsonProcessingException {
        JsonNode node = root.get(fieldName);
        if (node == null || node.isNull()) {
            return Map.of();
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("Field must be an object: " + fieldName);
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
    }

    private String extractJsonObject(String rawContent) {
        if (!StringUtils.hasText(rawContent)) {
            return null;
        }
        String candidate = rawContent.trim();
        Matcher codeFenceMatcher = CODE_FENCE_PATTERN.matcher(candidate);
        if (codeFenceMatcher.find()) {
            candidate = codeFenceMatcher.group(1).trim();
        }

        int start = candidate.indexOf('{');
        if (start < 0) {
            return null;
        }

        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < candidate.length(); i++) {
            char current = candidate.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return candidate.substring(start, i + 1).trim();
                }
            }
        }
        return null;
    }

    public record ParseResult(boolean success,
                              QualityAssessmentLlmPayload payload,
                              StructuredOutputPath path,
                              boolean repairApplied,
                              String error,
                              String normalizedContent) {
    }
}
