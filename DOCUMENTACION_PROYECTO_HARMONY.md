# PRÁCTICAS — RESUMEN INTEGRAL DEL PROYECTO HARMONY
## Documentación de Ingeniería de Software, Arquitectura, Modelo de Datos, Pagos, Trazabilidad (`state_movements`), Casos de Uso y Automatización Playwright E2E
- **Alumno:** Agustín Navarro (`gladito`)
- **Repositorio Oficial:** https://github.com/gladito/Harmonies_App
- **Stack Tecnológico:** Java 17 · Spring Boot 3.5 · Spring Security 6 · JPA/Hibernate · Flyway (V1–V5) · H2 / MySQL 8.4 · Thymeleaf (ES/EN) · Playwright Java + JUnit 5

---

## Índice General del Documento
- **1. Introducción, Problemática y Visión General del Proyecto**
  - 1.1 Problemáticas Reales que Resuelve la Plataforma
  - 1.2 Objetivos del Sistema (Generales y Específicos)
- **2. Alcance Funcional de los 8 Módulos del Sistema**
  - 2.1 Mapa Completo de Módulos, Pantallas y Endpoints
- **3. Actores, Matriz de Permisos y Reglas de Privacidad**
  - 3.1 Catálogo de Actores del Dominio
  - 3.2 Matriz de Control de Acceso por Operación Crítica
  - 3.3 Privacidad y Bloqueo Bidireccional (user_blocks)
- **4. Arquitectura de Software en Capas y Decisiones Técnicas**
  - 4.1 Organización de Paquetes y Componentes
  - 4.2 Gestión de Archivos de Imagen para Fotos de Perfil (AvatarStorageService)
- **5. Modelo de Datos Relacional y Migraciones Flyway (V1 a V5)**
  - 5.1 Diccionario Exhaustivo de las Tablas del Esquema
  - 5.2 Evolución del Esquema mediante Migraciones Flyway
- **6. Diseño de la Tabla de Movimientos (state_movements) y Trazabilidad**
  - 6.1 Estructura de la Tabla state_movements
  - 6.2 Máquinas de Estado Auditadas en Harmony
- **7. Caso de Uso 01: Solicitar Ingreso a una Banda y Ciclo de Estados**
  - 7.1 Ficha de Especificación Formal — CU-01
- **8. Análisis de Tablas y Estados del Caso de Uso 01**
  - 8.1 Cuadro Comparativo de Impacto por Transición (CU-01)
  - 8.2 Ejemplo de Traza SQL Real de una Solicitud Cancelada vs. Aprobada
- **9. Caso de Uso 02: Reserva de Sala de Ensayo y Circuito de Pago**
  - 9.1 Ficha de Especificación Formal — CU-02
  - 9.2 Impacto en Tablas y Estados — Caso de Uso 02
- **10. Seguridad, Concurrencia y Prevención de Condiciones de Carrera**
  - 10.1 Mecanismos de Seguridad Implementados
  - 10.2 Control de Concurrencia y Bloqueos Pesimistas
- **11. Validación Automatizada: JUnit 5, MockMvc y Suite Playwright E2E**
  - 11.1 Suite de Pruebas de Integración y Concurrencia (Spring Boot Test)
  - 11.2 Detalle de los 8 Ejercicios de Playwright con Java (com.tests)
  - 11.3 Comandos de Ejecución y Conclusiones

---

## 1. Introducción, Problemática y Visión General del Proyecto

Harmony es una plataforma web integral concebida para resolver las necesidades de comunicación, colaboración artística, reserva de infraestructura musical y contratación laboral dentro de la comunidad de músicos independientes y bandas emergentes. En el ecosistema musical actual, los artistas suelen depender de múltiples canales desconectados (redes sociales genéricas, chats informales, planillas manuales para reservar salas de ensayo y transferencias sin comprobante unificado) para llevar adelante sus proyectos.

El proyecto Harmony centraliza todo ese ciclo de vida en una única aplicación web robusta construida bajo arquitectura en capas con Java 17, Spring Boot 3.5, Spring Security 6, Spring Data JPA / Hibernate, migraciones versionadas con Flyway (V1 a V5), motores relacionales MySQL 8.4 y H2, plantillas SSR con Thymeleaf bilingüe (Español / Inglés) y una suite completa de pruebas de integración y extremo a extremo (E2E) automatizada con Playwright para Java y JUnit 5.

### 1.1 Problemáticas Reales que Resuelve la Plataforma

- Dispersión en la búsqueda de colaboradores: los músicos carecen de un directorio especializado donde evaluar perfiles por instrumento, escuchar demos reales de audio y consultar calificaciones verificadas de otros colegas.
- Falta de gobernanza interna en las bandas: cuando un grupo musical crece, necesita distinguir entre el líder/propietario (owner) y los demás integrantes, delegando permisos específicos para publicar material o aprobar solicitudes de ingreso sin ceder el control total del proyecto.
- Conflictos y superposiciones al reservar salas de ensayo o estudios: la gestión manual de turnos genera dobles reservas en una misma franja horaria y confusión sobre el estado del pago o del reembolso ante cancelaciones.
- Ausencia de un circuito formal de contratación y pago entre músicos: las búsquedas de sesionistas, shows en vivo, mezcla o clases requieren vincular la aceptación del postulante con una orden de pago trazable y su respectivo comprobante.
- Falta de trazabilidad histórica (auditoría de estados): en los sistemas transaccionales académicos y profesionales no basta con conocer el estado actual de una entidad; es indispensable registrar en una tabla de movimientos (state_movements) cada transición de estado, quién la ejecutó, en qué fecha/hora exacta y bajo qué motivo.

### 1.2 Objetivos del Sistema (Generales y Específicos)

El objetivo general de Harmony es proveer una red social y mercado colaborativo musical seguro, concurrente y auditable, donde cada interacción crítica quede respaldada por reglas de negocio transaccionales y pruebas automatizadas.

| Objetivo Específico | Implementación Concreta en el Proyecto Harmony |
| --- | --- |
| Identidad Musical y Perfiles con Multimedia | Separación de datos civiles (persons), credenciales (users) y perfil artístico (profiles) con biografía, calificaciones de 1 a 5 estrellas y carga híbrida de foto de perfil (archivo local MultipartFile o URL externa). |
| Difusión de Demos y Feedback Comunitario | Publicación de pistas de audio HTTPS (solistas o en nombre de una banda), categorización con hasta 5 etiquetas normalizadas, reacciones (+1 / -1) sin duplicados y comentarios moderables. |
| Gestión de Bandas y Permisos Delegados | Creación de bandas con foto grupal, flujo de solicitudes de ingreso (PENDING -> APPROVED / REJECTED / CANCELLED), asignación de instrumentos y delegación granular de permisos (can_publish, can_approve). |
| Reserva Atómica de Salas y Estudios | Catálogo de salas de ensayo, estudios de grabación y cabinas vocales con reservas por bloques de 1 a 8 horas protegidas mediante bloqueo pesimista (PESSIMISTIC_WRITE) para impedir solapamientos horarios. |
| Bolsa de Trabajo y Circuito de Pagos | Publicación de avisos laborales, postulaciones y generación automática de órdenes de pago en la tabla payments (Mercado Pago, Transferencia CBU/Alias o Tarjeta) con emisión de comprobante único (HRM-...). |
| Trazabilidad Total de Estados | Tabla polimórfica state_movements que audita automáticamente todas las transiciones de estado de solicitudes de banda, reservas de salas, avisos laborales, postulaciones y pagos. |

