package com.tmsproject.geocoding.client;

import com.tmsproject.geocoding.dto.KakaoGeocodingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 카카오 로컬 API(주소 검색)를 실제로 호출하는 클라이언트.
 * RestClient(Spring 6+ 내장 동기 HTTP 클라이언트) 빈은 KakaoClientConfig에서 만들어 주입받는다.
 *
 * RestClient(BaseUrl, 인증 헤더 설정)을 만드는 책임과, 실제로 API를 호출하는 책임을 분리
 * 이렇게 하면 다른 외부 API(예: v3의 카카오 길찾기)를 추가할 때도, global.config에 비슷한 패턴으로
 * RestClient 빈만 하나 더 추가하면 되는 구조가 만들어진다.
 */
@Component
public class KakaoGeocodingClient {

    private final RestClient kakaoRestClient;

    public KakaoGeocodingClient(@Qualifier("kakaoRestClient") RestClient kakaoRestClient) {
        this.kakaoRestClient = kakaoRestClient;
    }

    /**
     * 주소를 카카오 로컬 API에 전달해서 원본 응답을 그대로 반환한다.
     * 응답을 어떻게 해석할지(결과가 없을 때 처리 등)는 GeocodingService(상위 계층)의 책임으로 남긴다.
     */
    public KakaoGeocodingResponse searchAddress(String address) {
        return kakaoRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/local/search/address.json")
                        .queryParam("query", address)
                        .build())
                .retrieve()
                .body(KakaoGeocodingResponse.class);
    }
}
