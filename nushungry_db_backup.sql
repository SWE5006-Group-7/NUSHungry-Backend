-- MySQL dump 10.13  Distrib 8.4.6, for Win64 (x86_64)
--
-- Host: localhost    Database: nushungry_db
-- ------------------------------------------------------
-- Server version	8.4.6

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cafeteria`
--

DROP TABLE IF EXISTS `cafeteria`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cafeteria` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `description` varchar(255) DEFAULT NULL,
  `latitude` double NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `longitude` double NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `nearest_bus_stop` varchar(255) DEFAULT NULL,
  `nearest_carpark` varchar(255) DEFAULT NULL,
  `halal_info` varchar(255) DEFAULT NULL,
  `seating_capacity` int DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `term_time_opening_hours` varchar(255) DEFAULT NULL,
  `vacation_opening_hours` varchar(255) DEFAULT NULL,
  `average_rating` decimal(3,2) DEFAULT '0.00' COMMENT '平均评分',
  `review_count` int DEFAULT '0' COMMENT '评价数量',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cafeteria`
--

LOCK TABLES `cafeteria` WRITE;
/*!40000 ALTER TABLE `cafeteria` DISABLE KEYS */;
INSERT INTO `cafeteria` VALUES (1,'Fine Food',1.305,'Town Plaza',103.773,'Fine Food','University Town','Stephen Riady Centre','HALAL FOOD OPTIONS AVAILABLE',410,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Fine-Food-1-1024x684-1-898x600.jpg','Mon-Sun, 8.00am-8.30pm','Mon-Sun, 8.00am-8.30pm',0.00,0),(2,'Flavours @ UTown',1.30462,'UTown Stephen Riady Centre',103.77252,'Flavours @ UTown','University Town','Stephen Riady Centre','HALAL FOOD OPTIONS AVAILABLE',700,'https://uci.nus.edu.sg/wp-content/uploads/2025/08/Flavours-938x600.jpg','Mon-Sun, 7.30am-8.30pm','Mon-Sun, 7.30am-8.30pm',0.00,0),(3,'Central Square @ YIH',1.29886,'Yusof Ishak House',103.77432,'Central Square @ YIH','Yusof Ishak House','CP4 and CP5','HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',314,'https://uci.nus.edu.sg/wp-content/uploads/2025/05/YIH-800x600.jpg','Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm','Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm',0.00,0),(4,'Frontier',1.2961,'Faculty of Science',103.7831,'Frontier','Lower Kent Ridge Road - Blk S17','CP7','HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',700,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Frontier-Canteen-1024x684-1-898x600.jpg','Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed','Mon-Fri, 7.00am-7.00pm; Sat, 7.00am-2.00pm',0.00,0),(5,'PGP Aircon Canteen',1.29112,'Prince George\'s Park',103.78036,'PGP Aircon Canteen','Prince George\'s Park','Prince George\'s Park Foyer','VEGETARIAN FOOD OPTION AVAILABLE',308,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/PGP-canteen.jpg','Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm','Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm',0.00,0),(6,'Techno Edge',1.29796,'College of Design and Engineering',103.77153,'Techno Edge','Kent Ridge Crescent - Information Technology','CP17','HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',450,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Techno-Edge-1024x684-1-898x600.jpg','Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed','Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed',0.00,0),(7,'The Deck',1.2948,'Faculty of Arts & Social Sciences',103.7715,'The Deck','Lower Kent Ridge Rd - Blk S12','CP11, CP15','HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',1018,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/deck.jpg','Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed','Mon-Fri, 7.00am-6.00pm',0.00,0),(8,'The Terrace',1.2965,'Computing 3 (COM3)',103.7725,'The Terrace','COM 3','CP11','HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',756,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/WhatsApp-Image-2022-12-08-at-1.37.44-PM-1-1024x768-1-800x600.jpeg','Mon-Fri, 7.30am-7.00pm; Sat, 7.30am-2.00pm; Sun closed','Mon-Fri, 7.30am-3.00pm; Sat-Sun closed',0.00,0);
/*!40000 ALTER TABLE `cafeteria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `favorites`
--

DROP TABLE IF EXISTS `favorites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `favorites` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `stall_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `sort_order` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKg7bk3oavn6h0ayknbflxe2fsq` (`user_id`,`stall_id`),
  KEY `FKssdxaf4bnrbwcu3mgr3qykdxs` (`stall_id`),
  CONSTRAINT `FKk7du8b8ewipawnnpg76d55fus` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKssdxaf4bnrbwcu3mgr3qykdxs` FOREIGN KEY (`stall_id`) REFERENCES `stall` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `favorites`
--

