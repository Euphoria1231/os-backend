package com.tsy.oa.user.log.service;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SensitiveLogTextSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final Pattern SENSITIVE_KEY_VALUE = Pattern.compile(
            "(?i)(password|passwd|pwd|token|authorization|api[_-]?key|secret|jwt|"
                    + "database[_-]?password)\\s*[:=]\\s*(?:Bearer\\s+)?[^\\s,;]+"
    );
    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)Bearer\\s+[A-Za-z0-9._~+/=-]+"
    );
    private static final Pattern WINDOWS_ABSOLUTE_PATH = Pattern.compile(
            "(?i)[A-Z]:\\\\(?:[^\\s\\\\]+\\\\)*[^\\s\\\\]+"
    );

    public String sanitize(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        String withoutSensitiveValues = replaceSensitiveValues(normalized);
        String withoutBearerTokens = BEARER_TOKEN.matcher(withoutSensitiveValues)
                .replaceAll("Bearer " + REDACTED);
        String withoutPaths = WINDOWS_ABSOLUTE_PATH.matcher(withoutBearerTokens)
                .replaceAll("[PATH]");
        if (withoutPaths.length() <= maxLength) {
            return withoutPaths;
        }
        return withoutPaths.substring(0, maxLength);
    }

    private String replaceSensitiveValues(String value) {
        Matcher matcher = SENSITIVE_KEY_VALUE.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(matcher.group(1) + "=" + REDACTED)
            );
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
