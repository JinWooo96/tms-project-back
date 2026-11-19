-- =========================================================
-- V2: 낙관적 락(@Version)용 버전 컬럼 추가
-- 같은 차량·주문을 동시에 확정할 때 나중 커밋을 실패시켜 갱신 손실(Lost Update)을 막는다.
-- 기존 행은 DEFAULT 0으로 시작한다.
-- =========================================================

ALTER TABLE `vehicle`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전 (JPA @Version, UPDATE마다 1 증가)' AFTER `status`;

ALTER TABLE `transport_order`
    ADD COLUMN `version` BIGINT NOT NULL DEFAULT 0 COMMENT '낙관적 락 버전 (JPA @Version, UPDATE마다 1 증가)' AFTER `status`;