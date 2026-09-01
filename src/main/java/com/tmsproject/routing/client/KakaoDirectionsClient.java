package com.tmsproject.routing.client;

import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.routing.dto.KakaoDirectionsResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 카카오 길찾기(Directions) API를 실제로 호출하는 클라이언트.
 * RestClient 빈은 KakaoClientConfig에서 만들어 주입받는다.
 * (KakaoGeocodingClient와 같은 패턴이지만, 지오코딩과 호스트 자체가 다르므로
 * 별도의 ResClient 빈 ("kakaoDirectionsRestClient")를 주입받는다.
 */
@Component
public class KakaoDirectionsClient {

    private final RestClient kakaoDirectionsRestClient;

    public KakaoDirectionsClient(@Qualifier("kakaoDirectionsRestClient") RestClient kakaoDirectionsRestClient) {
        this.kakaoDirectionsRestClient = kakaoDirectionsRestClient;
    }

    /**
     * 두 좌표 사이의 실제 도로 경로를 카카오 길찾기 API에 요청해서 원본 응답을 그대로 반환한다.
     * 응답을 어떻게 해석할지(단위 변환, 결과 없을 때 처리 등)는 KakaoRoutingService의 책임으로 남긴다.
     *
     * 주의: 카카오 길찾기 API는 좌표를 "경도,위도"(x,y) 순서의 문자열로 요구한다.
     * 지오코딩 응답을 파싱할 때는 "카카오가 준 x/y를 우리 lat/lng으로 옮기는" 방향이었다면,
     * 여기서는 반대로 "우리 GeoPoint(lat, lng)를 카카오가 원하는 x,y 순서 문자열로 만들어 보내는" 방향이라
     * 순서를 헷갈리지 않도록 특히 주의해야 한다. (실제로 예전에 이 순서를 반대로 넣어 버그가 났던 적이 있다.)
     */
    public KakaoDirectionsResponse getDirections(GeoPoint origin, GeoPoint destination) {
        String originParam = origin.lng() + "," + origin.lat();
        String destinationParam = destination.lng() + "," + destination.lat();

        return kakaoDirectionsRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/directions")
                        .queryParam("origin", originParam)
                        .queryParam("destination", destinationParam)
                        .queryParam("priority", "RECOMMEND")
                        .build())
                .retrieve()
                .body(KakaoDirectionsResponse.class);
    }
}
