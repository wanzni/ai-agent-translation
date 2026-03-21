package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.PointsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PointsAccountRepository extends JpaRepository<PointsAccount, Long> {
    Optional<PointsAccount> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}