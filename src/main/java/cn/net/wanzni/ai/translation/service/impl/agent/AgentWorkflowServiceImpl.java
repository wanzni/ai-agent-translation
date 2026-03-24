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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
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
            TranslationResponse translationResponse = executeTranslateStep(task, translationRequest, ragContext);
            QualityAssessmentResponse qualityResponse = executeQualityStep(task, translationRequest, translationResponse);
            boolean tmStored = saveTranslationMemory(task, translationRequest, translationResponse, qualityResponse);
            finalizeTaskSuccess(task.getId(), translationResponse, qualityResponse, tmStored);
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
        output.put("plan", List.of("PLAN", "RETRIEVE_TERMINOLOGY", "TRANSLATE", "QUALITY_CHECK", "FINALIZE"));
        output.put("useRag", request.getUseRag());
        output.put("useTerminology", request.getUseTerminology());
        saveStep(task.getId(), 2, AgentStepTypeEnum.PLAN, "Plan task", null, null, input, output, AgentStepStatusEnum.SUCCESS, 0L, null);
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
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("keywordCount", ragContext.getKeywords() == null ? 0 : ragContext.getKeywords().size());
            output.put("glossaryHitCount", ragContext.getGlossaryMap() == null ? 0 : ragContext.getGlossaryMap().size());
            output.put("historyHitCount", ragContext.getHistorySnippets() == null ? 0 : ragContext.getHistorySnippets().size());
            output.put("tmHitCount", tmHistoryCount);
            output.put("historyFallbackCount", fallbackHistoryCount);
            output.put("buildTimeMs", ragContext.getBuildTimeMs());
            saveStep(task.getId(), 3, AgentStepTypeEnum.RETRIEVE_TERMINOLOGY, "Retrieve terminology and context",
                    "rag_service", null, buildRequestInput(request), output, AgentStepStatusEnum.SUCCESS,
                    System.currentTimeMillis() - start, null);
            updateCurrentStep(task.getId(), AgentStepTypeEnum.TRANSLATE.name());
            return ragContext;
        } catch (Exception e) {
            saveStep(task.getId(), 3, AgentStepTypeEnum.RETRIEVE_TERMINOLOGY, "Retrieve terminology and context",
                    "rag_service", null, buildRequestInput(request), Map.of("warning", "RAG retrieval failed"),
                    AgentStepStatusEnum.FAILED, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
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
        saveStep(task.getId(), 4, AgentStepTypeEnum.TRANSLATE, "Execute translation",
                "translation_service", response.getTranslationEngine(), buildRequestInput(request), output,
                response.isSuccessful() ? AgentStepStatusEnum.SUCCESS : AgentStepStatusEnum.FAILED,
                System.currentTimeMillis() - start, response.getErrorMessage());
        if (!response.isSuccessful()) {
            throw new IllegalStateException("Translation failed: " + response.getErrorMessage());
        }
        updateCurrentStep(task.getId(), AgentStepTypeEnum.QUALITY_CHECK.name());
        return response;
    }

    private QualityAssessmentResponse executeQualityStep(AgentTask task, TranslationRequest request, TranslationResponse translationResponse) throws Exception {
        long start = System.currentTimeMillis();
        QualityAssessmentRequest qualityRequest = QualityAssessmentRequest.builder()
                .translationRecordId(translationResponse.getTranslationId())
                .sourceText(request.getSourceText())
                .targetText(translationResponse.getTranslatedText())
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .glossaryMap(extractGlossaryMap(request))
                .save(true)
                .build();
        QualityAssessmentResponse qualityResponse = qualityAssessmentService.assess(qualityRequest);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("overallScore", qualityResponse.getOverallScore());
        output.put("numberScore", qualityResponse.getNumberScore());
        output.put("terminologyScore", qualityResponse.getTerminologyScore());
        output.put("qualityLevel", qualityResponse.getQualityLevel());
        output.put("assessmentEngine", qualityResponse.getAssessmentEngine());
        output.put("assessmentTime", qualityResponse.getAssessmentTime());
        output.put("tmEligible", qualityResponse.getTmEligible());
        output.put("tmRejectReasons", qualityResponse.getTmRejectReasons());
        output.put("hardRulePassed", qualityResponse.getHardRulePassed());
        output.put("sensitiveContentDetected", qualityResponse.getSensitiveContentDetected());
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("translationId", translationResponse.getTranslationId());
        saveStep(task.getId(), 5, AgentStepTypeEnum.QUALITY_CHECK, "Assess translation quality",
                "quality_assessment", qualityResponse.getAssessmentEngine(), input,
                output, AgentStepStatusEnum.SUCCESS, System.currentTimeMillis() - start, null);
        updateCurrentStep(task.getId(), AgentStepTypeEnum.FINALIZE.name());
        return qualityResponse;
    }

    @Transactional
    protected void finalizeTaskSuccess(Long taskId,
                                       TranslationResponse translationResponse,
                                       QualityAssessmentResponse qualityResponse,
                                       boolean tmStored) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + taskId));
        ReviewTaskCreationResult reviewTaskResult = createReviewTaskIfNeeded(task, translationResponse, qualityResponse);
        ReviewTask reviewTask = reviewTaskResult.reviewTask();
        task.setStatus(AgentTaskStatusEnum.COMPLETED);
        task.setCurrentStep(AgentStepTypeEnum.FINALIZE.name());
        task.setFinalResponse(translationResponse.getTranslatedText());
        task.setSelectedModel(translationResponse.getTranslationEngine());
        task.setFinalQualityScore(qualityResponse != null ? qualityResponse.getOverallScore() : null);
        task.setNeedHumanReview(reviewTask != null);
        task.setCompletedAt(LocalDateTime.now());
        agentTaskRepository.save(task);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", task.getStatus());
        output.put("finalQualityScore", task.getFinalQualityScore());
        output.put("translationId", translationResponse.getTranslationId());
        output.put("tmStored", tmStored);
        output.put("tmRejectedReasons", qualityResponse != null ? qualityResponse.getTmRejectReasons() : List.of());
        output.put("needHumanReview", qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getNeedsHumanReview()));
        output.put("reviewTaskCreated", reviewTaskResult.created());
        output.put("reviewReasonCode", reviewTask != null ? reviewTask.getReasonCode() : null);
        saveStep(taskId, 6, AgentStepTypeEnum.FINALIZE, "Finalize task", null, task.getSelectedModel(),
                Map.of("taskId", taskId), output, AgentStepStatusEnum.SUCCESS, 0L, null);
        agentTaskEventStreamService.publishTaskUpdate(task, reviewTask);
        agentTaskEventStreamService.publishResult(task, reviewTask);
        agentTaskEventStreamService.publishDone(taskId, task.getStatus());
        agentTaskEventStreamService.completeTask(taskId);
    }

    private ReviewTaskCreationResult createReviewTaskIfNeeded(AgentTask task,
                                                              TranslationResponse translationResponse,
                                                              QualityAssessmentResponse qualityResponse) {
        if (qualityResponse == null || !Boolean.TRUE.equals(qualityResponse.getNeedsHumanReview())) {
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
        String reasonCode = resolveReviewReasonCode(qualityResponse);
        String issueSummary = buildIssueSummary(qualityResponse);
        String suggestedText = translationResponse != null ? translationResponse.getTranslatedText() : null;

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
                                          TranslationResponse translationResponse,
                                          QualityAssessmentResponse qualityResponse) {
        try {
            return translationMemoryService.saveApprovedPair(
                    request.getSourceText(),
                    translationResponse.getTranslatedText(),
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

        saveStep(taskId, 6, AgentStepTypeEnum.FINALIZE, "Finalize task", null, null,
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
                            int stepNo,
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
                .stepNo(stepNo)
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

    private String buildIssueSummary(QualityAssessmentResponse qualityResponse) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("overallScore", qualityResponse != null ? qualityResponse.getOverallScore() : null);
        summary.put("qualityLevel", qualityResponse != null ? qualityResponse.getQualityLevel() : null);
        summary.put("tmRejectReasons", qualityResponse != null ? qualityResponse.getTmRejectReasons() : List.of());
        summary.put("sensitiveContentDetected", qualityResponse != null && Boolean.TRUE.equals(qualityResponse.getSensitiveContentDetected()));
        return toJson(summary);
    }

    private record ReviewTaskCreationResult(ReviewTask reviewTask, boolean created) {
    }
}
