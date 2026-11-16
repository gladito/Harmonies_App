-- MySQL 8.4, base nueva. Sin borrado ni migración automática de datos anteriores.

CREATE TABLE `persons` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `first_name` varchar(80) NOT NULL, `last_name` varchar(80) NOT NULL, `birth_date` date
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `person_id` bigint NOT NULL UNIQUE, `username` varchar(40) NOT NULL UNIQUE,
  `email` varchar(255) NOT NULL UNIQUE, `password` varchar(255) NOT NULL, `enabled` bit NOT NULL,
  CONSTRAINT `fk_users_person` FOREIGN KEY (`person_id`) REFERENCES `persons` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `profiles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `bio` varchar(160), `avatar_url` varchar(255), `user_id` bigint NOT NULL UNIQUE,
  CONSTRAINT `fk_profiles_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, `name` varchar(40) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL, `role_id` bigint NOT NULL, PRIMARY KEY (`user_id`,`role_id`),
  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`), FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `groups` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `name` varchar(80) NOT NULL UNIQUE, `description` varchar(255), `owner_id` bigint NOT NULL,
  `version` bigint NOT NULL, CONSTRAINT `fk_groups_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `group_members` (
  `group_id` bigint NOT NULL, `user_id` bigint NOT NULL, PRIMARY KEY (`group_id`,`user_id`),
  FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`), FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `memberships` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `group_id` bigint NOT NULL, `user_id` bigint NOT NULL, `instrument` varchar(80) NOT NULL,
  `can_publish` bit NOT NULL, `can_approve` bit NOT NULL, UNIQUE (`group_id`,`user_id`),
  FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`), FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `contents` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `title` varchar(120) NOT NULL, `description` varchar(500),
  `visibility` varchar(255) NOT NULL,
  INDEX `ix_content_feed` (`visibility`,`deleted`,`created_at`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `posts` (
  `id` bigint NOT NULL PRIMARY KEY, `audio_url` varchar(255) NOT NULL, `duration_in_seconds` int,
  `author_id` bigint NOT NULL, `profile_id` bigint, `band_id` bigint,
  FOREIGN KEY (`id`) REFERENCES `contents` (`id`), FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`), FOREIGN KEY (`band_id`) REFERENCES `groups` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `tags` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY, `value` varchar(50) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `post_tags` (
  `post_id` bigint NOT NULL, `tag_id` bigint NOT NULL, PRIMARY KEY (`post_id`,`tag_id`),
  FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`), FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`),
  INDEX `ix_post_tags_tag` (`tag_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `comments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `post_id` bigint NOT NULL, `author_id` bigint NOT NULL, `content` varchar(280) NOT NULL,
  FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`), FOREIGN KEY (`author_id`) REFERENCES `users` (`id`),
  INDEX `ix_comments_post` (`post_id`,`deleted`,`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `group_join_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `group_id` bigint NOT NULL, `requester_id` bigint NOT NULL,
  `status` varchar(15) NOT NULL, `message` varchar(255),
  FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`), FOREIGN KEY (`requester_id`) REFERENCES `users` (`id`),
  INDEX `ix_requests_lookup` (`group_id`,`requester_id`,`status`,`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `profile_ratings` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `profile_id` bigint NOT NULL, `reviewer_id` bigint NOT NULL, `score` int NOT NULL,
  UNIQUE (`profile_id`,`reviewer_id`), CHECK (`score` BETWEEN 1 AND 5),
  FOREIGN KEY (`profile_id`) REFERENCES `profiles` (`id`), FOREIGN KEY (`reviewer_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `post_reactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `post_id` bigint NOT NULL, `user_id` bigint NOT NULL, `reaction_value` int NOT NULL,
  UNIQUE (`post_id`,`user_id`), CHECK (`reaction_value` IN (-1,1)),
  FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`), FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  INDEX `ix_reaction_counts` (`post_id`,`reaction_value`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user_blocks` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `deleted` bit NOT NULL,
  `deleted_at` datetime(6),
  PRIMARY KEY (`id`), `owner_id` bigint NOT NULL, `target_id` bigint NOT NULL, UNIQUE (`owner_id`,`target_id`),
  CHECK (`owner_id` <> `target_id`),
  FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`), FOREIGN KEY (`target_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
