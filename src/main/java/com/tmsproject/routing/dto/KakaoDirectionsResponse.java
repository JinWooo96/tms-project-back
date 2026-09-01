package com.tmsproject.routing.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 카카오 길찾기(Directions) API 응답을 매핑하는 DTO
 * 실제 응답에는 section(도로별 상세정보), fare(요금), bound(경계좌표) 등 훨씬 많은 빌드가 있지만,
 * 우리가 필요한 "summary.distance" (총거리), "summary.duration" (총소요시간)만 뽑아 쓸 것이므로
 * @JsonIgnoreProperties 로 나머지 필드는 전부 무시하도록 설정한다.
 *
 * 주의 : 카카오 길찾기 응답은 distance(미터, 정수), duration(초, 정수) 단위로 온다.
 * km/분 단위로의 변환은 이 DTO가 아니라 KakaoRoutingService(상위 계층)의 책임으로 넘긴다.
 * (지오코딩 DTO의 x/y 문자열도 마찬가지로 "원본 그대로 받고, 해석은 Service가 한다"는 패턴을 유지)
 */
public record KakaoDirectionsResponse(List<Route> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(Summary summary) {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(Integer distance, Integer duration) {

    }

}
