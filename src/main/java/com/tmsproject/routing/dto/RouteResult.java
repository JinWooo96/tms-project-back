package com.tmsproject.routing.dto;

/**
 * 카카오 길찾기 결과(실거리, 실소요시간)을 담는 불변 값 객체
 * GeoPoint와 같은 설계 철학 - record이므로 생성 후 값이 절대 바뀌지 않는다.
 */
public record RouteResult(double distanceKm, int durationMin) {
}
