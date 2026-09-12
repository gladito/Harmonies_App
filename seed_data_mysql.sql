-- =================================================================================
-- Harmonyapp - Datos de ejemplo (MySQL / XAMPP)
-- Ejecutar sobre la base Harmony después de que existan las tablas.
-- Contraseña de todos los usuarios: Hola1
-- Excluye: tags, post_tags
-- =================================================================================

USE Harmony;

-- ROLES (2)
INSERT INTO roles (id, name) VALUES
    (1, 'ROLE_USER'),
    (2, 'ROLE_ADMIN')
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- USERS (2)
INSERT INTO users (id, first_name, last_name, birth_date, username, email, password, enabled, created_at, updated_at, deleted) VALUES
    (1, 'Agustin', 'Navarro', '1995-05-15', 'gladito', 'ejemplo1@gmail.com', '$2a$10$E.xnrQW1fR2UohUeMWguV.fb6XkkBBuXprG7ycgD/t6pmieh1UiLa', 1, NOW(), NOW(), 0),
    (2, 'Pepito', 'Perez', '1998-07-08', 'pepito', 'pepito@gmail.com', '$2a$10$E.xnrQW1fR2UohUeMWguV.fb6XkkBBuXprG7ycgD/t6pmieh1UiLa', 1, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- PROFILES (2)
INSERT INTO profiles (id, bio, avatar_url, user_id, created_at, updated_at, deleted) VALUES
    (1, 'Productor lo-fi y synthwave. Me gusta experimentar con capas atmosfericas.', 'https://cdn.example.com/avatars/gladito.png', 1, NOW(), NOW(), 0),
    (2, 'Bajista y amante del groove. Siempre buscando colaboraciones.', 'https://cdn.example.com/avatars/pepito.png', 2, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE bio = VALUES(bio);

-- USER_ROLES (2)
INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1),
    (2, 1)
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id);

-- CONTENTS (2) - base para posts (herencia JOINED)
INSERT INTO contents (id, title, description, visibility, created_at, updated_at, deleted) VALUES
    (1, 'Nebula Jam Session', 'Layers atmosfericos con sintetizadores analogicos y bateria suave.', 'PUBLIC', NOW(), NOW(), 0),
    (2, 'Midnight Groove', 'Groove funk con toques de neo-soul, ideal para improvisar sobre el.', 'PUBLIC', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE title = VALUES(title);

-- POSTS (2)
INSERT INTO posts (id, audio_url, duration_in_seconds, author_id, profile_id) VALUES
    (1, 'https://cdn.example.com/audio/nebula-session.mp3', 214, 1, 1),
    (2, 'https://cdn.example.com/audio/midnight-groove.mp3', 196, 2, 2)
ON DUPLICATE KEY UPDATE audio_url = VALUES(audio_url);

-- COMMENTS (2)
INSERT INTO comments (id, post_id, author_id, content, created_at, updated_at, deleted) VALUES
    (1, 1, 2, 'Suena increible! Podriamos agregar un bajo sutil al segundo verso.', NOW(), NOW(), 0),
    (2, 2, 1, 'El groove de la intro quedo muy redondo. Buen trabajo con el bajo.', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE content = VALUES(content);

-- GROUPS (2)
INSERT INTO `groups` (id, name, description, owner_id, created_at, updated_at, deleted) VALUES
    (1, 'Nebula Waves', 'Colectivo de productores lo-fi y synthwave enfocado en soundtracks espaciales.', 1, NOW(), NOW(), 0),
    (2, 'Midnight Collective', 'Banda orientada a funk y neo-soul con sesiones de improvisacion semanal.', 2, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- GROUP_MEMBERS (2)
INSERT INTO group_members (group_id, user_id) VALUES
    (1, 1),
    (2, 2)
ON DUPLICATE KEY UPDATE group_id = VALUES(group_id);

-- GROUP_JOIN_REQUESTS (2)
INSERT INTO group_join_requests (id, group_id, requester_id, status, message, created_at, updated_at, deleted) VALUES
    (1, 1, 2, 'PENDING', 'Hola! Me encantaria unirme a Nebula Waves para aportar lineas de bajo.', NOW(), NOW(), 0),
    (2, 2, 1, 'APPROVED', 'Tengo un beat que encajaria perfecto con el estilo de la banda.', NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE status = VALUES(status);
