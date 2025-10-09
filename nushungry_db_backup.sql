-- MySQL dump 10.13  Distrib 8.4.6, for Win64 (x86_64)
--
-- Host: localhost    Database: nushungry_db
-- ------------------------------------------------------
-- Server version	8.4.6
--
-- 更新日期: 2025-10-10
-- 更新说明:
--   1. 修改 review 表结构(添加user_id、image_urls、updated_at,修改comment类型)
--   2. 在 stall 表添加 average_rating 和 review_count
--   3. 在 cafeteria 表添加 average_rating 和 review_count
--   4. 新增 image 表用于图片管理

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
  `role` varchar(20) NOT NULL DEFAULT 'ROLE_USER',
  `avatar_url` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_username` (`username`),
  UNIQUE KEY `UK_email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
-- 初始管理员账户
-- 用户名: admin
-- 密码: admin123 (BCrypt加密)
INSERT INTO `users` VALUES (
    1,
    'admin',
    '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
    'admin@nushungry.com',
    NOW(),
    NOW(),
    1,
    'ROLE_ADMIN',
    NULL
);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

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
INSERT INTO `cafeteria` VALUES (1,'Fine Food',1.305,'Town Plaza',103.773,'Fine Food','University Town','Stephen Riady Centre','HALAL FOOD OPTIONS AVAILABLE',410,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Fine-Food-1-1024x684-1-898x600.jpg','Mon-Sun, 8.00am-8.30pm','Mon-Sun, 8.00am-8.30pm',0.00,0);

INSERT INTO `cafeteria`
VALUES
    (
        2,
        'Flavours @ UTown',
        1.30462,
        'UTown Stephen Riady Centre',
        103.77252,
        'Flavours @ UTown',
        'University Town',      -- nearest_bus_stop
        'Stephen Riady Centre', -- nearest_carpark
        'HALAL FOOD OPTIONS AVAILABLE',
        700,
        'https://uci.nus.edu.sg/wp-content/uploads/2025/08/Flavours-938x600.jpg',
        'Mon-Sun, 7.30am-8.30pm',
        'Mon-Sun, 7.30am-8.30pm',
        0.00,
        0
    );

INSERT INTO `cafeteria`
VALUES
    (
        3,
        'Central Square @ YIH',
        1.29886,
        'Yusof Ishak House',
        103.77432,
        'Central Square @ YIH',
        'Yusof Ishak House',
        'CP4 and CP5',
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        314,
        'https://uci.nus.edu.sg/wp-content/uploads/2025/05/YIH-800x600.jpg',
        'Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm',
        'Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm',
        0.00,
        0
    );

INSERT INTO `cafeteria`
VALUES
    (
        4,
        'Frontier',
        1.2961,
        'Faculty of Science',
        103.7831,
        'Frontier',
        'Lower Kent Ridge Road - Blk S17',
        'CP7',
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        700,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Frontier-Canteen-1024x684-1-898x600.jpg',
        'Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed',
        'Mon-Fri, 7.00am-7.00pm; Sat, 7.00am-2.00pm',
        0.00,
        0
    );

INSERT INTO `cafeteria`
VALUES
    (
        5,
        'PGP Aircon Canteen',
        1.29112,
        'Prince George''s Park',
        103.78036,
        'PGP Aircon Canteen',
        'Prince George''s Park',
        'Prince George''s Park Foyer',
        'VEGETARIAN FOOD OPTION AVAILABLE',
        308,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/PGP-canteen.jpg',
        'Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm',
        'Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm',
        0.00,
        0
    );

INSERT INTO `cafeteria`
VALUES
    (
        6,
        'Techno Edge',
        1.29796,
        'College of Design and Engineering',
        103.77153,
        'Techno Edge',
        'Kent Ridge Crescent - Information Technology',
        'CP17',
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        450,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Techno-Edge-1024x684-1-898x600.jpg',
        'Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed',
        'Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed',
        0.00,
        0
    );

INSERT INTO `cafeteria`
VALUES
    (
        7,
        'The Deck',
        1.2948,
        'Faculty of Arts & Social Sciences',
        103.7715,
        'The Deck',
        'Lower Kent Ridge Rd - Blk S12',
        'CP11, CP15',
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        1018,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/deck.jpg',
        'Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed',
        'Mon-Fri, 7.00am-6.00pm',
        0.00,
        0
    );

INSERT INTO `cafeteria`
VALUES
    (
        8,
        'The Terrace',
        1.2965,
        'Computing 3 (COM3)',
        103.7725,
        'The Terrace',
        'COM 3',
        'CP11',
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        756,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/WhatsApp-Image-2022-12-08-at-1.37.44-PM-1-1024x768-1-800x600.jpeg',
        'Mon-Fri, 7.30am-7.00pm; Sat, 7.30am-2.00pm; Sun closed',
        'Mon-Fri, 7.30am-3.00pm; Sat-Sun closed',
        0.00,
        0
    );

/*!40000 ALTER TABLE `cafeteria` ENABLE KEYS */;
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
  `average_rating` decimal(3,2) DEFAULT '0.00' COMMENT '平均评分',
  `review_count` int DEFAULT '0' COMMENT '评价数量',
  PRIMARY KEY (`id`),
  KEY `FK_stall_cafeteria` (`cafeteria_id`),
  CONSTRAINT `FK_stall_cafeteria` FOREIGN KEY (`cafeteria_id`) REFERENCES `cafeteria` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stall`
--

LOCK TABLES `stall` WRITE;
/*!40000 ALTER TABLE `stall` DISABLE KEYS */;
INSERT INTO `stall` VALUES (
    1,
    'N/A',
    'Taiwanese cuisine',
    'HALAL FOOD OPTIONS AVAILABLE',
    'https://www.taiwanichiban.com//image/cache/catalog/social/slider1-1920x800.jpg',
    'Taiwan Ichiban',
    4,
    0.00,
    0
);

INSERT INTO `stall` VALUES (
    2,
    'N/A',
    'Chinese cuisine',
    'HALAL FOOD OPTIONS AVAILABLE',
    'https://www.shicheng.news/images/image/1720/17206176.avif?1682420429',
    'Chinese',
    4,
    0.00,
    0
);

INSERT INTO `stall` VALUES (
    3,
    'N/A',
    'Pasta and Western cuisine',
    'HALAL FOOD OPTIONS AVAILABLE',
    'https://i0.wp.com/shopsinsg.com/wp-content/uploads/2020/10/build-your-own-pasta.jpg?w=560&ssl=1',
    'Pasta Express',
    4,
    0.00,
    0
);

INSERT INTO `stall` VALUES (
    4,
    'N/A',
    'Vegetarian cuisine',
    'HALAL FOOD OPTIONS AVAILABLE',
    'https://food-cms.grab.com/compressed_webp/merchants/4-C263REEVRJ2YNT/hero/b0183d8d0f9e47fe9e2583c52a558ddf_1641779012807417776.webp',
    'Ruyi Yuan Vegetarian',
    4,
    0.00,
    0
);

INSERT INTO `stall` VALUES (
    5,
    'N/A',
    'Yong Tao Foo',
    'HALAL FOOD OPTIONS AVAILABLE',
    'https://www.shicheng.news/images/image/1720/17206190.avif?1682420423',
    'Yong Tao Foo',
    4,
    0.00,
    0
);

INSERT INTO `stall` VALUES (
    6,
    'N/A',
    'Western cuisine',
    'HALAL FOOD OPTIONS AVAILABLE',
    'https://scontent.fsin14-1.fna.fbcdn.net/v/t39.30808-6/481079656_944403831193045_946033071545871075_n.jpg?_nc_cat=103&ccb=1-7&_nc_sid=127cfc&_nc_ohc=sJ1hi8uBELQQ7kNvwGXR15R&_nc_oc=Adl6g8Zj6XN3ZTT-XnQ3oKg-DzOqNFtcqjpP7JpMT5DPhdmWRQgVjG5VlFlXXQSHrrc&_nc_zt=23&_nc_ht=scontent.fsin14-1.fna&_nc_gid=lsFOzyKp7WEhQIJ6BrtugA&oh=00_AfZG0alde4WUj8z6GHbKgqhf71CA1oTjAHzO7ArQMlbxZA&oe=68E0004E',
    'Western Crave',
    4,
    0.00,
    0
);

-- 默认摊位(用于未配置摊位的食堂)
INSERT INTO `stall` VALUES (
    7,
    'N/A',
    'Default',
    'HALAL FOOD OPTIONS AVAILABLE',
    'https://th.bing.com/th/id/OIP.swinVrT6m0hoTGPwblccPgHaHa?w=192&h=193&c=7&r=0&o=5&dpr=2.4&pid=1.7',
    'Default',
    1,
    0.00,
    0
);

/*!40000 ALTER TABLE `stall` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review`
-- 重要更新:
--   1. 移除 author 字段,改为 user_id 关联用户表
--   2. comment 改为 text 类型支持长文本(10-1000字符)
--   3. 新增 image_urls JSON字段存储图片URL数组
--   4. 新增 updated_at 字段记录修改时间
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '评价用户ID',
  `stall_id` bigint NOT NULL COMMENT '摊位ID',
  `rating` int NOT NULL COMMENT '评分(1-5)',
  `comment` text NOT NULL COMMENT '评价内容(10-1000字符)',
  `image_urls` json DEFAULT NULL COMMENT '评价图片URL数组',
  `created_at` datetime(6) DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime(6) DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `FK_review_user` (`user_id`),
  KEY `FK_review_stall` (`stall_id`),
  KEY `idx_stall_created` (`stall_id`, `created_at`),
  KEY `idx_user_created` (`user_id`, `created_at`),
  CONSTRAINT `FK_review_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `FK_review_stall` FOREIGN KEY (`stall_id`) REFERENCES `stall` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_rating` CHECK ((`rating` >= 1 AND `rating` <= 5))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
-- 空表,等待用户评价数据
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `image`
-- 新增: 图片管理表,用于存储上传的图片信息
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
  KEY `idx_entity` (`entity_type`, `entity_id`),
  KEY `idx_uploaded_by` (`uploaded_by`, `created_at`),
  CONSTRAINT `FK_image_user` FOREIGN KEY (`uploaded_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `image`
--

LOCK TABLES `image` WRITE;
/*!40000 ALTER TABLE `image` DISABLE KEYS */;
-- 空表,等待用户上传图片
/*!40000 ALTER TABLE `image` ENABLE KEYS */;
UNLOCK TABLES;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-10-10 (updated version)
