package com.tmsproject.order.repository;

import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;
import org.hibernate.query.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransportOrderRepository extends JpaRepository<TransportOrder, Long> {

    // 상태별 목록 조회 (페이징 포함) - GET /api/orders?status= 에 사용
    // Page 반환타입을 쓰면 실제 쿼리가 2번 나간다. 하나는 실제 데이터 (LIMIT, OFFSET), 하나는 전체 건수 (count)
    Page<TransportOrder> findByStatus(OrderStatus status, Pageable pageable);

    // orderNo로 단건 조회 (추후 화주 문의 대응 등에 사용)
    Optional<TransportOrder> findByOrderNo(String orderNo);

    /**
     * 주문 상세 조회용 fetch join 쿼리.
     *
     * 이유: OrderResponse.from(order)에서 order.getCargoList()에 접근하는데,
     * cargoList는 LAZY 연관관계다. open-in-view: false 설정 때문에
     * TransportOrderService.getOrder()의 @Transactional이 끝난 뒤(Controller 시점)
     * cargoList에 접근하면 LazyInitializationException이 발생한다.
     * JOIN FETCH로 조회 시점에 cargoList까지 함께 가져와서 이 문제를 방지한다.
     *
     * 주의: OneToMany를 fetch join하면 Page(페이징)와 함께 쓰면 안 된다
     * (메모리에서 페이징을 처리하게 되어 성능 문제가 생길 수 있다는 Hibernate 경고가 발생함).
     * 그래서 페이징이 필요 없는 단건 조회(findById 대체용)에만 이 방식을 쓴다.
     *
     * 내부 쿼리 로직
     * SELECT *
     * FROM transport_order t
     * JOIN cargo c
     * ON t.id = c.transport_order_id
     * WHERE t.id = 2;
     */
    @Query("""
            SELECT o FROM TransportOrder o
            LEFT JOIN FETCH o.cargoList
            WHERE o.id = :id
            """)
    Optional<TransportOrder> findByIdWithCargoList(@Param("id") Long id);

    /**
     * v2: 자동 배차 추천용 - 특정 상태의 주문 전체를 cargoList까지 함께 조회한다.
     * 페이징이 필요 없다 (미배차 주문 전체를 대상으로 최적화 계산을 해야 하므로).
     * DispatchOptimizer에 넘길 각 주문의 totalWeight()/totalVolume() 계산에 cargoList가 필요하다.
     */
    @Query("""
            SELECT o FROM TransportOrder o
            LEFT JOIN FETCH o.cargoList
            WHERE o.status = :status
            """)
    List<TransportOrder> findAllWithCargoListByStatus(@Param("status") OrderStatus status);

    /**
     * v2: 선택 추천용 - 주어진 ID 목록 중 여전히 RECEIVED 상태인 주문만 조회한다.
     * (사용자가 선택한 뒤 다른 경로로 이미 배차되었거나 존재하지 않는 ID는 자연스럽게 결과에서 빠진다.
     *  이렇게 빠진 주문은 candidate가 하나도 안 생기므로, 최종적으로 unassignedOrderIds에 표시된다.)
     */
    @Query("""
           SELECT o FROM TransportOrder o
           LEFT JOIN FETCH o.cargoList
           WHERE o.id IN :ids AND o.status = :status
           """)
    List<TransportOrder> findAllWithCargoListByIdInAndStatus(@Param("ids") List<Long> ids, @Param("status") OrderStatus status);
}
