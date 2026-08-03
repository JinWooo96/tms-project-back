package com.tmsproject.geocoding.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 카카오 로컬 API(주소 검색) 응답을 매핑하는 DTO.
 * 실제 응답에는 훨씬 많은 필드(meta, road_address 등)가 있지만,
 * 우리가 필요한 좌표(x, y)만 뽑아 쓸 것이므로 @JsonIgnoreProperties로
 * 매핑하지 않는 나머지 필드는 무시하도록 설정한다.
 * (이걸 안 하면 Jackson이 "모르는 필드가 왔다"고 역직렬화 과정에서 에러를 던질 수 있다.)
 *
 * 카카오 응답은 snake_case(address_name)인데 우리 record 필드는 camelCase(addressName)라서,
 * @JsonProperty로 실제 JSON 필드명을 명시적으로 지정한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoGeocodingResponse(List<Document> documents) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Document(
            @JsonProperty("address_name")
            String addressName,
            String x, // 경도(longitude). 카카오 API는 문자열로 내려준다.
            String y // 위도 (latitude)
    ) {

    }
}
