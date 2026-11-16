# DOCUMENTACIÓN FUNCIONAL, MODELO DE DATOS Y CASOS DE USO
**Proyecto Harmony — Red Colaborativa de Músicos, Bandas, Salas y Bolsa de Trabajo**  
*Alumno: Agustín Navarro (gladito) · Repositorio: github.com/gladito/Harmonies_App*

---

## 1. Descripción Funcional del Sistema y Alcance

Harmony es una plataforma web colaborativa desarrollada en Java 17 con Spring Boot 3, Spring Security, JPA/Hibernate y Thymeleaf (bilingüe ES/EN). Su objetivo principal es conectar a músicos independientes y bandas emergentes en un único ecosistema que integra la difusión de demos de audio, la formación y administración de bandas con permisos delegados, la reserva por hora de salas de ensayo y estudios de grabación, la postulación a oportunidades laborales musicales y una auditoría transversal de cambios de estado mediante la tabla unificada state_movements.

### 1.1 Actores y Roles del Sistema

| Actor / Rol | Descripción y Responsabilidades en el Sistema |
| :--- | :--- |
| **Usuario Anónimo (Visitante)** | Puede visualizar la portada pública (/), conocer las métricas en vivo de la comunidad y acceder a los formularios de Inicio de Sesión (/login) y Registro (/register). |
| **Músico Registrado (USER)** | Actor principal autenticado. Edita su perfil (biografía y foto de perfil por archivo o URL), publica demos de audio con etiquetas, reacciona y comenta, califica a otros músicos (1 a 5 estrellas), bloquea usuarios, solicita unirse a bandas, reserva salas de ensayo y se postula a avisos laborales. |
| **Líder / Dueño de Banda (Owner)** | Músico creador de una banda (groups.owner_id) o que recibió la propiedad por transferencia. Puede editar nombre, descripción y foto de la banda, asignar instrumentos, delegar permisos (can_publish, can_approve), aprobar o rechazar solicitudes de ingreso y eliminar la banda. |
| **Integrante con Permisos Delegados** | Miembro de una banda cuya fila en la tabla memberships tiene activo can_publish = true (puede publicar demos en nombre de la banda) y/o can_approve = true (puede aprobar o rechazar solicitudes pendientes de ingreso a esa banda). |
| **Responsable de Sala / Estudio** | Músico propietario de un espacio en la tabla studios (REHEARSAL, RECORDING o VOCAL_BOOTH). Define tarifa por hora, equipamiento y ubicación, visualiza la agenda ocupada, puede cancelar reservas futuras o dar de baja la sala. |
| **Anunciante Laboral (Autor de Aviso)** | Músico que publica una búsqueda en job_postings (SESSION, LIVE_GIG, POST_PRODUCTION, BAND_MEMBER, TEACHING). Recibe las postulaciones, decide su aceptación o rechazo (ACCEPTED / REJECTED) y puede cerrar o reabrir el aviso (OPEN / CLOSED). |

### 1.2 Alcance de los Módulos de la Aplicación

