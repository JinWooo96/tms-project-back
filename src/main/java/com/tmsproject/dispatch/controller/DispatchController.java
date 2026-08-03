package com.tmsproject.dispatch.controller;


import com.tmsproject.dispatch.dto.request.ConfirmDispatchRequest;
import com.tmsproject.dispatch.dto.request.DispatchRecommendationRequest;
import com.tmsproject.dispatch.dto.response.DispatchCancelResponse;
import com.tmsproject.dispatch.dto.response.DispatchListItemResponse;
import com.tmsproject.dispatch.dto.response.DispatchRecommendationResponse;
import com.tmsproject.dispatch.dto.response.DispatchResponse;
import com.tmsproject.dispatch.entity.Dispatch;
import com.tmsproject.dispatch.entity.DispatchStatus;
import com.tmsproject.dispatch.service.DispatchService;
import com.tmsproject.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DispatchController {

    private final DispatchService dispatchService;

    /** POST /api/dispatches - 배차 확정 (API 명세서 3.1) */
    @PostMapping("/api/dispatches")
    public ResponseEntity<ApiResponse<DispatchResponse>> confirmDispatch(
            @Valid @RequestBody ConfirmDispatchRequest request) {

        Dispatch dispatch = dispatchService.confirmDispatch(request.orderId(), request.vehicleId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(DispatchResponse.from(dispatch)));
    }

    /** PATCH /api/dispatches/{dispatchId}/cancel - 배차 취소 (API 명세서 3.2) */
    @PatchMapping("/api/dispatches/{dispatchId}/cancel")
    public ResponseEntity<ApiResponse<DispatchCancelResponse>> cancelDispatch(
            @PathVariable Long dispatchId) {

        Dispatch dispatch = dispatchService.cancelDispatch(dispatchId);

        return ResponseEntity.ok(ApiResponse.success(DispatchCancelResponse.from(dispatch)));
    }

    /** GET /api/dispatches?status=&vehicleId= - 배차 목록 조회 (API 명세서 3.3) */
    @GetMapping("/api/dispatches")
    public ResponseEntity<ApiResponse<List<DispatchListItemResponse>>> getDispatches(
            @RequestParam(required = false) DispatchStatus status,
            @RequestParam(required = false) Long vehicleId) {

        List<Dispatch> dispatches = dispatchService.getDispatches(status, vehicleId);
        List<DispatchListItemResponse> response = dispatches.stream()
                .map(DispatchListItemResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/dispatches/recommendations - 자동 배차 추천 (API 명세서 v2, 4.1)
     * 요청 바디를 아예 생략하면(전체 추천) 미배차 주문 전체를 대상으로,
     * { "orderIds": [...] }를 담아 보내면(선택 추천) 그 주문들만 대상으로 계산한다.
     */
    @PostMapping("/api/dispatches/recommendations")
    public ResponseEntity<ApiResponse<DispatchRecommendationResponse>> getRecommendations(
            @RequestBody(required = false) DispatchRecommendationRequest request) {
        log.info("getRecommendations... request: {}", request);

        List<Long> selectedOrderIds = (request != null) ? request.orderIds() : null;
        DispatchRecommendationResponse response = dispatchService.getRecommendations(selectedOrderIds);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}