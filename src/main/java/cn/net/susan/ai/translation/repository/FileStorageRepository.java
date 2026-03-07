package cn.net.susan.ai.translation.repository;

import cn.net.susan.ai.translation.entity.FileStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 文件存储持久层接口
 */
@Repository
public interface FileStorageRepository extends JpaRepository<FileStorage, Long> {
}