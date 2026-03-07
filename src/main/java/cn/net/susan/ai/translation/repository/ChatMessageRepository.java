package cn.net.susan.ai.translation.repository;

import cn.net.susan.ai.translation.entity.ChatMessage;
import cn.net.susan.ai.translation.enums.MessageTypeEnum;
import cn.net.susan.ai.translation.enums.TranslationStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 聊天消息数据访问接口
 * 
 * 提供聊天消息的CRUD操作和自定义查询方法
 * 
 * @author 苏三
 * @version 1.0.0
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 根据会话ID查找消息
     * 
     * @param sessionId 会话ID
     * @param pageable 分页参数
     * @return 聊天消息分页结果
     */
    Page<ChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId, Pageable pageable);

    /**
     * 根据会话ID和消息类型查找消息
     * 
     * @param sessionId 会话ID
     * @param messageType 消息类型
     * @param pageable 分页参数
     * @return 聊天消息分页结果
     */
    Page<ChatMessage> findBySessionIdAndMessageTypeOrderByCreatedAtAsc(
            Long sessionId, MessageTypeEnum messageType, Pageable pageable);

    /**
     * 根据发送者ID查找消息
     * 
     * @param senderId 发送者ID
     * @param pageable 分页参数
     * @return 聊天消息分页结果
     */
    Page<ChatMessage> findBySenderIdOrderByCreatedAtDesc(Long senderId, Pageable pageable);

    /**
     * 根据会话ID和发送者ID查找消息
     * 
     * @param sessionId 会话ID
     * @param senderId 发送者ID
     * @param pageable 分页参数
     * @return 聊天消息分页结果
     */
    Page<ChatMessage> findBySessionIdAndSenderIdOrderByCreatedAtAsc(
            Long sessionId, Long senderId, Pageable pageable);

    /**
     * 根据翻译状态查找消息
     * 
     * @param translationStatus 翻译状态
     * @param pageable 分页参数
     * @return 聊天消息分页结果
     */
    Page<ChatMessage> findByTranslationStatusOrderByCreatedAtAsc(
            TranslationStatusEnum translationStatus, Pageable pageable);

    /**
     * 查找未读消息
     * 
     * @param receiverId 接收者ID
     * @param pageable 分页参数
     * @return 未读消息分页结果
     */
    Page<ChatMessage> findByReceiverIdAndIsReadFalseOrderByCreatedAtAsc(String receiverId, Pageable pageable);

    /**
     * 根据会话ID查找未读消息
     * 
     * @param sessionId 会话ID
     * @param receiverId 接收者ID
     * @return 未读消息列表
     */
    List<ChatMessage> findBySessionIdAndReceiverIdAndIsReadFalseOrderByCreatedAtAsc(
            Long sessionId, String receiverId);

    /**
     * 根据时间范围查找消息
     * 
     * @param sessionId 会话ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 聊天消息分页结果
     */
    Page<ChatMessage> findBySessionIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            Long sessionId, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 搜索消息内容
     * 
     * @param sessionId 会话ID
     * @param keyword 搜索关键词
     * @param pageable 分页参数
     * @return 聊天消息分页结果
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId AND " +
           "(cm.originalMessage LIKE %:keyword% OR cm.translatedMessage LIKE %:keyword%) " +
           "ORDER BY cm.createdAt ASC")
    Page<ChatMessage> searchMessageContent(
            @Param("sessionId") Long sessionId, 
            @Param("keyword") String keyword, 
            Pageable pageable);

    /**
     * 查找会话中的最新消息
     * 
     * @param sessionId 会话ID
     * @return 最新消息
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId ORDER BY cm.createdAt DESC")
    Optional<ChatMessage> findLatestMessageBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 查找会话中的第一条消息
     * 
     * @param sessionId 会话ID
     * @return 第一条消息
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.sessionId = :sessionId ORDER BY cm.createdAt ASC")
    Optional<ChatMessage> findFirstMessageBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 统计会话中的消息总数
     * 
     * @param sessionId 会话ID
     * @return 消息总数
     */
    long countBySessionId(Long sessionId);

    /**
     * 统计用户发送的消息总数
     * 
     * @param senderId 发送者ID
     * @return 消息总数
     */
    long countBySenderId(Long senderId);

    /**
     * 统计未读消息数量
     * 
     * @param receiverId 接收者ID
     * @return 未读消息数量
     */
    long countByReceiverIdAndIsReadFalse(String receiverId);

    /**
     * 统计会话中的未读消息数量
     * 
     * @param sessionId 会话ID
     * @param receiverId 接收者ID
     * @return 未读消息数量
     */
    long countBySessionIdAndReceiverIdAndIsReadFalse(Long sessionId, String receiverId);

    /**
     * 统计各翻译状态的消息数量
     * 
     * @return 翻译状态统计结果
     */
    @Query("SELECT cm.translationStatus, COUNT(cm) FROM ChatMessage cm GROUP BY cm.translationStatus")
    List<Object[]> countByTranslationStatus();

    /**
     * 统计各消息类型的数量
     * 
     * @return 消息类型统计结果
     */
    @Query("SELECT cm.messageType, COUNT(cm) FROM ChatMessage cm GROUP BY cm.messageType")
    List<Object[]> countByMessageType();

    /**
     * 获取平均翻译时间
     * 
     * @return 平均翻译时间（毫秒）
     */
    @Query("SELECT AVG(c.translationTime) FROM ChatMessage c WHERE c.translationStatus = cn.net.susan.ai.translation.enums.TranslationStatusEnum.COMPLETED")
    Optional<Double> getAverageTranslationTime();

    /**
     * 查找翻译失败的消息
     * 
     * @param pageable 分页参数
     * @return 翻译失败的消息分页结果
     */
    Page<ChatMessage> findByTranslationStatusOrderByCreatedAtDesc(
            TranslationStatusEnum translationStatus, Pageable pageable);

    /**
     * 查找需要重新翻译的消息
     * 
     * @return 需要重新翻译的消息列表
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.translationStatus IN (cn.net.susan.ai.translation.enums.TranslationStatusEnum.PENDING, cn.net.susan.ai.translation.enums.TranslationStatusEnum.TRANSLATING) ORDER BY cm.createdAt ASC")
    List<ChatMessage> findMessagesNeedingTranslation();

    /**
     * 批量标记消息为已读
     * 
     * @param sessionId 会话ID
     * @param receiverId 接收者ID
     * @return 更新的记录数
     */
    @Query("UPDATE ChatMessage cm SET cm.isRead = true WHERE cm.sessionId = :sessionId AND cm.receiverId = :receiverId AND cm.isRead = false")
    int markMessagesAsRead(@Param("sessionId") Long sessionId, @Param("receiverId") String receiverId);

    /**
     * 删除指定时间之前的消息
     * 
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    long deleteByCreatedAtBefore(LocalDateTime beforeTime);

    /**
     * 删除指定会话的所有消息
     * 
     * @param sessionId 会话ID
     * @return 删除的记录数
     */
    long deleteBySessionId(Long sessionId);

    /**
     * 查找指定时间范围内的消息统计
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 消息数量
     */
    long countByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找会话中指定序号范围的消息
     * 
     * @param sessionId 会话ID
     * @param startSequence 开始序号
     * @param endSequence 结束序号
     * @return 消息列表
     */
    List<ChatMessage> findBySessionIdAndMessageSequenceBetweenOrderByMessageSequenceAsc(
            Long sessionId, Integer startSequence, Integer endSequence);
}