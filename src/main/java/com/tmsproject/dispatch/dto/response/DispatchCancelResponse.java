package com.tmsproject.dispatch.dto.response;

import com.tmsproject.dispatch.entity.Dispatch;
import com.tmsproject.dispatch.entity.DispatchStatus;

/**
 * 배차 취소 응답. (API 명세서 3.2 참고)
 */
public record DispatchCancelResponse(
        Long id,
        DispatchStatus dispatchStatus
) {
    public static DispatchCancelResponse from(Dispatch dispatch) {
        return new DispatchCancelResponse(dispatch.getId(), dispatch.getDispatchStatus());
    }
}
