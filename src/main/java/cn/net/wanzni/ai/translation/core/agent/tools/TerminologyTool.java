package cn.net.wanzni.ai.translation.core.agent.tools;

import cn.net.wanzni.ai.translation.core.agent.Tool;
import cn.net.wanzni.ai.translation.entity.TerminologyEntry;
import cn.net.wanzni.ai.translation.repository.TerminologyEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TerminologyTool implements Tool {

    private final TerminologyEntryRepository terminologyEntryRepository;

    @Override
    public String getName() {
        return "terminology_search";
    }

    @Override
    public String getDescription() {
        return "Search for terminology translations in the terminology database. " +
                "Returns matching terms with source and target translations.";
    }

    @Override
    public Map<String, ToolParam> getParams() {
        Map<String, ToolParam> params = new HashMap<>();
        params.put("text", new ToolParam("text", "string", "Text to search terminology for", true));
        params.put("sourceLang", new ToolParam("sourceLang", "string", "Source language code", false));
        params.put("targetLang", new ToolParam("targetLang", "string", "Target language code", false));
        params.put("domain", new ToolParam("domain", "string", "Domain field", false));
        return params;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) throws Exception {
        String text = (String) params.get("text");
        String sourceLang = (String) params.getOrDefault("sourceLang", "auto");
        String targetLang = (String) params.getOrDefault("targetLang", "en");
        String domain = (String) params.getOrDefault("domain", null);

        if (text == null || text.isBlank()) {
            return ToolResult.fail("Text parameter is required");
        }

        List<TerminologyEntry> entries = terminologyEntryRepository
                .findBySourceTermsAndLanguagePair(List.of(text), sourceLang, targetLang);

        if (entries.isEmpty() && "auto".equals(sourceLang)) {
            entries = terminologyEntryRepository
                    .findBySourceLanguageAndTargetLanguageAndIsActiveTrue(sourceLang, targetLang, PageRequest.of(0, 50))
                    .getContent();
        }

        StringBuilder result = new StringBuilder();
        for (TerminologyEntry entry : entries) {
            result.append(entry.getSourceTerm())
                    .append(" -> ")
                    .append(entry.getTargetTerm());
            if (entry.getDomain() != null) {
                result.append(" [").append(entry.getDomain()).append("]");
            }
            result.append("\n");
        }

        return ToolResult.ok(result.length() > 0 ? result.toString() : "No terminology found");
    }
}