package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.dto.chat.*;
import cn.net.wanzni.ai.translation.entity.ChatMessage;
import cn.net.wanzni.ai.translation.entity.ChatSession;
import cn.net.wanzni.ai.translation.enums.SessionStatusEnum;
import cn.net.wanzni.ai.translation.enums.TranslationStatusEnum;
import cn.net.wanzni.ai.translation.repository.ChatMessageRepository;
import cn.net.wanzni.ai.translation.repository.ChatSessionRepository;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.ChatTranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import cn.net.wanzni.ai.translation.enums.MessageTypeEnum;
import lombok.extern.slf4j.Slf4j;

/**
 * 聊天翻译服务实现类，提供实时聊天翻译功能，支持多人聊天会话管理。
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatTranslationServiceImpl implements ChatTranslationService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final Map<String, ChatSessionResponse> chatSessions = new ConcurrentHashMap<>();
    private final Map<String, List<ChatMessageResponse>> sessionMessages = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> unreadCounts = new ConcurrentHashMap<>();
    
    /**
     * 获取聊天会话列表。
     *
     * @param userId 用户ID
     * @param status 会话状态
     * @return 会话列表
     */
    @Override
    public List<ChatSessionResponse> getChatSessions(String userId, SessionStatusEnum status) {
        List<ChatSession> sessions;
        if (status != null) {
            sessions = chatSessionRepository.findByUserIdAndStatus(userId, status, Pageable.unpaged()).getContent();
        } else {
            sessions = chatSessionRepository.findByUserId(userId, Pageable.unpaged()).getContent();
        }
        return sessions.stream()
                .sorted(Comparator.comparing(ChatSession::getLastActiveAt,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ChatSession::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ChatSessionResponse convertToResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .sessionId(session.getSessionId())
                .sessionName(session.getSessionTitle())
                .sourceLanguage(session.getUserALanguage())
                .targetLanguage(session.getUserBLanguage())
                .createdBy(session.getUserAId())
                .createdAt(session.getCreatedAt())
                .status(session.getStatus().name().toLowerCase())
                .participantCount(countParticipants(session))
                .messageCount(session.getMessageCount() == null ? 0 : session.getMessageCount())
                .lastActivity(session.getLastActiveAt())
                .build();
    }

    /**
     * 创建聊天会话。
     *
     * @param request 创建会话请求
     * @return 会话信息
     */
    @Override
    public ChatSessionResponse createChatSession(CreateChatSessionRequest request) {
        String sessionId = "session_" + System.currentTimeMillis();
        String creator = (request.getCreatedBy() != null && !request.getCreatedBy().isBlank())
                ? request.getCreatedBy()
                : "888";

        ChatSession entity = ChatSession.builder()
                .sessionId(sessionId)
                .userAId(creator)
                .userBId("888")
                .userALanguage(request.getSourceLanguage())
                .userBLanguage(request.getTargetLanguage())
                .sessionTitle(request.getSessionName())
                .status(SessionStatusEnum.ACTIVE)
                .messageCount(0)
                .lastActiveAt(LocalDateTime.now())
                .build();
        entity = chatSessionRepository.save(entity);

        ChatSessionResponse session = convertToResponse(entity);
        session.setDescription(request.getDescription());
        chatSessions.put(sessionId, session);
        sessionMessages.put(sessionId, new ArrayList<>());
        sessionUsers.put(sessionId, new HashSet<>(Arrays.asList(creator, "888")));
        unreadCounts.put(sessionId, new HashMap<>());
        return session;
        /*
        session.setSessionId(sessionId);
        session.setSessionName(request.getSessionName());
        session.setSourceLanguage(request.getSourceLanguage());
        session.setTargetLanguage(request.getTargetLanguage());
        String creator = (request.getCreatedBy() != null && !request.getCreatedBy().isBlank())
                ? request.getCreatedBy()
                : "888"; // 默认用户ID
        session.setCreatedBy(creator);
        session.setCreatedAt(LocalDateTime.now());
        session.setStatus(SessionStatusEnum.ACTIVE.name().toLowerCase());
        session.setParticipantCount(1);
        session.setMessageCount(0);
        session.setLastActivity(LocalDateTime.now());
        
        chatSessions.put(sessionId, session);
        sessionMessages.put(sessionId, new ArrayList<>());
        sessionUsers.put(sessionId, new HashSet<>(Collections.singletonList(creator)));
        unreadCounts.put(sessionId, new HashMap<>());
        
        return session;
        */
    }
    
    /**
     * 获取聊天会話信息。
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    @Override
    public ChatSessionResponse getChatSession(String sessionId) {
        return convertToResponse(findSessionEntity(sessionId));
        /*
        ChatSessionResponse session = chatSessions.get(sessionId);
        if (session == null) {
            throw new RuntimeException("聊天会话不存在: " + sessionId);
        }
        return session;
        */
    }
    
    /**
     * 更新聊天会话。
     *
     * @param sessionId 会话ID
     * @param request 更新请求
     * @return 更新后的会话信息
     */
    @Override
    public ChatSessionResponse updateChatSession(String sessionId, UpdateChatSessionRequest request) {
        ChatSession session = findSessionEntity(sessionId);

        if (request.getSessionName() != null) {
            session.setSessionTitle(request.getSessionName());
        }
        if (request.getSourceLanguage() != null) {
            session.setUserALanguage(request.getSourceLanguage());
        }
        if (request.getTargetLanguage() != null) {
            session.setUserBLanguage(request.getTargetLanguage());
        }

        session.setLastActiveAt(LocalDateTime.now());
        ChatSession saved = chatSessionRepository.save(session);
        ChatSessionResponse response = convertToResponse(saved);
        chatSessions.put(sessionId, response);
        return response;
        /*
        ChatSessionResponse session = getChatSession(sessionId);
        
        if (request.getSessionName() != null) {
            session.setSessionName(request.getSessionName());
        }
        if (request.getSourceLanguage() != null) {
            session.setSourceLanguage(request.getSourceLanguage());
        }
        if (request.getTargetLanguage() != null) {
            session.setTargetLanguage(request.getTargetLanguage());
        }
        
        session.setLastActivity(LocalDateTime.now());
        return session;
        */
    }
    
    /**
     * 结束聊天会话。
     *
     * @param sessionId 会话ID
     */
    @Override
    public void endChatSession(String sessionId) {
        ChatSession session = findSessionEntity(sessionId);
        session.endSession();
        session.setLastActiveAt(LocalDateTime.now());
        ChatSession saved = chatSessionRepository.save(session);
        chatSessions.put(sessionId, convertToResponse(saved));
        /*
        ChatSessionResponse session = getChatSession(sessionId);
        session.setStatus(SessionStatusEnum.ENDED.name().toLowerCase());
        session.setLastActivity(LocalDateTime.now());
        */
    }
    
    /**
     * 获取会话消息列表。
     *
     * @param sessionId 会话ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    @Override
    public List<ChatMessageResponse> getChatMessages(String sessionId, int page, int size) {
        // 优先从数据库分页加载历史消息，确保跨会话访问时仍可显示历史记录
        try {
            ChatSession session = chatSessionRepository.findBySessionId(sessionId);
            if (session == null) {
                return Collections.emptyList();
            }
            var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.max(size, 1));
            var pageData = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId(), pageable);
            return pageData.getContent().stream().map(this::convertToResponse).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            // 发生异常时回退到内存消息（兼容旧逻辑）
            List<ChatMessageResponse> messages = sessionMessages.getOrDefault(sessionId, new ArrayList<>());
            int start = page * size;
            int end = Math.min(start + size, messages.size());
            if (start >= messages.size()) {
                return new ArrayList<>();
            }
            return messages.subList(start, end);
        }
    }
    
    /**
     * 发送聊天消息。
     *
     * @param request 发送消息请求
     * @return 消息信息
     */
    @Override
    public ChatMessageResponse sendChatMessage(SendChatMessageRequest request) {
        String sessionIdStr = request.getSessionId();

        ChatSession session = chatSessionRepository.findBySessionId(sessionIdStr);

        if (session == null) {
            session = ChatSession.builder()
                    .sessionId(sessionIdStr)
                    .userAId(request.getSenderId())
                    .userBId(request.getReceiverId())
                    .userALanguage(request.getSourceLanguage())
                    .userBLanguage(request.getTargetLanguage())
                    .sessionTitle("Chat Session") // Default title
                    .build();
            session = chatSessionRepository.save(session);
        }

        String original = request.getMessage();
        if (original == null || original.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        String sourceLang = request.getSourceLanguage();
        String targetLang = request.getTargetLanguage();
        int typeIndex = request.getMessageType();
        MessageTypeEnum[] types = MessageTypeEnum.values();
        MessageTypeEnum type = (typeIndex >= 0 && typeIndex < types.length)
                ? types[typeIndex]
                : MessageTypeEnum.TEXT;

        // 解析发送者ID：优先当前登录用户，其次请求中的 userId，最后尝试解析 senderId
        Long resolvedSenderId = UserContext.getUserId();
        if (resolvedSenderId == null) {
            resolvedSenderId = request.getUserId();
        }
        if (resolvedSenderId == null && request.getSenderId() != null && !request.getSenderId().isBlank()) {
            try {
                resolvedSenderId = Long.valueOf(request.getSenderId().trim());
            } catch (NumberFormatException ignored) {
                // 保持为 null，后续抛错提示
            }
        }
        if (resolvedSenderId == null) {
            throw new IllegalStateException("发送者ID为空，请登录或在请求中提供有效的 senderId/userId");
        }

        // 解析接收者ID：默认使用 888 作为系统用户
        String resolvedReceiverId = (request.getReceiverId() != null && !request.getReceiverId().isBlank())
                ? request.getReceiverId().trim()
                : "888";

        if (session == null) {
            session = ChatSession.builder()
                    .sessionId(sessionIdStr)
                    .userAId(String.valueOf(resolvedSenderId))
                    .userBId(resolvedReceiverId)
                    .userALanguage(sourceLang)
                    .userBLanguage(targetLang)
                    .sessionTitle("Chat Session")
                    .status(SessionStatusEnum.ACTIVE)
                    .messageCount(0)
                    .lastActiveAt(LocalDateTime.now())
                    .build();
        } else {
            session.setUserAId(String.valueOf(resolvedSenderId));
            if (session.getUserBId() == null || session.getUserBId().isBlank()) {
                session.setUserBId(resolvedReceiverId);
            }
            if (sourceLang != null && !sourceLang.isBlank()) {
                session.setUserALanguage(sourceLang.trim());
            }
            if (targetLang != null && !targetLang.isBlank()) {
                session.setUserBLanguage(targetLang.trim());
            }
            if (session.getSessionTitle() == null || session.getSessionTitle().isBlank()) {
                session.setSessionTitle("Chat Session");
            }
            session.setStatus(SessionStatusEnum.ACTIVE);
            session.setLastActiveAt(LocalDateTime.now());
        }
        session = chatSessionRepository.save(session);
        sessionUsers.computeIfAbsent(sessionIdStr, k -> new HashSet<>())
                .addAll(Arrays.asList(String.valueOf(resolvedSenderId), resolvedReceiverId));

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(resolvedSenderId);
        message.setReceiverId(resolvedReceiverId);
        message.setOriginalMessage(original.trim());
        message.setSourceLanguage(sourceLang != null ? sourceLang.trim() : null);
        message.setTargetLanguage(targetLang != null ? targetLang.trim() : null);
        message.setMessageType(type);
        message.setUseTerminology(Boolean.TRUE.equals(request.getUseTerminology()));
        message.setTranslationEngine(request.getTranslationEngine());

        message = chatMessageRepository.save(message);

        // 同步翻译，确保立即返回包含回复内容
        message = translateMessageSync(message);

        ChatMessageResponse response = convertToResponse(message);
        session.incrementMessageCount();
        chatSessionRepository.save(session);
        chatSessions.put(sessionIdStr, convertToResponse(session));
        // 写入内存会话消息列表，便于实时对话读取
        sessionMessages.computeIfAbsent(sessionIdStr, k -> new ArrayList<>()).add(response);
        // 更新未读计数（将 Long senderId 转为字符串）
        updateUnreadCounts(sessionIdStr, String.valueOf(resolvedSenderId));

        return response;
    }

    /**
     * 创建并保存自动客服回复消息。
     */
    @Override
    public ChatMessageResponse createUserMessage(
            String sessionIdStr,
            Long senderId,
            String receiverId,
            String originalMessage,
            String sourceLanguage,
            String targetLanguage) {

        String normalizedSenderId = senderId != null ? String.valueOf(senderId) : "888";
        String normalizedReceiverId = (receiverId != null && !receiverId.isBlank()) ? receiverId.trim() : "888";
        String normalizedOriginal = originalMessage != null ? originalMessage.trim() : "";
        if (normalizedOriginal.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        ChatSession session = chatSessionRepository.findBySessionId(sessionIdStr);
        if (session == null) {
            session = ChatSession.builder()
                    .sessionId(sessionIdStr)
                    .userAId(normalizedSenderId)
                    .userBId(normalizedReceiverId)
                    .userALanguage(sourceLanguage != null ? sourceLanguage : "zh")
                    .userBLanguage(targetLanguage != null ? targetLanguage : "en")
                    .sessionTitle("Chat Session")
                    .status(SessionStatusEnum.ACTIVE)
                    .messageCount(0)
                    .lastActiveAt(LocalDateTime.now())
                    .build();
            session = chatSessionRepository.save(session);
        }

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(session.getId());
        msg.setSenderId(senderId != null ? senderId : 888L);
        msg.setReceiverId(normalizedReceiverId);
        msg.setOriginalMessage(normalizedOriginal);
        msg.setSourceLanguage(sourceLanguage != null ? sourceLanguage : "zh");
        msg.setTargetLanguage(targetLanguage);
        msg.setMessageType(MessageTypeEnum.TEXT);
        msg.setTranslationStatus(TranslationStatusEnum.COMPLETED);

        msg = chatMessageRepository.save(msg);

        ChatMessageResponse resp = convertToResponse(msg);
        session.incrementMessageCount();
        chatSessionRepository.save(session);
        chatSessions.put(sessionIdStr, convertToResponse(session));
        sessionUsers.computeIfAbsent(sessionIdStr, k -> new HashSet<>())
                .addAll(Arrays.asList(normalizedSenderId, normalizedReceiverId));
        sessionMessages.computeIfAbsent(sessionIdStr, k -> new ArrayList<>()).add(resp);
        updateUnreadCounts(sessionIdStr, normalizedSenderId);
        return resp;
    }

    /**
     * 创建并保存自动客服回复消息。
     */
    @Override
    public ChatMessageResponse createAssistantReply(
            String sessionIdStr,
            String assistantEnglishText,
            String translatedText,
            String sourceLanguage,
            String targetLanguage,
            String receiverId) {

        ChatSession session = chatSessionRepository.findBySessionId(sessionIdStr);
        if (session == null) {
            session = ChatSession.builder()
                    .sessionId(sessionIdStr)
                    .userAId(receiverId != null && !receiverId.isBlank() ? receiverId.trim() : "888")
                    .userBId("888")
                    .userALanguage(sourceLanguage != null ? sourceLanguage : "en")
                    .userBLanguage(targetLanguage != null ? targetLanguage : "zh")
                    .sessionTitle("Chat Session")
                    .status(SessionStatusEnum.ACTIVE)
                    .messageCount(0)
                    .lastActiveAt(LocalDateTime.now())
                    .build();
            session = chatSessionRepository.save(session);
        }

        ChatMessage msg = new ChatMessage();
        msg.setSessionId(session.getId());
        // 系统客服用户ID固定为 888
        msg.setSenderId(888L);
        msg.setReceiverId(receiverId != null && !receiverId.isBlank() ? receiverId.trim() : "888");
        msg.setOriginalMessage(assistantEnglishText != null ? assistantEnglishText : "");
        msg.setTranslatedMessage(translatedText);
        msg.setSourceLanguage(sourceLanguage != null ? sourceLanguage : "en");
        msg.setTargetLanguage(targetLanguage);
        msg.setMessageType(MessageTypeEnum.TEXT);
        msg.setTranslationStatus(TranslationStatusEnum.COMPLETED);
        msg.setTranslationEngine("QWEN");

        msg = chatMessageRepository.save(msg);

        ChatMessageResponse resp = convertToResponse(msg);
        session.incrementMessageCount();
        chatSessionRepository.save(session);
        chatSessions.put(sessionIdStr, convertToResponse(session));
        sessionUsers.computeIfAbsent(sessionIdStr, k -> new HashSet<>())
                .addAll(Arrays.asList("888", msg.getReceiverId()));
        sessionMessages.computeIfAbsent(sessionIdStr, k -> new ArrayList<>()).add(resp);
        // 更新未读计数，接收者为当前用户
        updateUnreadCounts(sessionIdStr, msg.getReceiverId());
        return resp;
    }

    private void translateMessageAsync(Long messageId) {
        // 异步翻译的实现（保留占位，当前使用同步翻译）
        chatMessageRepository.findById(messageId).ifPresent(this::translateMessageSync);
    }

    /**
     * 同步翻译消息并持久化状态与结果。
     */
    private ChatMessage translateMessageSync(ChatMessage message) {
        long start = System.currentTimeMillis();
        // 标记为翻译中
        message.setTranslationStatus(TranslationStatusEnum.TRANSLATING);
        message = chatMessageRepository.save(message);

        String translated = mockTranslate(message.getOriginalMessage(),
                message.getTargetLanguage() != null ? message.getTargetLanguage() : message.getSourceLanguage());
        long elapsed = System.currentTimeMillis() - start;

        message.setTranslatedMessage(translated);
        message.setTranslationTime(elapsed);
        message.setTranslationStatus(TranslationStatusEnum.COMPLETED);
        message = chatMessageRepository.save(message);

        // 同步更新内存列表中的已存在条目
        String sid = String.valueOf(message.getSessionId());
        List<ChatMessageResponse> list = sessionMessages.get(sid);
        if (list != null) {
            for (ChatMessageResponse r : list) {
                if (r.getMessageId().equals(String.valueOf(message.getId()))) {
                    r.setTranslatedText(message.getTranslatedMessage());
                    r.setSentAt(message.getCreatedAt());
                    break;
                }
            }
        }

        return message;
    }

    /**
     * 将 ChatMessage 实体转换为 ChatMessageResponse 数据传输对象。
     *
     * @param message 要转换的 ChatMessage 实体
     * @return 转换后的 ChatMessageResponse 对象
     */
    private ChatMessageResponse convertToResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .messageId(message.getId().toString())
                .sessionId(message.getSessionId().toString())
                .userId(message.getSenderId())
                .originalText(message.getOriginalMessage())
                .translatedText(message.getTranslatedMessage())
                .sourceLanguage(message.getSourceLanguage())
                .targetLanguage(message.getTargetLanguage())
                .sentAt(message.getCreatedAt())
                .messageType(message.getMessageType().ordinal())
                .build();
    }

    private ChatSession findSessionEntity(String sessionId) {
        ChatSession session = chatSessionRepository.findBySessionId(sessionId);
        if (session == null) {
            throw new RuntimeException("Chat session not found: " + sessionId);
        }
        return session;
    }

    private int countParticipants(ChatSession session) {
        int count = 0;
        if (session.getUserAId() != null && !session.getUserAId().isBlank()) {
            count++;
        }
        if (session.getUserBId() != null && !session.getUserBId().isBlank()) {
            count++;
        }
        return Math.max(count, 1);
    }
    
    /**
     * 标记消息为已读。
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     */
    @Override
    public void markMessageAsRead(String messageId, String userId) {
        // 查找消息并标记为已读
        for (List<ChatMessageResponse> messages : sessionMessages.values()) {
            for (ChatMessageResponse message : messages) {
                if (message.getMessageId().equals(messageId)) {
                    if (!message.getReadBy().contains(userId)) {
                        message.getReadBy().add(userId);
                    }
                    return;
                }
            }
        }
    }
    
    /**
     * 删除聊天消息。
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     */
    @Override
    public void deleteChatMessage(String messageId, String userId) {
        // 查找并删除消息（仅允许发送者删除）
        for (List<ChatMessageResponse> messages : sessionMessages.values()) {
            messages.removeIf(message -> 
                message.getMessageId().equals(messageId) && message.getUserId().equals(userId));
        }
    }
    
    /**
     * 获取未读消息数量。
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 未读消息数量
     */
    @Override
    public int getUnreadMessageCount(String sessionId, String userId) {
        return unreadCounts.getOrDefault(sessionId, new HashMap<>()).getOrDefault(userId, 0);
    }
    
    /**
     * 获取聊天统计信息。
     *
     * @param sessionId 会话ID
     * @return 统计信息
     */
    @Override
    public ChatStatsResponse getChatStats(String sessionId) {
        ChatSessionResponse session = getChatSession(sessionId);
        List<ChatMessageResponse> messages = sessionMessages.getOrDefault(sessionId, new ArrayList<>());
        
        ChatStatsResponse stats = new ChatStatsResponse();
        stats.setSessionId(sessionId);
        stats.setTotalMessages(messages.size());
        stats.setTotalParticipants(sessionUsers.getOrDefault(sessionId, new HashSet<>()).size());
        stats.setActiveParticipants(sessionUsers.getOrDefault(sessionId, new HashSet<>()).size());
        stats.setAverageResponseTime(2.5); // Mock数据
        stats.setTranslationAccuracy(0.95); // Mock数据
        stats.setSessionDuration(calculateSessionDuration(session));
        stats.setLanguagePairs(Arrays.asList(
            session.getSourceLanguage() + " -> " + session.getTargetLanguage()
        ));
        
        return stats;
    }
    
    /**
     * 处理WebSocket聊天消息。
     *
     * @param sessionId 会话ID
     * @param message 消息内容
     * @param userId 用户ID
     */
    @Override
    public void handleWebSocketMessage(String sessionId, String message, String userId) {
        // WebSocket消息处理逻辑
        // 这里可以实现实时翻译和消息广播
        System.out.println("处理WebSocket消息 - 会话: " + sessionId + ", 用户: " + userId + ", 消息: " + message);
    }
    
    /**
     * 用户加入聊天会话。
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    @Override
    public void joinChatSession(String sessionId, String userId) {
        if (userId == null || userId.isBlank()) {
            return; // 避免加入空用户
        }
        sessionUsers.computeIfAbsent(sessionId, k -> new HashSet<>()).add(userId);
        
        // 更新会话参与者数量
        ChatSessionResponse session = chatSessions.get(sessionId);
        if (session != null) {
            session.setParticipantCount(sessionUsers.get(sessionId).size());
            session.setLastActivity(LocalDateTime.now());
        }
    }
    
    /**
     * 用户离开聊天会话。
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    @Override
    public void leaveChatSession(String sessionId, String userId) {
        Set<String> users = sessionUsers.get(sessionId);
        if (users != null) {
            users.remove(userId);
            
            // 更新会话参与者数量
            ChatSessionResponse session = chatSessions.get(sessionId);
            if (session != null) {
                session.setParticipantCount(users.size());
                session.setLastActivity(LocalDateTime.now());
            }
        }
    }
    
    /**
     * 模拟翻译功能。
     *
     * @param text 要翻译的文本
     * @param targetLanguage 目标语言
     * @return 翻译后的文本
     */
    private String mockTranslate(String text, String targetLanguage) {
        // 更完善的 Mock 翻译逻辑，支持中英双向常见短语
        if (text == null) {
            return "";
        }

        String tl = targetLanguage == null ? "" : targetLanguage.trim().toLowerCase();
        boolean toEnglish = tl.startsWith("en") ||
                (targetLanguage != null && (targetLanguage.contains("英文") || targetLanguage.equalsIgnoreCase("english")));
        boolean toChinese = tl.startsWith("zh") ||
                (targetLanguage != null && (targetLanguage.contains("中文") || targetLanguage.contains("汉语")));

        // 英->中
        Map<String, String> en2zh = new HashMap<>();
        en2zh.put("Hello", "你好");
        en2zh.put("Hello, are you there?", "你好，在吗？");
        en2zh.put("How are you?", "你好吗？");
        en2zh.put("Thank you", "谢谢");
        en2zh.put("Goodbye", "再见");
        en2zh.put("Good morning", "早上好");

        // 中->英
        Map<String, String> zh2en = new HashMap<>();
        zh2en.put("你好", "Hello");
        zh2en.put("你好，在吗？", "Hello, are you there?");
        zh2en.put("你好吗？", "How are you?");
        zh2en.put("谢谢", "Thank you");
        zh2en.put("再见", "Goodbye");
        zh2en.put("早上好", "Good morning");

        if (toEnglish) {
            String mapped = zh2en.get(text);
            if (mapped != null) {
                return mapped;
            }
            // 如果文本本身是英文，直接返回
            if (text.matches(".*[A-Za-z].*")) {
                return text;
            }
            // 未命中词典的中文内容，返回英文占位但不添加标签，避免保存中文
            return "Translation needed for: " + text;
        }

        if (toChinese) {
            String mapped = en2zh.get(text);
            if (mapped != null) {
                return mapped;
            }
            // 如果文本本身是中文，直接返回
            if (text.matches(".*[\\p{IsHan}].*")) {
                return text;
            }
            return "需要翻译: " + text;
        }

        // 其他目标语言或未识别，保持原文，不添加标签
        return text;
    }
    
    /**
     * 更新未读消息计数。
     *
     * @param sessionId 会话ID
     * @param senderId 发送者ID
     */
    private void updateUnreadCounts(String sessionId, String senderId) {
        Set<String> users = sessionUsers.getOrDefault(sessionId, new HashSet<>());
        Map<String, Integer> counts = unreadCounts.computeIfAbsent(sessionId, k -> new HashMap<>());
        
        for (String uid : users) {
            if (uid == null || uid.isBlank()) {
                continue; // 跳过空用户，避免NPE
            }
            if (!uid.equals(senderId)) {
                counts.put(uid, counts.getOrDefault(uid, 0) + 1);
            }
        }
    }
    
    /**
     * 计算会话持续时间（分钟）。
     *
     * @param session 聊天会话
     * @return 会话持续时间（分钟）
     */
    private long calculateSessionDuration(ChatSessionResponse session) {
        if (session.getCreatedAt() == null) {
            return 0;
        }
        
        LocalDateTime endTime = SessionStatusEnum.ENDED.name().equalsIgnoreCase(session.getStatus()) ? 
            session.getLastActivity() : LocalDateTime.now();
            
        return java.time.Duration.between(session.getCreatedAt(), endTime).toMinutes();
    }
}