LOCK TABLES `favorites` WRITE;
/*!40000 ALTER TABLE `favorites` DISABLE KEYS */;
INSERT INTO `favorites` VALUES (1,'2025-10-10 09:44:03.776948',1,2,1760089443),(2,'2025-10-10 09:44:07.499948',4,2,1760089447),(3,'2025-10-10 09:44:12.944436',6,2,1760089452),(7,'2025-10-10 10:05:46.356440',1,1,1760090746),(11,'2025-10-12 08:03:22.428680',3,6,1);
/*!40000 ALTER TABLE `favorites` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `image`
--

DROP TABLE IF EXISTS `image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `image` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `file_name` varchar(255) NOT NULL COMMENT '文件名(含UUID)',
  `original_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_path` varchar(500) NOT NULL COMMENT '文件存储路径',
  `file_url` varchar(500) NOT NULL COMMENT '访问URL',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图URL',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `content_type` varchar(100) NOT NULL COMMENT 'MIME类型',
  `width` int DEFAULT NULL COMMENT '图片宽度',
  `height` int DEFAULT NULL COMMENT '图片高度',
  `uploaded_by` bigint DEFAULT NULL COMMENT '上传用户ID',
  `entity_type` varchar(50) DEFAULT NULL COMMENT '关联实体类型(STALL/CAFETERIA/REVIEW)',
  `entity_id` bigint DEFAULT NULL COMMENT '关联实体ID',
  `created_at` datetime(6) DEFAULT NULL COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `FK_image_user` (`uploaded_by`),
  KEY `idx_entity` (`entity_type`,`entity_id`),
  KEY `idx_uploaded_by` (`uploaded_by`,`created_at`),
  CONSTRAINT `FK_image_user` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image`
--

LOCK TABLES `image` WRITE;
/*!40000 ALTER TABLE `image` DISABLE KEYS */;
/*!40000 ALTER TABLE `image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `images`
--

DROP TABLE IF EXISTS `images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `images` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `image_url` varchar(255) NOT NULL,
  `thumbnail_url` varchar(255) DEFAULT NULL,
  `image_type` enum('PHOTO','MENU') NOT NULL,
  `uploaded_at` datetime(6) DEFAULT NULL,
  `uploaded_by` varchar(255) DEFAULT NULL,
  `cafeteria_id` bigint DEFAULT NULL,
  `stall_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKom2m7r609xi9cotfqbc0w2v6m` (`cafeteria_id`),
  KEY `FKe0tr7sja9p6y8rf8ja2mmydw1` (`stall_id`),
  CONSTRAINT `FKe0tr7sja9p6y8rf8ja2mmydw1` FOREIGN KEY (`stall_id`) REFERENCES `stall` (`id`),
  CONSTRAINT `FKom2m7r609xi9cotfqbc0w2v6m` FOREIGN KEY (`cafeteria_id`) REFERENCES `cafeteria` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `images`
--

LOCK TABLES `images` WRITE;
/*!40000 ALTER TABLE `images` DISABLE KEYS */;
INSERT INTO `images` VALUES (1,'/uploads/images/2025/10/13/4af173d6-7fe6-4bd7-bdbf-46cc8b3cf8d6.jpg','/uploads/thumbnails/db8752a8-c6e6-46b2-931c-a8a55bc270f2.jpg','PHOTO','2025-10-12 21:11:23.099082','anonymousUser',NULL,1),(2,'/uploads/images/2025/10/13/b1360c3d-ea85-4d78-ab5f-96b0318b675e.jpg','/uploads/thumbnails/176cdd68-fd73-4859-8317-08baa0d471e1.jpg','MENU','2025-10-12 21:11:28.383467','anonymousUser',NULL,1),(3,'/uploads/images/2025/10/13/47e17800-3d0e-4548-a3c0-950ceba0fae8.jpg','/uploads/thumbnails/35cbe8c3-951a-4104-a67e-6ef0f7208cb0.jpg','PHOTO','2025-10-12 21:11:37.444902','anonymousUser',NULL,4);
/*!40000 ALTER TABLE `images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `moderation_log`
--

DROP TABLE IF EXISTS `moderation_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `moderation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` enum('PENDING','APPROVED','REJECTED') NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `reason` text,
  `moderator_id` bigint NOT NULL,
  `review_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK5ulk497y3l3ttbo39hr4686ug` (`moderator_id`),
  KEY `FK8ah3wc8mivrjtv96xb7a6wjqn` (`review_id`),
  CONSTRAINT `FK5ulk497y3l3ttbo39hr4686ug` FOREIGN KEY (`moderator_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK8ah3wc8mivrjtv96xb7a6wjqn` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `moderation_log`
--

LOCK TABLES `moderation_log` WRITE;
/*!40000 ALTER TABLE `moderation_log` DISABLE KEYS */;
INSERT INTO `moderation_log` VALUES (1,'REJECTED','2025-10-10 10:16:37.006590','内容包含敏感词或不当言论',1,1),(2,'APPROVED','2025-10-12 18:06:18.875236',NULL,1,2),(3,'APPROVED','2025-10-12 18:06:22.110134',NULL,1,3);
/*!40000 ALTER TABLE `moderation_log` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `refresh_tokens`
--

DROP TABLE IF EXISTS `refresh_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `refresh_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `ip_address` varchar(45) DEFAULT NULL,
  `last_used_at` datetime(6) DEFAULT NULL,
  `revoked` bit(1) DEFAULT NULL,
  `revoked_at` datetime(6) DEFAULT NULL,
  `token` varchar(500) NOT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_ghpmfn23vmxfu3spu3lfg4r2d` (`token`),
  KEY `FK1lih5y2npsf8u5o3vhdb9y0os` (`user_id`),
  CONSTRAINT `FK1lih5y2npsf8u5o3vhdb9y0os` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `refresh_tokens`
--

LOCK TABLES `refresh_tokens` WRITE;
/*!40000 ALTER TABLE `refresh_tokens` DISABLE KEYS */;
INSERT INTO `refresh_tokens` VALUES (1,'2025-10-11 14:19:49.525480','2025-11-10 14:19:49.521905','0:0:0:0:0:0:0:1','2025-10-11 14:19:49.521905',_binary '','2025-10-11 15:15:48.579436','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMTkyMzg5LCJleHAiOjE3NjI3ODQzODl9.lv5DN4zN-budRESbEKsh6giCsEtc6VXOYSj72x_Y3K0','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',1),(2,'2025-10-11 15:16:09.920535','2025-11-10 15:16:09.920535','0:0:0:0:0:0:0:1','2025-10-11 15:16:09.920535',_binary '','2025-10-12 06:40:47.836876','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImNoYSIsImlhdCI6MTc2MDE5NTc2OSwiZXhwIjoxNzYyNzg3NzY5fQ.fcVqizGiqHj56uiBkjkv240vVvPjwCFC01l2bOOJCAg','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',3),(3,'2025-10-12 06:40:50.923549','2025-11-11 06:40:50.923549','0:0:0:0:0:0:0:1','2025-10-12 06:40:50.923549',_binary '','2025-10-12 06:41:01.917333','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMjUxMjUwLCJleHAiOjE3NjI4NDMyNTB9.Efs4Nr8So1Tnx4C1X7fyuHmgehx0Q5Bjtz7hS0p-Hqc','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',1),(4,'2025-10-12 06:41:06.242877','2025-11-11 06:41:06.241875','0:0:0:0:0:0:0:1','2025-10-12 06:41:06.241875',_binary '','2025-10-12 06:41:42.898224','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImNhb2hvbmciLCJpYXQiOjE3NjAyNTEyNjYsImV4cCI6MTc2Mjg0MzI2Nn0.dIRe3-gGt-QXanG2bXTEb01Ttgkvv_9o8e_jKPFVAMk','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',2),(5,'2025-10-12 06:42:15.338917','2025-11-11 06:42:15.338411','0:0:0:0:0:0:0:1','2025-10-12 06:42:15.338411',_binary '','2025-10-12 06:42:25.819356','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFzZGFzIiwiaWF0IjoxNzYwMjUxMzM1LCJleHAiOjE3NjI4NDMzMzV9.GrqYtNBX5tNdJ27nwh61Sef_ac5K8X4bDwzQkUeNJwA','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',4),(6,'2025-10-12 06:42:53.184467','2025-11-11 06:42:53.183944','0:0:0:0:0:0:0:1','2025-10-12 06:42:53.184467',_binary '','2025-10-12 06:43:03.672245','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFzZGFzMTEiLCJpYXQiOjE3NjAyNTEzNzMsImV4cCI6MTc2Mjg0MzM3M30.NftAYnTGYEkUOWIck-EfVwCwcYXLV4mPkfIvzo3RTLA','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',5),(7,'2025-10-12 06:43:16.881664','2025-11-11 06:43:16.881664','0:0:0:0:0:0:0:1','2025-10-12 06:43:16.881664',_binary '','2025-10-12 10:01:32.378027','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFzZGFzMjIiLCJpYXQiOjE3NjAyNTEzOTYsImV4cCI6MTc2Mjg0MzM5Nn0.ATI8niD-aDcWbuqFsU_X_qXYD_HS67hD-hyINenJZhI','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',6),(8,'2025-10-12 10:01:35.866239','2025-11-11 10:01:35.863728','0:0:0:0:0:0:0:1','2025-10-12 10:01:35.863728',_binary '','2025-10-12 10:02:47.236476','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMjYzMjk1LCJleHAiOjE3NjI4NTUyOTV9.UlXfrVOFjq9hgH-Iwg-XTPJ8HjwuGIwdh6H4DXwAuwc','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',1),(9,'2025-10-12 10:02:53.348570','2025-11-11 10:02:53.347563','0:0:0:0:0:0:0:1','2025-10-12 10:02:53.347563',_binary '','2025-10-12 10:02:56.081283','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFzZGFzMTEiLCJpYXQiOjE3NjAyNjMzNzMsImV4cCI6MTc2Mjg1NTM3M30.OdJKuPNaLJxwb0mq9v56AY3rJal-Cd3LRQLgJHyeakM','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',5),(10,'2025-10-12 10:03:00.974266','2025-11-11 10:03:00.973267','0:0:0:0:0:0:0:1','2025-10-12 14:40:35.892000',_binary '','2025-10-12 15:08:26.878975','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFzZGFzMjIiLCJpYXQiOjE3NjAyNjMzODAsImV4cCI6MTc2Mjg1NTM4MH0.CSC6Up6ua31XFosTCBNhvBXMYem56IjdbOQq1OHXhkc','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',6),(11,'2025-10-12 15:08:37.743031','2025-11-11 15:08:37.740470','0:0:0:0:0:0:0:1','2025-10-12 15:08:37.740470',_binary '','2025-10-12 15:08:53.436818','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFzZGFzMTEiLCJpYXQiOjE3NjAyODE3MTcsImV4cCI6MTc2Mjg3MzcxN30.sUj9S51SfqtWgXopM_0mViKgQ6ndSqS_HbalTiwOtLk','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',5),(12,'2025-10-12 15:09:02.240296','2025-11-11 15:09:02.239229','0:0:0:0:0:0:0:1','2025-10-12 15:09:02.239229',_binary '','2025-10-12 15:09:12.430450','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImNhb2hvbmciLCJpYXQiOjE3NjAyODE3NDIsImV4cCI6MTc2Mjg3Mzc0Mn0.p4I194mInSofvbJVAlS38PY2x3jabuEJpTm9VYwjI8k','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',2),(13,'2025-10-12 15:09:38.429409','2025-11-11 15:09:38.429409','0:0:0:0:0:0:0:1','2025-10-12 15:09:38.429409',_binary '','2025-10-12 15:09:57.264644','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6IjMyMTMyMTMyMTMxIiwiaWF0IjoxNzYwMjgxNzc4LCJleHAiOjE3NjI4NzM3Nzh9.-mgZyK6wMcMloRWQe7YW_D2o5_7ygmEhdKW0-w_6bLc','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',7),(14,'2025-10-12 15:10:10.327540','2025-11-11 15:10:10.327020','0:0:0:0:0:0:0:1','2025-10-12 15:10:10.327020',_binary '','2025-10-12 18:11:58.646210','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6IjExMTExMTExMTExMTExMTExMSIsImlhdCI6MTc2MDI4MTgxMCwiZXhwIjoxNzYyODczODEwfQ.JwGjzOxO7LdTFFTFQnPGMBmiQ6tymCwLZyPd0bZWbhI','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',8),(15,'2025-10-12 18:12:14.152763','2025-11-11 18:12:14.151764','0:0:0:0:0:0:0:1','2025-10-12 18:12:14.151764',_binary '','2025-10-12 18:12:26.423791','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6IjIxMzEyMzEyIiwiaWF0IjoxNzYwMjkyNzM0LCJleHAiOjE3NjI4ODQ3MzR9.zfIbxv3pOnUQ3-9iT4wUUzuyghcUoerqQFtgAmChH5A','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',9),(16,'2025-10-12 18:12:30.318845','2025-11-11 18:12:30.317839','0:0:0:0:0:0:0:1','2025-10-12 19:11:13.341905',_binary '','2025-10-13 05:27:05.222142','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMjkyNzUwLCJleHAiOjE3NjI4ODQ3NTB9.C3SeLMVJCAoA4ZPo2ClD3MlMQSNEnQXZ0CKYIbPg0bA','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',1),(17,'2025-10-13 05:27:08.942241','2025-11-12 05:27:08.940670','0:0:0:0:0:0:0:1','2025-10-13 05:27:08.940670',_binary '','2025-10-13 05:29:39.263520','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6IjIxMzEyMzEyIiwiaWF0IjoxNzYwMzMzMjI4LCJleHAiOjE3NjI5MjUyMjh9.p8tJeFZt02rMexqgVfM0qX1fl7X-Rj-P6GycAXy0u0A','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',9),(18,'2025-10-13 05:29:42.654946','2025-11-12 05:29:42.653940','0:0:0:0:0:0:0:1','2025-10-13 05:29:42.653940',_binary '\0',NULL,'eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMzMzMzgyLCJleHAiOjE3NjI5MjUzODJ9.np_aLEufcCom08xF8jlH2Fnn2emYnSAt7SrDuFAsmgs','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',1),(19,'2025-10-13 09:19:17.321235','2025-11-12 09:19:17.317165','0:0:0:0:0:0:0:1','2025-10-13 09:19:17.317165',_binary '','2025-10-13 09:19:30.606357','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMzQ3MTU3LCJleHAiOjE3NjI5MzkxNTd9.TK1pcUqMcufHcxY4tKVMWfdtNOwlFwX1xIxu3Amp5W8','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',1),(20,'2025-10-13 09:19:34.581347','2025-11-12 09:19:34.580841','0:0:0:0:0:0:0:1','2025-10-13 09:19:34.580841',_binary '','2025-10-13 09:21:00.264145','eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6IjExMTExMTExMTExMTExMTExMSIsImlhdCI6MTc2MDM0NzE3NCwiZXhwIjoxNzYyOTM5MTc0fQ.WPnTgZfqt1OR7tsPwxi4CmyvBjTiTU3W-1dcosKvDQg','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',8),(21,'2025-10-13 09:21:05.706957','2025-11-12 09:21:05.706957','0:0:0:0:0:0:0:1','2025-10-13 10:42:16.147859',_binary '\0',NULL,'eyJhbGciOiJIUzI1NiJ9.eyJ0eXBlIjoicmVmcmVzaCIsInN1YiI6ImFkbWluIiwiaWF0IjoxNzYwMzQ3MjY1LCJleHAiOjE3NjI5MzkyNjV9.9Z4MlUYZHmDao4u_g2yzOkISzliKlq3voimNJxglh4o','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36 Edg/141.0.0.0',1);
/*!40000 ALTER TABLE `refresh_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '评价用户ID',
  `stall_id` bigint NOT NULL COMMENT '摊位ID',
  `rating` double NOT NULL,
  `comment` text NOT NULL COMMENT '评价内容(10-1000字符)',
  `image_urls` json DEFAULT NULL COMMENT '评价图片URL数组',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  `author` varchar(255) DEFAULT NULL,
  `processed` tinyint(1) DEFAULT '0',
  `moderated_at` datetime(6) DEFAULT NULL,
  `moderated_by` varchar(255) DEFAULT NULL,
  `moderation_status` enum('PENDING','APPROVED','REJECTED') NOT NULL,
  `reject_reason` varchar(255) DEFAULT NULL,
  `environment_rating` double DEFAULT NULL,
  `like_count` int NOT NULL DEFAULT '0',
  `service_rating` double DEFAULT NULL,
  `taste_rating` double DEFAULT NULL,
  `value_rating` double DEFAULT NULL,
  `likes_count` int DEFAULT '0',
  `number_of_people` int DEFAULT NULL,
  `total_cost` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_review_user` (`user_id`),
  KEY `FK_review_stall` (`stall_id`),
  KEY `idx_stall_created` (`stall_id`,`created_at`),
  KEY `idx_user_created` (`user_id`,`created_at`),
  CONSTRAINT `FK_review_stall` FOREIGN KEY (`stall_id`) REFERENCES `stall` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_review_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_rating` CHECK (((`rating` >= 1) and (`rating` <= 5)))
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
INSERT INTO `review` VALUES (1,2,1,4,'大萨达撒大声地啊啊啊啊啊',NULL,'2025-10-10 10:03:07.969356','2025-10-11 14:20:00.622551','caohong',0,'2025-10-10 10:16:37.000075','admin','REJECTED','内容包含敏感词或不当言论',NULL,0,NULL,NULL,NULL,0,NULL,NULL),(2,1,2,4,'阿斯蒂芬发生的112',NULL,'2025-10-12 06:40:45.018930','2025-10-12 18:06:18.913013','admin',0,'2025-10-12 18:06:18.870385','admin','APPROVED',NULL,NULL,0,NULL,NULL,NULL,2,NULL,NULL),(3,6,2,4.5,'还可以还可以还可以还可以',NULL,'2025-10-12 14:10:42.489709','2025-10-12 18:06:22.112645','asdas22',0,'2025-10-12 18:06:22.110134','admin','APPROVED',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(4,5,2,3,'啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊',NULL,'2025-10-12 15:08:48.859056','2025-10-12 15:08:48.859056','asdas11',0,NULL,NULL,'PENDING',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(5,2,2,4.5,'111111111111111111111111111111',NULL,'2025-10-12 15:09:10.045307','2025-10-12 15:09:10.045307','caohong',0,NULL,NULL,'PENDING',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(6,7,2,3.5,'333333333333333333333333',NULL,'2025-10-12 15:09:51.258601','2025-10-12 15:09:51.258601','32132132131',0,NULL,NULL,'PENDING',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(7,8,2,4.5,'111111111111111111111111',NULL,'2025-10-12 15:10:19.797144','2025-10-12 15:10:19.797144','111111111111111111',0,NULL,NULL,'PENDING',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(8,9,2,3.5,'没有敏感词没有敏感词',NULL,'2025-10-12 18:12:22.528699','2025-10-12 18:12:22.528699','21312312',0,'2025-10-12 18:12:22.527699','SYSTEM','APPROVED',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(9,1,5,4,'无敏感词无敏感词无敏感词无敏感词',NULL,'2025-10-12 18:36:42.822373','2025-10-12 18:36:42.822373','admin',0,NULL,NULL,'PENDING',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(10,1,7,5,'bucuobucuo',NULL,'2025-10-13 04:48:00.352210','2025-10-13 04:48:00.352210','admin',0,NULL,NULL,'PENDING',NULL,NULL,0,NULL,NULL,NULL,0,NULL,NULL),(11,1,1,3.5,'11111111111111111111',NULL,'2025-10-13 10:42:16.240490','2025-10-13 10:42:16.240490','admin',0,NULL,NULL,'PENDING',NULL,NULL,0,NULL,NULL,NULL,0,2,15);
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_images`
--

DROP TABLE IF EXISTS `review_images`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_images` (
  `review_id` bigint NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  KEY `FKn6ocagcwsaswdoh2ntvrkk5en` (`review_id`),
  CONSTRAINT `FKn6ocagcwsaswdoh2ntvrkk5en` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_images`
--

LOCK TABLES `review_images` WRITE;
/*!40000 ALTER TABLE `review_images` DISABLE KEYS */;
INSERT INTO `review_images` VALUES (1,'/uploads/images/2025/10/10/9bba4a3a-6da3-4a5a-9b8f-30d49b91b87b.png'),(9,'/uploads/images/2025/10/13/cdf12812-2e9b-48aa-9984-b58c72a85162.jpg'),(9,'/uploads/images/2025/10/13/e992f757-a2fb-4aa6-8258-e43e2d2c22c3.jpg'),(9,'/uploads/images/2025/10/13/ce59f4f2-be5f-4af8-9885-aef09034305c.jpg'),(9,'/uploads/images/2025/10/13/83488593-d058-4814-851b-eb12788147f0.jpg'),(9,'/uploads/images/2025/10/13/93c4b86f-b32f-4028-900c-295ad87fdae2.jpg');
/*!40000 ALTER TABLE `review_images` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_likes`
--

DROP TABLE IF EXISTS `review_likes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_likes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `review_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_review_user` (`review_id`,`user_id`),
  UNIQUE KEY `UKb74o5l2fmrgqg556d9nyop0ns` (`review_id`,`user_id`),
  KEY `idx_review_id` (`review_id`),
  KEY `idx_user_id` (`user_id`),
  CONSTRAINT `FKnrdt2o636j9xws6sdviim740j` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`),
  CONSTRAINT `FKnual15vv88tiqnwmi60tb2l8d` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_likes`
--

LOCK TABLES `review_likes` WRITE;
/*!40000 ALTER TABLE `review_likes` DISABLE KEYS */;
INSERT INTO `review_likes` VALUES (7,'2025-10-12 06:40:55.545449',2,1),(8,'2025-10-12 06:41:08.238554',2,2);
/*!40000 ALTER TABLE `review_likes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review_reports`
--

DROP TABLE IF EXISTS `review_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review_reports` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `description` text,
  `handled_at` datetime(6) DEFAULT NULL,
  `handler_id` bigint DEFAULT NULL,
  `handler_note` text,
  `reason` enum('SPAM','OFFENSIVE','INAPPROPRIATE','FALSE_INFO','OFF_TOPIC','DUPLICATE','OTHER') NOT NULL,
  `status` enum('PENDING','REVIEWING','RESOLVED','REJECTED','CLOSED') NOT NULL,
  `reporter_id` bigint NOT NULL,
  `review_id` bigint NOT NULL,
  `handle_note` varchar(500) DEFAULT NULL,
  `handled_by` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_review_id` (`review_id`),
  KEY `idx_reporter_id` (`reporter_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `FK4p0e6ec0md2tqu2h9yunl4ds4` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKhhtiv0803yenhkj9yvnrhf118` FOREIGN KEY (`review_id`) REFERENCES `review` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review_reports`
--

LOCK TABLES `review_reports` WRITE;
/*!40000 ALTER TABLE `review_reports` DISABLE KEYS */;
INSERT INTO `review_reports` VALUES (1,'2025-10-11 14:20:04.698058','1321',NULL,NULL,NULL,'OFFENSIVE','PENDING',1,1,NULL,NULL),(2,'2025-10-11 15:16:16.694625','12321321','2025-10-11 15:44:57.464033',NULL,NULL,'OFFENSIVE','REJECTED',3,1,'管理员驳回了此举报','admin'),(3,'2025-10-12 06:40:59.385462','',NULL,NULL,NULL,'OFFENSIVE','PENDING',1,2,NULL,NULL),(4,'2025-10-12 06:41:22.784615','31',NULL,NULL,NULL,'SPAM','PENDING',2,2,NULL,NULL),(5,'2025-10-12 06:42:22.646175','',NULL,NULL,NULL,'INAPPROPRIATE','PENDING',4,2,NULL,NULL),(6,'2025-10-12 06:43:01.539145','',NULL,NULL,NULL,'FALSE_INFO','PENDING',5,2,NULL,NULL),(7,'2025-10-12 07:26:08.613341','',NULL,NULL,NULL,'OFF_TOPIC','PENDING',6,2,NULL,NULL);
/*!40000 ALTER TABLE `review_reports` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `search_history`
--

DROP TABLE IF EXISTS `search_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `search_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint DEFAULT NULL,
  `keyword` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `search_time` datetime NOT NULL,
  `search_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result_count` int DEFAULT NULL,
  `ip_address` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_search_time` (`search_time`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `search_history`
--

LOCK TABLES `search_history` WRITE;
/*!40000 ALTER TABLE `search_history` DISABLE KEYS */;
INSERT INTO `search_history` VALUES (1,1,'chinese','2025-10-10 21:18:16','stall',1,'0:0:0:0:0:0:0:1'),(2,1,'还行？','2025-10-10 21:18:30','stall',0,'0:0:0:0:0:0:0:1'),(3,1,'chinese','2025-10-10 21:18:35','stall',1,'0:0:0:0:0:0:0:1'),(4,1,'chinese','2025-10-10 21:27:36','stall',1,'0:0:0:0:0:0:0:1'),(5,6,'chinese','2025-10-12 14:22:15','stall',1,'0:0:0:0:0:0:0:1');
/*!40000 ALTER TABLE `search_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stall`
--

DROP TABLE IF EXISTS `stall`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stall` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `contact` varchar(255) DEFAULT NULL,
  `cuisine_type` varchar(255) DEFAULT NULL,
  `halal_info` varchar(255) DEFAULT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `cafeteria_id` bigint DEFAULT NULL,
  `average_rating` double DEFAULT NULL,
  `review_count` int DEFAULT '0' COMMENT '评价数量',
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `average_price` double DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_stall_cafeteria` (`cafeteria_id`),
  CONSTRAINT `FK_stall_cafeteria` FOREIGN KEY (`cafeteria_id`) REFERENCES `cafeteria` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stall`
--

LOCK TABLES `stall` WRITE;
/*!40000 ALTER TABLE `stall` DISABLE KEYS */;
INSERT INTO `stall` VALUES (1,'N/A','Chinese','HALAL FOOD OPTIONS AVAILABLE','https://www.taiwanichiban.com//image/cache/catalog/social/slider1-1920x800.jpg','Taiwan Ichiban',2,3.8,2,NULL,'2025-10-13 10:42:16.278791',1.30462,103.77252,7.5),(2,'N/A','Chinese cuisine','HALAL FOOD OPTIONS AVAILABLE','https://www.shicheng.news/images/image/1720/17206176.avif?1682420429','Chinese',2,4,3,NULL,'2025-10-12 18:12:22.541296',1.30462,103.77252,NULL),(3,'N/A','Pasta and Western cuisine','HALAL FOOD OPTIONS AVAILABLE','https://i0.wp.com/shopsinsg.com/wp-content/uploads/2020/10/build-your-own-pasta.jpg?w=560&ssl=1','Pasta Express',2,0,0,NULL,'2025-10-10 16:55:02.445542',1.30462,103.77252,NULL),(4,'N/A','Vegetarian cuisine','HALAL FOOD OPTIONS AVAILABLE','https://food-cms.grab.com/compressed_webp/merchants/4-C263REEVRJ2YNT/hero/b0183d8d0f9e47fe9e2583c52a558ddf_1641779012807417776.webp','Ruyi Yuan Vegetarian',1,0,0,NULL,'2025-10-10 18:00:40.193375',1.305,103.773,NULL),(5,'N/A','Yong Tao Foo','HALAL FOOD OPTIONS AVAILABLE','https://www.shicheng.news/images/image/1720/17206190.avif?1682420423','Yong Tao Foo',2,4,1,NULL,'2025-10-12 18:36:42.840452',1.30462,103.77252,NULL),(6,'N/A','Western cuisine','HALAL FOOD OPTIONS AVAILABLE','https://scontent.fsin14-1.fna.fbcdn.net/v/t39.30808-6/481079656_944403831193045_946033071545871075_n.jpg?_nc_cat=103&ccb=1-7&_nc_sid=127cfc&_nc_ohc=sJ1hi8uBELQQ7kNvwGXR15R&_nc_oc=Adl6g8Zj6XN3ZTT-XnQ3oKg-DzOqNFtcqjpP7JpMT5DPhdmWRQgVjG5VlFlXXQSHrrc&_nc_zt=2','Western Crave',3,0,0,NULL,'2025-10-10 16:29:04.896394',1.29886,103.77432,NULL),(7,'N/A','Default','HALAL FOOD OPTIONS AVAILABLE','https://th.bing.com/th/id/OIP.swinVrT6m0hoTGPwblccPgHaHa?w=192&h=193&c=7&r=0&o=5&dpr=2.4&pid=1.7','Default',1,5,1,NULL,'2025-10-13 04:48:00.370248',1.305,103.773,NULL),(8,'','Japanese','Muslim-owned','','试试看',NULL,0,0,'2025-10-13 04:49:13.621231','2025-10-13 11:17:59.810825',1.305205,103.773,NULL);
/*!40000 ALTER TABLE `stall` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(100) NOT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `role` enum('ROLE_USER','ROLE_ADMIN') NOT NULL,
  `avatar_url` varchar(500) DEFAULT NULL,
  `last_login` datetime(6) DEFAULT NULL,
  `bio` varchar(500) DEFAULT NULL,
  `date_of_birth` date DEFAULT NULL,
  `dietary_restrictions` varchar(200) DEFAULT NULL,
  `display_name` varchar(100) DEFAULT NULL,
  `email_notification_enabled` bit(1) DEFAULT NULL,
  `favorite_cuisines` varchar(200) DEFAULT NULL,
  `gender` varchar(10) DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `notification_enabled` bit(1) DEFAULT NULL,
  `phone_number` varchar(20) DEFAULT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `preferences` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_username` (`username`),
  UNIQUE KEY `UK_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'admin','$2b$10$rMxufY/xtJy6fhURCbzdeOWIzeTd2hk41KGYWy6GD7zXEZ8D93I7O','admin@nushungry.com','2025-10-09 17:15:23.000000','2025-10-12 10:01:18.528560',1,'ROLE_ADMIN','/uploads/avatars/c5ba8fd1-8fbe-4534-a16f-002e3453e623.jpg',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(2,'caohong','$2a$10$1.wOY8GG74RL4ea1PCxMQeON/xFvWfyp.oWjP5uHsuzJluzkKleKi','754413199@qq.com','2025-10-10 07:03:56.021748','2025-10-10 07:49:09.120740',1,'ROLE_USER',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(3,'cha','$2a$10$QAO4JQBHZmWEM.ShsD3AB.LF2MqtBOJoHaPU3FsQFHS/NVEeDk7Pu','caohong@qq.com','2025-10-11 15:16:09.893922','2025-10-11 15:16:09.893922',1,'ROLE_USER',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(4,'asdas','$2a$10$QOBrfdCxN9gOub.YTGObAOqXuKVUzY0TpxB/ftm05o36bSYNvDh8S','q@qq.com','2025-10-12 06:42:15.332272','2025-10-12 06:42:15.332272',1,'ROLE_USER',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(5,'asdas11','$2a$10$jT7akptkzP4aa3XCk52FWOIA1p6hk5gF1WjfRvJIyMwaXxVpvZsXK','qq@qq.com','2025-10-12 06:42:53.179711','2025-10-12 06:42:53.179711',1,'ROLE_USER',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(6,'asdas22','$2a$10$EjjI7ptnFe/JH4pjTpMBr.pZuYRAG5kbCKxFpM2l54xxaxK389B3e','q2@qq.com','2025-10-12 06:43:16.878146','2025-10-12 14:21:22.927143',1,'ROLE_USER','/uploads/avatars/624359f0-68c8-4a7c-ac20-be8413d0b144.jpg',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(7,'32132132131','$2a$10$cJu5oXhjxVZOcBeIK2BoBuHCWQvw5B327gvZ573fq6Y975sTczcSG','123@qq.ocm','2025-10-12 15:09:38.420683','2025-10-12 15:09:38.420683',1,'ROLE_USER',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(8,'111111111111111111','$2a$10$qLOKY72oXS6p9UzAq8w8h./38OwriywMeTRBbe6CNBetaIUDi2mVC','1231@qq.ocm','2025-10-12 15:10:10.318823','2025-10-12 15:10:10.318823',1,'ROLE_USER',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(9,'21312312','$2a$10$TlGbtdcvEvWxLRodIPmDbuj4NbPj9colJOmHAu95lB2YSY7GfXuuC','12311@qq.ocm','2025-10-12 18:12:14.144246','2025-10-12 19:16:54.561423',1,'ROLE_ADMIN',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `verification_codes`
--

DROP TABLE IF EXISTS `verification_codes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `verification_codes` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(10) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(255) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `ip_address` varchar(50) DEFAULT NULL,
  `type` varchar(50) NOT NULL,
  `used` bit(1) NOT NULL,
  `used_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `verification_codes`
--

LOCK TABLES `verification_codes` WRITE;
/*!40000 ALTER TABLE `verification_codes` DISABLE KEYS */;
INSERT INTO `verification_codes` VALUES (1,'675612','2025-10-10 07:37:03.383884','754413199@qq.com','2025-10-10 07:42:03.383884','0:0:0:0:0:0:0:1','PASSWORD_RESET',_binary '','2025-10-10 07:37:54.389509'),(2,'090682','2025-10-10 07:47:23.718793','754413199@qq.com','2025-10-10 07:52:23.718793','0:0:0:0:0:0:0:1','PASSWORD_RESET',_binary '','2025-10-10 07:48:03.779701'),(3,'440057','2025-10-10 07:48:35.067877','754413199@qq.com','2025-10-10 07:53:35.067877','0:0:0:0:0:0:0:1','PASSWORD_RESET',_binary '','2025-10-10 07:49:09.019835');
/*!40000 ALTER TABLE `verification_codes` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-10-13 21:14:44
