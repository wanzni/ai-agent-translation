package cn.net.susan.ai.translation.service;

import cn.net.susan.ai.translation.dto.chat.*;
import cn.net.susan.ai.translation.enums.SessionStatusEnum;

import java.util.List;

/**
 * 聊天翻译服务接口，提供实时聊天翻译功能，支持多人聊天会话管理。
 */
public interface ChatTranslationService {

    /**
     * 获取聊天会话列表。
     *
     * @param userId 用户ID
     * @param status 会话状态
     * @return 会话列表
     */
    List<ChatSessionResponse> getChatSessions(String userId, SessionStatusEnum status);

    /**
     * 创建聊天会话。
     *
     * @param request 创建会话请求
     * @return 会话信息
     */
    ChatSessionResponse createChatSession(CreateChatSessionRequest request);

    /**
     * 获取聊天会话信息。
     *
     * @param sessionId 会话ID
     * @return 会话信息
     */
    ChatSessionResponse getChatSession(String sessionId);

    /**
     * 更新聊天会话。
     *
     * @param sessionId 会话ID
     * @param request 更新请求
     * @return 更新后的会话信息
     */
    ChatSessionResponse updateChatSession(String sessionId, UpdateChatSessionRequest request);

    /**
     * 结束聊天会话。
     *
     * @param sessionId 会话ID
     */
    void endChatSession(String sessionId);

    /**
     * 获取会话消息列表。
     *
     * @param sessionId 会话ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<ChatMessageResponse> getChatMessages(String sessionId, int page, int size);

    /**
     * 发送聊天消息。
     *
     * @param request 发送消息请求
     * @return 消息信息
     */
    ChatMessageResponse sendChatMessage(SendChatMessageRequest request);

    /**
     * 创建并保存自动客服回复消息。
     *
     * @param sessionId 会话ID（字符串形式）
     * @param assistantEnglishText 客服英文原文
     * @param translatedText 翻译后的文本（根据目标语言）
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     * @param receiverId 接收者ID（字符串）
     * @return 回复消息信息
     */
    ChatMessageResponse createAssistantReply(
            String sessionId,
            String assistantEnglishText,
            String translatedText,
            String sourceLanguage,
            String targetLanguage,
            String receiverId);

    /**
     * 标记消息为已读。
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     */
    void markMessageAsRead(String messageId, String userId);

    /**
     * 删除聊天消息。
     *
     * @param messageId 消息ID
     * @param userId 用户ID
     */
    void deleteChatMessage(String messageId, String userId);

    /**
     * 获取未读消息数量。
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return 未读消息数量
     */
    int getUnreadMessageCount(String sessionId, String userId);

    /**
     * 获取聊天统计信息。
     *
     * @param sessionId 会话ID
     * @return 统计信息
     */
    ChatStatsResponse getChatStats(String sessionId);

    /**
     * 处理WebSocket聊天消息。
     *
     * @param sessionId 会话ID
     * @param message 消息内容
     * @param userId 用户ID
     */
    void handleWebSocketMessage(String sessionId, String message, String userId);

    /**
     * 用户加入聊天会话。
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void joinChatSession(String sessionId, String userId);

    /**
     * 用户离开聊天会话。
     *
     * @param sessionId 会话ID
     * @param userId 用户ID
     */
    void leaveChatSession(String sessionId, String userId);
}