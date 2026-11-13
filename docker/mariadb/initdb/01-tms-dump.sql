-- MariaDB dump 10.19-11.3.2-MariaDB, for Win64 (AMD64)
--
-- Host: localhost    Database: tms
-- ------------------------------------------------------
-- Server version	11.3.2-MariaDB

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cargo`
--

DROP TABLE IF EXISTS `cargo`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cargo` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '화물 ID',
  `transport_order_id` bigint(20) NOT NULL COMMENT '운송주문 ID (FK)',
  `width_cm` int(11) NOT NULL COMMENT '가로 (cm)',
  `length_cm` int(11) NOT NULL COMMENT '세로 (cm)',
  `height_cm` int(11) NOT NULL COMMENT '높이 (cm)',
  `weight_kg` decimal(8,2) NOT NULL COMMENT '중량 (kg)',
  `quantity` int(11) NOT NULL DEFAULT 1 COMMENT '개수',
  `cargo_type` varchar(20) NOT NULL DEFAULT 'GENERAL' COMMENT '화물유형 (GENERAL:일반, FROZEN:냉동, DANGEROUS:위험물)',
  PRIMARY KEY (`id`),
  KEY `fk_cargo_order` (`transport_order_id`),
  CONSTRAINT `fk_cargo_order` FOREIGN KEY (`transport_order_id`) REFERENCES `transport_order` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=24 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='화물';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cargo`
--

LOCK TABLES `cargo` WRITE;
/*!40000 ALTER TABLE `cargo` DISABLE KEYS */;
INSERT INTO `cargo` VALUES
(2,2,120,100,150,350.50,2,'GENERAL'),
(3,3,100,50,70,100.00,3,'GENERAL'),
(4,3,120,120,100,50.00,2,'GENERAL'),
(5,4,100,150,30,100.00,3,'GENERAL'),
(6,4,36,80,30,150.00,2,'FROZEN'),
(7,4,150,80,60,250.00,2,'GENERAL'),
(8,5,35,40,50,150.00,1,'GENERAL'),
(9,6,50,40,60,150.00,1,'GENERAL'),
(10,6,60,70,100,50.00,1,'GENERAL'),
(11,7,35,40,80,100.00,1,'GENERAL'),
(12,7,45,75,35,120.00,2,'GENERAL'),
(13,8,45,120,100,150.00,1,'GENERAL'),
(14,8,35,45,98,100.00,1,'GENERAL'),
(15,9,65,48,98,100.00,1,'GENERAL'),
(16,9,78,100,150,250.00,1,'GENERAL'),
(17,9,100,100,100,200.00,1,'GENERAL'),
(18,10,100,80,40,150.00,2,'GENERAL'),
(19,10,100,80,40,150.00,2,'GENERAL'),
(20,11,110,110,150,200.00,1,'GENERAL'),
(21,11,60,40,40,80.00,1,'GENERAL'),
(22,12,40,30,30,80.00,1,'GENERAL'),
(23,12,40,30,30,80.00,1,'GENERAL');
/*!40000 ALTER TABLE `cargo` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dispatch`
--

DROP TABLE IF EXISTS `dispatch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dispatch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '배차 ID',
  `transport_order_id` bigint(20) NOT NULL COMMENT '운송주문 ID (FK)',
  `vehicle_id` bigint(20) NOT NULL COMMENT '차량 ID (FK)',
  `dispatch_route_id` bigint(20) DEFAULT NULL COMMENT '배차경로 ID (FK, 멀티드롭 경로에 속한 경우만 값 있음. 수동 단건 배차는 NULL)',
  `driver_id` bigint(20) DEFAULT NULL COMMENT '기사 ID (추후 확장용, 1차 미사용)',
  `dispatch_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '배차상태 (PENDING:배차대기, CONFIRMED:배차확정, CANCELLED:배차취소)',
  `sequence` int(11) DEFAULT NULL COMMENT '방문순서 (추후 경로최적화 확장용, 1차 미사용)',
  `estimated_arrival_at` datetime DEFAULT NULL COMMENT '이 배차(주문)의 예상 도착(방문) 일시',
  `assigned_at` datetime DEFAULT NULL COMMENT '배차확정 일시',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '생성일시',
  PRIMARY KEY (`id`),
  KEY `fk_dispatch_order` (`transport_order_id`),
  KEY `idx_dispatch_status` (`dispatch_status`),
  KEY `idx_vehicle_status` (`vehicle_id`,`dispatch_status`),
  KEY `fk_dispatch_route` (`dispatch_route_id`),
  CONSTRAINT `fk_dispatch_order` FOREIGN KEY (`transport_order_id`) REFERENCES `transport_order` (`id`),
  CONSTRAINT `fk_dispatch_route` FOREIGN KEY (`dispatch_route_id`) REFERENCES `dispatch_route` (`id`),
  CONSTRAINT `fk_dispatch_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배차';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dispatch`