| Módulo | Rutas Principales | Alcance Funcional |
| :--- | :--- | :--- |
| **1. Autenticación, Portada y Cuenta** | `/, /login, /register, /account` | Portada institucional con contadores en tiempo real, registro con validación de contraseña fuerte y edad mínima, login con BCrypt y protección CSRF, cambio de nickname, clave y foto de perfil. |
| **2. Comunidad de Músicos y Perfiles** | `/people, /people/{id}, /profile` | Directorio de músicos activos, perfil público con avatar circular, biografía, listado de pistas propias, calificación promedio (1-5 estrellas) sin duplicados y bloqueo bidireccional de visibilidad (user_blocks). |
| **3. Feed Musical, Audio y Reacciones** | `/home, /posts/new, /posts/{id}` | Publicación de pistas de audio HTTPS (solistas o en nombre de una banda), filtrado normalizado por hasta 5 etiquetas (#jazz, #synthwave, etc.), paginación, likes/dislikes y comentarios moderables. |
| **4. Bandas, Membresías y Solicitudes** | `/groups, /groups/explore, /bands/{id}, /groups/requests` | Creación y edición de bandas con foto de perfil, solicitud de ingreso con mensaje opcional, aprobación/rechazo/cancelación, configuración de instrumentos, delegación de permisos y transferencia de liderazgo. |
| **5. Salas de Ensayo y Estudios** | `/studios, /studios/{id}` | Catálogo filtrable por tipo de espacio (Sala de ensayo, Estudio de grabación, Cabina vocal), reserva por bloques de 1 a 8 horas en horas exactas con bloqueo pesimista contra solapamientos y cancelación. |
| **6. Bolsa de Trabajo Musical** | `/jobs, /jobs/{id}` | Publicación de avisos laborales presenciales o remotos, postulación con mensaje al autor, aceptación o rechazo de candidatos y generación automática de la orden de pago al contratar al músico. |
| **7. Pagos, Contrataciones y Comprobantes** | `/payments, /payments/{id}/pay` | Circuito integral de pagos para reservas de salas y contrataciones laborales con selección de medio de pago (Mercado Pago, Transferencia CBU/Alias, Tarjeta), emisión de comprobante (HRM-...) y reembolsos automáticos. |
| **8. Trazabilidad y Movimientos** | `/movements` | Auditoría centralizada en la tabla state_movements que registra cada transición de estado (previous_status -> new_status), fecha exacta, usuario ejecutor (actor_user_id) y observación (note). |

---

## 2. Modelo de Datos y Tabla de Trazabilidad (`state_movements`)

El modelo de datos relacional está gestionado con JPA/Hibernate y migraciones versionadas en Flyway (V1__beta_schema.sql a V5__payments.sql), compatible tanto con MySQL 8.4 en producción como con H2 en modo MySQL para ejecución portable. Todas las entidades de negocio heredan de BaseEntity (id, created_at, updated_at, deleted, deleted_at), garantizando auditoría temporal y baja lógica.

### 2.1 Diccionario de Tablas y Relaciones

| Tabla SQL | Entidad JPA | Claves Foráneas / Relaciones | Propósito y Estados |
| :--- | :--- | :--- | :--- |
| `persons` | `Person` | 1:1 con users | Datos personales: first_name, last_name, birth_date. |
| `users` | `User` | person_id -> persons.id | Credenciales (username, email, password BCrypt) y estado enabled. |
| `profiles` | `Profile` | user_id -> users.id (UNIQUE) | Biografía (bio) y foto de perfil del usuario (avatar_url). |
| `groups` | `Group` | owner_id -> users.id | Bandas musicales: name, description, avatar_url y version (@Version). |
| `group_members` | `(JoinTable)` | group_id -> groups.id, user_id -> users.id | Relación N:M de integrantes activos que pertenecen a cada banda. |
| `memberships` | `Membership` | group_id -> groups.id, user_id -> users.id | Rol interno del integrante en la banda: instrument, can_publish, can_approve. |
| `group_join_requests` | `GroupJoinRequest` | group_id -> groups.id, requester_id -> users.id | Solicitudes de ingreso. Columna status: PENDING, APPROVED, REJECTED, CANCELLED. |
| `contents / posts` | `Content / Post` | author_id -> users.id, profile_id -> profiles.id, band_id -> groups.id | Herencia JOINED. Pistas de audio, duración, visibilidad (PUBLIC / PRIVATE). |
| `tags / post_tags` | `Tag` | post_id -> posts.id, tag_id -> tags.id | Etiquetas normalizadas en minúsculas asociadas a las publicaciones. |
| `comments` | `Comment` | post_id -> posts.id, author_id -> users.id | Comentarios en publicaciones (hasta 280 caracteres). |
| `post_reactions` | `PostReaction` | post_id -> posts.id, user_id -> users.id | Reacción única por usuario y post: reaction_value (+1 Like / -1 Dislike). |
| `profile_ratings` | `ProfileRating` | profile_id -> profiles.id, reviewer_id -> users.id | Calificación única por par evaluador-perfil: score (1 a 5 estrellas). |
| `user_blocks` | `UserBlock` | owner_id -> users.id, target_id -> users.id | Bloqueos entre usuarios para restringir visibilidad e interacciones. |
| `studios` | `Studio` | owner_id -> users.id | Salas y estudios: studio_type (REHEARSAL, RECORDING, VOCAL_BOOTH), hourly_rate. |
| `studio_bookings` | `StudioBooking` | studio_id -> studios.id, user_id -> users.id | Reservas horarias (start_at, end_at, hours, total_price). Estado: CONFIRMED, CANCELLED. |
| `job_postings` | `JobPosting` | author_id -> users.id | Avisos laborales (job_type, instrument, location, remote, pay). Estado: open (OPEN / CLOSED). |
| `job_applications` | `JobApplication` | job_id -> job_postings.id, applicant_id -> users.id | Postulaciones a avisos. Columna status: PENDING, ACCEPTED, REJECTED. |
| `payments` | `Payment` | payer_user_id -> users.id, recipient_user_id -> users.id | Órdenes de pago de salas y contrataciones (concept_type, reference_id, amount, payment_method: MERCADO_PAGO/BANK_TRANSFER/CARD, receipt_code). Estado: PENDING_PAYMENT, PAID, REFUNDED, CANCELLED. |
| `state_movements` | `StateMovement` | actor_user_id -> users.id | Tabla unificada de trazabilidad: entity_type, entity_id, entity_label, previous_status, new_status, actor_user_id, note, created_at. |

### 2.2 Funcionamiento de la Tabla de Movimientos (`state_movements`)

¿Cómo resuelve Harmony la trazabilidad de estados? En lugar de sobrescribir silenciosamente la columna status de una solicitud, reserva o postulación perdiendo la historia de quién y cuándo la modificó, cada servicio de negocio invoca a StateMovementService.record(...) dentro de la misma transacción (@Transactional READ_COMMITTED). De esta manera:
1) La tabla operativa (group_join_requests, studio_bookings, job_applications o job_postings) mantiene el estado actual para consultas rápidas e índices de negocio.
2) La tabla state_movements inserta un registro inmutable por cada transición con el estado anterior (previous_status, que es NULL / INICIAL en el alta), el nuevo estado (new_status), el identificador de la entidad afectada (entity_type + entity_id), el usuario que ejecutó la acción (actor_user_id), la fecha/hora exacta (created_at) y la observación (note).


