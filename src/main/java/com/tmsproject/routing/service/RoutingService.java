package com.tmsproject.routing.service;

import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.routing.dto.RouteResult;

/**
 * "두 지점 사이의 실제 도로 경로(거리/소요시간)"를 제공하는 퍼블릭 인터페이스.
 * dispatch 도메인은 이 인터페이스에만 의존한다.
 *
 * GeocodingService와 같은 설계 철학 : 지금은 카카오 길찾기 API를 쓰지만,
 * 나중에는 다른 경로 탐색 엔진으로 바꾸더라도 이 인터페이스의 구현체만 교체하면 되고
 * dispatch 쪽 코드는 전혀 건드릴 필요가 없다.
 */
public interface RoutingService {

    /**
     * 두 좌표 사이의 실거리 (km)/실소요시간(분)을 계산한다.
     * 계산 실패(경로를 못 찾음, 카카오 API 오류 등) 시 BusinessException(ErrorCode.ROUTE_CALCULATION_FAILED)을 던진다.
     */
    RouteResult getRoute(GeoPoint origin, GeoPoint destination);
}
