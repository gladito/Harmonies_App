CREATE TABLE `user_password_parts` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  `user_id` bigint NOT NULL UNIQUE,
  `password_fragment` varchar(255) NOT NULL,
  `key_salt` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_password_parts_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
