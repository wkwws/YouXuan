package com.example.secondhand_project.websocketserver;

import com.example.secondhand_project.conf.JwtConf;
import com.example.secondhand_project.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务端
 *
 * 连接地址：ws://localhost:8081/ws/{userId}
 * 例如用户ID为1的连接：ws://localhost:8081/ws/1
 *
 * 职责：只做服务端→客户端的消息推送
 * 客户端→服务端的通信走 REST API
 */
@Component
@ServerEndpoint("/ws/{userId}")
@Slf4j
public class WebSocketServer {

    /** 存放所有在线用户的会话 */
   // private static Map<String, Session> sessionMap = new HashMap<>();

            //maph中 存 tcp链接 、.getBasicRemote().text(传输的信息) 、isopen()
    private static ConcurrentHashMap<String, Session> sessionMap = new ConcurrentHashMap<>();

    /** 静态注入 JwtConf（解决 @ServerEndpoint 中 @Autowired 无效的问题） */
    private static JwtConf jwtConf;

    @Autowired
    public void setJwtConf(JwtConf conf) {
        WebSocketServer.jwtConf = conf;
    }

    /**
     * 连接建立成功
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String sid) throws IOException {
        log.info("WebSocket 连接请求: userId={}", sid);

        // 从查询参数取 token：ws://localhost:8081/ws/1?token=xxx
        String query = session.getQueryString();
        String token = null;
        if (query != null && query.startsWith("token=")) {
            token = query.substring(6);
        }

        // 校验 token
        if (token == null || jwtConf == null) {
            log.warn("用户 {} 连接失败：token 为空或配置未注入", sid);
            session.close();
            return;
        }

        try {
            Claims claims = JwtUtil.parseJWT(jwtConf.getSecretKey(), token);
            Integer userId = (Integer) claims.get("id");

            if (userId == null || !userId.toString().equals(sid)) {
                log.warn("用户 {} 连接失败：token 与 userId 不匹配", sid);
                session.close();
                return;
            }

            log.info("用户 {} 已上线", sid);
            // put 返回旧连接：同 userId 之前建立的连接（重复登录/换设备时非 null，首次登录为 null）
            Session oldSession = sessionMap.put(sid, session);
            // 用 && 判断：只有"旧连接存在 且 旧连接仍打开"才关闭，防止首次登录时对 null 调用 isOpen() 抛空指针
            if (oldSession != null && oldSession.isOpen()) {
                oldSession.close();
            }
        } catch (Exception e) {
            log.warn("用户 {} 连接失败：token 解析异常 {}", sid, e.getMessage());
            session.close();
        }
    }

    /**
     * 收到客户端消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("userId") String sid) {
        log.info("收到来自用户 {} 的消息: {}", sid, message);
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose(@PathParam("userId") String sid) {
        log.info("用户 {} 已断开", sid);
        sessionMap.remove(sid);
    }

    /**
     * 发生错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        // 出错后连接大概率不可用：从在线列表移除，并关闭连接，防止死连接残留
        sessionMap.values().removeIf(s -> s.equals(session));
        try {
            session.close();
        } catch (IOException ignored) {
        }
        log.error("WebSocket 错误: {}", error.getMessage());
    }

    /**
     * 向指定用户推送消息
     */
    public void sendToClient(String sid, String message) {
        Session session = sessionMap.get(sid);

        // 用户不在线（未连接或已断开）→ 跳过推送，消息已落库，对方上线后拉取历史记录兜底
        if (session == null || !session.isOpen()) {
            log.info("用户 {} 不在线，跳过推送", sid);
            return;
        }

        // 互斥锁：同一 Session 同一时刻只允许一个线程写入，防止并发 sendText 抛 IllegalStateException
        synchronized (session) {
            try {
                session.getBasicRemote().sendText(message);
                log.info("已推送给用户 {}", sid);
            } catch (Exception e) {
                // 推送失败 = 连接不可用，从在线列表移除，防止死连接残留
                log.error("推送给用户 {} 失败: {}", sid, e.getMessage());
                sessionMap.remove(sid);
            }
        }
    }

    public boolean isOnline(String sid) {
        return sessionMap.containsKey(sid) && sessionMap.get(sid).isOpen();
    }

    public int getOnlineCount() {
        return sessionMap.size();
    }

    public Collection<String> getOnlineUsers() {
        return sessionMap.keySet();
    }
}
