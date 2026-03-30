package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.AgentTaskStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentTaskStepRepository extends JpaRepository<AgentTaskStep, Long> {

    List<AgentTaskStep> findByTaskIdOrderByStepNoAsc(Long taskId);

    long countByTaskId(Long taskId);
}