---

## 3. Especificación Formal de Casos de Uso

### 3.1 Caso de Uso 01: Solicitud de Ingreso a una Banda (`group_join_requests`)

| Campo | Detalle de la Especificación |
| :--- | :--- |
| **Código y Nombre** | CU-01 — Solicitar Ingreso a una Banda y Gestionar su Ciclo de Vida |
| **Actores** | • Actor Primario: Músico Solicitante (User que desea sumarse a la banda).<br>• Actor Secundario: Líder de la Banda (groups.owner_id) o Integrante Delegado (memberships.can_approve = true). |
| **Objetivo** | Permitir que un músico solicite ingresar a una banda de la que no forma parte y que dicha solicitud transite de forma controlada y auditable desde el estado PENDING hacia APPROVED, REJECTED o CANCELLED, impactando las tablas de membresía y de movimientos. |
| **Precondiciones** | 1. El Músico Solicitante tiene sesión activa y cuenta habilitada (users.enabled = true, deleted = false).<br>2. La banda destino existe y está activa (groups.deleted = false).<br>3. El solicitante no es miembro actual de la banda (no figura en group_members) ni es su dueño.<br>4. No existe otra solicitud activa con status = 'PENDING' para el mismo par (group_id, requester_id). |
| **Flujo Principal (Envío y Aprobación)** | 1. El Músico Solicitante accede a '/groups/explore' o '/bands/{id}' y envía el formulario de solicitud con un mensaje opcional (hasta 255 caracteres).<br>2. GroupJoinRequestService.submitRequest(...) adquiere un bloqueo pesimista (PESSIMISTIC_WRITE) sobre la fila de la banda en 'groups' para evitar condiciones de carrera.<br>3. El sistema verifica que el usuario no sea miembro y que no tenga otra solicitud en estado PENDING.<br>4. El sistema inserta una nueva fila en 'group_join_requests' con status = 'PENDING'.<br>5. El sistema inserta en 'state_movements' un movimiento con entity_type = 'GROUP_JOIN_REQUEST', previous_status = NULL (INICIAL), new_status = 'PENDING', actor_user_id = requester_id.<br>6. El Líder de la banda (o miembro con can_approve = true) ingresa a '/groups/requests' y presiona 'Aprobar'.<br>7. GroupJoinRequestService.approveRequest(...) bloquea la banda, verifica permisos y cambia en la MISMA fila de 'group_join_requests' la columna status de 'PENDING' a 'APPROVED'.<br>8. El sistema invoca a GroupService.joinGroup(...), que inserta la relación (group_id, requester_id) en la tabla intermedia 'group_members'.<br>9. El sistema inserta en 'state_movements' el movimiento con previous_status = 'PENDING', new_status = 'APPROVED', actor_user_id = owner_id y note = 'Solicitud aprobada e integrante sumado a la banda'. |
| **Flujos Alternativos** | • Flujo Alternativo A1 — Rechazo por el Líder o Delegado (PENDING -> REJECTED):<br>  En el paso 6, el Líder presiona 'Rechazar' (POST /groups/requests/{id}/reject). El sistema actualiza 'group_join_requests.status' = 'REJECTED', NO inserta filas en 'group_members' y registra en 'state_movements' la transición PENDING -> REJECTED con actor_user_id = owner_id.<br><br>• Flujo Alternativo A2 — Cancelación por el Propio Solicitante (PENDING -> CANCELLED):<br>  Mientras la solicitud sigue en estado PENDING, el propio Músico Solicitante ingresa a '/groups/requests' (sección 'Mis solicitudes enviadas') y presiona 'Cancelar solicitud' (POST /groups/requests/{id}/cancel). El sistema valida que actor == requester_id, actualiza 'group_join_requests.status' = 'CANCELLED' (sin borrar físicamente la fila) y registra en 'state_movements' la transición PENDING -> CANCELLED con actor_user_id = requester_id.<br><br>• Flujo Alternativo A3 — Configuración posterior de Instrumento y Permisos (tabla memberships):<br>  Una vez aprobada la solicitud, al asignarle instrumento o permisos en '/bands/{id}', se crea o actualiza su fila en la tabla 'memberships' (instrument, can_publish, can_approve). |
| **Excepciones y Reglas de Negocio** | • E1 (Solicitud duplicada): Si ya existe una fila con status = 'PENDING' para esa banda y usuario, se rechaza con IllegalStateException ('Ya enviaste una solicitud pendiente para esta banda').<br>• E2 (Doble procesamiento): Si una solicitud ya está en APPROVED, REJECTED o CANCELLED, cualquier intento de volver a aprobarla, rechazarla o cancelarla lanza IllegalStateException ('La solicitud ya fue procesada').<br>• E3 (Sin permisos): Si un usuario que no es el solicitante intenta cancelar, o un integrante sin permiso can_approve intenta aprobar/rechazar, la operación se aborta. |
| **Postcondiciones** | • El estado final de la solicitud queda persistido en 'group_join_requests.status'.<br>• Solo en el caso APPROVED el usuario pasa a figurar en 'group_members'.<br>• La historia completa de la solicitud (creación y resolución) queda registrada cronológicamente en 'state_movements' y es visible en '/movements' y '/groups/requests'. |

