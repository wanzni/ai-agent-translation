package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.*;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.TerminologyService;
import cn.net.wanzni.ai.translation.util.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 术语库管理控制器，提供术语的增、删、改、查以及统计功能。
 */
@Slf4j
@RestController
@RequestMapping("/api/terminology")
@RequiredArgsConstructor
public class TerminologyController {

    private final TerminologyService terminologyService;

    /**
     * 分页查询术语列表，支持按关键字、语言对、分类等多种条件进行筛选。
     *
     * @param keyword        查询关键字，匹配源术语或目标术语
     * @param sourceLanguage 源语言代码 (e.g., "en")
     * @param targetLanguage 目标语言代码 (e.g., "zh")
     * @param category       术语分类
     * @param domain         术语所属领域
     * @param page           页码 (从 0 开始)
     * @param size           每页大小
     * @return {@link TerminologyListResponse} 包含术语列表和分页信息的响应
     * @throws Exception 如果查询过程中发生错误
     */
    @GetMapping
    public TerminologyListResponse list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sourceLanguage", required = false) String sourceLanguage,
            @RequestParam(value = "targetLanguage", required = false) String targetLanguage,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "domain", required = false) String domain,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) throws Exception {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        String effectiveCreatedBy = UserContext.getUserId().toString();
        Page<TerminologyEntry> result;
        if (keyword != null && !keyword.isBlank()) {
            result = terminologyService.searchTerminologyEntries(
                    keyword, sourceLanguage, targetLanguage, category, domain, effectiveCreatedBy, pageable
            );
        } else {
            result = terminologyService.getTerminologyEntries(
                    sourceLanguage, targetLanguage, category, domain, effectiveCreatedBy, pageable
            );
        }
        return TerminologyListResponse.builder()
                .data(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalPages(result.getTotalPages())
                .totalElements(result.getTotalElements())
                .build();
    }

    /**
     * 创建一个新的术语条目。
     *
     * @param payload     包含术语详细信息的请求体
     * @param httpRequest HTTP请求对象，用于在需要时获取用户信息
     * @return {@link TerminologyEntry} 创建成功后的术语对象
     * @throws Exception 如果创建过程中发生错误
     */
    @PostMapping
    public TerminologyEntry create(@RequestBody TerminologyCreateRequest payload, HttpServletRequest httpRequest) throws Exception {
        String sourceTerm = String.valueOf(payload.getSourceTerm()).trim();
        String targetTerm = String.valueOf(payload.getTargetTerm()).trim();
        String sourceLanguage = String.valueOf(payload.getSourceLanguage()).trim();
        String targetLanguage = String.valueOf(payload.getTargetLanguage()).trim();
        String category = payload.getCategory() == null || payload.getCategory().isBlank()
                ? "GENERAL" : payload.getCategory().trim();
        String domain = payload.getDomain() == null ? "" : payload.getDomain().trim();
        String definition = payload.getDefinition() == null ? "" : payload.getDefinition().trim();
        String context = payload.getContext() == null ? "" : payload.getContext().trim();

        String createdBy = UserContext.getUserId().toString();

        return terminologyService.createTerminologyEntry(
                sourceTerm, targetTerm, sourceLanguage, targetLanguage,
                category, domain, definition, context, createdBy
        );
    }

    /**
     * 更新一个已存在的术语条目。
     *
     * @param id      待更新术语的唯一标识符
     * @param payload 包含更新后术语信息的请求体
     * @return {@link TerminologyEntry} 更新成功后的术语对象
     * @throws Exception 如果更新过程中发生错误
     */
    @PutMapping("/{id}")
    public TerminologyEntry update(@PathVariable("id") Long id,
                                   @RequestBody TerminologyUpdateRequest payload) throws Exception {
        String sourceTerm = String.valueOf(payload.getSourceTerm()).trim();
        String targetTerm = String.valueOf(payload.getTargetTerm()).trim();
        String category = payload.getCategory() == null || payload.getCategory().isBlank()
                ? "GENERAL" : payload.getCategory().trim();
        String domain = payload.getDomain() == null ? "" : payload.getDomain().trim();
        String definition = payload.getDefinition() == null ? "" : payload.getDefinition().trim();

        return terminologyService.updateTerminologyEntry(
                id, sourceTerm, targetTerm, category, domain, definition, ""
        );
    }

    /**
     * 删除一个术语条目。
     *
     * @param id 待删除术语的唯一标识符
     * @return {@link DeleteResponse} 包含删除操作结果的响应
     * @throws Exception 如果删除过程中发生错误
     */
    @DeleteMapping("/{id}")
    public DeleteResponse delete(@PathVariable("id") Long id) throws Exception {
        boolean ok = terminologyService.deleteTerminologyEntry(id);
        return new DeleteResponse(id, ok);
    }

    /**
     * 获取术语库的统计信息，如总条数、按分类和语言对的分布情况。
     *
     * @param createdBy 可选参数，用于筛选特定用户创建的术语统计信息
     * @return {@link TerminologyStatsResponse} 包含统计数据的响应
     * @throws Exception 如果统计过程中发生错误
     */
    @GetMapping("/stats")
    public TerminologyStatsResponse stats(
            @RequestParam(value = "createdBy", required = false) String createdBy,
            HttpServletRequest httpRequest
    ) throws Exception {
        String effectiveCreatedBy;
        if (UserContextUtils.isValidUserId(createdBy)) {
            effectiveCreatedBy = createdBy.trim();
        } else {
            String currentUserId = UserContextUtils.getUserIdFromRequest(httpRequest);
            if (UserContextUtils.isValidUserId(currentUserId)) {
                effectiveCreatedBy = currentUserId;
                log.debug("术语统计默认使用当前登录用户ID: {}", effectiveCreatedBy);
            } else {
                // 无有效用户ID则按空用户处理（由服务层决定返回内容），不再返回全局统计
                log.warn("术语统计未提供有效的用户ID，按空用户处理");
                effectiveCreatedBy = null;
            }
        }
        return terminologyService.getTerminologyStatisticsResponse(effectiveCreatedBy);
    }
}