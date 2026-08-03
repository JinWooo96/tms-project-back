package com.tmsproject.geocoding.service;

import com.tmsproject.geocoding.client.KakaoGeocodingClient;
import com.tmsproject.geocoding.dto.GeoPoint;
import com.tmsproject.geocoding.dto.KakaoGeocodingResponse;
import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.global.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KakaoGeocodingService implements GeocodingService {

    private final KakaoGeocodingClient kakaoGeocodingClient;

    public KakaoGeocodingService(KakaoGeocodingClient kakaoGeocodingClient) {
        this.kakaoGeocodingClient = kakaoGeocodingClient;
    }

    @Override
    public GeoPoint geocode(String address) {
        KakaoGeocodingResponse response = kakaoGeocodingClient.searchAddress(address);

        List<KakaoGeocodingResponse.Document> documents = response.documents();
        if (documents == null || documents.isEmpty()) {
            // 주소를 찾지 못한 경우 (오타, 존재하지 않는 주소 등)
            throw new BusinessException(ErrorCode.GEOCODING_FAILED);
        }

        // 검색 결과가 여러 개일 수 있는데, 가장 정확도 높은 (카카오가 첫 번째로 반환한) 결과를 사용
        KakaoGeocodingResponse.Document first = documents.getFirst();

        // 주의: 카카오 API는 x=경도(lng), y=위도(lat) 순서로 응답한다. 헷갈리기 쉬운 지점이므로 명시적으로 변환.
        double lng = Double.parseDouble(first.x());
        double lat = Double.parseDouble(first.y());

        return new GeoPoint(lat, lng);
    }
}
