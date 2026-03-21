package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.TranslationRequest;
import cn.net.wanzni.ai.translation.dto.chat.ChatMessageResponse;
import cn.net.wanzni.ai.translation.dto.chat.ChatSessionResponse;
import cn.net.wanzni.ai.translation.dto.chat.CreateChatSessionRequest;
import cn.net.wanzni.ai.translation.dto.chat.SendChatMessageRequest;
import cn.net.wanzni.ai.translation.dto.chat.UpdateChatSessionRequest;
import cn.net.wanzni.ai.translation.enums.SessionStatusEnum;
import cn.net.wanzni.ai.translation.service.ChatTranslationService;
import cn.net.wanzni.ai.translation.service.llm.QwenChatService;
import cn.net.wanzni.ai.translation.service.sse.SseStreamService;
import cn.net.wanzni.ai.translation.service.translation.QwenTranslationService;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.entity.User;
import cn.net.wanzni.ai.translation.enums.MessageTypeEnum;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import cn.net.wanzni.ai.translation.service.sse.SessionSseHub;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 聊天翻译 API 控制器
 *
 * <p>该控制器负责处理与聊天翻译相关的所有 API 请求，
 * 包括创建、获取、更新和结束聊天会话，以及发送和接收聊天消息。
 * 它还提供了通过 Server-Sent Events (SSE) 实现的流式翻译功能，
 * 以支持实时对话翻译和自动客服回复。
 *
 * <p>主要功能包括：
 * <ul>
 *     <li>管理聊天会话的生命周期</li>
 *     <li>发送和获取聊天消息</li>
 *     <li>通过 SSE 提供流式翻译</li>
 *     <li>通过 SSE 提供自动客服回复</li>
 * </ul>
 *
 * @version 1.0.0
 * @since 2024-07-28
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatTranslationController {

    private final ChatTranslationService chatTranslationService;
    private final QwenChatService qwenChatService;
    private final QwenTranslationService qwenTranslationService;
    private final SessionSseHub sessionSseHub;
    private final SseStreamService sseStreamService;
    private final java.util.concurrent.Executor chatSseExecutor;

    /**
     * 创建一个新的聊天会话
     *
     * @param request 包含会话初始信息的请求体
     * @return 创建成功的会话信息
     */
    @PostMapping("/sessions")
    public ChatSessionResponse createSession(@RequestBody CreateChatSessionRequest request) {
        return chatTranslationService.createChatSession(request);
    }

    /**
     * 根据会话 ID 获取会话详情
     *
     * @param sessionId 会话的唯一标识符
     * @return 对应的会话信息
     */
    @GetMapping("/sessions/{sessionId}")
    public ChatSessionResponse getSession(@PathVariable("sessionId") String sessionId) {
        return chatTranslationService.getChatSession(sessionId);
    }

    /**
     * 更新指定会话的信息
     *
     * @param sessionId 要更新的会话的唯一标识符
     * @param request   包含更新后信息的请求体
     * @return 更新后的会话信息
     */
    @PutMapping("/sessions/{sessionId}")
    public ChatSessionResponse updateSession(@PathVariable("sessionId") String sessionId,
                                             @RequestBody UpdateChatSessionRequest request) {
        return chatTranslationService.updateChatSession(sessionId, request);
    }

    /**
     * 结束指定的聊天会话
     *
     * @param sessionId 要结束的会话的唯一标识符
     */
    @PostMapping("/sessions/{sessionId}/end")
    public void endSession(@PathVariable("sessionId") String sessionId) {
        chatTranslationService.endChatSession(sessionId);
    }

    /**
     * 根据状态获取会话列表
     *
     * @param status 会话状态
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public List<ChatSessionResponse> getSessionsByStatus(@RequestParam(value = "status", required = false) String status) {
        SessionStatusEnum statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = SessionStatusEnum.valueOf(status.toUpperCase());
        }
        String userId = String.valueOf(UserContext.getUserId());
        return chatTranslationService.getChatSessions(userId, statusEnum);
    }

    /**
     * 分页获取指定会话的消息列表
     *
     * @param sessionId 会话的唯一标识符
     * @param page      分页查询的页码（从 0 开始）
     * @param size      每页的消息数量
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessageResponse> listMessages(@PathVariable("sessionId") String sessionId,
                                                  @RequestParam(value = "page", defaultValue = "0") int page,
                                                  @RequestParam(value = "size", defaultValue = "20") int size) {
        return chatTranslationService.getChatMessages(sessionId, page, size);
    }

    /**
     * 发送一条聊天消息（同步返回）
     *
     * <p>此方法用于发送一条聊天消息，并同步返回包含翻译结果的完整消息。
     * 如果请求中未提供用户信息，则会尝试从当前用户上下文中获取。
     *
     * @param request 包含消息内容的请求体
     * @return 包含翻译结果的聊天消息
     */
    @Operation(summary = "发送消息")
    @PostMapping("/messages")
    public ChatMessageResponse sendMessage(@Valid @RequestBody SendChatMessageRequest request) {
        // 补齐用户信息（若未提供）
        if (request.getUserId() == null) {
            Long ctxUserId = UserContext.getUserId();
            if (ctxUserId != null) {
                request.setUserId(ctxUserId);
            }
        }
        if (request.getUserName() == null || request.getUserName().isBlank()) {
            User ctxUser = UserContext.get();
            if (ctxUser != null) {
                request.setUserName((ctxUser.getNickname() != null && !ctxUser.getNickname().isBlank()) ? ctxUser.getNickname() : ctxUser.getUsername());
            }
        }
        return chatTranslationService.sendChatMessage(request);
    }


    /**
     * 通过 SSE 提供流式翻译
     *
     * <p>此端点用于实现实时流式翻译。客户端通过此接口发起连接后，
     * 服务器会以 Server-Sent Events 的形式，逐段推送翻译结果。
     * 事件流包括 `start`、`delta`、`done` 和 `error` 四种事件。
     *
     * @param sessionId      会话的唯一标识符
     * @param message        需要翻译的原文
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param userId         用户 ID（可选）
     * @param userName       用户名（可选）
     * @return 一个 {@link SseEmitter} 实例，用于向客户端推送事件
     */
    @Operation(summary = "获取会话消息列表")
    @GetMapping(value = "/sessions/{sessionId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@PathVariable("sessionId") String sessionId,
                                    @RequestParam("message") String message,
                                    @RequestParam("sourceLanguage") String sourceLanguage,
                                    @RequestParam("targetLanguage") String targetLanguage,
                                    @RequestParam(value = "userId", required = false) String userId,
                                    @RequestParam(value = "userName", required = false) String userName,
                                    @RequestParam(value = "save", required = false) Boolean save) {
        // 30 秒超时，按需调整
        SseEmitter emitter = new SseEmitter(30_000L);
        // 附加错误/超时处理，避免异常向上冒泡
        emitter.onError(ex -> {
            log.warn("SSE连接错误: {}", ex.getMessage());
        });
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("timeout"));
            } catch (Exception ignore) {}
            try {
                emitter.complete();
            } catch (RuntimeException ignore) {}
        });

        //传统future只能阻塞当前线程，无法链式调用，无法处理异常
        CompletableFuture.runAsync(() -> {
            try {
                String translated = "";
                String startMessageId = "";
                // 当保存开启时持久化消息；否则仅做翻译但不入库
                if (save == null || Boolean.TRUE.equals(save)) {
                    SendChatMessageRequest req = SendChatMessageRequest.builder()
                            .sessionId(sessionId)
                            .userId(userId != null && !userId.isBlank() ? Long.parseLong(userId) : 888L)
                            .userName(userName != null && !userName.isBlank() ? userName : "用户888")
                            .message(message)
                            .sourceLanguage(sourceLanguage)
                            .targetLanguage(targetLanguage)
                            .messageType(MessageTypeEnum.TEXT.getValue())
                            .build();

                    ChatMessageResponse full = chatTranslationService.sendChatMessage(req);
                    translated = full.getTranslatedText();
                    if (translated == null) translated = "";
                    startMessageId = full.getMessageId();
                }

                // 使用真实翻译服务进行翻译（若可用），避免 Mock 前缀导致未实际翻译
                try {
                    String srcCode = toLangCode(sourceLanguage);
                    String trgCode = toLangCode(targetLanguage);
                    if (srcCode != null && trgCode != null) {
                        var real = qwenTranslationService.translate(
                                TranslationRequest.builder()
                                        .sourceText(message)
                                        .sourceLanguage(srcCode)
                                        .targetLanguage(trgCode)
                                        .useTerminology(true)//是否使用术语库
                                        .useRag(true)//是否使用RAG增强
                                        .build()
                        );
                        if (real != null && real.getTranslatedText() != null && !real.getTranslatedText().isBlank()) {
                            translated = real.getTranslatedText();
                        }
                    }
                } catch (Exception ignore) {
                    // 失败时保留 Mock 结果
                }
                //流式发送（打字机效果）
                // 开始事件，携带 messageId 便于前端关联
                String startPayload = json("{\"messageId\":\"" + safe(startMessageId) + "\"}");
                sseStreamService.sendEventAndHub(emitter, sessionSseHub, sessionId, "start", startPayload);

                // 分段推送 delta（英文采用更细分片与更长停顿，营造打字效果）
                // 英文：每8个字符发一次，延迟120ms
                // 中文：每16个字符发一次，延迟30ms
                String targetCode = toLangCode(targetLanguage);
                int chunkSize = "en".equals(targetCode) ? 8 : 16;
                long delayMs = "en".equals(targetCode) ? 120L : 30L;
                sseStreamService.streamChunks(emitter, sessionSseHub, sessionId, translated, chunkSize, delayMs, "delta");
                // 发送用户译文完成事件（done），仅返回翻译结果，避免重复客服回复
                String donePayload = json("{\"messageId\":\"" + safe(startMessageId) + "\",\"translatedText\":\"" + safe(translated) + "\"}");
                sseStreamService.sendEventAndHub(emitter, sessionSseHub, sessionId, "done", donePayload);

                try {
                    emitter.complete();
                } catch (RuntimeException ignore) {
                    // 忽略由于响应已出错导致的完成异常
                }
            } catch (Exception e) {
                log.warn("SSE流式翻译失败: {}", e.getMessage(), e);
                // 使用 completeWithError 标记错误，避免二次完成导致异常
                try {
                    emitter.completeWithError(e);
                } catch (RuntimeException ignore) {
                    // 忽略由于响应已出错导致的异常
                }
            }
        }, chatSseExecutor);

        return emitter;
    }

    @GetMapping(value = "/sessions/{sessionId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable("sessionId") String sessionId) {
        return sessionSseHub.register(sessionId);
    }

    /**
     * 通过 SSE 提供自动客服回复
     *
     * <p>此端点用于模拟自动客服回复场景。它接收一段英文消息，
     * 调用大语言模型（LLM）生成客服风格的英文回复，然后将该回复翻译成中文，
     * 并通过 SSE 流式推送到客户端。
     *
     * @param sessionId 会话的唯一标识符
     * @param userId    用户 ID（可选）
     * @return 一个 {@link SseEmitter} 实例，用于向客户端推送事件
     */
    @GetMapping(value = "/sessions/{sessionId}/auto-reply", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter autoReply(@PathVariable("sessionId") String sessionId,
                                @RequestParam("message") String message,
                                @RequestParam(value = "sourceLanguage", required = false) String sourceLanguage,
                                @RequestParam(value = "targetLanguage", required = false) String targetLanguage,
                                @RequestParam(value = "userId", required = false) String userId,
                                @RequestParam(value = "save", required = false) Boolean save) {
        SseEmitter emitter = new SseEmitter(30_000L);
        emitter.onError(ex -> {
            log.warn("SSE连接错误[auto-reply]: {}", ex.getMessage());
        });
        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event().name("error").data("timeout"));
            } catch (Exception ignore) {}
            try {
                emitter.complete();
            } catch (RuntimeException ignore) {}
        });

        CompletableFuture.runAsync(() -> {
            try {
                // 解析语言参数（默认：源=中文，目标=英文）
                String srcLabel = (sourceLanguage == null || sourceLanguage.isBlank()) ? "中文" : sourceLanguage;
                String trgLabel = (targetLanguage == null || targetLanguage.isBlank()) ? "英文" : targetLanguage;
                String srcCode = toLangCode(srcLabel);
                String trgCode = toLangCode(trgLabel);
                if (srcCode == null) srcCode = "zh";
                if (trgCode == null) trgCode = "en";

                // 将用户消息（目标语言）按需翻译为英文以供 LLM 生成客服回复
                String messageEnForLLM = message;
                if (!"en".equalsIgnoreCase(trgCode)) {
                    try {
                        var resp = qwenTranslationService.translate(
                                TranslationRequest.builder()
                                        .sourceText(message)
                                        .sourceLanguage(trgCode)
                                        .targetLanguage("en")
                                        .useTerminology(true)
                                        .useRag(true)
                                        .build()
                        );
                        messageEnForLLM = resp.getTranslatedText();
                    } catch (Exception ignore) {
                        messageEnForLLM = message; // 回退使用原文本
                    }
                }

                // 生成客服英文回复
                String serviceEn = qwenChatService.replyAsCustomerService(messageEnForLLM);
                if (serviceEn == null) serviceEn = "";

                // 将英文客服回复翻译为目标语言
                String serviceTarget;
                if ("en".equalsIgnoreCase(trgCode)) {
                    serviceTarget = serviceEn;
                } else {
                    try {
                        var resp = qwenTranslationService.translate(
                                TranslationRequest.builder()
                                        .sourceText(serviceEn)
                                        .sourceLanguage("en")
                                        .targetLanguage(trgCode)
                                        .useTerminology(true)
                                        .useRag(true)
                                        .build()
                        );
                        serviceTarget = resp.getTranslatedText();
                    } catch (Exception e) {
                        serviceTarget = "";
                    }
                    if (serviceTarget == null) serviceTarget = "";
                }

                // 首先流式输出“客服（目标语言）”：首帧 bot_start，后续 bot_delta（打字效果）
                sseStreamService.streamStartDelta(emitter, serviceTarget, 8, 120L, "bot_start", "bot_delta");

                // 再将客服（目标语言）翻译为源语言，并以 delta 事件流式输出
                String translatedSource;
                try {
                    if (srcCode.equalsIgnoreCase(trgCode)) {
                        translatedSource = serviceTarget; // 同语种时直接复用
                    } else {
                        var resp = qwenTranslationService.translate(
                                TranslationRequest.builder()
                                        .sourceText(serviceTarget)
                                        .sourceLanguage(trgCode)
                                        .targetLanguage(srcCode)
                                        .useTerminology(true)
                                        .useRag(true)
                                        .build()
                        );
                        translatedSource = resp.getTranslatedText();
                    }
                } catch (Exception e) {
                    translatedSource = "";
                }
                if (translatedSource == null) translatedSource = "";

                // 根据开关选择性持久化机器人回复消息（原文=目标语，译文=源语）
                if (save == null || Boolean.TRUE.equals(save)) {
                    String receiver = (userId != null && !userId.isBlank()) ? userId : "";
                    chatTranslationService.createAssistantReply(
                            sessionId, serviceTarget, translatedSource, trgCode, srcCode, receiver);
                }

                // 目标语已推送完毕，下面开始推送源语译文增量

                // 源语流式输出也采用更细分片与较长停顿，营造打字效果
                sseStreamService.streamChunks(emitter, null, null, translatedSource, 8, 120L, "delta");

                // 结束事件：直接文本（源语译文全文），不再使用 JSON 封装
                sseStreamService.sendEvent(emitter, "done", translatedSource);
                try {
                    emitter.complete();
                } catch (RuntimeException ignore) {
                    // 忽略由于响应已出错导致的完成异常
                }
            } catch (Exception e) {
                try {
                    emitter.completeWithError(e);
                } catch (RuntimeException ignore) {
                    // 忽略由于响应已出错导致的异常
                }
            }
        }, chatSseExecutor);

        return emitter;
    }

    /**
     * 将字符串包装为 JSON 格式
     *
     * @param s 待包装的字符串
     * @return UTF-8 编码的 JSON 字符串
     */
    private String json(String s) {
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }

    /**
     * 对字符串进行简单的安全转义
     *
     * <p>此方法用于对字符串进行转义，以避免在 SSE 事件流中破坏数据格式。
     * 它主要处理反斜杠、双引号、换行符和回车符。
     *
     * @param s 待转义的字符串
     * @return 转义后的字符串
     */
    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * 将语言名称转换为语言代码
     *
     * <p>此方法提供一个简单的映射，将常见的语言名称（如“中文”、“英文”）
     * 或其缩写转换为标准的语言代码（如“zh”、“en”）。
     *
     * @param lang 语言名称或缩写
     * @return 对应的语言代码，如果无法识别则返回 `null`
     */
    private String toLangCode(String lang) {
        if (lang == null) return null;
        String v = lang.trim().toLowerCase();
        switch (v) {
            case "中文": case "zh": case "cn": return "zh";
            case "英文": case "英语": case "en": return "en";
            case "日文": case "日语": case "ja": return "ja";
            case "韩文": case "韩语": case "ko": return "ko";
            case "法文": case "法语": case "fr": return "fr";
            case "德文": case "德语": case "de": return "de";
            case "西班牙文": case "西班牙语": case "es": return "es";
            case "俄文": case "俄语": case "ru": return "ru";
            default: return null;
        }
    }
}