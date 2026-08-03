package com.tmsproject.geocoding.service;

import com.tmsproject.geocoding.dto.GeoPoint;

/**
 * "주소 -> 좌표 변환"이라는 기능을 제공하는 퍼블릭 인터페이스.
 * order, vehicle 도메인은 이 인터페이스에만 의존한다.
 *
 * 인터페이스로 분리해둔 이유: 지금은 카카오 API를 쓰지만,
 * 나중에 다른 지도 사업자(네이버 등)로 바꾸거나 여러 개를 조합해야 하는 상황이 오더라도,
 * 이 인터페이스의 구현체만 교체하면 되고 order/vehicle 쪽 코드는 전혀 건드릴 필요가 없다.
 */
public interface GeocodingService {
    /**
     * 주소를 좌표로 변환한다.
     * 변환 실패(주소를 못 찾음) 시 BusinessException(ErrorCode.GEOCODING_FAILED)을 던진다.
     */
    GeoPoint geocode(String address);
}
