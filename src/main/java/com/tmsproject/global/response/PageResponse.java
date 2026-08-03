package com.tmsproject.global.response;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * 목록 조회 API의 페이징 응답 포맷. (API 명세서 1.2 "운송주문 목록 조회" 응답 형식 참고)
 * Spring Data의 Page<Entity>를 그대로 반환하지 않고, 우리가 원하는 필드만 노출하기 위해 감싼다.
 */
@Getter
public class PageResponse<T> {

    private final List<T> content;
    private final long totalElements;
    private final int totalPages;
    private final int page;
    private final int size;

    private PageResponse(List<T> content, long totalElements, int totalPages, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.page = page;
        this.size = size;
    }

    /**
     * Page<Entity>를 Page<Dto>로 변환하면서 PageResponse로 감싼다.
     * mapper: Entity -> Dto 변환 함수 (예: OrderSummaryResponse::from)
     */
    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> mapper) {
        List<D> content = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(content, page.getTotalElements(), page.getTotalPages(),
                page.getNumber(), page.getSize());
    }
}
