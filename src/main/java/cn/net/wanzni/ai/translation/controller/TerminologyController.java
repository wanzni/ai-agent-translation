package cn.net.wanzni.ai.translation.controller;

import cn.net.wanzni.ai.translation.dto.DeleteResponse;
import cn.net.wanzni.ai.translation.dto.TerminologyCreateRequest;
import cn.net.wanzni.ai.translation.dto.TerminologyListResponse;
import cn.net.wanzni.ai.translation.dto.TerminologyStatsResponse;
import cn.net.wanzni.ai.translation.dto.TerminologyUpdateRequest;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.security.UserContext;
import cn.net.wanzni.ai.translation.service.TerminologyService;
import cn.net.wanzni.ai.translation.util.UserContextUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/terminology")
@RequiredArgsConstructor
public class TerminologyController {

    private final TerminologyService terminologyService;

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
        String effectiveCreatedBy = String.valueOf(UserContext.getUserId());
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

    @PostMapping
    public TerminologyEntry create(@RequestBody TerminologyCreateRequest payload, HttpServletRequest httpRequest)
            throws Exception {
        String sourceTerm = String.valueOf(payload.getSourceTerm()).trim();
        String targetTerm = String.valueOf(payload.getTargetTerm()).trim();
        String sourceLanguage = String.valueOf(payload.getSourceLanguage()).trim();
        String targetLanguage = String.valueOf(payload.getTargetLanguage()).trim();
        String category = payload.getCategory() == null || payload.getCategory().isBlank()
                ? "GENERAL" : payload.getCategory().trim();
        String domain = payload.getDomain() == null ? "" : payload.getDomain().trim();
        String definition = payload.getDefinition() == null ? "" : payload.getDefinition().trim();
        String context = payload.getContext() == null ? "" : payload.getContext().trim();

        String createdBy = String.valueOf(UserContext.getUserId());

        return terminologyService.createTerminologyEntry(
                sourceTerm, targetTerm, sourceLanguage, targetLanguage,
                category, domain, definition, context, createdBy
        );
    }

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

    @DeleteMapping("/{id}")
    public DeleteResponse delete(@PathVariable("id") Long id) throws Exception {
        boolean ok = terminologyService.deleteTerminologyEntry(id);
        return new DeleteResponse(id, ok);
    }

    @GetMapping("/stats")
    public TerminologyStatsResponse stats(
            @RequestParam(value = "createdBy", required = false) String createdBy,
            HttpServletRequest httpRequest
    ) throws Exception {
        String effectiveCreatedBy;
        if (UserContextUtils.isValidUserId(createdBy)) {
            effectiveCreatedBy = createdBy.trim();
        } else if (UserContext.getUserId() != null) {
            effectiveCreatedBy = String.valueOf(UserContext.getUserId());
            log.debug("Terminology stats defaulting to current user ID: {}", effectiveCreatedBy);
        } else {
            String currentUserId = UserContextUtils.getUserIdFromRequest(httpRequest);
            if (UserContextUtils.isValidUserId(currentUserId)) {
                effectiveCreatedBy = currentUserId;
                log.debug("Terminology stats falling back to request user ID: {}", effectiveCreatedBy);
            } else {
                log.warn("Terminology stats request has no valid user ID");
                effectiveCreatedBy = null;
            }
        }
        return terminologyService.getTerminologyStatisticsResponse(effectiveCreatedBy);
    }
}