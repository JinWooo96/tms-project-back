package com.tmsproject.order.service;

import com.tmsproject.global.exception.BusinessException;
import com.tmsproject.order.repository.TransportOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

/**
 * 실무에서는 Mockito로 Repository를 가짜(Mock)으로 만들어서, Service 로직만 빠르게 테스트하는 게 훨씬 많다.
 * 단위테스트에 속한다.
 *
 * DB 연결없이 메모리에서만 돌기 때문에 실행 속도가 훨씬 빠르다. CI 파이프라인에서 커밋할 때마다 매번 돌려도 부담이 없다.
 */
@ExtendWith(MockitoExtension.class)
public class TransportOrderServiceMockTest {

    @Mock
    private TransportOrderRepository transportOrderRepository; // 진짜 DB 없이 가짜 객체

    @InjectMocks
    private TransportOrderService transportOrderService; // Mock을 자동으로 주입받음

    @Test
    void getOrder_throwsException_whenNotFound() {
        given(transportOrderRepository.findById(999L)).willReturn(Optional.empty());

        assertThatExceptionOfType(BusinessException.class)
                .isThrownBy(() -> transportOrderService.getOrder(999L));
    }

}
