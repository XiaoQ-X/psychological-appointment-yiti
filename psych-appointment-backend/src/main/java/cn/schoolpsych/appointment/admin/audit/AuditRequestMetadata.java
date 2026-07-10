package cn.schoolpsych.appointment.admin.audit;

import jakarta.servlet.http.HttpServletRequest;

public record AuditRequestMetadata(String ip, String userAgent) {

    private static final int MAX_IP_LENGTH = 64;
    private static final int MAX_USER_AGENT_LENGTH = 500;

    public static AuditRequestMetadata from(HttpServletRequest request) {
        return new AuditRequestMetadata(
                truncate(request.getRemoteAddr(), MAX_IP_LENGTH),
                truncate(request.getHeader("User-Agent"), MAX_USER_AGENT_LENGTH));
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
