package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {
    Page<Moment> findAllByOrderByCreatedAtDesc(Pageable pageable);
}