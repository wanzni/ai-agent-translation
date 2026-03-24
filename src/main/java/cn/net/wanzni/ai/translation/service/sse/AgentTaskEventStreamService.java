package cn.net.wanzni.ai.translation.service.sse;

import cn.net.wanzni.ai.translation.dto.agent.AgentTaskConnectedEvent;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskDoneEvent;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskResultEvent;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskStatusEvent;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskStepResponse;
import cn.net.wanzni.ai.translation.dto.agent.AgentTaskTimelineResponse;
import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.entity.AgentTaskStep;
import cn.net.wanzni.ai.translation.entity.ReviewTask;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTaskEventStreamService {

    private final ObjectMapper objectMapper;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> taskEmitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long taskId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        taskEmitters.computeIfAbsent(taskId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> unregister(taskId, emitter));
        emitter.onTimeout(() -> unregister(taskId, emitter));
        emitter.onError(ex -> unregister(taskId, emitter));
        return emitter;
    }

    public void unregister(Long taskId, SseEmitter emitter) {
        List<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters == null) {
            return;
        }
        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            taskEmitters.remove(taskId);
        }
    }

    public void sendConnected(SseEmitter emitter, AgentTaskResponse task) {
        sendToEmitter(emitter, "connected", AgentTaskConnectedEvent.builder()
                .taskId(task.getId())
                .taskNo(task.getTaskNo())
                .traceId(task.getTraceId())
                .build());
    }

    public void sendSnapshot(SseEmitter emitter, AgentTaskTimelineResponse timeline) {
        sendToEmitter(emitter, "snapshot", timeline);
    }

    public void sendResult(SseEmitter emitter, AgentTaskResponse task, AgentTaskTimelineResponse timeline) {
        sendToEmitter(emitter, "result", buildResultEvent(task, timeline));
    }

    public void sendDone(SseEmitter emitter, Long taskId, AgentTaskStatusEnum status) {
        sendToEmitter(emitter, "done", AgentTaskDoneEvent.builder()
                .taskId(taskId)
                .terminalStatus(status)
                .build());
    }

    public void publishTaskUpdate(AgentTask task, ReviewTask reviewTask) {
        broadcast(task.getId(), "task", buildStatusEvent(task, reviewTask));
    }

    public void publishStepUpdate(AgentTaskStep step) {
        broadcast(step.getTaskId(), "step", AgentTaskStepResponse.builder()
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
                .build());
    }

    public void publishResult(AgentTask task, ReviewTask reviewTask) {
        broadcast(task.getId(), "result", AgentTaskResultEvent.builder()
                .taskId(task.getId())
                .finalResponse(task.getFinalResponse())
                .finalQualityScore(task.getFinalQualityScore())
                .needHumanReview(task.getNeedHumanReview())
                .reviewTaskId(reviewTask != null ? reviewTask.getId() : null)
                .reviewStatus(reviewTask != null ? reviewTask.getReviewStatus() : null)
                .reviewReasonCode(reviewTask != null ? reviewTask.getReasonCode() : null)
                .selectedModel(task.getSelectedModel())
                .build());
    }

    public void publishDone(Long taskId, AgentTaskStatusEnum status) {
        broadcast(taskId, "done", AgentTaskDoneEvent.builder()
                .taskId(taskId)
                .terminalStatus(status)
                .build());
    }

    public void completeTask(Long taskId) {
        List<SseEmitter> emitters = taskEmitters.remove(taskId);
        if (emitters == null) {
            return;
        }
        emitters.forEach(emitter -> {
            try {
                emitter.complete();
            } catch (Exception ignore) {
                // ignore emitter close failures during cleanup
            }
        });
    }

    private void broadcast(Long taskId, String eventName, Object payload) {
        List<SseEmitter> emitters = taskEmitters.get(taskId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sendToEmitter(emitter, eventName, payload);
        }
    }

    private void sendToEmitter(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(toJson(payload)));
        } catch (IOException e) {
            log.warn("Failed to send task SSE event[name={}]: {}", eventName, e.getMessage());
            try {
                emitter.completeWithError(e);
            } catch (Exception ignore) {
                // ignore emitter cleanup failures
            }
        }
    }

    private AgentTaskStatusEvent buildStatusEvent(AgentTask task, ReviewTask reviewTask) {
        return AgentTaskStatusEvent.builder()
                .taskId(task.getId())
                .taskStatus(task.getStatus())
                .currentStep(task.getCurrentStep())
                .needHumanReview(task.getNeedHumanReview())
                .reviewTaskId(reviewTask != null ? reviewTask.getId() : null)
                .reviewStatus(reviewTask != null ? reviewTask.getReviewStatus() : null)
                .reviewReasonCode(reviewTask != null ? reviewTask.getReasonCode() : null)
                .finalQualityScore(task.getFinalQualityScore())
                .errorMessage(task.getErrorMessage())
                .selectedModel(task.getSelectedModel())
                .build();
    }

    private AgentTaskResultEvent buildResultEvent(AgentTaskResponse task, AgentTaskTimelineResponse timeline) {
        return AgentTaskResultEvent.builder()
                .taskId(task.getId())
                .finalResponse(task.getFinalResponse())
                .finalQualityScore(task.getFinalQualityScore())
                .needHumanReview(task.getNeedHumanReview())
                .reviewTaskId(timeline.getReviewTaskId())
                .reviewStatus(timeline.getReviewStatus())
                .reviewReasonCode(timeline.getReviewReasonCode())
                .selectedModel(task.getSelectedModel())
                .build();
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize task SSE payload: {}", e.getMessage());
            return "{}";
        }
    }
}
