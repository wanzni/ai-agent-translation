package cn.net.wanzni.ai.translation.service.impl;

import cn.net.wanzni.ai.translation.config.DashscopeProperties;
import cn.net.wanzni.ai.translation.dto.PolishRequest;
import cn.net.wanzni.ai.translation.dto.PolishResponse;
import cn.net.wanzni.ai.translation.dto.PolishedTermHit;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.service.PolishService;
import cn.net.wanzni.ai.translation.service.TerminologyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpStatusCode;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 润色服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolishServiceImpl implements PolishService {

    private final TerminologyService terminologyService;
    private final DashscopeProperties properties;
    private final WebClient webClient = WebClient.builder().build();

    /**
     * 润色方法
     * @param request 润色请求，包含待润色文本、术语库、风格提示等
     * @return
     * @throws Exception
     */
    @Override
    public PolishResponse polish(PolishRequest request) throws Exception {
        if (!StringUtils.hasText(request.getMtText())) {
            return PolishResponse.builder()
                    .polishedText("")
                    .appliedTermsCount(0)
                    .termHits(Collections.emptyList())
                    .qualityScore(0.0)
                    .errorMessage("mtText 不能为空")
                    .build();
        }

        // 获取术语库（按用户/语言对/分类/领域过滤）
        List<TerminologyEntry> glossary = fetchGlossary(
                request.getSourceLanguage(),
                request.getTargetLanguage(),
                request.getCategory(),
                request.getDomain(),
                request.getUserId()
        );

        // 构造提示
        String sys = buildSystemPrompt();
        String usr = buildUserPrompt(request, glossary);

        String polished = callDashscope(sys, usr);
        if (!StringUtils.hasText(polished)) {
            polished = request.getMtText();
        }

        // 统计术语命中（简单按目标术语计数）
        List<PolishedTermHit> hits = new ArrayList<>();
        int totalHits = 0;
        for (TerminologyEntry e : glossary) {
            String term = safe(e.getTargetTerm());
            if (!StringUtils.hasText(term)) continue;
            int c = countOccurrences(polished, term);
            if (c > 0) {
                hits.add(PolishedTermHit.builder().term(term).count(c).build());
                totalHits += c;
            }
        }

        // 质量分（简易）：按术语命中与文本长度给出一个稳健分数
        double score = Math.min(100.0, 70.0 + Math.min(30.0, totalHits * 2.0));

        return PolishResponse.builder()
                .polishedText(polished)
                .termHits(hits)
                .appliedTermsCount(totalHits)
                .qualityScore(score)
                .build();
    }

    /**
     * 获取术语库。
     *
     * @param sourceLanguage 源语言
     * @param targetLanguage 目标语言
     * @param category 分类
     * @param domain 领域
     * @param userId 用户ID
     * @return 术语库条目列表
     */
    private List<TerminologyEntry> fetchGlossary(String sourceLanguage, String targetLanguage,
                                                 String category, String domain, String userId) {
        try {
            var page = terminologyService.getTerminologyEntries(
                    sourceLanguage, targetLanguage,
                    category, domain, userId,
                    org.springframework.data.domain.Pageable.unpaged()
            );
            // 最多取前 100 条
            return page.getContent().stream()
                    .filter(Objects::nonNull)
                    .limit(100)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("获取术语库失败，使用空术语库: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 构建系统提示。
     *
     * @return 系统提示字符串
     */
    private String buildSystemPrompt() {
        return "你是专业的译后编辑助理。按照术语库和风格要求，对机器翻译文本进行润色。" +
                "必须保留原有含义，提升可读性与专业性，不要添加解释。" +
                "仅输出润色后的目标语文本。";
    }

    /**
     * 构建用户提示。
     *
     * @param req 润色请求
     * @param glossary 术语库
     * @return 用户提示字符串
     */
    private String buildUserPrompt(PolishRequest req, List<TerminologyEntry> glossary) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(req.getSourceText())) {
            sb.append("原文:\n").append(req.getSourceText()).append("\n\n");
        }
        sb.append("机器翻译结果(需润色):\n").append(req.getMtText()).append("\n\n");

        if (!glossary.isEmpty()) {
            sb.append("术语约束(如出现相关表达，统一使用右侧目标术语):\n");
            for (TerminologyEntry e : glossary) {
                if (StringUtils.hasText(e.getSourceTerm()) && StringUtils.hasText(e.getTargetTerm())) {
                    sb.append("- ").append(e.getSourceTerm()).append(" => ").append(e.getTargetTerm()).append("\n");
                }
            }
            sb.append("\n");
        }

        if (StringUtils.hasText(req.getStyle())) {
            sb.append("风格要求:\n").append(req.getStyle()).append("\n\n");
        } else {
            sb.append("风格要求:\n").append("用词专业、句子简洁、标点规范，避免口语化。\n\n");
        }

        sb.append("输出要求: 仅返回润色后的目标语文本，不要额外说明。");
        return sb.toString();
    }

    /**
     * 调用 DashScope API。
     *
     * @param systemPrompt 系统提示
     * @param userPrompt 用户提示
     * @return API 返回的文本
     */
    private String callDashscope(String systemPrompt, String userPrompt) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("DashScope API Key 未配置，返回原文保底");
            return null;
        }

        String url = properties.getBaseUrl();
        Map<String, Object> body = new HashMap<>();
        body.put("model", properties.resolveModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (properties.getTemperature() != null) {
            body.put("temperature", properties.getTemperature());
        }

        try {
            Map<String, Object> resp = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (ClientResponse r) ->
                            r.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("DashScope 4xx 响应(后编辑): status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 4xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .onStatus(HttpStatusCode::is5xxServerError, (ClientResponse r) ->
                            r.bodyToMono(String.class).flatMap(errBody -> {
                                log.error("DashScope 5xx 响应(后编辑): status={}, body={}", r.statusCode(), errBody);
                                return Mono.error(new RuntimeException("DashScope 5xx: " + r.statusCode() + " - " + errBody));
                            }))
                    .bodyToMono(Map.class)
                    .block();

            return extractText(resp);
        } catch (Exception e) {
            log.error("后编辑调用失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 API 响应中提取文本。
     *
     * @param resp API 响应
     * @return 提取的文本
     */
    @SuppressWarnings("unchecked")
    private String extractText(Map<String, Object> resp) {
        if (resp == null) return null;
        try {
            List<Map<String, Object>> choices = (List<Map<String, Object>>) resp.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> first = choices.get(0);
                Map<String, Object> message = (Map<String, Object>) first.get("message");
                Object content = message != null ? message.get("content") : null;
                return content != null ? String.valueOf(content) : null;
            }
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * 计算子字符串在文本中出现的次数。
     *
     * @param text 文本
     * @param find 子字符串
     * @return 出现次数
     */
    private int countOccurrences(String text, String find) {
        if (text == null || find == null || text.isEmpty() || find.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(find, idx)) != -1) {
            count++;
            idx += find.length();
        }
        return count;
    }

    /**
     * 安全地转换对象为字符串，避免 null。
     *
     * @param obj 对象
     * @return 字符串
     */
    private String safe(Object obj) {
        return obj == null ? "" : String.valueOf(obj).trim();
    }
}