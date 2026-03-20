package cn.net.susan.ai.translation.repository;

import cn.net.susan.ai.translation.entity.ReviewTask;
import cn.net.susan.ai.translation.enums.ReviewStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {

    Page<ReviewTask> findByReviewStatusOrderByCreatedAtDesc(ReviewStatusEnum reviewStatus, Pageable pageable);
}
