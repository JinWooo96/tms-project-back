-- =========================================================
-- V1: 초기 스키마 (v2 완료 시점 + v3 준비분 dispatch_route)
-- 원본: 2026-11-13 로컬 DB 덤프에서 구조만 정리
-- 정리 내용: dispatch_route.total_distance_km DECIMAL(38,2) → DECIMAL(8,2), 누락된 COMMENT 보완
-- 주의: 이미 적용된 마이그레이션 파일은 절대 수정하지 않는다. 변경은 V2, V3...로 추가한다.
-- =========================================================

-- 운송주문
CREATE TABLE `transport_order` (
                                   `id`                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '운송주문 ID',
                                   `order_no`              VARCHAR(30)  NOT NULL COMMENT '주문번호',
                                   `origin_address`        VARCHAR(255) NOT NULL COMMENT '상차지 주소',
                                   `origin_lat`            DOUBLE       DEFAULT NULL COMMENT '상차지 위도',
                                   `origin_lng`            DOUBLE       DEFAULT NULL COMMENT '상차지 경도',
                                   `dest_address`          VARCHAR(255) NOT NULL COMMENT '도착지 주소',
                                   `dest_lat`              DOUBLE       DEFAULT NULL COMMENT '도착지 위도',
                                   `dest_lng`              DOUBLE       DEFAULT NULL COMMENT '도착지 경도',
                                   `requested_pickup_at`   DATETIME     DEFAULT NULL COMMENT '상차 희망일시',
                                   `requested_delivery_at` DATETIME     DEFAULT NULL COMMENT '도착 희망일시',
                                   `status`                VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED' COMMENT '주문상태 (RECEIVED:접수, DISPATCHED:배차완료, IN_TRANSIT:운송중, COMPLETED:완료)',
                                   `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                                   `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '수정일시',
                                   PRIMARY KEY (`id`),
                                   UNIQUE KEY `order_no` (`order_no`),
                                   KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='운송주문';

-- 차량
CREATE TABLE `vehicle` (
                           `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '차량 ID',
                           `vehicle_no`    VARCHAR(20)   NOT NULL COMMENT '차량번호',
                           `vehicle_type`  VARCHAR(20)   NOT NULL COMMENT '차량유형 (1TON, 2_5TON, 5TON 등 톤수)',
                           `max_weight_kg` DECIMAL(8,2)  NOT NULL COMMENT '최대 적재중량 (kg)',
                           `bed_width_cm`  INT           NOT NULL COMMENT '적재함 가로 (cm)',
                           `bed_length_cm` INT           NOT NULL COMMENT '적재함 세로 (cm)',
                           `bed_height_cm` INT           NOT NULL COMMENT '적재함 높이 (cm)',
                           `base_address`  VARCHAR(255)  DEFAULT NULL COMMENT '차고지 주소',
                           `base_lat`      DOUBLE        DEFAULT NULL COMMENT '차고지 위도',
                           `base_lng`      DOUBLE        DEFAULT NULL COMMENT '차고지 경도',
                           `status`        VARCHAR(20)   NOT NULL DEFAULT 'AVAILABLE' COMMENT '차량상태 (AVAILABLE:운행가능, ASSIGNED:배차중, MAINTENANCE:정비중)',
                           PRIMARY KEY (`id`),
                           UNIQUE KEY `vehicle_no` (`vehicle_no`),
                           KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='차량';

-- 화물 (운송주문 1 : N)
CREATE TABLE `cargo` (
                         `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '화물 ID',
                         `transport_order_id` BIGINT       NOT NULL COMMENT '운송주문 ID (FK)',
                         `width_cm`           INT          NOT NULL COMMENT '가로 (cm)',
                         `length_cm`          INT          NOT NULL COMMENT '세로 (cm)',
                         `height_cm`          INT          NOT NULL COMMENT '높이 (cm)',
                         `weight_kg`          DECIMAL(8,2) NOT NULL COMMENT '중량 (kg)',
                         `quantity`           INT          NOT NULL DEFAULT 1 COMMENT '개수',
                         `cargo_type`         VARCHAR(20)  NOT NULL DEFAULT 'GENERAL' COMMENT '화물유형 (GENERAL:일반, FROZEN:냉동, DANGEROUS:위험물)',
                         PRIMARY KEY (`id`),
                         KEY `fk_cargo_order` (`transport_order_id`),
                         CONSTRAINT `fk_cargo_order` FOREIGN KEY (`transport_order_id`) REFERENCES `transport_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='화물';

-- 배차경로 (v3 멀티드롭: 차량 1대의 한 번 운행)
CREATE TABLE `dispatch_route` (
                                  `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '배차경로 ID',
                                  `vehicle_id`         BIGINT       NOT NULL COMMENT '차량 ID (FK)',
                                  `route_status`       VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMED' COMMENT '경로상태 (CONFIRMED:확정, CANCELLED:취소)',
                                  `total_distance_km`  DECIMAL(8,2) DEFAULT NULL COMMENT '총 이동거리(km)',
                                  `total_duration_min` INT          DEFAULT NULL COMMENT '총 소요시간(분)',
                                  `confirmed_at`       DATETIME     NOT NULL COMMENT '경로확정 일시',
                                  `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                                  PRIMARY KEY (`id`),
                                  KEY `fk_dispatch_route_vehicle` (`vehicle_id`),
                                  KEY `idx_route_status` (`route_status`),
                                  CONSTRAINT `fk_dispatch_route_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배차경로 (멀티드롭 - 한 차량의 하나의 운행에 속한 여러 배차를 묶는 상위 단위)';

-- 배차 (주문과 차량의 연결 이력, 경로에 속할 수도 있음)
CREATE TABLE `dispatch` (
                            `id`                   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '배차 ID',
                            `transport_order_id`   BIGINT      NOT NULL COMMENT '운송주문 ID (FK)',
                            `vehicle_id`           BIGINT      NOT NULL COMMENT '차량 ID (FK)',
                            `dispatch_route_id`    BIGINT      DEFAULT NULL COMMENT '배차경로 ID (FK, 멀티드롭 경로 소속일 때만 값 있음. 수동 단건 배차는 NULL)',
                            `driver_id`            BIGINT      DEFAULT NULL COMMENT '기사 ID (추후 확장용)',
                            `dispatch_status`      VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '배차상태 (PENDING:배차대기, CONFIRMED:배차확정, CANCELLED:배차취소)',
                            `sequence`             INT         DEFAULT NULL COMMENT '방문순서 (v3 route_stop 도입 후 사용 여부 재검토)',
                            `estimated_arrival_at` DATETIME    DEFAULT NULL COMMENT '예상 도착(방문) 일시',
                            `assigned_at`          DATETIME    DEFAULT NULL COMMENT '배차확정 일시',
                            `created_at`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성일시',
                            PRIMARY KEY (`id`),
                            KEY `fk_dispatch_order` (`transport_order_id`),
                            KEY `idx_dispatch_status` (`dispatch_status`),
                            KEY `idx_vehicle_status` (`vehicle_id`, `dispatch_status`),
                            KEY `fk_dispatch_route` (`dispatch_route_id`),
                            CONSTRAINT `fk_dispatch_order`   FOREIGN KEY (`transport_order_id`) REFERENCES `transport_order` (`id`),
                            CONSTRAINT `fk_dispatch_vehicle` FOREIGN KEY (`vehicle_id`)         REFERENCES `vehicle` (`id`),
                            CONSTRAINT `fk_dispatch_route`   FOREIGN KEY (`dispatch_route_id`)  REFERENCES `dispatch_route` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배차';