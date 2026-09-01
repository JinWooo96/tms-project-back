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

    /**
     * v3 신규: 카카오 길찾기(Directions) API 전용 RestClient.
     * 지오코딩(dapi.kakao.com)과 호스트가 다르므로 별도 빈으로 분리한다.
     * API 키는 지오코딩과 동일한 카카오 REST API 키를 그대로 재사용한다.
     */
    @Bean
    public RestClient kakaoDirectionsRestClient(
            @Value("${kakao.api-key}") String apiKey,
            @Value("${kakao.directions-base-url:https://apis-navi.kakaomobility.com}") String directionsBaseUrl
    ) {
        return RestClient.builder()
                .baseUrl(directionsBaseUrl)
                .defaultHeader("Authorization", "KakaoAK " + apiKey)
                .build();
    }
}
