package cn.net.susan.ai.translation.repository;

import cn.net.susan.ai.translation.entity.ChatSession;
import cn.net.susan.ai.translation.enums.SessionStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    ChatSession findBySessionId(String sessionId);

    @Query("SELECT cs FROM ChatSession cs WHERE (cs.userAId = :userId OR cs.userBId = :userId) AND cs.status = :status")
    Page<ChatSession> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") SessionStatusEnum status, Pageable pageable);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.userAId = :userId OR cs.userBId = :userId")
    Page<ChatSession> findByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("SELECT cs FROM ChatSession cs WHERE cs.status = :status AND cs.lastActiveAt < :threshold")
    List<ChatSession> findInactiveSessions(@Param("status") SessionStatusEnum status, @Param("threshold") LocalDateTime threshold);
}