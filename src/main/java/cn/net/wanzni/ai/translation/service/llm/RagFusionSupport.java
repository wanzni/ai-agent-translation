package cn.net.wanzni.ai.translation.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
class RagFusionSupport {

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;

    RagFusionSupport(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModelProvider = embeddingModelProvider;
    }

    List<String> fuse(String sourceText, List<String> contextSnippets, int topK) {
        if (contextSnippets == null || contextSnippets.isEmpty()) {
            return List.of();
        }
        List<String> deduplicated = contextSnippets.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
        return rankByEmbedding(sourceText, deduplicated, topK);
    }

    private List<String> rankByEmbedding(String sourceText, List<String> candidates, int topK) {
        try {
            EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
            if (embeddingModel == null || !StringUtils.hasText(sourceText) || candidates == null || candidates.isEmpty()) {
                return candidates;
            }
            List<float[]> srcVecs = embeddingModel.embed(List.of(sourceText));
            float[] srcVec = srcVecs.get(0);
            List<float[]> candVecs = embeddingModel.embed(candidates);
            List<Map.Entry<Integer, Double>> scored = new ArrayList<>();
            for (int i = 0; i < candVecs.size(); i++) {
                scored.add(Map.entry(i, cosineSimilarity(srcVec, candVecs.get(i))));
            }
            scored.sort((left, right) -> Double.compare(right.getValue(), left.getValue()));
            int limit = Math.min(topK, scored.size());
            List<String> ranked = new ArrayList<>(limit);
            for (int i = 0; i < limit; i++) {
                ranked.add(candidates.get(scored.get(i).getKey()));
            }
            return ranked;
        } catch (Exception e) {
            log.warn("Embedding ranking failed, fallback to candidate order: {}", e.getMessage());
            return candidates;
        }
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || right.length == 0 || left.length != right.length) {
            return 0.0d;
        }
        double dot = 0.0d;
        double leftNorm = 0.0d;
        double rightNorm = 0.0d;
        for (int i = 0; i < left.length; i++) {
            double x = left[i];
            double y = right[i];
            dot += x * y;
            leftNorm += x * x;
            rightNorm += y * y;
        }
        double denominator = Math.sqrt(leftNorm) * Math.sqrt(rightNorm);
        return denominator == 0.0d ? 0.0d : dot / denominator;
    }
}
