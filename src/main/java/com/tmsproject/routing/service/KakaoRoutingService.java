package com.tmsproject.routing.service;

import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import com.tmsproject.routing.client.KakaoDirectionsClient;
import com.tmsproject.routing.dto.KakaoDirectionsResponse;
import com.tmsproject.routing.dto.RouteResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KakaoRoutingService implements RoutingService {

    private final KakaoDirectionsClient kakaoDirectionsClient;

    public KakaoRoutingService(KakaoDirectionsClient kakaoDirectionsClient) {
        this.kakaoDirectionsClient = kakaoDirectionsClient;
    }

    @Override
    public RouteResult getRoute(GeoPoint origin, GeoPoint destination) {
        KakaoDirectionsResponse response = kakaoDirectionsClient.getDirections(origin, destination);

        List<KakaoDirectionsResponse.Route> routes = response.routes();
        if (routes == null || routes.isEmpty()) {
            // 경로를 찾지 못한 경우 (두 좌표 사이에 길이 없는 등 예외 상황)
            throw new BusinessException(ErrorCode.ROUTE_CALCULATION_FAILED);
        }

        KakaoDirectionsResponse.Summary summary = routes.getFirst().summary();
        if (summary == null || summary.distance() == null || summary.duration() == null) {
            throw new BusinessException(ErrorCode.ROUTE_CALCULATION_FAILED);
        }

        // 카카오 응답은 거리(m) / 소요시간(초) 단위로 온다. km/분 단위로 변환한다.
        double distanceKm = summary.distance() / 1000.0;
        int durationMin = (int) Math.round(summary.duration() / 60.0);

        return new RouteResult(distanceKm, durationMin);
    }
}
