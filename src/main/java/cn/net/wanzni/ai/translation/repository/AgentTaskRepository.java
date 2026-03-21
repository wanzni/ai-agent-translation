package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.AgentTask;
import cn.net.wanzni.ai.translation.enums.AgentTaskStatusEnum;
import cn.net.wanzni.ai.translation.enums.AgentTaskTypeEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AgentTaskRepository extends JpaRepository<AgentTask, Long> {

    Optional<AgentTask> findByTaskNo(String taskNo);

    Page<AgentTask> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<AgentTask> findByStatusOrderByCreatedAtDesc(AgentTaskStatusEnum status, Pageable pageable);

    Page<AgentTask> findByTaskTypeOrderByCreatedAtDesc(AgentTaskTypeEnum taskType, Pageable pageable);
}