#### Impacto en Tablas y Estados (CU-01)

| Paso / Evento | `group_join_requests` (Misma tabla) | `state_movements` (Trazabilidad) | `group_members` / `memberships` (Relacionadas) |
| :--- | :--- | :--- | :--- |
| **1. Músico envía solicitud** | INSERT fila nueva con status = 'PENDING' | INSERT: prev = NULL (INICIAL) -> new = 'PENDING' (actor = solicitante) | Sin cambios todavía. |
| **2A. Líder aprueba solicitud** | UPDATE misma fila: status pasa de 'PENDING' a 'APPROVED' | INSERT: prev = 'PENDING' -> new = 'APPROVED' (actor = líder/delegado) | INSERT en group_members (group_id, user_id). Luego INSERT/UPDATE en memberships al definir instrumento. |
| **2B. Líder rechaza solicitud** | UPDATE misma fila: status pasa de 'PENDING' a 'REJECTED' | INSERT: prev = 'PENDING' -> new = 'REJECTED' (actor = líder/delegado) | Sin cambios (no ingresa a la banda). |
| **2C. Solicitante cancela su solicitud** | UPDATE misma fila: status pasa de 'PENDING' a 'CANCELLED' (deleted sigue en false) | INSERT: prev = 'PENDING' -> new = 'CANCELLED' (actor = solicitante) | Sin cambios. Al dejar de estar PENDING, el usuario puede volver a postularse en el futuro si lo desea. |

