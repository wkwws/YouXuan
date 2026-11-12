package com.example.secondhand_project.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.secondhand_project.admin.context.AdminContext;
import com.example.secondhand_project.admin.dto.AdminPageQuery;
import com.example.secondhand_project.admin.entity.AdminOperationLog;
import com.example.secondhand_project.admin.mapper.AdminOperationLogMapper;
import com.example.secondhand_project.admin.vo.AdminPageVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 后台操作日志：只做“记录 + 查询”，日志本身不提供修改/删除接口。
 */
@Service
public class AdminLogService extends ServiceImpl<AdminOperationLogMapper, AdminOperationLog> {

    public void record(String module, String action, String target) {
        Integer adminId = AdminContext.getId();
        if (adminId == null) {
            return;
        }
        try {
            AdminOperationLog log = new AdminOperationLog()
                    .setAdminId(adminId)
                    .setAdminName(AdminContext.getNickname().isBlank()
                            ? AdminContext.getUsername() : AdminContext.getNickname())
                    .setModule(module)
                    .setAction(action)
                    .setTarget(target == null ? "" : truncate(target, 255))
                    .setResult("成功")
                    .setIp(clientIp())
                    .setCreatedAt(LocalDateTime.now());
            this.save(log);
        } catch (Exception ignored) {
            // 日志失败不能影响主业务
        }
    }

    public AdminPageVO<Map<String, Object>> page(AdminPageQuery query) {
        int page = query.safePage();
        int pageSize = query.safePageSize();
        String keyword = query.trimmedKeyword();

        var wrapper = lambdaQuery()
                .eq(query.hasText(query.getModule()), AdminOperationLog::getModule, query.getModule())
                .and(query.hasText(keyword), w -> w
                        .like(AdminOperationLog::getAdminName, keyword).or()
                        .like(AdminOperationLog::getTarget, keyword).or()
                        .like(AdminOperationLog::getIp, keyword))
                .orderByDesc(AdminOperationLog::getCreatedAt);

        Page<AdminOperationLog> result = wrapper.page(new Page<>(page, pageSize));
        List<Map<String, Object>> list = result.getRecords().stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return AdminPageVO.of(list, result.getTotal(), page, pageSize);
    }

    private Map<String, Object> toVO(AdminOperationLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("time", log.getCreatedAt());
        map.put("admin", log.getAdminName());
        map.put("module", log.getModule());
        map.put("action", log.getAction());
        map.put("target", log.getTarget());
        map.put("result", log.getResult());
        map.put("ip", log.getIp());
        return map;
    }

    private String clientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "";
            }
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
            }
            String real = request.getHeader("X-Real-IP");
            return real == null || real.isBlank() ? request.getRemoteAddr() : real;
        } catch (Exception e) {
            return "";
        }
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
