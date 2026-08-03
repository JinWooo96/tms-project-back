package com.tmsproject.global.util;

/**
 * 두 지점(위도/경도) 간의 직선거리를 계산하는 유틸리티.
 * Haversine 공식을 사용한다 — 지구를 완전한 구(sphere)로 가정하고,
 * 위경도 좌표만으로 두 지점 사이의 "대권거리(great-circle distance)"를 계산하는 방법이다.
 *
 * DB나 엔티티에 의존하지 않는 순수 함수이므로 어디서든 자유롭게 재사용할 수 있고,
 * 단위 테스트도 스프링 컨텍스트 없이 바로 짤 수 있다.
 *
 * v3(경로 최적화)에서는 이 직선거리 대신 카카오 길찾기 API의 실거리로 대체될 예정이다.
 * (요구사항 정의서 v2, 2.5절 참고)
 */
public class DistanceCalculator {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private DistanceCalculator() {
        // 유틸리티 클래스이므로 인스턴스를 만들 필요 없다. 생성자를 막아 실수로 new 하는 걸 방지
    }

    /**
     * 두 좌표 간 직선거리를 km 단위로 반환
     */
    public static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }


}
