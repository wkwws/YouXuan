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

        // GET 请求访问 /post 路径不需要登录
        if (request.getRequestURI().startsWith("/posts") && "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (request.getRequestURI().startsWith("/products") && "GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        //先从请求头中获取请求头
        String token = request.getHeader(jwtConf.getUserTokenName());
        if(token==null||token.isEmpty()){
            response.setStatus(401);
            return false;
        }

        if(token != null&&token.startsWith("Bearer ")){
            //由于这里是用更加官方的书写方式，所以从请求头之中获取到的token是带头部标识的，token的头部标识是Bearer,
            //所以再检验的时候需要先截取一下
          token = token.substring(7);
        }
        //从请求头中获取用户信息
        Claims claims = JwtUtil.parseJWT(jwtConf.getSecretKey(), token);
        //无法获取则返回401
        if(claims==null){
            response.setStatus(401);
        }

        //将用户信息存到Threadlocal中
        //这里之所以使用这样的转换方式是因为claims.get("id")是Integer,与Long为平级关系所以应该先转换成Number大类，然后再转换成Long
        BaseContext.setCurrentId(((Number)claims.get("id")).longValue());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
       //结束后关闭Threadlocal
        BaseContext.removeCurrentId();
    }
}
