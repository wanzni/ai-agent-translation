package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.dto.TranslationMemoryMatch;
import cn.net.wanzni.ai.translation.entity.TranslationMemoryEntry;

import java.util.List;
import java.util.Optional;

public interface TranslationMemoryService {

    List<TranslationMemoryMatch> searchSimilar(String sourceText,
                                              String sourceLanguage,
                                              String targetLanguage,
                                              String domain,
                                              int topK);

    Optional<TranslationMemoryEntry> saveApprovedPair(String sourceText,
                                                      String targetText,
                                                      String sourceLanguage,
                                                      String targetLanguage,
                                                      String domain,
                                                      Integer overallScore,
                                                      Boolean hardRulePassed,
                                                      Boolean sensitiveContentDetected,
                                                      Boolean tmEligible,
                                                      Long createdFromTaskId,
                                                      Long createdBy);
}
