package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.dto.TranslationMemoryMatch;
import cn.net.wanzni.ai.translation.entity.TranslationMemoryEntry;
import cn.net.wanzni.ai.translation.repository.TranslationMemoryEntryRepository;
import cn.net.wanzni.ai.translation.service.TranslationMemoryService;
import com.huaban.analysis.jieba.JiebaSegmenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationMemoryServiceImpl implements TranslationMemoryService {

    private static final int CANDIDATE_FETCH_SIZE = 30;
    private static final double MIN_SIMILARITY = 0.2d;

    private final TranslationMemoryEntryRepository translationMemoryEntryRepository;
    private final JiebaSegmenter jiebaSegmenter = new JiebaSegmenter();

    @Override
    @Transactional
    public List<TranslationMemoryMatch> searchSimilar(String sourceText,
                                                      String sourceLanguage,
                                                      String targetLanguage,
                                                      String domain,
                                                      int topK) {
        if (!StringUtils.hasText(sourceText) || !StringUtils.hasText(sourceLanguage) || !StringUtils.hasText(targetLanguage)) {
            return List.of();
        }

        String normalizedSourceLanguage = normalizeLanguage(sourceLanguage);
        String normalizedTargetLanguage = normalizeLanguage(targetLanguage);
        String normalizedDomain = normalizeDomain(domain);
        String normalizedSourceText = normalizeText(sourceText);
        String sourceHash = sha256(normalizedSourceText);

        List<TranslationMemoryEntry> exactMatches = translationMemoryEntryRepository
                .findBySourceTextHashAndSourceLanguageAndTargetLanguage(sourceHash, normalizedSourceLanguage, normalizedTargetLanguage)
                .stream()
                .filter(entry -> Boolean.TRUE.equals(entry.getApproved()))
                .filter(entry -> domainMatches(normalizedDomain, normalizeDomain(entry.getDomain())))
                .sorted(Comparator
                        .comparing((TranslationMemoryEntry entry) -> safeInt(entry.getQualityScore())).reversed()
                        .thenComparing(TranslationMemoryEntry::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(Math.max(1, topK))
                .collect(Collectors.toList());

        if (!exactMatches.isEmpty()) {
            bumpHitCount(exactMatches);
            return exactMatches.stream()
                    .map(entry -> toMatch(entry, 1.0d, true))
                    .collect(Collectors.toList());
        }

        Map<Long, TranslationMemoryEntry> candidates = new LinkedHashMap<>();
        if (StringUtils.hasText(normalizedDomain)) {
            translationMemoryEntryRepository
                    .findBySourceLanguageAndTargetLanguageAndDomainAndApprovedTrueOrderByUpdatedAtDesc(
                            normalizedSourceLanguage,
                            normalizedTargetLanguage,
                            normalizedDomain,
                            PageRequest.of(0, CANDIDATE_FETCH_SIZE))
                    .getContent()
                    .forEach(entry -> candidates.putIfAbsent(entry.getId(), entry));
        }

        translationMemoryEntryRepository
                .findBySourceLanguageAndTargetLanguageAndApprovedTrueOrderByUpdatedAtDesc(
                        normalizedSourceLanguage,
                        normalizedTargetLanguage,
                        PageRequest.of(0, CANDIDATE_FETCH_SIZE))
                .getContent()
                .forEach(entry -> candidates.putIfAbsent(entry.getId(), entry));

        List<TranslationMemoryMatch> matches = candidates.values().stream()
                .map(entry -> Map.entry(entry, calculateSimilarity(normalizedSourceText, normalizeText(entry.getSourceText()))))
                .filter(entry -> entry.getValue() >= MIN_SIMILARITY)
                .sorted((left, right) -> {
                    int similarityCompare = Double.compare(right.getValue(), left.getValue());
                    if (similarityCompare != 0) {
                        return similarityCompare;
                    }
                    return Integer.compare(safeInt(right.getKey().getQualityScore()), safeInt(left.getKey().getQualityScore()));
                })
                .limit(Math.max(1, topK))
                .map(entry -> toMatch(entry.getKey(), entry.getValue(), false))
                .collect(Collectors.toList());

        if (!matches.isEmpty()) {
            bumpHitCount(matches.stream()
                    .map(TranslationMemoryMatch::getEntryId)
                    .map(candidates::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList()));
        }

        return matches;
    }

    @Override
    @Transactional
    public Optional<TranslationMemoryEntry> saveApprovedPair(String sourceText,
                                                             String targetText,
                                                             String sourceLanguage,
                                                             String targetLanguage,
                                                             String domain,
                                                             Integer overallScore,
                                                             Boolean hardRulePassed,
                                                             Boolean sensitiveContentDetected,
                                                             Boolean tmEligible,
                                                             Long createdFromTaskId,
                                                             Long createdBy) {
        if (!Boolean.TRUE.equals(tmEligible) || !Boolean.TRUE.equals(hardRulePassed) || Boolean.TRUE.equals(sensitiveContentDetected)) {
            return Optional.empty();
        }
        if (!StringUtils.hasText(sourceText) || !StringUtils.hasText(targetText)) {
            return Optional.empty();
        }

        String normalizedSourceLanguage = normalizeLanguage(sourceLanguage);
        String normalizedTargetLanguage = normalizeLanguage(targetLanguage);
        String normalizedDomain = normalizeDomain(domain);
        String sourceHash = sha256(normalizeText(sourceText));
        int normalizedOverallScore = safeInt(overallScore);

        Optional<TranslationMemoryEntry> existingOptional = findVersionScopedEntry(
                sourceHash,
                normalizedSourceLanguage,
                normalizedTargetLanguage,
                normalizedDomain
        );

        if (existingOptional.isPresent()) {
            TranslationMemoryEntry existing = existingOptional.get();
            if (safeInt(existing.getQualityScore()) >= normalizedOverallScore) {
                return Optional.of(existing);
            }
            existing.setTargetText(targetText);
            existing.setQualityScore(normalizedOverallScore);
            existing.setApproved(Boolean.TRUE);
            existing.setCreatedFromTaskId(createdFromTaskId);
            if (existing.getCreatedBy() == null) {
                existing.setCreatedBy(createdBy);
            }
            TranslationMemoryEntry updated = translationMemoryEntryRepository.save(existing);
            log.info("Updated translation memory entry: id={}, taskId={}, qualityScore={}", updated.getId(), createdFromTaskId, normalizedOverallScore);
            return Optional.of(updated);
        }

        TranslationMemoryEntry entry = TranslationMemoryEntry.builder()
                .sourceText(sourceText)
                .targetText(targetText)
                .sourceLanguage(normalizedSourceLanguage)
                .targetLanguage(normalizedTargetLanguage)
                .domain(normalizedDomain)
                .sourceTextHash(sourceHash)
                .qualityScore(normalizedOverallScore)
                .approved(Boolean.TRUE)
                .hitCount(0)
                .createdFromTaskId(createdFromTaskId)
                .createdBy(createdBy)
                .build();
        TranslationMemoryEntry saved = translationMemoryEntryRepository.save(entry);
        log.info("Saved translation memory entry: id={}, taskId={}, qualityScore={}", saved.getId(), createdFromTaskId, normalizedOverallScore);
        return Optional.of(saved);
    }

    private Optional<TranslationMemoryEntry> findVersionScopedEntry(String sourceHash,
                                                                    String sourceLanguage,
                                                                    String targetLanguage,
                                                                    String domain) {
        if (StringUtils.hasText(domain)) {
            return translationMemoryEntryRepository
                    .findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomain(
                            sourceHash, sourceLanguage, targetLanguage, domain);
        }
        return translationMemoryEntryRepository
                .findFirstBySourceTextHashAndSourceLanguageAndTargetLanguageAndDomainIsNull(
                        sourceHash, sourceLanguage, targetLanguage);
    }

    private void bumpHitCount(Collection<TranslationMemoryEntry> entries) {
        entries.forEach(entry -> entry.setHitCount(safeInt(entry.getHitCount()) + 1));
        translationMemoryEntryRepository.saveAll(entries);
    }

    private TranslationMemoryMatch toMatch(TranslationMemoryEntry entry, double similarity, boolean exactMatch) {
        return TranslationMemoryMatch.builder()
                .entryId(entry.getId())
                .sourceText(entry.getSourceText())
                .targetText(entry.getTargetText())
                .domain(entry.getDomain())
                .qualityScore(entry.getQualityScore())
                .hitCount(entry.getHitCount())
                .similarityScore(similarity)
                .exactMatch(exactMatch)
                .build();
    }

    private double calculateSimilarity(String left, String right) {
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0.0d;
        }
        if (left.equals(right)) {
            return 1.0d;
        }
        if (left.contains(right) || right.contains(left)) {
            return 0.85d;
        }

        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
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

    private Set<String> tokenize(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        if (containsChinese(text)) {
            return jiebaSegmenter.sentenceProcess(text).stream()
                    .map(this::normalizeText)
                    .filter(token -> token.length() >= 2)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return java.util.Arrays.stream(text.split("\\s+"))
                .map(this::normalizeText)
                .filter(token -> token.length() >= 2)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean containsChinese(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.UnicodeScript.of(text.charAt(i)) == Character.UnicodeScript.HAN) {
                return true;
            }
        }
        return false;
    }

    private String normalizeLanguage(String language) {
        if (!StringUtils.hasText(language)) {
            return language;
        }
        String normalized = language.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("zh")) {
            return "zh";
        }
        if (normalized.startsWith("en")) {
            return "en";
        }
        return normalized;
    }

    private String normalizeDomain(String domain) {
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        return domain.trim().toLowerCase(Locale.ROOT);
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

    private boolean domainMatches(String expectedDomain, String actualDomain) {
        if (!StringUtils.hasText(expectedDomain)) {
            return true;
        }
        return expectedDomain.equals(actualDomain) || actualDomain == null;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
