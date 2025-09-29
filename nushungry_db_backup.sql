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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cafeteria`
--

LOCK TABLES `cafeteria` WRITE;
/*!40000 ALTER TABLE `cafeteria` DISABLE KEYS */;
INSERT INTO `cafeteria` VALUES (1,'Fine Food',1.305,'Town Plaza',103.773,'Fine Food','University Town','Stephen Riady Centre','HALAL FOOD OPTIONS AVAILABLE',410,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Fine-Food-1-1024x684-1-898x600.jpg','Mon-Sun, 8.00am-8.30pm','Mon-Sun, 8.00am-8.30pm');
INSERT INTO `cafeteria`
VALUES
    (
        2,
        'Flavours @ UTown',
        1.30462,
        'UTown Stephen Riady Centre',
        103.77252,
        'Flavours @ UTown',
        'University Town',      -- nearest_bus_stop（待确认，可调整）
        'Stephen Riady Centre', -- nearest_carpark（待确认，可调整）
        'HALAL FOOD OPTIONS AVAILABLE',
        700,
        'https://uci.nus.edu.sg/wp-content/uploads/2025/08/Flavours-938x600.jpg',
        'Mon-Sun, 7.30am-8.30pm',
        'Mon-Sun, 7.30am-8.30pm'
    );
INSERT INTO `cafeteria`
VALUES
    (
        3,
        'Central Square @ YIH',
        1.29886,                       -- latitude
        'Yusof Ishak House',
        103.77432,                     -- longitude
        'Central Square @ YIH',
        'Yusof Ishak House',
        'CP4 and CP5',
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        314,
        'https://uci.nus.edu.sg/wp-content/uploads/2025/05/YIH-800x600.jpg',
        'Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm',
        'Mon-Fri, 8.00am-8.00pm; Sat, 8.30am-2.30pm'
    );
INSERT INTO `cafeteria`
VALUES
    (
        4,
        'Frontier',
        1.2961,                       -- latitude
        'Faculty of Science',
        103.7831,                     -- longitude
        'Frontier',
        'Lower Kent Ridge Road - Blk S17',  -- nearest_bus_stop
        'CP7',                        -- nearest_carpark
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        700,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Frontier-Canteen-1024x684-1-898x600.jpg',
        'Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed',
        'Mon-Fri, 7.00am-7.00pm; Sat, 7.00am-2.00pm'
    );
INSERT INTO `cafeteria`
VALUES
    (
        5,
        'PGP Aircon Canteen',
        1.29112,                       -- latitude
        'Prince George’s Park',
        103.78036,                     -- longitude
        'PGP Aircon Canteen',
        'Prince George''s Park',
        'Prince George''s Park Foyer',
        'VEGETARIAN FOOD OPTION AVAILABLE',
        308,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/PGP-canteen.jpg',
        'Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm',
        'Mon-Fri, 7.30am-8.00pm; Sat-Sun, 8.00am-8.00pm'
    );
INSERT INTO `cafeteria`
VALUES
    (
        6,
        'Techno Edge',
        1.29796,                       -- latitude
        'College of Design and Engineering',
        103.77153,                     -- longitude
        'Techno Edge',
        'Kent Ridge Crescent - Information Technology',  -- nearest_bus_stop
        'CP17',                        -- nearest_carpark
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        450,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Techno-Edge-1024x684-1-898x600.jpg',
        'Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed',
        'Mon-Fri, 7.00am-8.00pm; Sat, 7.30am-2.00pm; Sun/PH closed'
    );
INSERT INTO `cafeteria`
VALUES
    (
        7,
        'The Deck',
        1.2948,                       -- latitude
        'Faculty of Arts & Social Sciences',
        103.7715,                     -- longitude
        'The Deck',
        'Lower Kent Ridge Rd - Blk S12',  -- nearest_bus_stop
        'CP11, CP15',                        -- nearest_carpark
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        1018,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/deck.jpg',
        'Mon-Fri, 7.30am-4.00pm/8.00pm; Sat, 7.30am-3.00pm; Sun/PH closed',
        'Mon-Fri, 7.00am-6.00pm'
    );
INSERT INTO `cafeteria`
VALUES
    (
        8,
        'The Terrace',
        1.2965,                       -- latitude
        'Computing 3 (COM3)',
        103.7725,                     -- longitude
        'The Terrace',
        'COM 3',                      -- nearest_bus_stop
        'CP11',                       -- nearest_carpark
        'HALAL & VEGETARIAN FOOD OPTIONS AVAILABLE',
        756,
        'https://uci.nus.edu.sg/wp-content/uploads/2024/02/WhatsApp-Image-2022-12-08-at-1.37.44-PM-1-1024x768-1-800x600.jpeg',
        'Mon-Fri, 7.30am-7.00pm; Sat, 7.30am-2.00pm; Sun closed',
        'Mon-Fri, 7.30am-3.00pm; Sat-Sun closed'
    );


/*!40000 ALTER TABLE `cafeteria` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `review`
--

DROP TABLE IF EXISTS `review`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `author` varchar(255) DEFAULT NULL,
  `comment` varchar(255) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `rating` int NOT NULL,
  `stall_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKfhs5118nbc3s1f9ytnfsb6a4t` (`stall_id`),
  CONSTRAINT `FKfhs5118nbc3s1f9ytnfsb6a4t` FOREIGN KEY (`stall_id`) REFERENCES `stall` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `review`
--

LOCK TABLES `review` WRITE;
/*!40000 ALTER TABLE `review` DISABLE KEYS */;
/*!40000 ALTER TABLE `review` ENABLE KEYS */;
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
  PRIMARY KEY (`id`),
  KEY `FKced5797u7ld98omk6gvy8lcow` (`cafeteria_id`),
  CONSTRAINT `FKced5797u7ld98omk6gvy8lcow` FOREIGN KEY (`cafeteria_id`) REFERENCES `cafeteria` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
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
                               4
                           );

INSERT INTO `stall` VALUES (
                               2,
                               'N/A',
                               'Chinese cuisine',
                               'HALAL FOOD OPTIONS AVAILABLE',
                               'https://www.shicheng.news/images/image/1720/17206176.avif?1682420429',
                               'Chinese',
                               4
                           );

INSERT INTO `stall` VALUES (
                               3,
                               'N/A',
                               'Pasta and Western cuisine',
                               'HALAL FOOD OPTIONS AVAILABLE',
                               'https://i0.wp.com/shopsinsg.com/wp-content/uploads/2020/10/build-your-own-pasta.jpg?w=560&ssl=1',
                               'Pasta Express',
                               4
                           );

INSERT INTO `stall` VALUES (
                               4,
                               'N/A',
                               'Vegetarian cuisine',
                               'HALAL FOOD OPTIONS AVAILABLE',
                               'https://food-cms.grab.com/compressed_webp/merchants/4-C263REEVRJ2YNT/hero/b0183d8d0f9e47fe9e2583c52a558ddf_1641779012807417776.webp',
                               'Ruyi Yuan Vegetarian',
                               4
                           );


INSERT INTO `stall` VALUES (
                               5,
                               'N/A',
                               'Yong Tao Foo',
                               'HALAL FOOD OPTIONS AVAILABLE',
                               'https://www.shicheng.news/images/image/1720/17206190.avif?1682420423',
                               'Yong Tao Foo',
                               4
                           );


INSERT INTO `stall` VALUES (
                               6,
                               'N/A',
                               'Western cuisine',
                               'HALAL FOOD OPTIONS AVAILABLE',
                               'https://scontent.fsin14-1.fna.fbcdn.net/v/t39.30808-6/481079656_944403831193045_946033071545871075_n.jpg?_nc_cat=103&ccb=1-7&_nc_sid=127cfc&_nc_ohc=sJ1hi8uBELQQ7kNvwGXR15R&_nc_oc=Adl6g8Zj6XN3ZTT-XnQ3oKg-DzOqNFtcqjpP7JpMT5DPhdmWRQgVjG5VlFlXXQSHrrc&_nc_zt=23&_nc_ht=scontent.fsin14-1.fna&_nc_gid=lsFOzyKp7WEhQIJ6BrtugA&oh=00_AfZG0alde4WUj8z6GHbKgqhf71CA1oTjAHzO7ArQMlbxZA&oe=68E0004E',
                               'Western Crave',
                               4
                           );
/*
 This is a default stall to be updated later for other cafeterias without stalls.
 */
INSERT INTO `stall` VALUES (
                               7,
                               'N/A',
                               'Default',
                               'HALAL FOOD OPTIONS AVAILABLE',
                               'https://th.bing.com/th/id/OIP.swinVrT6m0hoTGPwblccPgHaHa?w=192&h=193&c=7&r=0&o=5&dpr=2.4&pid=1.7',
                               'Default',
                               1
                           );

/*!40000 ALTER TABLE `stall` ENABLE KEYS */;

UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-09-12 12:18:14
