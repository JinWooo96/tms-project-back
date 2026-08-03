package com.tmsproject.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 카카오 API 호출 전용 RestClient를 빈으로 등록한다.
 * baseUrl과 인증 헤더(Authorization: KakaoAK {키})를 여기서 한 번만 설정해두면,
 * 실제 호출부(KakaoGeocodingClient)는 요청 경로/파라미터만 신경 쓰면 된다.
 */
@Configuration
public class KakaoClientConfig {

    @Bean
    public RestClient kakaoRestClient(
            @Value("${kakao.api-key}") String apiKey,
            @Value("${kakao.base-url:https://dapi.kakao.com}") String baseUrl
    ) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }
}
