package com.tmsproject.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 설정.
 *
 * 브라우저는 기본적으로, 웹페이지가 자신을 서비스한 출처(origin)와 다른 출처로
 * XMLHttpRequest/fetch 요청을 보내는 것을 막는다. (보안을 위한 브라우저의 기본 정책)
 *
 * 지금 우리 상황:
 * - 프론트(Vite dev server): http://localhost:5173
 * - 백엔드(Spring Boot):     http://localhost:8081 (또는 8080)
 *
 * 포트 번호가 다르면 "다른 출처"로 취급되기 때문에, 백엔드가 명시적으로
 * "5173번 포트에서 오는 요청은 허용한다"고 알려줘야 한다. 이게 이 클래스의 역할이다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**") // /api로 시작하는 모든 요청에 대해
                .allowedOrigins("http://localhost:5173") // 이 출처(프론트 개발 서버)에서 오는 요청만 허용
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true) // 쿠키나 인증 정보를 포함한 요청도 허용할지 (로그인 기능이 생길 것을 대비)
                .maxAge(3600); // 브라우저가 이 설정(preflight 응답)을 캐싱하는 시간(초)
    }
}