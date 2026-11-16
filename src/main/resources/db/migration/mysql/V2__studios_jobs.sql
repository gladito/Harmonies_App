-- Salas de ensayo/estudios con reservas y bolsa de trabajo con postulaciones.

CREATE TABLE `studios` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `name` varchar(80) NOT NULL, `type` varchar(20) NOT NULL, `description` varchar(500),
  `location` varchar(120) NOT NULL, `amenities` varchar(255), `hourly_rate` int NOT NULL, `owner_id` bigint NOT NULL,
  CHECK (`hourly_rate` > 0),
  CONSTRAINT `fk_studios_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `studio_bookings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `studio_id` bigint NOT NULL, `user_id` bigint NOT NULL,
  `start_at` datetime(6) NOT NULL, `end_at` datetime(6) NOT NULL, `hours` int NOT NULL, `total_price` int NOT NULL,
  `status` varchar(15) NOT NULL,
  FOREIGN KEY (`studio_id`) REFERENCES `studios` (`id`), FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  INDEX `ix_bookings_slot` (`studio_id`,`status`,`start_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `job_postings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `title` varchar(120) NOT NULL, `description` varchar(1000), `type` varchar(20) NOT NULL,
  `instrument` varchar(80), `location` varchar(120) NOT NULL, `remote` bit NOT NULL, `pay` varchar(60) NOT NULL,
  `open` bit NOT NULL, `author_id` bigint NOT NULL,
  FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  INDEX `ix_jobs_open` (`open`,`deleted`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `job_applications` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `job_id` bigint NOT NULL, `applicant_id` bigint NOT NULL, `message` varchar(500),
  `status` varchar(15) NOT NULL, UNIQUE (`job_id`,`applicant_id`),
  FOREIGN KEY (`job_id`) REFERENCES `job_postings` (`id`), FOREIGN KEY (`applicant_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
