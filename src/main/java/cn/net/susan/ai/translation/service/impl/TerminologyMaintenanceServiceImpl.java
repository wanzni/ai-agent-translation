package cn.net.susan.ai.translation.service.impl;

import cn.net.susan.ai.translation.entity.TerminologyEntry;
import cn.net.susan.ai.translation.repository.TerminologyEntryRepository;
import cn.net.susan.ai.translation.service.TerminologyMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 术语库数据维护服务实现类
 *
 * @author sushan
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TerminologyMaintenanceServiceImpl implements TerminologyMaintenanceService {

    private final TerminologyEntryRepository terminologyEntryRepository;

    @Override
    public Map<String, Object> validateTerminologyEntry(String sourceTerm, String targetTerm, String sourceLanguage, String targetLanguage) throws Exception {
        try {
            log.info("验证术语条目: {}", sourceTerm);

            Map<String, Object> result = new HashMap<>();
            result.put("valid", true);
            result.put("message", "验证功能待实现");

            return result;
        } catch (Exception e) {
            log.error("验证术语条目失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Map<String, Object> mergeDuplicateTerms(Long primaryId, List<Long> duplicateIds) throws Exception {
        try {
            log.info("合并重复术语: primaryId={}", primaryId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "合并功能待实现");

            return result;
        } catch (Exception e) {
            log.error("合并重复术语失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public boolean incrementTermUsage(Long id) throws Exception {
        try {
            log.info("更新术语使用次数: {}", id);

            Optional<TerminologyEntry> entry = terminologyEntryRepository.findById(id);
            if (entry.isPresent()) {
                TerminologyEntry terminologyEntry = entry.get();
                terminologyEntry.incrementUsageCount();
                terminologyEntryRepository.save(terminologyEntry);
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("更新术语使用次数失败: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public int batchIncrementTermUsage(List<Long> ids) throws Exception {
        try {
            log.info("批量更新术语使用次数，数量: {}", ids.size());

            int updatedCount = 0;
            for (Long id : ids) {
                if (incrementTermUsage(id)) {
                    updatedCount++;
                }
            }

            return updatedCount;
        } catch (Exception e) {
            log.error("批量更新术语使用次数失败: {}", e.getMessage());
            throw e;
        }
    }
}