package com.integral.mall.api.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private RequestIdInterceptor requestIdInterceptor;
    @Autowired
    private JwtAuthInterceptor jwtAuthInterceptor;
    @Autowired
    private AdminStatePersistInterceptor adminStatePersistInterceptor;
    @Autowired
    private AppStatePersistInterceptor appStatePersistInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/api/v1/admin/**", "/api/v1/app/**");
        registry.addInterceptor(requestIdInterceptor).addPathPatterns("/api/v1/app/**");
        registry.addInterceptor(adminStatePersistInterceptor).addPathPatterns("/api/v1/admin/**");
        registry.addInterceptor(appStatePersistInterceptor)
                .addPathPatterns(
                        "/api/v1/app/addresses/**",
                        "/api/v1/app/exchanges/**",
                        "/api/v1/app/orders/**"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://154.12.88.131:*",
                        "https://154.12.88.131:*",
                        "https://shangcheng.20060626.xyz",
                        "http://shangcheng.20060626.xyz"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
