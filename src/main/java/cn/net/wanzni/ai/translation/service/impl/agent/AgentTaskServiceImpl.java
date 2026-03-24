package cn.net.wanzni.ai.translation.service.impl.agent;

import cn.net.wanzni.ai.translation.dto.agent.AgentTaskCreateRequest;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskStepResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskTimelineResponse;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.AgentTaskStep;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.enums.AgentStepStatusEnum;
import cn.net.wanzni.ai.translation.enums.AgentStepTypeEnum;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.AgentTaskTypeEnum;
import cn.net.wanzni.ai.translation.exception.ResourceNotFoundException;
import cn.net.wanzni.ai.translation.repository.AgentTaskRepository;
import cn.net.wanzni.ai.translation.repository.AgentTaskStepRepository;
import cn.net.wanzni.ai.translation.repository.ReviewTaskRepository;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.agent.AgentTaskService;
import cn.net.wanzni.ai.translation.service.agent.AgentWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgentTaskServiceImpl implements AgentTaskService {

    private final AgentTaskRepository agentTaskRepository;
    private final AgentTaskStepRepository agentTaskStepRepository;
    private final ReviewTaskRepository reviewTaskRepository;
    private final AgentWorkflowService agentWorkflowService;

    @Override
    @Transactional
    public AgentTaskResponse createTextTask(AgentTaskCreateRequest request) {
        Long userId = request.getUserId() != null ? request.getUserId() : UserContext.getUserId();

        AgentTask task = AgentTask.builder()
                .taskNo(generateTaskNo())
                .userId(userId)
                .taskType(AgentTaskTypeEnum.TEXT)
                .bizType(StringUtils.hasText(request.getBizType()) ? request.getBizType() : "TEXT_TRANSLATION")
                .bizId(request.getBizId())
                .sourceLanguage(request.getSourceLanguage())
                .targetLanguage(request.getTargetLanguage())
                .domain(request.getDomain())
                .selectedModel(normalizeTranslationEngine(request.getTranslationEngine()))
                .sourceText(request.getSourceText())
                .status(AgentTaskStatusEnum.PENDING)
                .currentStep(AgentStepTypeEnum.CREATED.name())
                .retryCount(0)
                .needHumanReview(Boolean.FALSE)
                .traceId(generateTraceId())
                .build();
        AgentTask savedTask = agentTaskRepository.save(task);

        AgentTaskStep createdStep = AgentTaskStep.builder()
                .taskId(savedTask.getId())
                .stepNo(1)
                .stepType(AgentStepTypeEnum.CREATED)
                .stepName("Task created")
                .status(AgentStepStatusEnum.SUCCESS)
                .inputJson(buildCreateInputJson(request, userId))
                .outputJson("{\"status\":\"PENDING\"}")
                .durationMs(0L)
                .build();
        agentTaskStepRepository.save(createdStep);

        agentWorkflowService.submitTextTask(savedTask.getId());
        return toResponse(savedTask);
    }

    @Override
    @Transactional(readOnly = true)
    public AgentTaskResponse getTask(Long taskId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + taskId));
        return toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentTaskStepResponse> getTaskSteps(Long taskId) {
        if (!agentTaskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Agent task not found: " + taskId);
        }
        return agentTaskStepRepository.findByTaskIdOrderByStepNoAsc(taskId)
                .stream()
                .map(this::toStepResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AgentTaskTimelineResponse getTaskTimeline(Long taskId) {
        AgentTask task = agentTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent task not found: " + taskId));
        List<AgentTaskStepResponse> steps = agentTaskStepRepository.findByTaskIdOrderByStepNoAsc(taskId)
                .stream()
                .map(this::toStepResponse)
                .toList();
        ReviewTask reviewTask = reviewTaskRepository.findFirstByAgentTaskIdOrderByCreatedAtDesc(taskId).orElse(null);
        return AgentTaskTimelineResponse.builder()
                .taskId(task.getId())
                .taskNo(task.getTaskNo())
                .traceId(task.getTraceId())
                .taskStatus(task.getStatus())
                .currentStep(task.getCurrentStep())
                .needHumanReview(task.getNeedHumanReview())
                .reviewTaskId(reviewTask != null ? reviewTask.getId() : null)
                .reviewStatus(reviewTask != null ? reviewTask.getReviewStatus() : null)
                .reviewReasonCode(reviewTask != null ? reviewTask.getReasonCode() : null)
                .steps(steps)
                .build();
    }

    private AgentTaskResponse toResponse(AgentTask task) {
        return AgentTaskResponse.builder()
                .id(task.getId())
                .taskNo(task.getTaskNo())
                .userId(task.getUserId())
                .taskType(task.getTaskType())
                .bizType(task.getBizType())
                .bizId(task.getBizId())
                .sourceLanguage(task.getSourceLanguage())
                .targetLanguage(task.getTargetLanguage())
                .domain(task.getDomain())
                .sourceText(task.getSourceText())
                .status(task.getStatus())
                .currentStep(task.getCurrentStep())
                .selectedModel(task.getSelectedModel())
                .retryCount(task.getRetryCount())
                .needHumanReview(task.getNeedHumanReview())
                .finalQualityScore(task.getFinalQualityScore())
                .finalResponse(task.getFinalResponse())
                .traceId(task.getTraceId())
                .errorMessage(task.getErrorMessage())
                .startedAt(task.getStartedAt())
                .completedAt(task.getCompletedAt())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private AgentTaskStepResponse toStepResponse(AgentTaskStep step) {
        return AgentTaskStepResponse.builder()
                .id(step.getId())
                .taskId(step.getTaskId())
                .stepNo(step.getStepNo())
                .stepType(step.getStepType())
                .stepName(step.getStepName())
                .toolName(step.getToolName())
                .modelName(step.getModelName())
                .inputJson(step.getInputJson())
                .outputJson(step.getOutputJson())
                .status(step.getStatus())
                .durationMs(step.getDurationMs())
                .errorMessage(step.getErrorMessage())
                .createdAt(step.getCreatedAt())
                .build();
    }

    private String generateTaskNo() {
        return "AT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private String generateTraceId() {
        return "trace-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildCreateInputJson(AgentTaskCreateRequest request, Long userId) {
        String sourceText = request.getSourceText() == null ? "" : request.getSourceText();
        String escaped = sourceText.replace("\\", "\\\\").replace("\"", "\\\"");
        return "{\"userId\":"
                + (userId == null ? "null" : userId)
                + ",\"sourceLanguage\":\"" + request.getSourceLanguage() + "\""
                + ",\"targetLanguage\":\"" + request.getTargetLanguage() + "\""
                + ",\"domain\":" + (request.getDomain() == null ? "null" : "\"" + request.getDomain() + "\"")
                + ",\"translationEngine\":"
                + (request.getTranslationEngine() == null ? "null" : "\"" + normalizeTranslationEngine(request.getTranslationEngine()) + "\"")
                + ",\"sourceText\":\"" + escaped + "\"}";
    }

    private String normalizeTranslationEngine(String engine) {
        if (!StringUtils.hasText(engine)) {
            return null;
        }
        return switch (engine.trim().toUpperCase()) {
            case "ALIYUN" -> "ALIBABA_CLOUD";
            case "GOOGLE", "MICROSOFT", "DEEPL", "TENCENT", "BAIDU", "QWEN", "ALIBABA_CLOUD" -> engine.trim().toUpperCase();
            default -> engine.trim().toUpperCase();
        };
    }
}
