package com.tmsproject.geocoding.dto;

/**
 * 지오코딩 결과(위도/경도)를 담는 불변 값 객체.
 * record이므로 생성 후 값이 절대 바뀌지 않는다. (좌표값을 실수로 변경하는 걸 원천 차단)
 * 결과를 담아 잠깐 옮기는 값 = 값 객체 (Value Object) -> record가 어울림
 */
public record GeoPoint(Double lat, Double lng) {
}
