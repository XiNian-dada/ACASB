package com.leeinx.acasb.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;




@Component
public class AuthInterceptor implements HandlerInterceptor {
    private boolean enableJwt = false;
    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (!enableJwt) {
            return true;
        }
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            return true;
        }

        String authHeader = req.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            String token = authHeader.substring(7);
            if (JwtUtils.validateToken(token)) {
                return true; 
            }
        }
        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write("鉴权失败：请使用控制台打印的最新 Token");
        return false;
    }
}