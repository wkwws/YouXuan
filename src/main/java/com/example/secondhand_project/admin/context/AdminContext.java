package com.example.secondhand_project.admin.context;

/**
 * 当前管理员上下文。由 AdminJwtInterceptor 写入，Controller/Service 只读。
 */
public final class AdminContext {

    private static final ThreadLocal<AdminInfo> HOLDER = new ThreadLocal<>();

    private AdminContext() {
    }

    public record AdminInfo(Integer id, Integer role, String username, String nickname) {
    }

    public static void set(AdminInfo info) {
        HOLDER.set(info);
    }

    public static AdminInfo get() {
        return HOLDER.get();
    }

    public static Integer getId() {
        AdminInfo info = HOLDER.get();
        return info == null ? null : info.id();
    }

    public static Integer getRole() {
        AdminInfo info = HOLDER.get();
        return info == null ? null : info.role();
    }

    public static String getUsername() {
        AdminInfo info = HOLDER.get();
        return info == null ? "" : info.username();
    }

    public static String getNickname() {
        AdminInfo info = HOLDER.get();
        return info == null ? "" : info.nickname();
    }

    public static boolean isSuperAdmin() {
        Integer role = getRole();
        return role != null && role >= 2;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
