package com.tmsproject.dispatch.dto.request;

import java.util.List;

/**
 * 자동 배차 추천 요청.
 * orderIds가 없거나 비어있으면 "미배차 주문 전체"를 대상으로 계산한다 (전체 추천).
 * orderIds가 있으면 그 주문들만 대상으로 계산한다 (선택 추천).
 *
 * 요청 바디 자체를 아예 생략해도 되므로(@RequestBody required = false),
 * 프론트는 "전체 추천" 버튼은 바디 없이, "선택 추천" 버튼은 이 DTO를 채워서 호출한다.
 */
public record DispatchRecommendationRequest(List<Long> orderIds) {
}
