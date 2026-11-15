package com.example.secondhand_project.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 客户端 IP 解析工具。
 *
 * <p>仅在服务部署于可信反向代理之后时，X-Forwarded-For 才可信；直连公网时该头可被伪造。</p>
 */
public final class IpUtil {

    private static final String UNKNOWN = "unknown";

    private static final String[] IP_HEADERS = {"X-Forwarded-For", "X-Real-IP"};

    private IpUtil() {
    }

    /**
     * 获取客户端 IP：优先 X-Forwarded-For，其次 X-Real-IP，最后 remoteAddr。
     *
     * @param request 当前请求，可为 null
     * @return 客户端 IP；无法获取时返回 "unknown"
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }
        for (String header : IP_HEADERS) {
            String value = request.getHeader(header);
            if (value == null || value.isBlank() || UNKNOWN.equalsIgnoreCase(value)) {
                continue;
            }
            int comma = value.indexOf(',');
            String first = (comma > 0 ? value.substring(0, comma) : value).trim();
            if (!first.isEmpty() && !UNKNOWN.equalsIgnoreCase(first)) {
                return first;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr == null || remoteAddr.isBlank()) ? UNKNOWN : remoteAddr;
    }
}
