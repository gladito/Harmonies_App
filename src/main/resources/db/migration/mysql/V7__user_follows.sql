CREATE TABLE `user_follows` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  `follower_id` bigint NOT NULL,
  `target_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE (`follower_id`, `target_id`),
  CHECK (`follower_id` <> `target_id`),
  FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`),
  FOREIGN KEY (`target_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
