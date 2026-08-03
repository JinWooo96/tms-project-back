package com.tmsproject.dispatch.repository;

import com.tmsproject.dispatch.entity.Dispatch;
import com.tmsproject.dispatch.entity.DispatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    // 특정 주문의 "현재 유효한" 배차 조회 (CONFIRMED 상태인 것 하나).
    // 배차 확정 전 "이미 배차된 주문인지" 검증할 때 사용.
    Optional<Dispatch> findByTransportOrderIdAndDispatchStatus(Long transportOrderId, DispatchStatus dispatchStatus);

    /**
     * 배차 단건 조회용 fetch join 쿼리 (transportOrder, vehicle까지 함께 로딩).
     *
     * 이유: Dispatch.cancel() 내부에서 transportOrder.revertToReceived(),
     * vehicle.markAsAvailable()처럼 연관 엔티티의 "메서드"를 호출한다.
     * 단순 findById()로 조회하면 transportOrder/vehicle이 초기화되지 않은
     * Hibernate 프록시 상태로 남아있는데, 프록시의 메서드를 호출하는 순간
     * 지연 로딩이 트리거되면서 LazyInitializationException이 발생할 수 있다.
     * (트랜잭션이 여전히 열려있어도, 프록시 구현 방식에 따라 이 문제가 발생할 수 있으므로
     *  애초에 프록시가 아닌 완전히 로딩된 실제 엔티티를 갖고 시작하는 게 안전하다.)
     */
    @Query("""
            SELECT d FROM Dispatch d
            JOIN FETCH d.transportOrder
            JOIN FETCH d.vehicle
            WHERE d.id = :id
            """)
    Optional<Dispatch> findByIdWithOrderAndVehicle(@Param("id") Long id);

    /**
     * 배차 목록 조회용 fetch join 쿼리.
     *
     * 이유: DispatchListItemResponse는 order.orderNo, vehicle.vehicleNo처럼
     * 연관 엔티티의 필드가 필요하다. transportOrder/vehicle은 LAZY 로딩인데,
     * open-in-view: false 설정 때문에 Service 트랜잭션이 끝난 뒤 Controller에서
     * 이 필드에 접근하면 LazyInitializationException이 발생한다.
     * JOIN FETCH로 한 번의 쿼리에서 연관 엔티티까지 미리 조회해서 이 문제를 방지한다.
     */
    @Query("""
            SELECT d FROM Dispatch d
            JOIN FETCH d.transportOrder
            JOIN FETCH d.vehicle
            WHERE (:status IS NULL OR d.dispatchStatus = :status)
            AND (:vehicleId IS NULL OR d.vehicle.id = :vehicleId)
            """)
    List<Dispatch> findAllWithOrderAndVehicle(
            @Param("status") DispatchStatus status,
            @Param("vehicleId") Long vehicleId);
}
