-- V1__Create_tables.sql

-- 8. R2DBC 엔티티: chat_room
CREATE TABLE `chat_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(255) NOT NULL,
  `is_activated` BOOLEAN DEFAULT TRUE,
  `parent_region_code` VARCHAR(50) NOT NULL,
  `parent_region_name` VARCHAR(100) NOT NULL,
  `deactivated_at` DATETIME NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_parent_region_code` (`parent_region_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 9. R2DBC 엔티티: chat_message
CREATE TABLE `chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `chat_room_id` BIGINT NOT NULL,
  `sender_id` BIGINT NOT NULL,
  `message` TEXT NOT NULL,
  `timestamp` DATETIME NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `FK_chat_message_chat_room` (`chat_room_id`),
  CONSTRAINT `FK_chat_message_chat_room` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 10. R2DBC 엔티티: chat_room_member
CREATE TABLE `chat_room_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `chat_room_id` BIGINT NOT NULL,
  `user_id` BIGINT NOT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `FK_chat_room_member_chat_room` (`chat_room_id`),
  CONSTRAINT `FK_chat_room_member_chat_room` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 11. R2DBC 엔티티: weather_alert
CREATE TABLE `weather_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_region_code` VARCHAR(50) NOT NULL,
  `parent_region_name` VARCHAR(100) NOT NULL,
  `region_code` VARCHAR(50) NOT NULL,
  `region_name` VARCHAR(100) NOT NULL,
  `announcement_time` VARCHAR(50) NOT NULL,
  `effective_time` VARCHAR(50),
  `alert_type` VARCHAR(100) NOT NULL,
  `alert_level` VARCHAR(50) NOT NULL,
  `command` VARCHAR(255),
  `end_time` DATETIME NULL,
  `is_activated` BOOLEAN DEFAULT TRUE,
  `deactivated_at` DATETIME NULL,
  `chat_room_id` BIGINT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `FK_weather_alert_chat_room` (`chat_room_id`),
  CONSTRAINT `FK_weather_alert_chat_room` FOREIGN KEY (`chat_room_id`) REFERENCES `chat_room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


---- 1. JPA 엔티티: users
--CREATE TABLE `users` (
--
--  `is_completed_survey` BIT(1) DEFAULT NULL,
--  `is_deleted` BIT(1) DEFAULT NULL,
--  `is_login` BIT(1) NOT NULL,
--  `level` INT NOT NULL,
--  `point` INT NOT NULL,
--  `sweat` BIT(1) DEFAULT NULL,
--   `cold` BIT(1) DEFAULT NULL,
--   `hot` BIT(1) DEFAULT NULL,
--  `created_at` DATETIME(6) DEFAULT NULL,
--  `updated_at` DATETIME(6) DEFAULT NULL,
--  `user_id` BIGINT NOT NULL AUTO_INCREMENT,
--  `nickname` VARCHAR(12) DEFAULT NULL,
--  `password` VARCHAR(255) NOT NULL,
--  `refresh_token` VARCHAR(255) DEFAULT NULL,
--  `social_id` VARCHAR(255) NOT NULL,
--  `provider` ENUM('DEFAULT','KAKAO') NOT NULL,
--  `role` ENUM('ADMIN','GUEST','USER') NOT NULL,
--  PRIMARY KEY (`user_id`),
--  UNIQUE KEY `UKg6t6xceecpep2b40vy35x1nve` (`social_id`)
--) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
--
---- 2. JPA 엔티티: location_entity
--CREATE TABLE `location` (
--  `latitude` DOUBLE NOT NULL,
--  `longitude` DOUBLE NOT NULL,
--  `location_id` BIGINT NOT NULL AUTO_INCREMENT,
--  `location_name` VARCHAR(255) NOT NULL,
--  PRIMARY KEY (`location_id`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
--
---- 3. JPA 엔티티: board_entity
--CREATE TABLE `board` (
--  `downvote_count` INT NOT NULL,
--  `upvote_count` INT NOT NULL,
--  `version` INT NOT NULL,
--  `board_id` BIGINT NOT NULL AUTO_INCREMENT,
--  `created_at` DATETIME(6) DEFAULT NULL,
--  `location_id` BIGINT NOT NULL,
--  `updated_at` DATETIME(6) DEFAULT NULL,
--  `user_id` BIGINT DEFAULT NULL,
--  `content` VARCHAR(255) NOT NULL,
--  `title` VARCHAR(255) NOT NULL,
--  PRIMARY KEY (`board_id`),
--  KEY `FK6e6yyg1h2mmqw1evjnjwffm43` (`location_id`),
--  KEY `FKr09s89rmwa811cd3ugu8ltx03` (`user_id`),
--  CONSTRAINT `FK6e6yyg1h2mmqw1evjnjwffm43` FOREIGN KEY (`location_id`) REFERENCES `location` (`location_id`),
--  CONSTRAINT `FKr09s89rmwa811cd3ugu8ltx03` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
--
---- 4. JPA 엔티티: board_vote_entity
--CREATE TABLE `board_vote` (
--  `board_id` BIGINT NOT NULL,
--  `user_id` BIGINT NOT NULL,
--  `vote_id` BIGINT NOT NULL AUTO_INCREMENT,
--  `vote_type` ENUM('DOWNVOTE','UPVOTE') NOT NULL,
--  PRIMARY KEY (`vote_id`),
--  KEY `FKm7l7vagt9nxkgup00kl8i27j9` (`board_id`),
--  KEY `FKf1rcbrglt2qv899a9p4b76q8b` (`user_id`),
--  CONSTRAINT `FKf1rcbrglt2qv899a9p4b76q8b` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
--  CONSTRAINT `FKm7l7vagt9nxkgup00kl8i27j9` FOREIGN KEY (`board_id`) REFERENCES `board` (`board_id`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
--
---- 6. JPA 엔티티: missions
--CREATE TABLE `missions` (
--  `point` INT NOT NULL,
--  `created_at` DATETIME(6) DEFAULT NULL,
--  `mission_id` BIGINT NOT NULL AUTO_INCREMENT,
--  `updated_at` DATETIME(6) DEFAULT NULL,
--  `name` VARCHAR(255) DEFAULT NULL,
--  `question` VARCHAR(255) DEFAULT NULL,
--  `mission_type` ENUM('COLD','HOT','RAIN','SNOW','SUNNY') DEFAULT NULL,
--  PRIMARY KEY (`mission_id`)
--) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
--
---- 7. JPA 엔티티: mission_histories
--CREATE TABLE `mission_histories` (
--  `is_completed` BIT(1) NOT NULL,
--  `completed_at` DATETIME(6) DEFAULT NULL,
--  `created_at` DATETIME(6) DEFAULT NULL,
--  `mission_history_id` BIGINT NOT NULL AUTO_INCREMENT,
--  `mission_id` BIGINT DEFAULT NULL,
--  `updated_at` DATETIME(6) DEFAULT NULL,
--  `user_id` BIGINT DEFAULT NULL,
--  `store_file_name` VARCHAR(255) DEFAULT NULL,
--  `upload_file_name` VARCHAR(255) DEFAULT NULL,
--  `mission_time` ENUM('AFTERNOON','EVENING','MORNING') DEFAULT NULL,
--  PRIMARY KEY (`mission_history_id`),
--  KEY `FK8v62cdyr5s27bjrs7t4ckwlnw` (`mission_id`),
--  KEY `FK9jfdb9eci2grjnvqyqxbolft9` (`user_id`),
--  CONSTRAINT `FK8v62cdyr5s27bjrs7t4ckwlnw` FOREIGN KEY (`mission_id`) REFERENCES `missions` (`mission_id`),
--  CONSTRAINT `FK9jfdb9eci2grjnvqyqxbolft9` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

