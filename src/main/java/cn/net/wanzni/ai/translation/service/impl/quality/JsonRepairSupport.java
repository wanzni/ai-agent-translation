package cn.net.wanzni.ai.translation.service.impl.quality;

import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

final class JsonRepairSupport {

    private static final String JSON_REPAIR_CLASS = "io.github.haibiiin.jsonrepair.JSONRepair";
    private static final String JSON_REPAIR_CONFIG_CLASS = "io.github.haibiiin.jsonrepair.JSONRepairConfig";
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
        String repairedByLibrary = repairWithLibrary(raw);
        if (StringUtils.hasText(repairedByLibrary)) {
            return repairedByLibrary;
        }

        return repairWithFallbackRules(raw);
    }

    private static String repairWithLibrary(String raw) {
        try {
            Class<?> repairClass = Class.forName(JSON_REPAIR_CLASS);
            Object repairInstance = createRepairInstance(repairClass);
            Method repairMethod = repairClass.getMethod("repair", String.class);
            Object result = repairMethod.invoke(repairInstance, raw);
            return result instanceof String value ? value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Object createRepairInstance(Class<?> repairClass) throws Exception {
        try {
            Class<?> configClass = Class.forName(JSON_REPAIR_CONFIG_CLASS);
            Object config = configClass.getDeclaredConstructor().newInstance();
            invokeIfPresent(configClass, config, "enableExtractJSON");
            invokeIfPresent(configClass, config, "setExtractJSON", boolean.class, true);
            return repairClass.getDeclaredConstructor(configClass).newInstance(config);
        } catch (ClassNotFoundException ignored) {
            return repairClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException ignored) {
            return repairClass.getDeclaredConstructor().newInstance();
        }
    }

    private static void invokeIfPresent(Class<?> type, Object target, String methodName, Class<?> paramType, Object arg) {
        try {
            Method method = type.getMethod(methodName, paramType);
            method.invoke(target, arg);
        } catch (Exception ignored) {
        }
    }

    private static void invokeIfPresent(Class<?> type, Object target, String methodName) {
        try {
            Method method = type.getMethod(methodName);
            method.invoke(target);
        } catch (Exception ignored) {
        }
    }

    private static String repairWithFallbackRules(String raw) {
        String repaired = raw.trim();
        repaired = repaired.replace('\u201c', '"').replace('\u201d', '"').replace('\u2018', '\'').replace('\u2019', '\'');
        repaired = SINGLE_QUOTED_PATTERN.matcher(repaired).replaceAll("\"$1\"");
        repaired = UNQUOTED_KEY_PATTERN.matcher(repaired).replaceAll("$1\"$2\"$3");
        repaired = MISSING_COMMA_BEFORE_KEY_PATTERN.matcher(repaired).replaceAll("$1, $2");
        repaired = TRAILING_COMMA_PATTERN.matcher(repaired).replaceAll("$1");
        return repaired;
    }
}
