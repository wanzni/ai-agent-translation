package cn.net.susan.ai.translation.repository;

import cn.net.susan.ai.translation.entity.PointsTransaction;
import cn.net.susan.ai.translation.enums.TransactionTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointsTransactionRepository extends JpaRepository<PointsTransaction, Long> {
    @Query("select t from PointsTransaction t where t.userId = :userId and t.type = :type")
    List<PointsTransaction> findByUserIdAndType(@Param("userId") Long userId, @Param("type") TransactionTypeEnum type);

    boolean existsByUserIdAndReferenceIdAndTypeAndDelta(Long userId, String referenceId, TransactionTypeEnum type, Long delta);
}