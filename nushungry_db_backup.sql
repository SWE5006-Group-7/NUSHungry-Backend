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
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cafeteria`
--

LOCK TABLES `cafeteria` WRITE;
/*!40000 ALTER TABLE `cafeteria` DISABLE KEYS */;
INSERT INTO `cafeteria` VALUES (1,'University Town',1.305,'UTown',103.773,'UTown','University Town','Stephen Riady Centre');
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
  `seating_capacity` int DEFAULT NULL,
  `term_time_opening_hours` varchar(255) DEFAULT NULL,
  `vacation_opening_hours` varchar(255) DEFAULT NULL,
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
INSERT INTO `stall` VALUES (1,NULL,NULL,'(MUSLIM OWNED)','https://uci.nus.edu.sg/wp-content/uploads/2024/02/Briyani-1024x660-1-931x600.jpg','Bismillah Biryani',NULL,'Mon-Fri: 11.30am - 7.30pm, Sat: 11.30am - 5.00pm','Mon-Fri: 11.30am - 7.30pm, Sat: 11.30am - 5.00pm',1),(2,NULL,NULL,'(HALAL FOOD OPTIONS AVAILABLE)','https://uci.nus.edu.sg/wp-content/uploads/2024/02/Fine-Food-1-1024x684-1-898x600.jpg','Fine Food',410,'Mon-Sun, 8.00am-8.30pm','Mon-Sun, 8.00am-8.30pm',1),(3,NULL,NULL,'(HALAL FOOD OPTIONS AVAILABLE)','https://uci.nus.edu.sg/wp-content/uploads/2025/08/Flavours-938x600.jpg','Flavours @ UTown',700,'Mon-Sun: 7.30am to 8.30pm','Mon-Sun: 7.30am to 8.30pm',1),(4,'9833 0603',NULL,NULL,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Hwangs-UTown-1024x684-1-898x600.jpg','Hwang鈥檚 Korean Restaurant',114,'Mon-Sat, 10.30am-9.00pm','Mon-Sat, 10.30am-9.00pm',1),(5,NULL,NULL,NULL,'https://uci.nus.edu.sg/wp-content/uploads/2024/11/Jollibee-2-938x588.jpg','Jollibee',NULL,'9 am to 9 pm daily','9 am to 9 pm daily',1),(6,NULL,NULL,NULL,'https://uci.nus.edu.sg/wp-content/uploads/2024/08/Lixin-840x600.jpg','LiXin',40,'Mon-Sun, 10.30am-9.30pm','Mon-Sun, 10.30am-9.30pm',1),(7,NULL,NULL,'(HALAL CERTIFIED)','https://uci.nus.edu.sg/wp-content/uploads/2024/02/Bean-2-1024x660-1-931x600.jpg','Mr Bean / Do Qoo',NULL,'Mon-Fri 7.30am - 9.00pm, Sat-Sat & PH 8.30am - 7.30pm','Mon-Fri, 8.30am - 7.30pm',1),(8,'6910 1127',NULL,NULL,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Starbucks-UTown-1024x684-1-898x600.jpg','Starbucks',330,'Daily 24/7','Daily 7.30am to 9.00pm',1),(9,NULL,NULL,'(HALAL CERTIFIED)','https://uci.nus.edu.sg/wp-content/uploads/2024/02/Subway-1024x768-1-800x600.jpg','Subway',15,'Mon-Sun, 8.30am to 9.30pm','Mon-Sun, 8.30am to 9.30pm',1),(10,NULL,NULL,'(PENDING HALAL CERTIFICATION)','https://uci.nus.edu.sg/wp-content/uploads/2024/02/Supersnacks-Edited-1024x684-1-898x600.jpg','Supersnacks',NULL,'Mon-Fri, 11.00am-2.00am, Sat/Sun/PH, 6.00pm-2.00am',NULL,1),(11,'97771353',NULL,'(HALAL CERTIFIED)','https://uci.nus.edu.sg/wp-content/uploads/2024/02/The-Royals-Cafe-1024x684-1-898x600.jpg','The Royals Bistro',60,'Mon-Sat, 11.00am-8.00pm','Mon-Sat, 11.00am-8.00pm',1),(12,NULL,NULL,'(HALAL CERTIFICATION)','https://uci.nus.edu.sg/wp-content/uploads/2024/02/Triplets-1024x768-1-800x600.jpg','Triplets',8,'Mon-Sun: 9.00am to 9.00pm','Mon-Sun: 9.00am to 9.00pm',1),(13,'66946240',NULL,NULL,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Udon-1024x689-1-892x600.jpg','Udon Don Bar',NULL,'Mon-Sun: 11.00am-9.30pm','Mon-Sun: 11.00am-9.00pm',1),(14,'82230550',NULL,NULL,'https://uci.nus.edu.sg/wp-content/uploads/2024/02/Waa-Cow-1-1024x684-1-898x600.jpg','Waa Cow',NULL,'Mon-Fri, 11.30am-8.30pm, Sat/Sun/PH, 12.00pm-8.30pm','Mon-Fri, 11.30am-8.30pm, Sat/Sun/PH, 12.00pm-8.30pm',1);
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
