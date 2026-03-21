package cn.net.wanzni.ai.translation.service;

import java.util.List;
import java.util.Map;

/**
 * 术语库维护服务接口
 *
 * @version 1.0.0
 */
public interface TerminologyMaintenanceService {

    /**
     * 验证术语条目
     *
     * @param sourceTerm 源术语
     * @param targetTerm 目标术语
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @return 验证结果Map，包含是否有效、错误信息、建议等
     * @throws Exception 验证过程中的异常
     */
    Map<String, Object> validateTerminologyEntry(
            String sourceTerm,
            String targetTerm,
            String sourceLanguage,
            String targetLanguage
    ) throws Exception;

    /**
     * 合并重复术语
     *
     * @param primaryId 主术语ID
     * @param duplicateIds 重复术语ID列表
     * @return 合并结果信息
     * @throws Exception 合并过程中的异常
     */
    Map<String, Object> mergeDuplicateTerms(Long primaryId, List<Long> duplicateIds) throws Exception;

    /**
     * 更新术语使用次数
     *
     * @param id 术语条目ID
     * @return 是否成功更新
     * @throws Exception 更新过程中的异常
     */
    boolean incrementTermUsage(Long id) throws Exception;

    /**
     * 批量更新术语使用次数
     *
     * @param ids 术语条目ID列表
     * @return 成功更新的条目数量
     * @throws Exception 更新过程中的异常
     */
    int batchIncrementTermUsage(List<Long> ids) throws Exception;
}