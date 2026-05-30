package com.example.secondhand_project.interceptor;

import com.example.secondhand_project.conf.JwtConf;
import com.example.secondhand_project.context.BaseContext;
import com.example.secondhand_project.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aopalliance.intercept.Interceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class interceptor implements HandlerInterceptor {


    @Autowired
    private JwtConf jwtConf;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //先从请求头中获取请求头
        String token = request.getHeader(jwtConf.getUserTokenName());

        if(token != null&&token.startsWith("Bearer ")){
          token = token.substring(7);
        }
        //从请求头中获取用户信息
        Claims claims = JwtUtil.parseJWT(jwtConf.getSecretKey(), token);
        //无法获取则返回401
        if(claims==null){
            response.setStatus(401);
        }

        //将用户信息存到Threadlocal中
        BaseContext.setCurrentId(claims.get("id"));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       //结束后关闭Threadlocal
        BaseContext.removeCurrentId();
    }
}
