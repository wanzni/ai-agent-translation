package cn.net.wanzni.ai.translation.service.impl.quality;

import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

final class JsonRepairSupport {

    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",\\s*([}\\]])");
    private static final Pattern UNQUOTED_KEY_PATTERN = Pattern.compile("([\\{,]\\s*)([A-Za-z_][A-Za-z0-9_]*)(\\s*:)");
    private static final Pattern SINGLE_QUOTED_PATTERN = Pattern.compile("'([^'\\\\]*(?:\\\\.[^'\\\\]*)*)'");
    private static final Pattern MISSING_COMMA_BEFORE_KEY_PATTERN = Pattern.compile("([\\d\\}\"\\]])\\s*(\"[A-Za-z_][A-Za-z0-9_]*\"\\s*:)");

    private JsonRepairSupport() {
    }

    static String repair(String raw) {
        if (!StringUtils.hasText(raw)) {
            return raw;
        }
        String repaired = raw.trim();
        repaired = repaired.replace('\u201c', '"').replace('\u201d', '"').replace('\u2018', '\'').replace('\u2019', '\'');
        repaired = SINGLE_QUOTED_PATTERN.matcher(repaired).replaceAll("\"$1\"");
        repaired = UNQUOTED_KEY_PATTERN.matcher(repaired).replaceAll("$1\"$2\"$3");
        repaired = MISSING_COMMA_BEFORE_KEY_PATTERN.matcher(repaired).replaceAll("$1, $2");
        repaired = TRAILING_COMMA_PATTERN.matcher(repaired).replaceAll("$1");
        return repaired;
    }
}