### 3.2 Caso de Uso 02: Reserva de Sala de Ensayo / Estudio (`studio_bookings`)

| Campo | Detalle de la Especificación |
| :--- | :--- |
| **Código y Nombre** | CU-02 — Reservar Turno en Sala de Ensayo / Estudio y Gestionar Cancelaciones |
| **Actores** | • Actor Primario: Músico Cliente (User que realiza la reserva).<br>• Actor Secundario: Responsable / Dueño de la Sala (studios.owner_id). |
| **Objetivo** | Permitir que un músico reserve un bloque horario de 1 a 8 horas en una sala de ensayo, estudio de grabación o cabina vocal verificando en forma atómica que no exista solapamiento horario, y registrar todo su ciclo de estados (CONFIRMED -> CANCELLED) en state_movements. |
| **Precondiciones** | 1. El Músico Cliente está autenticado y no está bloqueado mutuamente con el dueño de la sala (user_blocks).<br>2. La sala existe y está activa (studios.deleted = false).<br>3. El Músico Cliente NO es el dueño de la sala (no puede reservar su propio espacio).<br>4. El horario elegido (start_at) es futuro, comienza en hora exacta (minutos = 00) y no supera los 180 días de anticipación. |
| **Flujo Principal (Reserva Confirmada)** | 1. El Músico Cliente ingresa a '/studios/{id}', selecciona fecha/hora de inicio (start) y cantidad de horas (1 a 8 h) y envía el formulario (POST /studios/{id}/bookings).<br>2. StudioService.book(...) adquiere un bloqueo pesimista (LockModeType.PESSIMISTIC_WRITE) sobre la fila de la sala en 'studios' para serializar reservas concurrentes sobre el mismo espacio.<br>3. El sistema calcula end_at = start_at + hours y ejecuta la consulta de intersección horaria sobre 'studio_bookings' filtrando por studio_id, status = 'CONFIRMED' y la condición (b.start_at < :end AND b.end_at > :start).<br>4. Al verificar que hay 0 reservas superpuestas, calcula total_price = hourly_rate * hours e inserta la fila en 'studio_bookings' con status = 'CONFIRMED'.<br>5. El sistema inserta en 'state_movements' un movimiento con entity_type = 'STUDIO_BOOKING', previous_status = NULL (INICIAL), new_status = 'CONFIRMED', actor_user_id = user_id y note = 'Reserva confirmada por X h'.<br>6. El turno pasa a mostrarse en 'Mis reservas' (/studios) y en la agenda ocupada de la sala (/studios/{id}). |
| **Flujos Alternativos** | • Flujo Alternativo A1 — Cancelación por el Músico Cliente (CONFIRMED -> CANCELLED):<br>  Antes de que finalice el turno (isUpcoming() == true), el músico presiona 'Cancelar reserva' (POST /studios/bookings/{bookingId}/cancel). StudioService.cancel(...) cambia en la misma fila 'studio_bookings.status' de 'CONFIRMED' a 'CANCELLED' y registra en 'state_movements' la transición CONFIRMED -> CANCELLED con note = 'Reserva cancelada por quien reservó'. El bloque horario queda inmediatamente liberado para que otro músico pueda reservarlo.<br><br>• Flujo Alternativo A2 — Cancelación por el Dueño de la Sala (CONFIRMED -> CANCELLED):<br>  El dueño de la sala cancela una reserva futura desde el panel 'Administrar sala' en '/studios/{id}'. Se actualiza 'studio_bookings.status' = 'CANCELLED' y se registra en 'state_movements' con actor_user_id = owner_id y note = 'Reserva cancelada por el responsable de la sala'.<br><br>• Flujo Alternativo A3 — Baja de la Sala y Cancelación en Cascada:<br>  Si el dueño da de baja la sala (POST /studios/{id}/delete), StudioService.remove(...) marca 'studios.deleted' = true, pasa todas sus reservas futuras de CONFIRMED a CANCELLED e inserta un registro individual en 'state_movements' por cada reserva afectada ('Reserva cancelada por baja de la sala'). |
| **Excepciones y Reglas de Negocio** | • E1 (Solapamiento de horario): Si otra reserva con status = 'CONFIRMED' se cruza parcial o totalmente con [start_at, end_at), se lanza IllegalArgumentException ('Ese horario ya está reservado. Elegí otro.').<br>• E2 (Autoreserva): Si actor == studio.owner_id, se rechaza con 'No podés reservar tu propia sala'.<br>• E3 (Reserva pasada o ya cancelada): Si la reserva ya ocurrió o ya está en estado CANCELLED, no se permite volver a cancelarla. |
| **Postcondiciones** | • La reserva queda registrada en 'studio_bookings' con su estado actual (CONFIRMED o CANCELLED).<br>• Las reservas canceladas liberan automáticamente la disponibilidad del bloque horario sin perder el registro histórico.<br>• Toda alta o cancelación queda auditada en 'state_movements' y puede consultarse filtrando por 'Reserva de sala' en '/movements'. |

