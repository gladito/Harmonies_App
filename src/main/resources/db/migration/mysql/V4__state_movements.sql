CREATE TABLE `state_movements` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  `entity_type` varchar(32) NOT NULL,
  `entity_id` bigint NOT NULL,
  `entity_label` varchar(160) NOT NULL,
  `previous_status` varchar(32) NULL,
  `new_status` varchar(32) NOT NULL,
  `actor_user_id` bigint NOT NULL,
  `note` varchar(255) NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_movements_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`id`),
  INDEX `ix_movements_entity` (`entity_type`, `entity_id`, `created_at`),
  INDEX `ix_movements_actor` (`actor_user_id`, `created_at`),
  INDEX `ix_movements_timeline` (`created_at`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
