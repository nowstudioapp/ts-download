package com.ts.download.interceptor;

import com.alibaba.fastjson2.JSON;
import com.ts.download.annotation.RequireAdmin;
import com.ts.download.domain.vo.R;
import com.ts.download.domain.vo.UserVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

@Slf4j
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_USER_KEY = "currentUser";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SESSION_USER_KEY) == null) {
            writeUnauthorized(response, "未登录，请先登录");
            return false;
        }

        UserVO user = (UserVO) session.getAttribute(SESSION_USER_KEY);

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            RequireAdmin requireAdmin = handlerMethod.getMethodAnnotation(RequireAdmin.class);
            if (requireAdmin != null && !"admin".equals(user.getRole()) && !"superAdmin".equals(user.getRole())) {
                writeForbidden(response, "权限不足，需要管理员权限");
                return false;
            }
        }

        request.setAttribute("currentUser", user);
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(R.fail(message)));
    }

    private void writeForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(R.fail(403, message)));
    }
}
