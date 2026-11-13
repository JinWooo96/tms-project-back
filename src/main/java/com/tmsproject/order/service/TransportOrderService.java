package com.tmsproject.order.service;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.order.dto.response.OrderSummaryResponse;
import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.order.repository.TransportOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransportOrderService {
    private final TransportOrderRepository transportOrderRepository;

    /**
     * 운송주문을 등록한다. Cargo는 이미 order.addCargo()로 연결된 상태로 전달받는다고 가정한다.
     * (Controller/DTO 계층에서 요청을 받아 TransportOrder + Cargo 객체를 조립한 뒤 이 메서드를 호출할 것)
     */
    @Transactional
    public TransportOrder registerOrder(TransportOrder order) {
        // orderNo는 서버가 생성
        String orderNo = generateOrderNo();
        order.assignOrderNo(orderNo);

        return transportOrderRepository.save(order);
    }

    /**
     * 상태별 주문 목록 조회 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<TransportOrder> getOrders(OrderStatus status, Pageable pageable) {
        if (status == null) {
            return transportOrderRepository.findAll(pageable);
        }
        return transportOrderRepository.findByStatus(status, pageable);
    }

    /**
     * 목록 화면용: 트랜잭션 안에서 DTO(총 중량/부피 포함)로 변환해서 반환한다.
     * open-in-view=false 이므로 cargoList 접근(OrderSummaryResponse.from)을 반드시 여기서 끝내야 한다.
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> getOrderSummaries(OrderStatus status, Pageable pageable) {
        Page<TransportOrder> orders = (status == null)
                ? transportOrderRepository.findAll(pageable)
                : transportOrderRepository.findByStatus(status, pageable);
        return orders.map(OrderSummaryResponse::from); // 트랜잭션 안에서 변환
    }

    /**
     * 주문 상세 조회. 없으면 예외 발생
     * findByIdWithCargoList()로 cargoList까지 함께 조회한다.
     * (호출부에서 order.getCargoList()나 order.totalWeight() 등을 트랜잭션 밖에서도
     *  안전하게 쓸 수 있도록, 여기서 항상 미리 로딩해둔다.)
     */
    @Transactional(readOnly = true)
    public TransportOrder getOrder(Long orderId) {
        return transportOrderRepository.findByIdWithCargoList(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * v2: 자동 배차 추천 대상이 되는 미배차(RECEIVED) 주문 전체를 조회한다. (페이징 없음)
     * cargoList까지 함께 로딩되어 있어, 호출부에서 order.totalWeight()/totalVolume()을
     * 트랜잭션 밖에서도 안전하게 쓸 수 있다.
     */
    @Transactional(readOnly = true)
    public List<TransportOrder> getReceivedOrders() {
        return transportOrderRepository.findAllWithCargoListByStatus(OrderStatus.RECEIVED);
    }

    /**
     * v2: 사용자가 선택한 주문 ID들 중, 여전히 미배차(RECEIVED) 상태인 것만 조회한다. (선택 추천용)
     */
    @Transactional(readOnly = true)
    public List<TransportOrder> getReceivedOrdersByIds(List<Long> orderIds) {
        return transportOrderRepository.findAllWithCargoListByIdInAndStatus(orderIds, OrderStatus.RECEIVED);
    }

    /**
     * 주문번호 생성 규칙: TO-yyyyMMdd-일련번호(4자리)
     * 주의: 지금은 "오늘 등록된 주문 수 + 1"로 간단히 구현했다.
     * 동시에 여러 요청이 들어오면 같은 번호가 생성될 수 있는 동시성 문제가 있는데,
     * 사이드 프로젝트 1차 범위에서는 트래픽이 낮으니 우선 단순하게 두고,
     * 나중에 DB 시퀀스나 UUID 조합으로 개선하는 걸 확장 포인트로 남겨둔다.
     */
    private String generateOrderNo() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        long todayCount = transportOrderRepository.count(); // 단순화된 방식 (전체 카운트 기준)
        return String.format("TO-%s-%04d", today, todayCount + 1);
    }

}
