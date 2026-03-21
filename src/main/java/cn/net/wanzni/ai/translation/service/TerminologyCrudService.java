package cn.net.wanzni.ai.translation.service;

import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 术语库 CRUD 服务接口，提供术语的增删改查功能。
 */
public interface TerminologyCrudService {

    /**
     * 创建一个术语条目。
     *
     * @param sourceTerm 源术语
     * @param targetTerm 目标术语
     * @param sourceLanguage 源语言代码
     * @param targetLanguage 目标语言代码
     * @param category 术语分类
     * @param domain 领域标签
     * @param definition 术语定义
     * @param context 使用上下文
     * @param createdBy 创建者ID
     * @return 创建的术语条目
     * @throws Exception 创建过程中的异常
     */
    TerminologyEntry createTerminologyEntry(
            String sourceTerm,
            String targetTerm,
            String sourceLanguage,
            String targetLanguage,
            String category,
            String domain,
            String definition,
            String context,
            String createdBy
    ) throws Exception;

    /**
     * 批量创建多个术语条目。
     *
     * @param entries 术语条目列表
     * @return 成功创建的术语条目列表
     * @throws Exception 创建过程中的异常
     */
    List<TerminologyEntry> batchCreateTerminologyEntries(List<TerminologyEntry> entries) throws Exception;

    /**
     * 根据条件分页获取术语条目列表。
     *
     * @param sourceLanguage 源语言（可选）
     * @param targetLanguage 目标语言（可选）
     * @param category 分类（可选）
     * @param domain 领域（可选）
     * @param createdBy 创建者（可选）
     * @param pageable 分页参数
     * @return 术语条目分页列表
     * @throws Exception 查询过程中的异常
     */
    Page<TerminologyEntry> getTerminologyEntries(
            String sourceLanguage,
            String targetLanguage,
            String category,
            String domain,
            String createdBy,
            Pageable pageable
    ) throws Exception;

    /**
     * 根据 ID 获取单个术语条目。
     *
     * @param id 术语条目ID
     * @return 术语条目详情，不存在则返回null
     * @throws Exception 查询过程中的异常
     */
    TerminologyEntry getTerminologyEntryById(Long id) throws Exception;

    /**
     * 更新一个现有的术语条目。
     *
     * @param id 术语条目ID
     * @param sourceTerm 源术语
     * @param targetTerm 目标术语
     * @param category 术语分类
     * @param domain 领域标签
     * @param definition 术语定义
     * @param context 使用上下文
     * @return 更新后的术语条目
     * @throws Exception 更新过程中的异常
     */
    TerminologyEntry updateTerminologyEntry(
            Long id,
            String sourceTerm,
            String targetTerm,
            String category,
            String domain,
            String definition,
            String context
    ) throws Exception;

    /**
     * 根据 ID 删除一个术语条目。
     *
     * @param id 术语条目ID
     * @return 是否成功删除
     * @throws Exception 删除过程中的异常
     */
    boolean deleteTerminologyEntry(Long id) throws Exception;

    /**
     * 批量删除多个术语条目。
     *
     * @param ids 术语条目ID列表
     * @return 成功删除的条目数量
     * @throws Exception 删除过程中的异常
     */
    int batchDeleteTerminologyEntries(List<Long> ids) throws Exception;
}