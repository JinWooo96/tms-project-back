package com.tmsproject.dispatch.optimization.dto;

/**
 * "이 주문을 이 차량에 배정하면 거리가 얼마인가"를 나타내는 후보 조합.
 * DispatchOptimizer는 엔티티(TransportOrder, Vehicle)를 전혀 몰라야 하므로,
 * 순수 데이터만 담은 이 타입으로만 소통한다. (백엔드 아키텍처 문서 v2, 4.1절 참고)
 *
 * 용량 초과 등으로 애초에 불가능한 조합은 이 리스트에 아예 포함시키지 않는다
 * (DispatchService가 canCarry()로 미리 걸러낸 것만 넘어온다).
 */
public record AssignmentCandidate(Long orderId, Long vehicleId, double distanceKm) {
}
