package com.integral.mall.api.config;

import com.integral.mall.api.controller.AdminBusinessController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

@Component
public class AdminStatePersistInterceptor implements HandlerInterceptor {

    @Autowired
    private AdminBusinessController adminBusinessController;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        if (ex != null) return;
        if (!(handler instanceof HandlerMethod)) return;

        HandlerMethod method = (HandlerMethod) handler;
        if (!AdminBusinessController.class.isAssignableFrom(method.getBeanType())) return;

        String httpMethod = request.getMethod() == null ? "" : request.getMethod().toUpperCase(Locale.ROOT);
        if ("GET".equals(httpMethod) || "OPTIONS".equals(httpMethod) || "HEAD".equals(httpMethod)) return;

        adminBusinessController.persistStateToMysql();
    }
}

