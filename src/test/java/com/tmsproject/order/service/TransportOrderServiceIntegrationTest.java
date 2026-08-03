package com.tmsproject.order.service;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * TestContainers
 * 테스트 환경과 운영 환경의 DB 종류가 완전히 똑같다는게 핵심 장점
 * Docker만 있으면 도입할 수 있다.
 */
//@TestContainers
@SpringBootTest
public class TransportOrderServiceIntegrationTest {
//    @Container
//    static MariaDBContainer<?> mariadb = new MariaDBContainer<>("mariadb:11");
    // 테스트 실행 시 Docker로 진짜 MariaDB 컨테이너를 띄웠다가, 테스트 끝나면 자동 삭제
}
