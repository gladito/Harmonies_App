# Harmony — Verificación y alcance pendiente

Fecha: 13/09/2026. Rama local: `beta-entrega-hoy`. Verificación realizada sin Docker.

## Resultado comprobado

- Compilación y generación del ejecutable completadas.
- **25 pruebas automáticas, sin fallos** (exit 0), ejecutadas con H2 en modo de compatibilidad MySQL.
- Cobertura integral: separación Persona/Usuario/Perfil, registro, permisos y transferencia de bandas, instrumentos, aprobación de integrantes, privacidad, bloqueos, ratings, reacciones, CSRF, votos simultáneos.
- Nuevas pruebas añadidas:
  - Guard de cobertura i18n que valida la ausencia de centinelas en español ("Guardar cambios", "Crear banda", "Integrantes:", "Duración:", "Eliminar publicación", "Sin descripción") al navegar en inglés.
  - Flujo de cierre de sesión (`POST /logout`), invalidación de sesión y redirección a login.
  - Rechazo de credenciales inválidas con redirección a `/login?error`.
  - Validación de política de contraseñas al cambiar contraseña vía HTTP (`POST /profile/password`).
  - Renderizado exitoso de formularios de edición (`/posts/{id}/edit` y `/groups/{id}/edit`).
- Navegación web y templates completamente internacionalizados (bundles `messages.properties` y `messages_en.properties` con 152 claves sincronizadas 1 a 1).
- Eliminación de scripts legacy huérfanos de Oracle/APEX (`ords_auto_rest.sql`, `ords_services.sql`, `schema_migration.sql`, `security_setup.sql`, `seed_data_mysql.sql`).

## Correcciones técnicas aplicadas

1. **Alineación Flyway MySQL y JPA**: se eliminó la trampa de tipos `enum(...)` en `V1__beta_schema.sql` (`visibility` -> `varchar(255)`, `status` -> `varchar(15)`), evitando fallas con Hibernate `ddl-auto=validate`. Los 12 campos `bit` se conservan (mapeo estándar de boolean en MySQL).
2. **Eliminación de código muerto en auth**: se removieron los handlers redundantes de login y logout en `AuthController` (interceptados previamente por Spring Security) y el método no invocado `validatePassword` en `User`.
3. **Fuga de enumeración de usuarios prevenida**: unificado el mensaje de credenciales no válidas en `AuthService`.
4. **Credenciales sin defaults inseguros**: eliminación de contraseñas hardcodeadas por defecto en `application.properties` y `application-mysql.properties`.

## Estado frente a lo validado por tu amigo

| Parte del pedido | Estado |
|---|---|
| Separar Persona y Usuario, con perfil asociado | Implementado y probado con H2 |
| Crear perfiles, bandas y publicaciones | Implementado y probado |
| Mostrar integrantes con su instrumento, sin máximo fijo de cuatro | Implementado y probado |
| Delegar publicación y aprobación; transferir propiedad | Implementado y probado |
| Bloquear acceso al perfil y contenido del autor | Implementado y probado dentro de Harmony |
| Ratings, promedio/cantidad y reacciones editables/retirables | Implementado y probado, incluida concurrencia de votos |
| Maquetas de feed/perfil y pantallas necesarias de bandas | Integradas; revisión funcional y visual local |
| Etiquetas y paginación | Implementadas y probadas |
| Español/inglés completo | Implementado y probado: bundles sincronizados y templates sin español hardcodeado |
| MySQL y migración Flyway | Script V1 corregido y alineado con JPA; pendiente de ejecución en un servidor MySQL 8.4 real |
| Recuperación/verificación por correo y límites de intentos de acceso | Pendientes (fuera de alcance de la beta local) |
| Reportes, moderación y suspensión desde administración | Pendientes; existe control de cuenta activa en el backend (`ActiveAccountFilter`), no el panel de UI |
| Respaldos/restauración operativa, alertas y despliegue automatizado | Pendientes |
| Prueba de carga para 100 registrados/10 simultáneos | Pendiente; la prueba de votos simultáneos no equivale a una prueba de carga |

La disponibilidad de seis horas indicada en las respuestas no fue ensayada ni se ofrece como garantía. La pérdida tolerable figura como “4” sin unidad: debe precisarse antes de definir respaldos.

## Procedimiento para verificación real contra MySQL 8.4

Cuando se disponga de un servidor MySQL 8.4:
1. Crear una base de datos vacía:
   ```sql
   CREATE DATABASE harmony_beta CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Establecer las variables de entorno de conexión requeridas:
   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/harmony_beta?serverTimezone=UTC"
   $env:DB_USER = "harmony_user"
   $env:DB_PASSWORD = "harmony_secure_password"
   ```
3. Iniciar la aplicación con el perfil mysql:
   ```powershell
   .\gradlew.bat bootRun --args="--spring.profiles.active=mysql"
   ```
4. Confirmar que Flyway ejecute la migración `V1__beta_schema.sql` y que Hibernate valide el esquema sin lanzar `SchemaManagementException`.

## Postergado en el alcance

Matchmaking por instrumento y subida directa de archivos de audio quedaron para más adelante. Eventos, empleos, estudios, reservas y pagos tampoco forman parte de la beta entregada.

## Límites de la verificación

- La aplicación funciona como beta local bajo el perfil `demo` con base H2 persistente en `./data/harmony-demo.mv.db`.
- Los recursos de audio externos dependen del enlace provisto: se comprobó el flujo de publicación con URLs HTTPS.
- HTTPS, cookie segura (`secure=true`), HSTS y políticas anti-fuerza bruta deberán implementarse antes de exponer la aplicación a internet.

**Conclusión:** El alcance completo de la beta local está resguardado, probado y listo para revisión. Los pendientes operativos y de infraestructura para una salida a internet pública están claramente registrados.

