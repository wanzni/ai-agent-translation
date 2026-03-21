package cn.net.wanzni.ai.translation.repository;

import cn.net.wanzni.ai.translation.entity.UserMembership;
import cn.net.wanzni.ai.translation.enums.MembershipStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserMembershipRepository extends JpaRepository<UserMembership, Long> {
    Optional<UserMembership> findFirstByUserIdAndStatusOrderByEndAtDesc(Long userId, MembershipStatusEnum status);
    Optional<UserMembership> findTopByUserIdOrderByEndAtDesc(Long userId);
    boolean existsByUserIdAndStatusAndEndAtAfter(Long userId, MembershipStatusEnum status, LocalDateTime now);
}