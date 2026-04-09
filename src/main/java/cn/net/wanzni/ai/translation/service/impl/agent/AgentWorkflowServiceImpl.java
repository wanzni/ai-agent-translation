package cn.net.wanzni.ai.translation.service.impl.agent;

import cn.net.wanzni.ai.translation.dto.QualityAssessmentRequest;
import cn.net.wanzni.ai.translation.dto.QualityAssessmentResponse;
import cn.net.wanzni.ai.translation.dto.RagContext;
import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.TranslationResponse;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.AgentTaskStep;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.entity.TranslationRecord;
import cn.net.wanzni.ai.translation.enums.AgentStepStatusEnum;
import cn.net.wanzni.ai.translation.enums.AgentStepTypeEnum;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.ReviewStatusEnum;
import cn.net.wanzni.ai.translation.exception.ResourceNotFoundException;
import cn.net.wanzni.ai.translation.repository.AgentTaskRepository;
import cn.net.wanzni.ai.translation.repository.AgentTaskStepRepository;
import cn.net.wanzni.ai.translation.repository.ReviewTaskRepository;
import cn.net.wanzni.ai.translation.repository.TranslationRecordRepository;
import cn.net.wanzni.ai.translation.service.QualityAssessmentService;
import cn.net.wanzni.ai.translation.service.ReviewTaskService;
import cn.net.wanzni.ai.translation.service.TranslationMemoryService;
import cn.net.wanzni.ai.translation.service.TranslationService;
import cn.net.wanzni.ai.translation.service.agent.AgentWorkflowService;
import cn.net.wanzni.ai.translation.service.llm.RagService;
import cn.net.wanzni.ai.translation.service.sse.AgentTaskEventStreamService;
import cn.net.wanzni.ai.translation.service.translation.QwenTranslationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class AgentWorkflowServiceImpl implements AgentWorkflowService {

    private final AgentTaskRepository agentTaskRepository;
    private final AgentTaskStepRepository agentTaskStepRepository;
    private final RagService ragService;
    private final TranslationService translationService;
    private final QwenTranslationService qwenTranslationService;
    private final TranslationMemoryService translationMemoryService;
    private final TranslationRecordRepository translationRecordRepository;
    private final ReviewTaskRepository reviewTaskRepository;
    private final ReviewTaskService reviewTaskService;
    private final QualityAssessmentService qualityAssessmentService;
    private final AgentTaskEventStreamService agentTaskEventStreamService;
    private final ObjectMapper objectMapper;
    private final Executor workflowExecutor;

    public AgentWorkflowServiceImpl(AgentTaskRepository agentTaskRepository,
                                    AgentTaskStepRepository agentTaskStepRepository,
                                    RagService ragService,
                                    TranslationService translationService,
                                    QwenTranslationService qwenTranslationService,
                                    TranslationMemoryService translationMemoryService,
                                    TranslationRecordRepository translationRecordRepository,
                                    ReviewTaskRepository reviewTaskRepository,
                                    ReviewTaskService reviewTaskService,
                                    QualityAssessmentService qualityAssessmentService,
                                    AgentTaskEventStreamService agentTaskEventStreamService,
                                    ObjectMapper objectMapper,
                                    @Qualifier("chatSseExecutor") Executor workflowExecutor) {
        this.agentTaskRepository = agentTaskRepository;
        this.agentTaskStepRepository = agentTaskStepRepository;
        this.ragService = ragService;
        this.translationService = translationService;
        this.qwenTranslationService = qwenTranslationService;
        this.translationMemoryService = translationMemoryService;
        this.translationRecordRepository = translationRecordRepository;
        this.reviewTaskRepository = reviewTaskRepository;
        this.reviewTaskService = reviewTaskService;
        this.qualityAssessmentService = qualityAssessmentService;
        this.agentTaskEventStreamService = agentTaskEventStreamService;
        this.objectMapper = objectMapper;
        this.workflowExecutor = workflowExecutor;
    }

    @Override
    public void submitTextTask(Long taskId) {
        workflowExecutor.execute(() -> executeTextTask(taskId));
    }

    @Override
    public void executeTextTask(Long taskId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + taskId));

        if (task.getStatus() == AgentTaskStatusEnum.RUNNING || task.getStatus() == AgentTaskStatusEnum.COMPLETED) {
            log.info("Skip workflow execution for task {}, current status={}", taskId, task.getStatus());
            return;
        }

        updateTaskRunning(task);

        TranslationRequest.TranslationRequestBuilder requestBuilder = TranslationRequest.builder()
                .userId(task.getUserId())
                .agentTaskId(task.getId())
                .sourceText(task.getSourceText())
                .sourceLanguage(task.getSourceLanguage())
                .targetLanguage(task.getTargetLanguage())
                .domain(task.getDomain())
                .useTerminology(Boolean.TRUE)
                .useRag(Boolean.TRUE)
                .needQualityAssessment(Boolean.TRUE);
        if (StringUtils.hasText(task.getSelectedModel())) {
            requestBuilder.translationEngine(task.getSelectedModel());
        }
        TranslationRequest translationRequest = requestBuilder.build();

        try {
            executePlanStep(task, translationRequest);
            RagContext ragContext = executeRetrieveStep(task, translationRequest);
            executeFusionStep(task, translationRequest, ragContext);
            TranslationResponse translationResponse = executeTranslateStep(task, translationRequest, ragContext);
            RevisionExecution revisionExecution = executeRevisionLoop(task, translationRequest, translationResponse);
            syncLatestTranslationRecord(task.getId(), revisionExecution.finalText());
            boolean tmStored = saveTranslationMemory(task, translationRequest, revisionExecution.finalText(), revisionExecution.finalQualityResponse());
            finalizeTaskSuccess(task.getId(), translationResponse, revisionExecution, tmStored);
        } catch (Exception e) {
            log.error("Workflow execution failed for task {}: {}", taskId, e.getMessage(), e);
            finalizeTaskFailure(task.getId(), e);
        }
    }

    @Transactional
    protected void updateTaskRunning(AgentTask task) {
        task.setStatus(AgentTaskStatusEnum.RUNNING);
        task.setCurrentStep(AgentStepTypeEnum.PLAN.name());
        task.setStartedAt(LocalDateTime.now());
        AgentTask savedTask = agentTaskRepository.save(task);
        agentTaskEventStreamService.publishTaskUpdate(savedTask, null);
    }

    private void executePlanStep(AgentTask task, TranslationRequest request) {
        Map<String, Object> input = Map.of(
                "sourceLanguage", request.getSourceLanguage(),
                "targetLanguage", request.getTargetLanguage(),
                "domain", request.getDomain(),
                "textLength", request.getCharacterCount()
        );
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("plan", List.of("PLAN", "RETRIEVE_TERMINOLOGY", "RETRIEVE_MEMORY", "FUSION", "TRANSLATE", "QUALITY_CHECK", "REVISE(optional)", "FINALIZE"));
        output.put("useRag", request.getUseRag());
        output.put("useTerminology", request.getUseTerminology());
        saveStep(task.getId(), AgentStepTypeEnum.PLAN, "Plan task", null, null, input, output, AgentStepStatusEnum.SUCCESS, 0L, null);
        updateCurrentStep(task.getId(), AgentStepTypeEnum.RETRIEVE_TERMINOLOGY.name());
    }

    private RagContext executeRetrieveStep(AgentTask task, TranslationRequest request) throws Exception {
        long start = System.currentTimeMillis();
        try {
            RagContext ragContext = ragService.buildRagContext(request);
            long tmHistoryCount = ragContext.getHistorySnippets() == null ? 0 : ragContext.getHistorySnippets().stream()
                    .filter(snippet -> "TM".equalsIgnoreCase(snippet.getSourceType())
                            || "HUMAN_REVIEWED".equalsIgnoreCase(snippet.getSourceType()))
                    .count();
            long fallbackHistoryCount = ragContext.getHistorySnippets() == null ? 0 : ragContext.getHistorySnippets().stream()
                    .filter(snippet -> "HISTORY".equalsIgnoreCase(snippet.getSourceType()))
                    .count();
            Map<String, Object> terminologyOutput = new LinkedHashMap<>();
            terminologyOutput.put("keywordCount", ragContext.getKeywords() == null ? 0 : ragContext.getKeywords().size());
            terminologyOutput.put("terminologyRetrievalTriggered", ragContext.getTerminologyRetrievalTriggered());
            terminologyOutput.put("glossaryHitCount", ragContext.getGlossaryHitCount());
            terminologyOutput.put("retrievalReasons", ragContext.getRetrievalReasons());
            terminologyOutput.put("buildTimeMs", ragContext.getBuildTimeMs());
            saveStep(task.getId(), AgentStepTypeEnum.RETRIEVE_TERMINOLOGY, "Retrieve terminology constraints",
                    "rag_service", null, buildRequestInput(request), terminologyOutput, AgentStepStatusEnum.SUCCESS,
                    System.currentTimeMillis() - start, null);
            updateCurrentStep(task.getId(), AgentStepTypeEnum.RETRIEVE_MEMORY.name());

            Map<String, Object> memoryOutput = new LinkedHashMap<>();
            memoryOutput.put("historyRetrievalTriggered", ragContext.getHistoryRetrievalTriggered());
            memoryOutput.put("historyHitCount", ragContext.getHistoryHitCount());
            memoryOutput.put("tmHitCount", tmHistoryCount);
            memoryOutput.put("historyFallbackCount", fallbackHistoryCount);
            memoryOutput.put("retrievalReasons", ragContext.getRetrievalReasons());
            memoryOutput.put("buildTimeMs", ragContext.getBuildTimeMs());
            saveStep(task.getId(), AgentStepTypeEnum.RETRIEVE_MEMORY, "Retrieve translation memory",
                    "rag_service", null, buildRequestInput(request), memoryOutput, AgentStepStatusEnum.SUCCESS,
                    System.currentTimeMillis() - start, null);
            updateCurrentStep(task.getId(), AgentStepTypeEnum.FUSION.name());
            return ragContext;
        } catch (Exception e) {
            saveStep(task.getId(), AgentStepTypeEnum.RETRIEVE_TERMINOLOGY, "Retrieve terminology and context",
                    "rag_service", null, buildRequestInput(request), Map.of("warning", "RAG retrieval failed"),
                    AgentStepStatusEnum.FAILED, System.currentTimeMillis() - start, e.getMessage());
            saveStep(task.getId(), AgentStepTypeEnum.RETRIEVE_MEMORY, "Retrieve translation memory",
                    "rag_service", null, buildRequestInput(request), Map.of("warning", "RAG retrieval failed"),
                    AgentStepStatusEnum.FAILED, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    private void executeFusionStep(AgentTask task, TranslationRequest request, RagContext ragContext) {
        long start = System.currentTimeMillis();
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("retrievalTriggered", ragContext != null && Boolean.TRUE.equals(ragContext.getRetrievalTriggered()));
        output.put("selectedContextCount", ragContext == null || ragContext.getContextSnippets() == null
                ? 0 : ragContext.getContextSnippets().size());
        output.put("glossaryHitCount", ragContext == null || ragContext.getGlossaryHitCount() == null
                ? 0 : ragContext.getGlossaryHitCount());
        output.put("historyHitCount", ragContext == null || ragContext.getHistoryHitCount() == null
                ? 0 : ragContext.getHistoryHitCount());
        output.put("retrievalReasons", ragContext == null ? List.of() : ragContext.getRetrievalReasons());
        output.put("buildTimeMs", ragContext == null || ragContext.getBuildTimeMs() == null
                ? 0L : ragContext.getBuildTimeMs());

        saveStep(task.getId(), AgentStepTypeEnum.FUSION, "Fuse retrieval results into prompt context",
                "rag_fusion", null, buildRequestInput(request), output, AgentStepStatusEnum.SUCCESS,
                System.currentTimeMillis() - start, null);
        updateCurrentStep(task.getId(), AgentStepTypeEnum.TRANSLATE.name());
    }

    private TranslationResponse executeTranslateStep(AgentTask task, TranslationRequest request, RagContext ragContext) throws Exception {
        long start = System.currentTimeMillis();
        request.setRagContext(buildRagPayload(ragContext));
        TranslationResponse response = translationService.translate(request);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("translationId", response.getTranslationId());
        output.put("status", response.getStatus());
        output.put("translationEngine", response.getTranslationEngine());
        output.put("usedTerminology", response.getUsedTerminology());
        output.put("qualityScore", response.getQualityScore());
        output.put("translatedText", response.getTranslatedText());
        saveStep(task.getId(), AgentStepTypeEnum.TRANSLATE, "Execute translation",
                "translation_service", response.getTranslationEngine(), buildRequestInput(request), output,
                response.isSuccessful() ? AgentStepStatusEnum.SUCCESS : AgentStepStatusEnum.FAILED,
                System.currentTimeMillis() - start, response.getErrorMessage());
        if (!response.isSuccessful()) {
            throw new IllegalStateException("Translation failed: " + response.getErrorMessage());
        }
        updateCurrentStep(task.getId(), AgentStepTypeEnum.QUALITY_CHECK.name());
        return response;
    }

    private QualityStepResult executeQualityStep(AgentTask task,
                                                 TranslationRequest request,
                                                 Long translationId,
                                                 String targetText,
                                                 String phase,
                                                 boolean reviseSupported) throws Exception {
        long start = System.currentTimeMillis();
        QualityAssessmentRequest qualityRequest = QualityAssessmentRequest.builder()
                .translationRecordId(translationId)
                .sourceText(request.getSourceText())
                .targetText(targetText)
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .glossaryMap(extractGlossaryMap(request))
                .save(true)
                .build();
        QualityAssessmentResponse qualityResponse = qualityAssessmentService.assess(qualityRequest);
        List<String> failedRuleTypes = deriveFailedRuleTypes(qualityResponse);
        String reviseDecision = decideReviseDecision(qualityResponse, failedRuleTypes, phase, reviseSupported);
        String reviseSkipReason = resolveReviseSkipReason(qualityResponse, failedRuleTypes, phase, reviseSupported);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("phase", phase);
        output.put("overallScore", qualityResponse.getOverallScore());
        output.put("numberScore", qualityResponse.getNumberScore());
        output.put("terminologyScore", qualityResponse.getTerminologyScore());
        output.put("formatScore", qualityResponse.getFormatScore());
        output.put("qualityLevel", qualityResponse.getQualityLevel());
        output.put("assessmentEngine", qualityResponse.getAssessmentEngine());
        output.put("assessmentTime", qualityResponse.getAssessmentTime());
        output.put("tmEligible", qualityResponse.getTmEligible());
        output.put("tmRejectReasons", qualityResponse.getTmRejectReasons());
        output.put("needsRetry", qualityResponse.getNeedsRetry());
        output.put("needsHumanReview", qualityResponse.getNeedsHumanReview());
        output.put("hardRulePassed", qualityResponse.getHardRulePassed());
        output.put("sensitiveContentDetected", qualityResponse.getSensitiveContentDetected());
        output.put("failedRuleTypes", failedRuleTypes);
        output.put("reviseDecision", reviseDecision);
        output.put("reviseSupported", reviseSupported);
        output.put("reviseSkipReason", reviseSkipReason);
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("translationId", translationId);
        input.put("phase", phase);
        saveStep(task.getId(), AgentStepTypeEnum.QUALITY_CHECK, "Assess translation quality",
                "quality_assessment", qualityResponse.getAssessmentEngine(), input,
                output, AgentStepStatusEnum.SUCCESS, System.currentTimeMillis() - start, null);
        return new QualityStepResult(qualityResponse, failedRuleTypes, reviseDecision);
    }

    private RevisionExecution executeRevisionLoop(AgentTask task,
                                                  TranslationRequest request,
                                                  TranslationResponse translationResponse) throws Exception {
        String draftText = translationResponse.getTranslatedText();
        boolean reviseSupported = supportsAutoRevise(task);
        QualityStepResult initialQuality = executeQualityStep(
                task,
                request,
                translationResponse.getTranslationId(),
                draftText,
                "INITIAL",
                reviseSupported
        );

        if (Boolean.TRUE.equals(initialQuality.response().getSensitiveContentDetected())) {
            return new RevisionExecution(draftText, draftText, initialQuality.response(), false, null, null);
        }

        if (initialQuality.failedRuleTypes().isEmpty()) {
            return new RevisionExecution(draftText, draftText, initialQuality.response(), false, null, null);
        }

        if (!reviseSupported) {
            return new RevisionExecution(draftText, draftText, initialQuality.response(), false,
                    "Selected engine does not support auto revise in v1", "MANUAL_REVIEW_REQUIRED");
        }

        updateCurrentStep(task.getId(), AgentStepTypeEnum.REVISE.name());
        ReviseStepResult reviseStepResult = executeReviseStep(task, request, draftText, initialQuality.failedRuleTypes());
        if (reviseStepResult == null || !Boolean.TRUE.equals(reviseStepResult.revisionApplied())) {
            return new RevisionExecution(draftText, draftText, initialQuality.response(), false,
                    "Auto revise did not produce a usable revision", "MANUAL_REVIEW_REQUIRED");
        }

        updateCurrentStep(task.getId(), AgentStepTypeEnum.QUALITY_CHECK.name());
        QualityStepResult postReviseQuality = executeQualityStep(
                task,
                request,
                translationResponse.getTranslationId(),
                reviseStepResult.revisedText(),
                "POST_REVISE",
                reviseSupported
        );

        if (!postReviseQuality.failedRuleTypes().isEmpty()) {
            return new RevisionExecution(
                    draftText,
                    reviseStepResult.revisedText(),
                    postReviseQuality.response(),
                    true,
                    reviseStepResult.revisionSummary(),
                    "AUTO_REVISE_FAILED"
            );
        }

        return new RevisionExecution(
                draftText,
                reviseStepResult.revisedText(),
                postReviseQuality.response(),
                true,
                reviseStepResult.revisionSummary(),
                null
        );
    }

    private ReviseStepResult executeReviseStep(AgentTask task,
                                               TranslationRequest request,
                                               String draftText,
                                               List<String> failedRuleTypes) {
        long start = System.currentTimeMillis();
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("draftText", draftText);
        input.put("failedRuleTypes", failedRuleTypes);
        input.put("sourceLanguage", request.getSourceLanguage());
        input.put("targetLanguage", request.getTargetLanguage());

        try {
            String userPrompt = buildRevisePrompt(request, draftText, failedRuleTypes);
            String revisedText = qwenTranslationService.complete(
                    "You are a professional translation reviewer. Fix only the requested hard-rule issues and output only the corrected target text.",
                    userPrompt
            );
            if (!StringUtils.hasText(revisedText)) {
                throw new IllegalStateException("Revise step returned empty text");
            }

            String normalized = revisedText.trim();
            String summary = buildRevisionSummary(failedRuleTypes);
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("draftText", draftText);
            output.put("revisedText", normalized);
            output.put("appliedFixTypes", failedRuleTypes);
            output.put("revisionApplied", true);
            output.put("revisionSummary", summary);
            saveStep(task.getId(), AgentStepTypeEnum.REVISE, "Revise hard-rule issues",
                    "auto_revise", task.getSelectedModel(), input, output, AgentStepStatusEnum.SUCCESS,
                    System.currentTimeMillis() - start, null);
            return new ReviseStepResult(normalized, true, summary);
        } catch (Exception e) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("draftText", draftText);
            output.put("revisionApplied", false);
            output.put("appliedFixTypes", failedRuleTypes);
            saveStep(task.getId(), AgentStepTypeEnum.REVISE, "Revise hard-rule issues",
                    "auto_revise", task.getSelectedModel(), input, output, AgentStepStatusEnum.FAILED,
                    System.currentTimeMillis() - start, buildErrorMessage(e));
            log.warn("Revise step failed for task {}: {}", task.getId(), e.getMessage());
            return null;
        }
    }

    @Transactional
    protected void finalizeTaskSuccess(Long taskId,
                                       TranslationResponse translationResponse,
                                       QualityAssessmentResponse qualityResponse,
                                       boolean tmStored) {
        finalizeTaskSuccess(
                taskId,
                translationResponse,
                new RevisionExecution(
                        translationResponse != null ? translationResponse.getTranslatedText() : null,
                        translationResponse != null ? translationResponse.getTranslatedText() : null,
                        qualityResponse,
                        false,
                        null,
                        null
                ),
                tmStored
        );
    }

    @Transactional
    protected void finalizeTaskSuccess(Long taskId,
                                       TranslationResponse translationResponse,
                                       RevisionExecution revisionExecution,
                                       boolean tmStored) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + taskId));
        ReviewTaskCreationResult reviewTaskResult = createReviewTaskIfNeeded(task, translationResponse, revisionExecution);
        ReviewTask reviewTask = reviewTaskResult.reviewTask();
        task.setStatus(AgentTaskStatusEnum.COMPLETED);
        task.setCurrentStep(AgentStepTypeEnum.FINALIZE.name());
        task.setFinalResponse(revisionExecution.finalText());
        task.setSelectedModel(translationResponse.getTranslationEngine());
        task.setFinalQualityScore(revisionExecution.finalQualityResponse() != null ? revisionExecution.finalQualityResponse().getOverallScore() : null);
        task.setNeedHumanReview(reviewTask != null);
        task.setCompletedAt(LocalDateTime.now());
        agentTaskRepository.save(task);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", task.getStatus());
        output.put("finalQualityScore", task.getFinalQualityScore());
        output.put("translationId", translationResponse.getTranslationId());
        output.put("draftResponse", revisionExecution.draftText());
        output.put("finalResponse", revisionExecution.finalText());
        output.put("revisionApplied", revisionExecution.revisionApplied());
        output.put("revisionSummary", revisionExecution.revisionSummary());
        output.put("tmStored", tmStored);
        output.put("tmRejectedReasons", revisionExecution.finalQualityResponse() != null ? revisionExecution.finalQualityResponse().getTmRejectReasons() : List.of());
        output.put("needHumanReview", reviewTask != null);
        output.put("reviewTaskCreated", reviewTaskResult.created());
        output.put("reviewReasonCode", reviewTask != null ? reviewTask.getReasonCode() : null);
        saveStep(taskId, AgentStepTypeEnum.FINALIZE, "Finalize task", null, task.getSelectedModel(),
                Map.of("taskId", taskId), output, AgentStepStatusEnum.SUCCESS, 0L, null);
        agentTaskEventStreamService.publishTaskUpdate(task, reviewTask);
        agentTaskEventStreamService.publishResult(task, reviewTask,
                revisionExecution.draftText(),
                revisionExecution.revisionApplied(),
                revisionExecution.revisionSummary());
        agentTaskEventStreamService.publishDone(taskId, task.getStatus());
        agentTaskEventStreamService.completeTask(taskId);
    }

    private ReviewTaskCreationResult createReviewTaskIfNeeded(AgentTask task,
                                                              TranslationResponse translationResponse,
                                                              RevisionExecution revisionExecution) {
        QualityAssessmentResponse qualityResponse = revisionExecution.finalQualityResponse();
        String overrideReasonCode = revisionExecution.reviewReasonCode();
        if ((qualityResponse == null || !Boolean.TRUE.equals(qualityResponse.getNeedsHumanReview()))
                && !StringUtils.hasText(overrideReasonCode)) {
            return new ReviewTaskCreationResult(null, false);
        }

        ReviewTask existingPending = reviewTaskRepository
                .findFirstByAgentTaskIdAndReviewStatus(task.getId(), ReviewStatusEnum.PENDING)
                .orElse(null);
        if (existingPending != null) {
            return new ReviewTaskCreationResult(existingPending, false);
        }

        String bizType = task.getBizType() != null ? task.getBizType() : "TEXT_TRANSLATION";
        String bizId = translationResponse != null && translationResponse.getTranslationId() != null
                ? String.valueOf(translationResponse.getTranslationId())
                : String.valueOf(task.getId());
        String reasonCode = StringUtils.hasText(overrideReasonCode) ? overrideReasonCode : resolveReviewReasonCode(qualityResponse);
        String issueSummary = buildIssueSummary(qualityResponse, revisionExecution);
        String suggestedText = revisionExecution.finalText();

        ReviewTask reviewTask = reviewTaskService.createPendingReviewTask(
                task.getId(),
                bizType,
                bizId,
                reasonCode,
                issueSummary,
                suggestedText
        );

        translationRecordRepository.findFirstByAgentTaskIdOrderByCreatedAtDesc(task.getId())
                .ifPresent(record -> {
                    record.setReviewStatus(ReviewStatusEnum.PENDING.name());
                    translationRecordRepository.save(record);
                });
        return new ReviewTaskCreationResult(reviewTask, true);
    }

    private boolean saveTranslationMemory(AgentTask task,
                                          TranslationRequest request,
                                          String finalText,
                                          QualityAssessmentResponse qualityResponse) {
        try {
            return translationMemoryService.saveApprovedPair(
                    request.getSourceText(),
                    finalText,
                    request.getSourceLanguage(),
                    request.getTargetLanguage(),
                    request.getDomain(),
                    qualityResponse == null ? null : qualityResponse.getOverallScore(),
                    qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getHardRulePassed()),
                    qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getSensitiveContentDetected()),
                    qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getTmEligible()),
                    Boolean.FALSE,
                    task.getId(),
                    task.getUserId()
            ).isPresent();
        } catch (Exception e) {
            log.warn("Save translation memory failed for task {}: {}", task.getId(), e.getMessage());
            return false;
        }
    }

    @Transactional
    protected void finalizeTaskFailure(Long taskId, Exception e) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + taskId));
        String errorMessage = buildErrorMessage(e);
        task.setStatus(AgentTaskStatusEnum.FAILED);
        task.setCurrentStep(AgentStepTypeEnum.FINALIZE.name());
        task.setErrorMessage(errorMessage);
        task.setCompletedAt(LocalDateTime.now());
        agentTaskRepository.save(task);

        saveStep(taskId, AgentStepTypeEnum.FINALIZE, "Finalize task", null, null,
                Map.of("taskId", taskId), Map.of("status", "FAILED"),
                AgentStepStatusEnum.FAILED, 0L, errorMessage);
        agentTaskEventStreamService.publishTaskUpdate(task, null);
        agentTaskEventStreamService.publishDone(taskId, task.getStatus());
        agentTaskEventStreamService.completeTask(taskId);
    }

    @Transactional
    protected void updateCurrentStep(Long taskId, String currentStep) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + taskId));
        task.setCurrentStep(currentStep);
        AgentTask savedTask = agentTaskRepository.save(task);
        agentTaskEventStreamService.publishTaskUpdate(savedTask, null);
    }

    @Transactional
    protected void saveStep(Long taskId,
                            AgentStepTypeEnum stepType,
                            String stepName,
                            String toolName,
                            String modelName,
                            Object input,
                            Object output,
                            AgentStepStatusEnum status,
                            Long durationMs,
                            String errorMessage) {
        AgentTaskStep step = AgentTaskStep.builder()
                .taskId(taskId)
                .stepNo(nextStepNo(taskId))
                .stepType(stepType)
                .stepName(stepName)
                .toolName(toolName)
                .modelName(modelName)
                .inputJson(toJson(input))
                .outputJson(toJson(output))
                .status(status)
                .durationMs(durationMs)
                .errorMessage(errorMessage)
                .build();
        AgentTaskStep savedStep = agentTaskStepRepository.save(step);
        agentTaskEventStreamService.publishStepUpdate(savedStep);
    }

    private int nextStepNo(Long taskId) {
        return Math.toIntExact(agentTaskStepRepository.countByTaskId(taskId) + 1);
    }

    private Map<String, Object> buildRequestInput(TranslationRequest request) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("sourceLanguage", request.getSourceLanguage());
        input.put("targetLanguage", request.getTargetLanguage());
        input.put("domain", request.getDomain());
        input.put("textLength", request.getCharacterCount());
        input.put("useRag", request.getUseRag());
        input.put("useTerminology", request.getUseTerminology());
        return input;
    }

    private Map<String, Object> buildRagPayload(RagContext ragContext) {
        if (ragContext == null) {
            return Map.of();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contextSnippets", ragContext.getContextSnippets());
        payload.put("glossaryMap", ragContext.getGlossaryMap());
        payload.put("historySnippets", ragContext.getHistorySnippets());
        payload.put("keywords", ragContext.getKeywords());
        payload.put("topK", ragContext.getTopK());
        payload.put("buildTimeMs", ragContext.getBuildTimeMs());
        payload.put("preprocessedSourceText", ragContext.getPreprocessedSourceText());
        payload.put("retrievalTriggered", ragContext.getRetrievalTriggered());
        payload.put("terminologyRetrievalTriggered", ragContext.getTerminologyRetrievalTriggered());
        payload.put("historyRetrievalTriggered", ragContext.getHistoryRetrievalTriggered());
        payload.put("retrievalReasons", ragContext.getRetrievalReasons());
        payload.put("glossaryHitCount", ragContext.getGlossaryHitCount());
        payload.put("historyHitCount", ragContext.getHistoryHitCount());
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> extractGlossaryMap(TranslationRequest request) {
        if (request.getRagContext() == null) {
            return Map.of();
        }
        Object glossary = request.getRagContext().get("glossaryMap");
        if (!(glossary instanceof Map<?, ?> glossaryMap)) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        glossaryMap.forEach((key, value) -> {
            if (key != null && value != null) {
                result.put(String.valueOf(key), String.valueOf(value));
            }
        });
        return result;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String buildErrorMessage(Exception e) {
        if (e == null) {
            return "Unknown workflow error";
        }
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            return e.getMessage();
        }
        return e.getClass().getSimpleName();
    }

    private String resolveReviewReasonCode(QualityAssessmentResponse qualityResponse) {
        if (qualityResponse == null) {
            return "MANUAL_REVIEW_REQUIRED";
        }
        if (Boolean.TRUE.equals(qualityResponse.getSensitiveContentDetected())) {
            return "SENSITIVE_CONTENT";
        }
        if (qualityResponse.getTmRejectReasons() != null && !qualityResponse.getTmRejectReasons().isEmpty()) {
            return qualityResponse.getTmRejectReasons().get(0);
        }
        return "MANUAL_REVIEW_REQUIRED";
    }

    private String buildIssueSummary(QualityAssessmentResponse qualityResponse, RevisionExecution revisionExecution) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("overallScore", qualityResponse != null ? qualityResponse.getOverallScore() : null);
        summary.put("qualityLevel", qualityResponse != null ? qualityResponse.getQualityLevel() : null);
        summary.put("tmRejectReasons", qualityResponse != null ? qualityResponse.getTmRejectReasons() : List.of());
        summary.put("sensitiveContentDetected", qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getSensitiveContentDetected()));
        summary.put("revisionApplied", revisionExecution != null && Boolean.TRUE.equals(revisionExecution.revisionApplied()));
        summary.put("revisionSummary", revisionExecution != null ? revisionExecution.revisionSummary() : null);
        summary.put("draftResponse", revisionExecution != null ? revisionExecution.draftText() : null);
        summary.put("finalResponse", revisionExecution != null ? revisionExecution.finalText() : null);
        return toJson(summary);
    }

    private List<String> deriveFailedRuleTypes(QualityAssessmentResponse qualityResponse) {
        List<String> failedRuleTypes = new ArrayList<>();
        if (qualityResponse == null) {
            return failedRuleTypes;
        }
        if (qualityResponse.getNumberScore() != null && qualityResponse.getNumberScore() < 100) {
            failedRuleTypes.add("NUMBER");
        }
        if (qualityResponse.getTerminologyScore() != null && qualityResponse.getTerminologyScore() < 100) {
            failedRuleTypes.add("TERMINOLOGY");
        }
        if (qualityResponse.getFormatScore() != null && qualityResponse.getFormatScore() < 100) {
            failedRuleTypes.add("FORMAT");
        }
        return failedRuleTypes;
    }

    private String decideReviseDecision(QualityAssessmentResponse qualityResponse,
                                        List<String> failedRuleTypes,
                                        String phase,
                                        boolean reviseSupported) {
        if (qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getSensitiveContentDetected())) {
            return "HUMAN_REVIEW";
        }
        if ("POST_REVISE".equalsIgnoreCase(phase) && !failedRuleTypes.isEmpty()) {
            return "HUMAN_REVIEW";
        }
        if (!failedRuleTypes.isEmpty()) {
            return reviseSupported ? "REVISE" : "HUMAN_REVIEW";
        }
        return "FINALIZE";
    }

    private String resolveReviseSkipReason(QualityAssessmentResponse qualityResponse,
                                           List<String> failedRuleTypes,
                                           String phase,
                                           boolean reviseSupported) {
        if (qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getSensitiveContentDetected())) {
            return "SENSITIVE_CONTENT";
        }
        if ("POST_REVISE".equalsIgnoreCase(phase)) {
            return failedRuleTypes.isEmpty() ? "POST_REVISE_PASSED" : "POST_REVISE_FAILED";
        }
        if (failedRuleTypes == null || failedRuleTypes.isEmpty()) {
            return "QUALITY_PASSED";
        }
        if (!reviseSupported) {
            return "ENGINE_UNSUPPORTED";
        }
        return "REVISE_TRIGGERED";
    }

    private boolean supportsAutoRevise(AgentTask task) {
        return task != null && "QWEN".equalsIgnoreCase(task.getSelectedModel());
    }

    private String buildRevisePrompt(TranslationRequest request, String draftText, List<String> failedRuleTypes) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("[Source Text]\n").append(request.getSourceText()).append("\n\n");
        prompt.append("[Draft Translation]\n").append(draftText).append("\n\n");
        prompt.append("[Target Language]\n").append(request.getTargetLanguage()).append("\n\n");
        prompt.append("[Fix Types]\n").append(String.join(", ", failedRuleTypes)).append("\n");
        prompt.append("- NUMBER: fix numbers and units only.\n");
        prompt.append("- TERMINOLOGY: align with glossary terminology if present.\n");
        prompt.append("- FORMAT: preserve punctuation, spacing and formatting.\n\n");
        prompt.append("[Requirements]\n");
        prompt.append("1. Keep the original meaning unchanged.\n");
        prompt.append("2. Fix only the listed hard-rule issues.\n");
        prompt.append("3. Output only the corrected target text.\n");
        return prompt.toString();
    }

    private String buildRevisionSummary(List<String> failedRuleTypes) {
        if (failedRuleTypes == null || failedRuleTypes.isEmpty()) {
            return "No hard-rule revision applied";
        }
        return "Auto revised: " + String.join(", ", failedRuleTypes);
    }

    private void syncLatestTranslationRecord(Long taskId, String finalText) {
        if (!StringUtils.hasText(finalText)) {
            return;
        }
        translationRecordRepository.findFirstByAgentTaskIdOrderByCreatedAtDesc(taskId)
                .ifPresent(record -> {
                    record.setTranslatedText(finalText);
                    translationRecordRepository.save(record);
                });
    }

    private record QualityStepResult(QualityAssessmentResponse response,
                                     List<String> failedRuleTypes,
                                     String reviseDecision) {
    }

    private record ReviseStepResult(String revisedText,
                                    Boolean revisionApplied,
                                    String revisionSummary) {
    }

    private record RevisionExecution(String draftText,
                                     String finalText,
                                     QualityAssessmentResponse finalQualityResponse,
                                     Boolean revisionApplied,
                                     String revisionSummary,
                                     String reviewReasonCode) {
    }

    private record ReviewTaskCreationResult(ReviewTask reviewTask, boolean created) {
    }
}
