package com.tmsproject.dispatch.optimization.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * RouteOptimizer가 다루는 "방문 지점" 하나.
 * 노드 배열의 0번은 항상 DEPOT(차고지)이고, 그 뒤로 각 주문마다 PICKUP/DELIVERY가 하나씩 붙는다.
 *
 * weightKg는 "이 노드를 지날 때 짐칸 무게가 얼마나 바뀌는가"를 의미한다.
 * PICKUP은 +weight, DELIVERY는 -weight, DEPOT은 0 — Capacity Dimension이 그대로 누적해서 쓸 값이다.
 *
 * RouteOptimizer도 TransportOrder/Vehicle 엔티티를 직접 참조하지 않고, RouteNode 리스트만 받아서 계산하는 구조로 간다.
 */
public record RouteNode(
        NodeType type,
        Long orderId,             // PICKUP/DELIVERY일 때만 값 존재, DEPOT은 null
        Double lat,
        Double lng,
        BigDecimal weightKg,       // PICKUP: +무게, DELIVERY: -무게, DEPOT: 0
        LocalDateTime windowStart, // 시간창 시작. 없으면 null (시간 제약 없음)
        LocalDateTime windowEnd
) {
    public enum NodeType { DEPOT, PICKUP, DELIVERY }
}