--

LOCK TABLES `dispatch` WRITE;
/*!40000 ALTER TABLE `dispatch` DISABLE KEYS */;
INSERT INTO `dispatch` VALUES
(1,2,1,NULL,NULL,'CANCELLED',NULL,NULL,'2026-07-28 13:47:29','2026-07-28 13:47:29'),
(2,2,3,NULL,NULL,'CANCELLED',NULL,NULL,'2026-07-28 13:47:41','2026-07-28 13:47:41'),
(3,2,1,NULL,NULL,'CANCELLED',NULL,NULL,'2026-07-28 14:41:27','2026-07-28 14:41:27'),
(4,6,3,NULL,NULL,'CANCELLED',NULL,NULL,'2026-07-28 14:41:34','2026-07-28 14:41:34'),
(5,4,2,NULL,NULL,'CANCELLED',NULL,NULL,'2026-07-28 14:54:01','2026-07-28 14:54:01'),
(6,3,4,NULL,NULL,'CANCELLED',NULL,NULL,'2026-07-28 14:55:50','2026-07-28 14:55:50'),
(7,3,3,NULL,NULL,'CANCELLED',NULL,NULL,'2026-07-29 17:53:46','2026-07-29 17:53:46'),
(8,2,6,NULL,NULL,'CANCELLED',NULL,NULL,'2026-08-03 15:02:06','2026-08-03 15:02:06'),
(9,7,9,NULL,NULL,'CANCELLED',NULL,NULL,'2026-08-03 15:03:23','2026-08-03 15:03:23'),
(10,4,8,NULL,NULL,'CANCELLED',NULL,NULL,'2026-11-10 13:58:08','2026-11-10 13:58:08'),
(11,5,2,NULL,NULL,'CANCELLED',NULL,NULL,'2026-11-10 13:58:40','2026-11-10 13:58:40'),
(12,10,7,NULL,NULL,'CANCELLED',NULL,NULL,'2026-11-10 16:56:10','2026-11-10 16:56:10'),
(13,10,6,NULL,NULL,'CONFIRMED',NULL,NULL,'2026-11-10 17:13:26','2026-11-10 17:13:26'),
(14,3,7,NULL,NULL,'CONFIRMED',NULL,NULL,'2026-11-10 18:45:48','2026-11-10 18:45:48'),
(15,2,1,NULL,NULL,'CONFIRMED',NULL,NULL,'2026-11-10 18:53:48','2026-11-10 18:53:48');
/*!40000 ALTER TABLE `dispatch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dispatch_route`
--

DROP TABLE IF EXISTS `dispatch_route`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dispatch_route` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '배차경로 ID',
  `vehicle_id` bigint(20) NOT NULL COMMENT '차량 ID (FK)',
  `route_status` varchar(20) NOT NULL DEFAULT 'CONFIRMED' COMMENT '경로상태 (CONFIRMED:확정, CANCELLED:취소)',
  `total_distance_km` decimal(38,2) DEFAULT NULL,
  `total_duration_min` int(11) DEFAULT NULL COMMENT '총 소요시간(분)',
  `confirmed_at` datetime NOT NULL COMMENT '경로확정 일시',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '생성일시',
  PRIMARY KEY (`id`),
  KEY `fk_dispatch_route_vehicle` (`vehicle_id`),
  KEY `idx_route_status` (`route_status`),
  CONSTRAINT `fk_dispatch_route_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='배차경로 (멀티드롭 - 한 차량의 하나의 운행에 속한 여러 배차를 묶는 상위 단위)';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dispatch_route`
--

LOCK TABLES `dispatch_route` WRITE;
/*!40000 ALTER TABLE `dispatch_route` DISABLE KEYS */;
/*!40000 ALTER TABLE `dispatch_route` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transport_order`
--

DROP TABLE IF EXISTS `transport_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `transport_order` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '운송주문 ID',
  `order_no` varchar(30) NOT NULL COMMENT '주문번호',
  `origin_address` varchar(255) NOT NULL COMMENT '상차지 주소',
  `origin_lat` double DEFAULT NULL,
  `origin_lng` double DEFAULT NULL,
  `dest_address` varchar(255) NOT NULL COMMENT '도착지 주소',
  `dest_lat` double DEFAULT NULL,
  `dest_lng` double DEFAULT NULL,
  `requested_pickup_at` datetime DEFAULT NULL COMMENT '상차 희망일시',
  `requested_delivery_at` datetime DEFAULT NULL COMMENT '도착 희망일시',
  `status` varchar(20) NOT NULL DEFAULT 'RECEIVED' COMMENT '주문상태 (RECEIVED:접수, DISPATCHED:배차완료, IN_TRANSIT:운송중, COMPLETED:완료)',
  `created_at` datetime NOT NULL DEFAULT current_timestamp() COMMENT '생성일시',
  `updated_at` datetime NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp() COMMENT '수정일시',
  PRIMARY KEY (`id`),
  UNIQUE KEY `order_no` (`order_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='운송주문';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transport_order`
--

LOCK TABLES `transport_order` WRITE;
/*!40000 ALTER TABLE `transport_order` DISABLE KEYS */;
INSERT INTO `transport_order` VALUES
(2,'TO-20260727-0001','서울시 강남구 테헤란로 123',37.5012,127.0396,'부산시 해운대구 센텀중앙로 45',35.1691,129.1306,'2026-08-01 09:00:00','2026-08-02 18:00:00','DISPATCHED','2026-07-27 14:07:40','2026-11-10 18:53:48'),
(3,'TO-20260727-0002','부산시 사상구 주례1동',35.1513298047166,129.001139491523,'부산시 사상구 백양대로 506번길',35.1561642070833,128.997740867788,'2026-07-28 15:13:00','2026-07-30 12:00:00','DISPATCHED','2026-07-27 17:41:24','2026-11-10 18:45:48'),
(4,'TO-20260728-0003','서울시 강남구 테헤란로 15',37.5041073,127.0475882,'부산시 해운대구 센텀중앙로 46',35.1767337,129.1251564,'2026-07-30 06:00:00','2026-07-31 12:00:00','RECEIVED','2026-07-28 09:51:22','2026-11-10 16:57:04'),
(5,'TO-20260728-0004','부산시 부산진구',NULL,NULL,'부산시 사상구',NULL,NULL,'2026-07-28 08:00:00','2026-07-30 12:00:00','RECEIVED','2026-07-28 13:51:08','2026-11-10 16:57:05'),
(6,'TO-20260728-0005','부산시 사상구',NULL,NULL,'부산시 중구',NULL,NULL,'2026-07-28 08:00:00','2026-07-30 00:00:00','RECEIVED','2026-07-28 14:25:41','2026-07-29 15:03:51'),
(7,'TO-20260729-0006','부산 부산진구 백양대로 12-1',35.162590358446,129.048081142434,'부산 부산진구 가야공원로 1',35.1532332080052,129.029827776357,'2026-07-29 08:00:00','2026-07-31 08:00:00','RECEIVED','2026-07-29 15:03:02','2026-11-10 16:57:02'),
(8,'TO-20260803-0007','경남 거제시 거제면 거제남서로 3215-3',34.8336578337757,128.60109520558,'경남 창원시 진해구 신항로 635-36',35.0797138990512,128.764818423144,'2026-08-26 08:00:00','2026-08-28 17:00:00','RECEIVED','2026-08-03 13:39:07','2026-08-03 15:00:19'),
(9,'TO-20260803-0008','부산 사상구 가야대로 1',35.1527830158489,128.973172344268,'부산 해운대구 APEC로 17',35.1659324519479,129.132411521392,'2026-08-26 08:04:00','2026-08-27 08:00:00','RECEIVED','2026-08-03 13:40:06','2026-08-03 15:00:19'),
(10,'TO-20261109-0009','부산 강서구 낙동남로 369',35.1112329486289,128.860372408132,'부산 기장군 기장읍 공수해안길 2',35.1835518082308,129.210356757035,'2026-11-09 18:00:00','2026-11-10 09:00:00','DISPATCHED','2026-11-09 17:04:32','2026-11-10 17:13:26'),
(11,'TO-20261110-0010','부산 강서구 낙동남로 376',35.1104112301569,128.860137565033,'부산 기장군 정관읍 가동길 8',35.3262776898141,129.179715597209,'2026-11-11 09:00:00','2026-11-11 18:00:00','RECEIVED','2026-11-10 16:39:22','2026-11-10 16:39:22'),
(12,'TO-20261111-0011','경기 성남시 분당구 판교역로 6',37.3837334150366,127.111970228259,'경기 성남시 분당구 고기로 216',37.352322170204,127.082927288982,NULL,NULL,'RECEIVED','2026-11-11 09:17:33','2026-11-11 09:17:33');
/*!40000 ALTER TABLE `transport_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `vehicle`
--

DROP TABLE IF EXISTS `vehicle`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `vehicle` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '차량 ID',
  `vehicle_no` varchar(20) NOT NULL COMMENT '차량번호',
  `vehicle_type` varchar(20) NOT NULL COMMENT '차량유형 (1TON, 2_5TON, 5TON 등 톤수)',
  `max_weight_kg` decimal(8,2) NOT NULL COMMENT '최대 적재중량 (kg)',
  `bed_width_cm` int(11) NOT NULL COMMENT '적재함 가로 (cm)',
  `bed_length_cm` int(11) NOT NULL COMMENT '적재함 세로 (cm)',
  `bed_height_cm` int(11) NOT NULL COMMENT '적재함 높이 (cm)',
  `base_address` varchar(255) DEFAULT NULL COMMENT '차고지 주소',
  `base_lat` double DEFAULT NULL,
  `base_lng` double DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '차량상태 (AVAILABLE:운행가능, ASSIGNED:배차중, MAINTENANCE:정비중)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `vehicle_no` (`vehicle_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='차량';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `vehicle`
--

LOCK TABLES `vehicle` WRITE;
/*!40000 ALTER TABLE `vehicle` DISABLE KEYS */;
INSERT INTO `vehicle` VALUES
(1,'12가3456','1TON',1000.00,160,280,170,NULL,NULL,NULL,'ASSIGNED'),
(2,'34나5678','2_5TON',2500.00,180,420,190,NULL,NULL,NULL,'AVAILABLE'),
(3,'56다7890','3_5TON',3500.00,200,470,200,NULL,NULL,NULL,'AVAILABLE'),
(4,'78라1234','5TON',5000.00,230,620,220,NULL,NULL,NULL,'AVAILABLE'),
(5,'90마5678','11TON',11000.00,230,950,240,NULL,NULL,NULL,'AVAILABLE'),
(6,'12가1234','1TON',1000.00,160,280,170,'경남 창원시 진해구 신항로 635-36',35.0797138990512,128.764818423144,'ASSIGNED'),
(7,'34나1234','2_5TON',2500.00,180,420,190,'부산 연제구 거제대로 92',35.176698643672,129.068165870973,'ASSIGNED'),
(8,'56다1234','3_5TON',3500.00,200,470,200,'경남 양산시 물금읍 부산대학로 16',35.3246670253856,129.000796240407,'AVAILABLE'),
(9,'78라5484','5TON',5000.00,230,620,220,'부산 해운대구 APEC로 17',35.1659324519479,129.132411521392,'AVAILABLE'),
(10,'90마1234','11TON',11000.00,230,950,240,'경남 창원시 진해구 신항로 635-36',35.0797138990512,128.764818423144,'AVAILABLE');
/*!40000 ALTER TABLE `vehicle` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping events for database 'tms'
--

--
-- Dumping routines for database 'tms'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-13 10:52:24
