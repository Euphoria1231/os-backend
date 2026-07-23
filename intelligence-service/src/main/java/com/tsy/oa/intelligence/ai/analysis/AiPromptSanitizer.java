package com.tsy.oa.intelligence.ai.analysis;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class AiPromptSanitizer {

    private static final int MAX_REASON_LENGTH = 800;
    private static final String REDACTED = "[REDACTED]";
    private static final List<Pattern> SENSITIVE_PATTERNS = List.of(
            Pattern.compile("(?i)bearer\\s+[a-z0-9._-]+"),
            Pattern.compile("(?i)\\b(token|password|passwd|api[-_ ]?key|secret)\\s*[:=]\\s*\\S+"),
            Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)"),
            Pattern.compile("(?i)(?<![a-z0-9])\\d{17}[0-9x](?![a-z0-9])"),
            Pattern.compile("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}")
    );

    public String sanitizeApprovalReason(String reason) {
        String sanitized = reason == null ? "" : reason;
        for (Pattern pattern : SENSITIVE_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll(REDACTED);
        }
        return sanitized.length() <= MAX_REASON_LENGTH ? sanitized : sanitized.substring(0, MAX_REASON_LENGTH);
    }
}
