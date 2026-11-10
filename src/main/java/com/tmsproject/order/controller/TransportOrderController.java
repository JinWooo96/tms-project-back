package com.tmsproject.order.controller;

import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.geocoding.service.GeocodingService;
import com.tmsproject.global.response.ApiResponse;
import com.tmsproject.global.response.PageResponse;
import com.tmsproject.order.dto.request.CreateOrderRequest;
import com.tmsproject.order.dto.response.OrderCreateResponse;
import com.tmsproject.order.dto.response.OrderResponse;
import com.tmsproject.order.dto.response.OrderSummaryResponse;
import com.tmsproject.order.entity.OrderStatus;
import com.tmsproject.order.entity.TransportOrder;
import com.tmsproject.order.service.TransportOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TransportOrderController {

    private final TransportOrderService transportOrderService;
    private final GeocodingService geocodingService;

    /**
     * POST /api/orders - 운송주문 등록
     *
     * v2: 상차지/도착지 주소를 각각 지오코딩하여 좌표를 함께 저장한다.
     * 지오코딩(외부 API 호출 2회)은 트랜잭션이 없는 이 Controller 계층에서 먼저 끝내고,
     * 좌표까지 다 채운 완성된 엔티티를 TransportOrderService에 넘겨서 그때부터 짧은 트랜잭션을 연다.
     */
    @PostMapping("/api/orders")
    public ResponseEntity<ApiResponse<OrderCreateResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        log.info("createOrder... request : {}", request);

        GeoPoint originPoint = geocodingService.geocode(request.originAddress());
        GeoPoint destPoint = geocodingService.geocode(request.destAddress());

        TransportOrder order = request.toEntity(originPoint, destPoint);
        TransportOrder savedOrder = transportOrderService.registerOrder(order);

        OrderCreateResponse response = OrderCreateResponse.from(savedOrder);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /** GET /api/orders?status=&page=&size= - 운송주문 목록 조회 */
    @GetMapping("/api/orders")
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {
        log.info("getOrders... status : {}", status);

        // 서비스에서 이미 DTO로 변환된 Page를 받는다. (매핑이 필요 없으므로 identity 함수 사용)
        Page<OrderSummaryResponse> orders = transportOrderService.getOrderSummaries(status, pageable);
        PageResponse<OrderSummaryResponse> response = PageResponse.of(orders, Function.identity());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/orders/{orderId} - 운송주문 상세 조회 */
    @GetMapping("/api/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId) {
        log.info("getOrder... orderId : {}", orderId);

        TransportOrder order = transportOrderService.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(OrderResponse.from(order)));
    }
}