#### Impacto en Tablas y Estados (CU-02)

| Paso / Evento | `studio_bookings` (Misma tabla) | `state_movements` (Trazabilidad) | `studios` (Relacionada) |
| :--- | :--- | :--- | :--- |
| **1. Músico confirma reserva** | INSERT fila nueva con status = 'CONFIRMED', start_at, end_at, hours, total_price | INSERT: prev = NULL (INICIAL) -> new = 'CONFIRMED' (actor = músico cliente) | Se bloquea con PESSIMISTIC_WRITE durante la validación de disponibilidad. |
| **2A. Músico cancela su reserva** | UPDATE misma fila: status pasa de 'CONFIRMED' a 'CANCELLED' (libera el horario) | INSERT: prev = 'CONFIRMED' -> new = 'CANCELLED' (actor = músico cliente) | La agenda de la sala vuelve a aceptar reservas en ese rango horario. |
| **2B. Dueño de la sala cancela reserva** | UPDATE misma fila: status pasa de 'CONFIRMED' a 'CANCELLED' | INSERT: prev = 'CONFIRMED' -> new = 'CANCELLED' (actor = dueño de la sala) | Sin cambios en la fila de studios. |
| **2C. Dueño da de baja la sala completa** | UPDATE en cascada de reservas futuras: status pasa de 'CONFIRMED' a 'CANCELLED' | INSERT por cada reserva cancelada: prev = 'CONFIRMED' -> new = 'CANCELLED' | UPDATE studios: deleted = true, deleted_at = NOW(). |