---

## 2. Alcance Funcional de los 8 Módulos del Sistema

Harmony está estructurado en ocho módulos funcionales cohesivos. Todos ellos comparten la estética visual institucional del proyecto (paleta obsidiana #080808, superficies #121212 y dorado metálico #d4af37 con tipografías Cinzel y Montserrat), soporte completo de internacionalización (ES/EN) y navegación unificada.

### 2.1 Mapa Completo de Módulos, Pantallas y Endpoints

| Módulo | Controlador y Servicio | Rutas HTTP / Vistas | Funcionalidades Clave |
| --- | --- | --- | --- |
| 1. Portada Pública e Identidad | AuthController<br>AuthService | GET / (index.html)<br>GET/POST /login<br>GET/POST /register<br>GET /account (profile.html) | Landing page con contadores reales en vivo, bandas destacadas y arquitectura; registro con validación de clave robusta; login con BCrypt y sesión HTTP; actualización de nickname, contraseña y avatar. |
| 2. Comunidad de Músicos | CommunityController<br>CommunityService<br>AvatarStorageService | GET /people<br>GET /people/{id}<br>POST /profile/bio<br>POST /profile/avatar<br>POST /people/{id}/rating<br>POST /people/{id}/block | Directorio de músicos activos; perfil público con foto circular o inicial dorada; edición de biografía (160 car.) y subida de foto (PNG/JPG/WEBP/GIF/SVG hasta 5 MB); calificación 1-5 estrellas; bloqueo bidireccional. |
| 3. Feed Musical y Demos | HomeController<br>PostController<br>PostService<br>CommentService | GET /home<br>GET/POST /posts/new<br>GET /posts/{id}<br>GET/POST /posts/{id}/edit<br>POST /posts/{id}/delete<br>POST /posts/{id}/reaction | Feed paginado (12 tracks por página) con filtro por etiqueta; reproductor de audio HTML5; reacciones Like/Dislike conmutables; comentarios de hasta 280 caracteres; visibilidad PUBLIC o PRIVATE. |
| 4. Bandas y Solicitudes | GroupController<br>CommunityController<br>GroupService<br>GroupJoinRequestService | GET /groups<br>GET /groups/explore<br>GET/POST /groups/new<br>GET /bands/{id}<br>POST /bands/{id}/avatar<br>GET /groups/requests | Creación y edición de bandas con foto de perfil; envío de solicitud de ingreso; aprobación, rechazo y cancelación por el propio solicitante; asignación de instrumento, permisos delegados y transferencia de dueño. |
| 5. Salas de Ensayo y Estudios | StudioController<br>StudioService | GET/POST /studios<br>GET /studios/{id}<br>POST /studios/{id}/book<br>POST /studios/bookings/{id}/cancel<br>POST /studios/{id}/delete | Filtro por categoría (REHEARSAL, RECORDING, VOCAL_BOOTH); publicación de sala con tarifa horaria; reserva de 1 a 8 horas en horas exactas sin solapamiento; cancelación por cliente o dueño; baja lógica en cascada. |
| 6. Bolsa de Trabajo Musical | JobController<br>JobService | GET/POST /jobs<br>GET /jobs/{id}<br>POST /jobs/{id}/apply<br>POST /jobs/{id}/applications/{appId}<br>POST /jobs/{id}/open | Avisos por tipo (SESSION, LIVE_GIG, POST_PRODUCTION, BAND_MEMBER, TEACHING); modalidad presencial o remota; postulación de músicos; aceptación o rechazo por el autor; cierre y reapertura del aviso. |
| 7. Pagos y Comprobantes | PaymentController<br>PaymentService | GET /payments<br>POST /payments/{id}/pay |  Tablero financiero con totales abonados, cobrados y pendientes; pago inmediato o diferido de reservas de salas y contrataciones laborales vía Mercado Pago, Transferencia CBU/Alias o Tarjeta; reembolso automático. |
| 8. Trazabilidad (Movimientos) | MovementController<br>StateMovementService | GET /movements<br>GET /movements?type=... | Historial cronológico auditable de la tabla state_movements filtrable por tipo de entidad (Solicitud de banda, Postulación, Aviso, Reserva de sala, Pago) y por usuario ejecutor. |

---

## 3. Actores, Matriz de Permisos y Reglas de Privacidad

El control de acceso en Harmony combina autenticación basada en sesiones de Spring Security (rol base ROLE_USER y verificación en tiempo real mediante ActiveAccountFilter) con reglas de autorización basadas en propiedad y delegación explícita dentro del dominio (Ownership & Delegated Permissions).

### 3.1 Catálogo de Actores del Dominio

| Actor | Responsabilidades y Alcance de Autorización |
| --- | --- |
| Visitante Anónimo | Solo tiene acceso a rutas públicas autorizadas en SecurityConfig: /, /login, /register, /css/**, /js/**, /images/**, /avatars/**. Cualquier intento de acceder a rutas internas redirige a /login. |
| Músico Registrado | Usuario autenticado con cuenta activa (enabled = true, deleted = false). Puede gestionar su propio perfil, avatar y publicaciones, calificar a terceros, solicitar ingreso a bandas, reservar salas, postularse a empleos y abonar sus órdenes de pago. |
| Líder / Dueño de Banda | Usuario referenciado en groups.owner_id. Posee control total sobre la banda: edita datos y foto grupal, publica como banda, aprueba/rechaza solicitudes, otorga o revoca permisos can_publish y can_approve a otros miembros, transfiere la propiedad o elimina la banda. |
| Integrante Delegado | Miembro de una banda cuya fila en memberships posee can_publish = true (habilitado a publicar y editar posts asociados a la banda) y/o can_approve = true (habilitado a aprobar o rechazar solicitudes de ingreso pendientes). No puede transferir la banda ni cambiar permisos de otros. |
| Responsable de Sala | Usuario referenciado en studios.owner_id. Visualiza todas las reservas futuras de su espacio, puede cancelar turnos futuros y despublicar la sala (disparando la cancelación y reembolso de turnos pendientes). No puede reservar su propia sala. |
| Anunciante Laboral | Usuario referenciado en job_postings.author_id. Visualiza la lista completa de postulantes a su aviso, decide su aceptación o rechazo, abona la contratación del músico aceptado y puede cerrar o reabrir la búsqueda. |

### 3.2 Matriz de Control de Acceso por Operación Crítica

| Operación del Sistema | Anónimo | Músico | Miembro Delegado | Líder Banda | Dueño Sala / Aviso |
| --- | --- | --- | --- | --- | --- |
| Ver portada (/) y registrarse | SÍ | Redirige /home | Redirige /home | Redirige /home | Redirige /home |
| Cambiar foto de perfil de usuario | NO | SÍ (propia) | SÍ (propia) | SÍ (propia) | SÍ (propia) |
| Cambiar foto o editar banda | NO | NO | NO | SÍ (su banda) | NO |
| Publicar track como banda | NO | NO | SÍ (can_publish) | SÍ | NO |
| Aprobar/Rechazar solicitud banda | NO | NO | SÍ (can_approve) | SÍ | NO |
| Cancelar solicitud de ingreso | NO | SÍ (solicitante) | NO | NO | NO |
| Transferir liderazgo de banda | NO | NO | NO | SÍ (a miembro) | NO |
| Reservar sala de ensayo | NO | SÍ (no dueño) | SÍ (no dueño) | SÍ (no dueño) | NO (su propia sala) |
| Cancelar reserva futura de sala | NO | SÍ (su reserva) | SÍ (su reserva) | SÍ (su reserva) | SÍ (dueño de sala) |
| Aceptar/Rechazar postulación | NO | NO | NO | NO | SÍ (autor aviso) |
| Abonar orden de pago (payments) | NO | SÍ (pagador) | SÍ (pagador) | SÍ (pagador) | SÍ (contratante) |

### 3.3 Privacidad y Bloqueo Bidireccional (user_blocks)

Cuando un usuario A bloquea a un usuario B (POST /people/{id}/block), se persiste una fila en la tabla user_blocks (owner_id = A, target_id = B) con restricción CHECK (owner_id <> target_id). A partir de ese instante, CommunityService.canSeeUser(...) e impide que B visualice el perfil de A, sus publicaciones en el feed (aplicado antes de paginar para no distorsionar el tamaño de página), sus comentarios, sus salas de ensayo o sus avisos laborales, e impide calificarlo o reaccionar a su contenido.

---

## 4. Arquitectura de Software en Capas y Decisiones Técnicas

El código fuente sigue una arquitectura multicapa estricta en la cual las vistas Thymeleaf nunca acceden directamente a repositorios ni entidades sin pasar por los controladores y servicios transaccionales. Esta separación facilita el testing automatizado tanto a nivel de servicios como mediante MockMvc y Playwright.

### 4.1 Organización de Paquetes y Componentes

| Paquete Java | Clases Incluidas y Responsabilidad Arquitectónica |
| --- | --- |
| com.example.demo.config | • SecurityConfig: define PasswordEncoder (BCrypt), UserDetailsService por email, SecurityFilterChain, protección CSRF y gestión de sesión.<br>• ActiveAccountFilter: filtro ejecutado tras la autenticación que invalida inmediatamente la sesión si el usuario fue deshabilitado o dado de baja lógica.<br>• LanguageConfig: configura CookieLocaleResolver ('harmony-language') y LocaleChangeInterceptor ('lang') para alternar entre Español e Inglés.<br>• DataSeeder: inicializador idempotente (@Profile('demo')) que puebla los 20 músicos, 5 bandas, avatares SVG, 12 pistas de audio, comentarios, reacciones, calificaciones, 5 salas, 6 empleos, solicitudes, pagos y movimientos iniciales. |
| com.example.demo.Controller | • AuthController, HomeController, CommunityController, PostController, GroupController, StudioController, JobController, PaymentController, MovementController y CommunityErrors (@ControllerAdvice para captura global de excepciones de dominio y violaciones de integridad). |
| com.example.demo.Services | • AuthService, CommunityService, PostService, CommentService, GroupService, GroupJoinRequestService, StudioService, JobService, PaymentService, StateMovementService y AvatarStorageService.<br>• Todos los métodos mutadores se ejecutan dentro de transacciones @Transactional con aislamiento READ_COMMITTED. |
| com.example.demo.Models | • Entidades JPA: BaseEntity, Person, User, Profile, Role, Group, Membership, GroupJoinRequest, Content, Post, Tag, Comment, PostReaction, ProfileRating, UserBlock, Studio, StudioBooking, JobPosting, JobApplication, Payment y StateMovement.<br>• Enumeraciones de estado y tipo: Visibility, GroupJoinRequestStatus, StudioType, BookingStatus, JobType, ApplicationStatus, PaymentMethod, PaymentStatus, PaymentConceptType y MovementEntityType. |
| com.example.demo.Repositories | • Interfaces Spring Data JPA: UserRepository, ProfileRepository, GroupRepository, GroupJoinRequestRepository, PostRepository, CommentRepository y TagRepository, complementadas con consultas JPQL optimizadas con JOIN FETCH en EntityManager. |
| com.example.demo.Dtos | • Objetos de transferencia desacoplados para formularios y vistas: AuthLoginRequestDto, AuthRegisterRequestDto, ProfileUpdateRequestDto, PasswordChangeRequestDto, GroupCreateRequestDto, GroupSummaryDto, MemberDto, GroupJoinRequestCreateDto, GroupJoinRequestResponseDto, PostCreateRequestDto, PostUpdateRequestDto, PostFeedResponseDto, PostDetailDto y CommentCreateRequestDto. |

### 4.2 Gestión de Archivos de Imagen para Fotos de Perfil (AvatarStorageService)

Para permitir que tanto los usuarios como las bandas posean foto de perfil sin depender de servicios cloud externos durante la evaluación académica, se diseñó AvatarStorageService con soporte dual:
1) Subida de archivos locales (MultipartFile): valida que el archivo no supere los 5 MB, verifica que la extensión y el MIME type correspondan a formatos de imagen seguros (PNG, JPG, JPEG, WEBP, GIF, SVG), normaliza el nombre con un prefijo e identificador único (por ejemplo user-1-a8f921b0.png o band-3-9c12e4f1.png), previene ataques de Path Traversal verificando Path.startsWith(storageDir) y sirve el archivo desde el endpoint GET /avatars/{filename} con cabeceras Cache-Control.
2) Enlaces externos o avatares vectoriales: valida esquemas http/https o rutas internas /images/avatars/..., permitiendo también quitar la foto actual para restablecer el avatar circular con la inicial dorada.

---

## 5. Modelo de Datos Relacional y Migraciones Flyway (V1 a V5)

La base de datos de Harmony fue diseñada en Tercera Forma Normal (3FN), separando claramente las entidades maestras (persons, users, profiles, groups, studios, job_postings), las tablas operativas con ciclo de vida (group_join_requests, studio_bookings, job_applications, payments) y la tabla histórica de auditoría (state_movements).

### 5.1 Diccionario Exhaustivo de las Tablas del Esquema

| Tabla SQL | Entidad JPA | Claves Foráneas e Índices | Columnas Principales y Restricciones |
| --- | --- | --- | --- |
| persons | Person | PK: id | first_name (80), last_name (80), birth_date (date), campos BaseEntity. |
| users | User | FK: person_id (UNIQUE)<br>UK: username, email | username (40), email (255), password (BCrypt 255), enabled (bit). |
| profiles | Profile | FK: user_id (UNIQUE) | bio (160), avatar_url (255) para la foto de perfil del músico. |
| roles / user_roles | Role | PK compuesta (user_id, role_id) | name (40, UNIQUE). Asocia roles de seguridad a los usuarios. |
| groups | Group | FK: owner_id -> users.id<br>UK: name | name (80), description (255), avatar_url (255, V3), version (@Version bigint). |
| group_members | (JoinTable) | PK: (group_id, user_id) | Tabla puente N:M de integrantes activos de cada banda. |
| memberships | Membership | UK: (group_id, user_id) | instrument (80), can_publish (bit), can_approve (bit). |
| group_join_requests | GroupJoinRequest | FK: group_id, requester_id<br>IX: ix_requests_lookup | status (15: PENDING, APPROVED, REJECTED, CANCELLED), message (255). |
| contents | Content | PK: id<br>IX: ix_content_feed | Tabla padre (JOINED): title (120), description (500), visibility (PUBLIC/PRIVATE). |
| posts | Post | PK/FK: id -> contents.id<br>FK: author_id, profile_id, band_id | audio_url (255), duration_in_seconds (int). Permite publicar como solista o banda. |
| tags / post_tags | Tag | PK: (post_id, tag_id)<br>IX: ix_post_tags_tag | value (50, UNIQUE). Hasta 5 etiquetas normalizadas por publicación. |
| comments | Comment | FK: post_id, author_id<br>IX: ix_comments_post | content (280). Comentarios ordenados cronológicamente. |
| post_reactions | PostReaction | UK: (post_id, user_id)<br>CHECK: reaction_value IN (-1,1) | reaction_value (int: +1 Me gusta, -1 No me gusta). |
| profile_ratings | ProfileRating | UK: (profile_id, reviewer_id)<br>CHECK: score BETWEEN 1 AND 5 | score (int 1..5). Impide autocalificación y duplicados por par de usuarios. |
| user_blocks | UserBlock | UK: (owner_id, target_id)<br>CHECK: owner_id <> target_id | Registra bloqueos de privacidad entre usuarios. |
| studios | Studio | FK: owner_id -> users.id<br>IX: ix_studios_type | name (80), studio_type (20), description (500), location (120), amenities (255), hourly_rate (int). |
| studio_bookings | StudioBooking | FK: studio_id, user_id<br>IX: ix_bookings_slot | start_at (datetime), end_at (datetime), hours (1..8), total_price (int), status (CONFIRMED, CANCELLED). |
| job_postings | JobPosting | FK: author_id -> users.id<br>IX: ix_jobs_open_type | title (120), description (1000), job_type (20), instrument (80), location (120), remote (bit), pay (60), is_open (bit). |
| job_applications | JobApplication | UK: (job_id, applicant_id) | message (500), status (16: PENDING, ACCEPTED, REJECTED). |
| payments | Payment | FK: payer_user_id, recipient_user_id<br>IX: ix_payments_reference | concept_type (32), reference_id (bigint), description (180), amount (int), payment_method (32), status (32), receipt_code (64), paid_at. |
| state_movements | StateMovement | FK: actor_user_id -> users.id<br>IX: ix_movements_entity | entity_type (32), entity_id (bigint), entity_label (160), previous_status (32), new_status (32), note (255). |

### 5.2 Evolución del Esquema mediante Migraciones Flyway

- V1__beta_schema.sql: crea las 14 tablas fundacionales de comunidad, usuarios, perfiles, bandas, membresías, publicaciones de audio, etiquetas, comentarios, reacciones, calificaciones y bloqueos.
- V2__studios_jobs.sql: incorpora las 4 tablas de los módulos de Salas de Ensayo (studios, studio_bookings) y Bolsa de Trabajo (job_postings, job_applications) con sus respectivos índices de agenda y búsqueda.
- V3__group_avatar.sql: extiende la tabla groups agregando la columna avatar_url varchar(255) para soportar fotos de perfil en las bandas.
- V4__state_movements.sql: crea la tabla unificada state_movements con índices por entidad, actor y línea de tiempo para la trazabilidad de estados.
- V5__payments.sql: crea la tabla payments para gestionar órdenes de pago, métodos de pago, comprobantes y reembolsos.

---

## 6. Diseño de la Tabla de Movimientos (state_movements) y Trazabilidad

Uno de los requerimientos centrales de ingeniería de software y bases de datos exigidos en la cátedra es contar con una tabla de 'movimientos' que permita conocer en todo momento por qué estados pasó cada entidad del sistema, quién ejecutó cada cambio y en qué instante exacto ocurrió.

En Harmony, este requerimiento se resolvió mediante un diseño híbrido de alto rendimiento compuesto por el estado actual en la tabla operativa más el registro histórico inmutable en la tabla polimórfica state_movements.

### 6.1 Estructura de la Tabla state_movements

| Columna | Tipo SQL | Descripción y Rol en la Auditoría |
| --- | --- | --- |
| id | bigint PK AUTO_INCREMENT | Identificador único secuencial del movimiento. |
| created_at | datetime(6) NOT NULL | Fecha y hora exacta (con precisión de microsegundos) en que ocurrió la transición. |
| entity_type | varchar(32) NOT NULL | Discriminador del módulo: GROUP_JOIN_REQUEST, STUDIO_BOOKING, JOB_POSTING, JOB_APPLICATION o PAYMENT. |
| entity_id | bigint NOT NULL | Clave primaria del registro afectado en su tabla origen (por ejemplo group_join_requests.id). |
| entity_label | varchar(160) NOT NULL | Descripción legible del objeto afectado (ej. 'nicofontana -> Nebula Waves' o 'gladito -> Subsuelo Groove'). |
| previous_status | varchar(32) NULL | Estado previo antes del cambio. Es NULL (mostrado como INICIAL) cuando el registro se crea por primera vez. |
| new_status | varchar(32) NOT NULL | Nuevo estado al que pasó la entidad tras ejecutarse la acción de negocio. |
| actor_user_id | bigint FK NOT NULL | Clave foránea a users(id) que identifica de forma fehaciente qué usuario ejecutó la transición. |
| note | varchar(255) NULL | Motivo, mensaje adjunto, código de comprobante o explicación de la transición. |

### 6.2 Máquinas de Estado Auditadas en Harmony

| Tipo (entity_type) | Tabla Operativa | Transiciones Permitidas (previous -> new) |  Eventos Disparadores |
| --- | --- | --- | --- |
| GROUP_JOIN_REQUEST | group_join_requests | • NULL -> PENDING<br>• PENDING -> APPROVED<br>• PENDING -> REJECTED<br>• PENDING -> CANCELLED | • Músico envía solicitud a banda.<br>• Líder/delegado aprueba (suma a group_members).<br>• Líder/delegado rechaza.<br>• El propio solicitante cancela su solicitud. |
| STUDIO_BOOKING | studio_bookings | • NULL -> CONFIRMED<br>• CONFIRMED -> CANCELLED | • Músico reserva horario disponible.<br>• Músico o dueño de sala cancela reserva futura, o se da de baja la sala. |
| JOB_POSTING | job_postings | • NULL -> OPEN<br>• OPEN -> CLOSED<br>• CLOSED -> OPEN | • Autor publica aviso laboral.<br>• Autor cierra recepción de postulantes.<br>• Autor reabre el aviso. |
| JOB_APPLICATION | job_applications | • NULL -> PENDING<br>• PENDING -> ACCEPTED<br>• PENDING -> REJECTED | • Músico se postula a un aviso abierto.<br>• Autor acepta al músico (genera orden de pago).<br>• Autor rechaza la postulación. |
| PAYMENT | payments | • NULL -> PENDING_PAYMENT<br>• PENDING_PAYMENT -> PAID<br>• PAID -> REFUNDED<br>• PENDING_PAYMENT -> CANCELLED | • Se genera orden por reserva o contratación.<br>• Pagador confirma medio de pago (emite recibo).<br>• Se cancela reserva ya abonada (reembolso).<br>• Se cancela reserva aún no abonada. |

---

## 7. Caso de Uso 01: Solicitar Ingreso a una Banda y Ciclo de Estados

A continuación se presenta la especificación formal del Caso de Uso 01 (CU-01), detallando su flujo principal, flujos alternativos (incluyendo la cancelación por el propio solicitante), validaciones de concurrencia y el impacto exacto en la tabla group_join_requests y en las tablas relacionadas (state_movements, groups, group_members y memberships).

### 7.1 Ficha de Especificación Formal — CU-01

| Atributo | Especificación Detallada |
| --- | --- |
| Código y Nombre | CU-01 — Solicitar Ingreso a una Banda y Gestionar su Ciclo de Vida |
| Actores | • Actor Primario: Músico Solicitante (User autenticado que no pertenece a la banda).<br>• Actor Secundario: Líder de la Banda (groups.owner_id) o Integrante con Permiso Delegado (memberships.can_approve = true). |
| Descripción Resumen | Un músico explora las bandas disponibles en Harmony y envía una solicitud de ingreso con un mensaje de presentación. La solicitud nace en estado PENDING y puede finalizar en APPROVED (integrando al músico a la banda), REJECTED (rechazada por el líder/delegado) o CANCELLED (cancelada voluntariamente por el propio solicitante antes de ser respondida). |
| Precondiciones | 1. El Músico Solicitante inició sesión y su cuenta está activa (users.enabled = true, deleted = false).<br>2. La banda destino existe y no fue eliminada (groups.deleted = false).<br>3. El solicitante no es el propietario de la banda ni figura ya como miembro en group_members.<br>4. No existe en group_join_requests una fila activa con status = 'PENDING' para ese mismo group_id y requester_id. |
| Flujo Principal<br>(Alta y Aprobación) | 1. El Músico Solicitante ingresa a '/groups/explore' o '/bands/{groupId}', redacta un mensaje opcional (máx. 255 caracteres) y envía el formulario (POST /groups/{groupId}/request).<br>2. GroupJoinRequestService.submitRequest(...) adquiere un bloqueo pesimista (LockModeType.PESSIMISTIC_WRITE) sobre la entidad Group y la refresca desde la base de datos para impedir condiciones de carrera.<br>3. El servicio verifica que el usuario no sea miembro actual y que no posea otra solicitud en estado PENDING para esa banda.<br>4. Se instancia GroupJoinRequest con status inicial = GroupJoinRequestStatus.PENDING y se persiste en la tabla 'group_join_requests'.<br>5. En la misma transacción, StateMovementService.record(...) inserta en 'state_movements' una fila con entity_type = 'GROUP_JOIN_REQUEST', entity_id = request.id, previous_status = NULL (INICIAL), new_status = 'PENDING' y actor_user_id = requester_id.<br>6. El Líder de la banda (o un integrante con can_approve = true) accede a '/groups/requests', visualiza la solicitud pendiente y presiona el botón 'Aprobar' (POST /groups/requests/{requestId}/approve).<br>7. GroupJoinRequestService.approveRequest(...) bloquea la banda con PESSIMISTIC_WRITE, verifica que el actor tenga permiso de aprobación y comprueba que request.status == PENDING.<br>8. Se invoca request.approve(), actualizando EN LA MISMA FILA de 'group_join_requests' la columna status al valor 'APPROVED' y actualizando updated_at.<br>9. Se invoca GroupService.joinGroup(groupId, requesterId), el cual inserta el par (group_id, requester_id) en la tabla intermedia 'group_members'.<br>10. StateMovementService.record(...) inserta en 'state_movements' la transición con previous_status = 'PENDING', new_status = 'APPROVED', actor_user_id = ownerId y note = 'Solicitud aprobada e integrante sumado a la banda'. |
| Flujo Alternativo A1<br>(Rechazo por Líder) | En el paso 6, el Líder o delegado presiona 'Rechazar' (POST /groups/requests/{requestId}/reject). El sistema valida permisos y que status == PENDING, ejecuta request.reject() cambiando 'group_join_requests.status' a 'REJECTED' en la misma fila, NO modifica 'group_members' e inserta en 'state_movements' el registro PENDING -> REJECTED con actor_user_id = ownerId. |
| Flujo Alternativo A2<br>(Cancelación Propia) | Mientras la solicitud permanece en estado PENDING, el propio Músico Solicitante accede a '/groups/requests' (sección 'Mis solicitudes enviadas') y presiona 'Cancelar solicitud' (POST /groups/requests/{requestId}/cancel). El método cancelRequest(...) verifica que requester_id == actorId y que status == PENDING, ejecuta request.cancel() pasando 'group_join_requests.status' a 'CANCELLED' (sin borrar físicamente la fila; deleted sigue en false), e inserta en 'state_movements' la transición PENDING -> CANCELLED con actor_user_id = requesterId. |
| Flujo Alternativo A3<br>(Rol en memberships) | Una vez que el solicitante fue aprobado y figura en 'group_members', desde '/bands/{id}' el líder o el propio músico definen su instrumento (POST /bands/{id}/members/{userId}), creando o actualizando su registro en la tabla 'memberships'. |
| Postcondiciones | • La fila en 'group_join_requests' conserva su trazabilidad de creación y refleja su estado actual (PENDING, APPROVED, REJECTED o CANCELLED).<br>• Si el estado final es REJECTED o CANCELLED, el usuario queda habilitado para enviar una nueva solicitud en el futuro (ya que la validación de duplicados filtra únicamente por status = PENDING).<br>• Cada cambio de estado quedó registrado en forma inmutable en 'state_movements'. |

---

## 8. Análisis de Tablas y Estados del Caso de Uso 01

Para responder con precisión de base de datos cómo interactúa la tabla de solicitudes con el resto del esquema, a continuación se detalla qué ocurre en la misma tabla (group_join_requests) frente a las tablas relacionadas (state_movements, groups, group_members y memberships) en cada transición.

### 8.1 Cuadro Comparativo de Impacto por Transición (CU-01)

| Evento / Transición | En la MISMA tabla (group_join_requests) | En la tabla de Trazabilidad (state_movements) | En las Otras Tablas Relacionadas |
| --- | --- | --- | --- |
| 1. Envío de Solicitud<br>(INICIAL -> PENDING) | Se hace INSERT de una nueva fila:<br>• status = 'PENDING'<br>• deleted = false<br>• group_id, requester_id, message | Se hace INSERT de fila #1:<br>• entity_type = 'GROUP_JOIN_REQUEST'<br>• previous_status = NULL<br>• new_status = 'PENDING'<br>• actor_user_id = requester_id | • users: lectura de requester_id.<br>• groups: bloqueo PESSIMISTIC_WRITE.<br>• group_members: consulta para verificar que aún no sea miembro. |
| 2A. Aprobación<br>(PENDING -> APPROVED) | Se hace UPDATE sobre la misma fila:<br>• status = 'APPROVED'<br>• updated_at = NOW()<br>• deleted = false | Se hace INSERT de fila #2:<br>• entity_type = 'GROUP_JOIN_REQUEST'<br>• previous_status = 'PENDING'<br>• new_status = 'APPROVED'<br>• actor_user_id = líder/delegado | • group_members: se hace INSERT (group_id, requester_id).<br>• memberships: se consulta para verificar permiso can_approve del actor y luego para guardar el instrumento del nuevo miembro. |
| 2B. Rechazo<br>(PENDING -> REJECTED) | Se hace UPDATE sobre la misma fila:<br>• status = 'REJECTED'<br>• updated_at = NOW()<br>• deleted = false | Se hace INSERT de fila #2:<br>• entity_type = 'GROUP_JOIN_REQUEST'<br>• previous_status = 'PENDING'<br>• new_status = 'REJECTED'<br>• actor_user_id = líder/delegado | • group_members: NO se toca (el usuario no ingresa a la banda).<br>• memberships: solo lectura para validar permiso del actor. |
| 2C. Cancelación por el Solicitante<br>(PENDING -> CANCELLED) | Se hace UPDATE sobre la misma fila:<br>• status = 'CANCELLED'<br>• updated_at = NOW()<br>• deleted = false (no se borra) | Se hace INSERT de fila #2:<br>• entity_type = 'GROUP_JOIN_REQUEST'<br>• previous_status = 'PENDING'<br>• new_status = 'CANCELLED'<br>• actor_user_id = requester_id | • group_members: NO se toca.<br>• Al dejar de estar en PENDING, la restricción de solicitud única pendiente queda liberada. |

### 8.2 Ejemplo de Traza SQL Real de una Solicitud Cancelada vs. Aprobada

Supongamos que la música Elena (user_id = 19) solicita unirse a la banda 'Nebula Waves' (group_id = 1, cuyo dueño es Gladito, user_id = 1) y luego decide cancelarla ella misma; mientras que Nicolás (user_id = 20) envía otra solicitud y Gladito la aprueba. Las filas resultantes en state_movements son:

| id | entity_type | entity_id | entity_label | prev_status | new_status | actor / note |
| --- | --- | --- | --- | --- | --- | --- |
| 10 | GROUP_JOIN_REQUEST | 3 | elenavarela -> Nebula Waves | NULL (INICIAL) | PENDING | elenavarela: Solicitud enviada |
| 11 | GROUP_JOIN_REQUEST | 3 | elenavarela -> Nebula Waves | PENDING | CANCELLED | elenavarela: Cancelada por el solicitante |
| 12 | GROUP_JOIN_REQUEST | 4 | nicofontana -> Nebula Waves | NULL (INICIAL) | PENDING | nicofontana: Solicitud enviada |
| 13 | GROUP_JOIN_REQUEST | 4 | nicofontana -> Nebula Waves | PENDING | APPROVED | gladito: Solicitud aprobada e integrante sumado |

---

## 9. Caso de Uso 02: Reserva de Sala de Ensayo y Circuito de Pago

El segundo Caso de Uso fundamental del proyecto (CU-02) abarca la reserva de bloques horarios en salas de ensayo, estudios de grabación o cabinas vocales, integrando la validación atómica de disponibilidad horaria, la generación y cobro de la orden de pago en la tabla payments y el registro de movimientos tanto de la reserva como del pago/reembolso.

### 9.1 Ficha de Especificación Formal — CU-02

| Atributo | Especificación Detallada |
| --- | --- |
| Código y Nombre | CU-02 — Reservar Turno en Sala de Ensayo / Estudio y Procesar su Pago o Reembolso |
| Actores | • Actor Primario: Músico Cliente / Pagador (User que reserva el espacio y abona el turno).<br>• Actor Secundario: Responsable / Dueño de la Sala (studios.owner_id, receptor del pago). |
| Objetivo | Permitir que un músico reserve entre 1 y 8 horas en una sala publicada verificando sin condiciones de carrera que el rango horario [start_at, end_at) esté libre, generar la orden de pago correspondiente con el medio elegido (Mercado Pago, Transferencia Bancaria o Tarjeta), emitir el comprobante y gestionar cancelaciones con reembolso automático. |
| Precondiciones | 1. El Músico Cliente tiene sesión activa y no existe bloqueo en user_blocks entre él y el dueño de la sala.<br>2. La sala existe y está activa (studios.deleted = false).<br>3. El Músico Cliente no es el propietario de la sala (actorId != studio.owner.id).<br>4. La fecha/hora de inicio (start_at) es futura, cae en hora exacta (minutos == 00) y no excede los 180 días. |
| Flujo Principal<br>(Reserva y Pago) | 1. El Músico Cliente accede a '/studios/{id}', elige fecha y hora de inicio, duración (1 a 8 h), selecciona el método de pago (MERCADO_PAGO, BANK_TRANSFER o CARD), mantiene tildada la opción 'Abonar ahora en el acto' y envía el formulario (POST /studios/{id}/book).<br>2. StudioService.book(...) adquiere un bloqueo pesimista (LockModeType.PESSIMISTIC_WRITE) sobre la fila de la sala en 'studios'.<br>3. El servicio calcula end_at = start_at.plusHours(hours) y ejecuta la consulta de intersección sobre 'studio_bookings' para esa sala con status = 'CONFIRMED' y condición (b.startAt < :end AND b.endAt > :start).<br>4. Al comprobar que overlapping == 0, calcula total_price = studio.hourlyRate * hours e inserta la fila en 'studio_bookings' con status = 'CONFIRMED'.<br>5. StateMovementService.record(...) registra en 'state_movements' el alta de la reserva: entity_type = 'STUDIO_BOOKING', previous_status = NULL, new_status = 'CONFIRMED'.<br>6. PaymentService.createOrder(...) inserta en la tabla 'payments' la orden con concept_type = 'STUDIO_BOOKING', reference_id = booking.id, amount = total_price, payment_method elegido, status = 'PENDING_PAYMENT', payer = cliente y recipient = dueño de la sala, registrando en 'state_movements' (entity_type = 'PAYMENT') la transición NULL -> PENDING_PAYMENT.<br>7. Como payNow == true, dentro de la misma transacción se ejecuta PaymentService.completePayment(...), que genera el código único de comprobante (HRM-MP-..., HRM-TR-... o HRM-TC-...), actualiza 'payments.status' a 'PAID', asigna paid_at = NOW() y registra en 'state_movements' la transición PENDING_PAYMENT -> PAID. |
| Flujo Alternativo A1<br>(Pago Diferido) | En el paso 1, el músico destilda 'Abonar ahora en el acto'. La reserva queda en 'CONFIRMED' y la orden en 'payments' permanece en 'PENDING_PAYMENT'. Más tarde, desde '/studios/{id}' o '/payments', el músico selecciona el medio de pago y presiona 'Confirmar y abonar' (POST /payments/{id}/pay), pasando el pago de PENDING_PAYMENT a PAID con emisión de comprobante. |
| Flujo Alternativo A2<br>(Cancelación y Reembolso) | Antes de que transcurra el horario reservado, el músico cliente o el dueño de la sala presionan 'Cancelar reserva' (POST /studios/bookings/{bookingId}/cancel). StudioService.cancel(...) actualiza 'studio_bookings.status' a 'CANCELLED' (liberando el bloque horario para otros músicos), registra CONFIRMED -> CANCELLED en 'state_movements' e invoca a PaymentService.refundOrCancelForReference(...). Si el pago estaba en estado 'PAID', pasa automáticamente a 'REFUNDED' (o a 'CANCELLED' si aún estaba en 'PENDING_PAYMENT') y registra la transición en 'state_movements'. |
| Flujo Alternativo A3<br>(Baja de Sala) | Si el propietario elimina la sala (POST /studios/{id}/delete), todas sus reservas futuras confirmadas se cancelan en cascada y sus respectivos pagos se reembolsan o anulan automáticamente. |
| Postcondiciones | • No existen dos reservas con status = 'CONFIRMED' cuyos intervalos [start_at, end_at) se superpongan en la misma sala.<br>• Toda reserva tiene su correspondiente registro financiero en 'payments' y su trazabilidad completa en 'state_movements'. |

### 9.2 Impacto en Tablas y Estados — Caso de Uso 02

| Evento | Tabla studio_bookings | Tabla payments | Tabla state_movements |
| --- | --- | --- | --- |
| 1. Reserva con pago inmediato | INSERT fila con status = 'CONFIRMED', start_at, end_at, total_price. | INSERT en PENDING_PAYMENT y UPDATE inmediato a status = 'PAID' con receipt_code = 'HRM-...'. | • STUDIO_BOOKING: NULL -> CONFIRMED<br>• PAYMENT: NULL -> PENDING_PAYMENT<br>• PAYMENT: PENDING_PAYMENT -> PAID |
| 2. Reserva con pago diferido | INSERT fila con status = 'CONFIRMED'. | INSERT fila con status = 'PENDING_PAYMENT' (receipt_code = NULL). | • STUDIO_BOOKING: NULL -> CONFIRMED<br>• PAYMENT: NULL -> PENDING_PAYMENT |
| 3. Abono posterior desde /payments | Sin cambios (sigue CONFIRMED). | UPDATE misma fila: status = 'PAID', paid_at = NOW(), receipt_code = 'HRM-...'. | • PAYMENT: PENDING_PAYMENT -> PAID (con número de comprobante en note). |
| 4. Cancelación de reserva ya pagada | UPDATE misma fila: status pasa de 'CONFIRMED' a 'CANCELLED'. | UPDATE misma fila: status pasa de 'PAID' a 'REFUNDED'. | • STUDIO_BOOKING: CONFIRMED -> CANCELLED<br>• PAYMENT: PAID -> REFUNDED |

---

## 10. Seguridad, Concurrencia y Prevención de Condiciones de Carrera

Harmony fue diseñado aplicando prácticas de programación defensiva, aislamiento transaccional y seguridad web para evitar vulnerabilidades frecuentes (OWASP Top 10) e inconsistencias por acceso concurrente.

### 10.1 Mecanismos de Seguridad Implementados

| Vector / Área de Seguridad | Implementación en Harmony |
| --- | --- |
| Hashing de Contraseñas y Política de Claves | Uso de BCryptPasswordEncoder. Al registrarse o cambiar contraseña, AuthService exige al menos 8 caracteres, combinación de mayúsculas, minúsculas y dígitos, y prohíbe reutilizar la clave anterior. |
| Protección contra CSRF y Fijación de Sesión | Todos los formularios POST incluyen el token CSRF de Spring Security. Al iniciar sesión se aplica sessionFixation().changeSessionId() y cookies HttpOnly con SameSite=Lax. |
| Revocación Inmediata de Cuentas (ActiveAccountFilter) | Filtro personalizado ejecutado después de AnonymousAuthenticationFilter que verifica en cada request que el usuario en sesión exista, no esté eliminado (deleted = false) y siga habilitado (enabled = true). |
| Prevención de XSS y Validación de Enlaces | Las plantillas Thymeleaf utilizan th:text (que escapa HTML automáticamente). En PostService y AvatarStorageService se valida con java.net.URI que los enlaces externos utilicen esquemas permitidos sin credenciales embebidas. |
| Seguridad en Subida de Archivos | AvatarStorageService limita el tamaño a 5 MB, valida lista blanca de extensiones (png, jpg, jpeg, webp, gif, svg), genera nombres UUID aleatorios y verifica que la ruta normalizada permanezca dentro de ./data/avatars. |

### 10.2 Control de Concurrencia y Bloqueos Pesimistas

En escenarios multiusuario reales, dos personas pueden intentar simultáneamente reservar el mismo horario en una sala, aprobar la misma solicitud de banda o votar/calificar al mismo tiempo. Harmony previene estas condiciones de carrera combinando tres niveles de protección:
1) Bloqueo Pesimista en Escritura (LockModeType.PESSIMISTIC_WRITE): antes de validar cupos, horarios o estados previos, los servicios bloquean la fila maestra en la base de datos (por ejemplo em.lock(studio, PESSIMISTIC_WRITE) en StudioService, em.lock(group, PESSIMISTIC_WRITE) en GroupService y GroupJoinRequestService, em.lock(user, PESSIMISTIC_WRITE) en CommunityService y em.lock(payment, PESSIMISTIC_WRITE) en PaymentService).
2) Versionado Optimista (@Version): la entidad Group incluye el campo @Version private long version para detectar modificaciones concurrentes.
3) Restricciones UNIQUE Compuestas en Base de Datos: las tablas memberships (group_id, user_id), profile_ratings (profile_id, reviewer_id), post_reactions (post_id, user_id), user_blocks (owner_id, target_id) y job_applications (job_id, applicant_id) poseen índices UNIQUE físicos verificados por el test de estrés ConcurrentVoteTests.

---

## 11. Validación Automatizada: JUnit 5, MockMvc y Suite Playwright E2E

La calidad del proyecto está respaldada por dos suites de pruebas automatizadas complementarias: una suite de integración transaccional sobre Spring Boot (BetaFlowTests y ConcurrentVoteTests) y una suite de 8 ejercicios de interfaz de usuario de extremo a extremo (E2E) desarrollada con Playwright para Java y JUnit 5 (paquete com.tests).

### 11.1 Suite de Pruebas de Integración y Concurrencia (Spring Boot Test)

- Persistencia separada entre Person, User y Profile y rechazo de contraseñas débiles en registro y cambio de clave.
- Calificaciones de 1 a 5 estrellas sin duplicados, actualización de puntaje, retiro de calificación y rechazo de autocalificación.
- Conmutación y retiro de reacciones (+1 / -1) bajo ejecución concurrente multihilo (ConcurrentVoteTests con CountDownLatch y ExecutorService).
- Privacidad de publicaciones privadas/eliminadas y bloqueo bidireccional en feed, perfiles, comentarios y calificaciones.
- Flujo completo de bandas: asignación de instrumentos, delegación y revocación de permisos (can_publish, can_approve), aprobación de solicitudes por miembro delegado y transferencia de propiedad.
- Internacionalización ES/EN verificando ausencia de textos en español hardcodeados cuando el usuario elige inglés.
- Subida de foto de perfil de usuario y de banda (MultipartFile y URL), control de permisos del dueño de banda y descarga desde /avatars/{filename}.
- Trazabilidad completa en state_movements para solicitudes (PENDING, APPROVED, CANCELLED), reservas de salas (CONFIRMED, CANCELLED), avisos (OPEN, CLOSED), postulaciones (PENDING, REJECTED) y pagos (PENDING_PAYMENT, PAID, REFUNDED).

### 11.2 Detalle de los 8 Ejercicios de Playwright con Java (com.tests)

| Clase de Test | Objetivo Técnico Evaluado | Flujo Ejecutado sobre Harmony y Aserciones Aplicadas |
| --- | --- | --- |
| BaseTest.java | Ciclo de vida JUnit 5 (@BeforeAll, @BeforeEach, @AfterEach) | Verifica si Harmony responde en http://127.0.0.1:8080 (y si no está corriendo, levanta automáticamente harmony.jar en segundo plano). Inicializa Playwright, lanza Chromium con setHeadless(false) y setSlowMo(1500) para visualización clara en video, crea BrowserContext y Page aislados por test y cierra todo limpiamente en tearDown(). |
| Ejercicio01.java | Navegación inicial y lectura del título | Navega a la portada principal de Harmony (BASE_URL + '/') con page.navigate(...) e imprime en consola el título real del documento mediante System.out.println('Título de la página: ' + page.title()). |
| Ejercicio02.java | Aserciones web-first de título exacto y Regex | Incluye dos métodos @Test independientes: el primero verifica assertThat(page).hasTitle('Harmony · Red Colaborativa de Músicos y Bandas') y el segundo valida coincidencia parcial con Pattern.compile('Harmony.*'). |
| Ejercicio03.java | Localización semántica por rol (AriaRole.LINK) y click() | Parte desde la portada ('/'), localiza el enlace 'Iniciar sesión →' usando page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName('Iniciar sesión →')), ejecuta click() y verifica con assertThat(page).hasURL(BASE_URL + '/login'). |
| Ejercicio04.java | Interacción con formularios (getByLabel, fill, inputValue) | Navega a '/login', localiza los campos 'Email' y 'Contraseña' mediante page.getByLabel(...) sin usar selectores CSS (#id ni .clase), los completa con fill(), recupera los valores con inputValue(), los imprime en consola, valida con assertEquals y envía el formulario. |
| Ejercicio05.java | Flujo de Login Exitoso y Login Fallido | Dos métodos @Test independientes: loginExitoso() ingresa con gladito@example.test / Harmony123 y verifica redirección a '/home' con hasURL; loginFallido() ingresa credenciales inválidas y verifica que permanece en '/login?error'. |
| Ejercicio06.java | Verificación de visibilidad y texto en pantalla | Provoca el estado de credenciales inválidas en '/login', localiza la alerta de error y verifica primero su visibilidad con assertThat(alerta).isVisible() y luego su contenido exacto con assertThat(alerta).containsText('Email o contraseña incorrectos.'). |
| Ejercicio07.java | Capturas de pantalla completa y de elemento individual | Inicia sesión, crea la carpeta screenshots/ si no existe, guarda capturas de página completa (setFullPage(true)) de login, home y salas, y captura un elemento específico (tarjeta hero) usando locator.screenshot(...). |
| Ejercicio08.java | Flujo E2E multipágina con historial (goBack / goForward) | Inicia sesión y recorre 5 pantallas reales (/home -> /people -> /groups -> /studios -> /people/1), verificando en cada paso tanto hasURL como un elemento visible propio de la página, y ejercita page.goBack() y page.goForward(). |

### 11.3 Comandos de Ejecución y Conclusiones

Para ejecutar la suite completa de pruebas de integración de Spring Boot y empaquetar el ejecutable:
  .\gradlew.bat test --tests "com.example.demo.*" bootJar

Para ejecutar los 8 ejercicios de Playwright abriendo el navegador Chromium en pantalla para demostración o video:
  .\gradlew.bat test --tests "com.tests.Ejercicio*" --rerun-tasks --info

Conclusión: Harmony integra en un único proyecto académico y profesional el diseño de bases de datos relacionales con migraciones versionadas, seguridad web, concurrencia pesimista, circuito de pagos con comprobantes, auditoría transversal mediante tabla de movimientos (state_movements) y pruebas automatizadas E2E alineadas al 100% con los estándares de la cátedra.

---
