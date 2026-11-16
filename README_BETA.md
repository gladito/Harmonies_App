# Harmony — Beta local para la entrega de hoy

> Verificación actualizada el 13/09/2026: 25 pruebas pasaron y se comprobaron los recorridos nuevos en el navegador. Consultá `VERIFICACION.md` para los resultados y los pendientes. MySQL todavía no está verificado en servidor real.

## Abrir la aplicación

Necesitás Java 17 o posterior. En la PC de desarrollo se ejecutó con Java 21.

**Paquete listo para enviar:** descomprimí `HARMONY_BETA_HOY.zip`, ejecutá `INICIAR.bat` y abrí [Harmony](http://127.0.0.1:8080). Conservá abierta la consola mientras usás la aplicación. Para detenerla, presioná Ctrl+C en esa consola.

El paquete incluye el ejecutable, estas instrucciones, la guía de producción en texto (`GUIA_PASOS_PRODUCCION.txt`) y el código fuente. No requiere Docker, MySQL ni descargar dependencias para ejecutar el JAR. La primera compilación del código fuente sí necesita descargar dependencias.

Desde el código fuente, ejecutá `INICIAR_BETA.bat`, o:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

El modo `demo` guarda los datos en `data/harmony-demo.mv.db`. Los cambios se conservan al reiniciar. Los datos ficticios se cargan únicamente cuando la base está vacía, por lo que no se sobrescriben las cuentas ni los permisos modificados.

## Cuentas de demostración

| Email | Contraseña | Situación inicial |
|---|---|---|
| gladito@example.test | Harmony123 | Creador de Nebula Waves; sintetizador |
| pepito@example.test | Harmony123 | Integrante de Nebula Waves; bajo y permiso para publicar |

Son cuentas ficticias para la beta local. También podés registrar cuentas desde la pantalla de acceso. La contraseña debe tener entre 8 y 72 caracteres, una mayúscula y un número.

## Recorrido sugerido para mostrarla

1. Ingresar como Gladito: ver el feed y reproducir el audio sintético incluido.
2. Abrir Mi perfil y guardar una biografía.
3. Abrir Músicos y visitar el perfil de Pepito. Calificarlo, cambiar el voto y retirarlo.
4. Dar me gusta/no me gusta en el feed y cambiar o retirar la reacción.
5. Abrir Mis bandas → Nebula Waves. Ver integrantes e instrumentos.
6. Modificar el instrumento o los permisos de Pepito; ingresar con su cuenta y comprobar qué acciones tiene habilitadas.
7. Registrar una tercera cuenta, explorar bandas y solicitar ingreso. Volver al dueño o a un integrante con permiso para aprobarla.
8. Transferir la banda a un integrante. El dueño anterior deja de tener los permisos delegados; el nuevo dueño puede volver a asignarlos.
9. Bloquear a otro usuario y entrar con esa cuenta: el perfil y contenido del bloqueador quedan inaccesibles dentro de Harmony. Desbloquear desde Músicos.
10. Cerrar y volver a abrir la aplicación para comprobar que los cambios se conservaron.

Las publicaciones nuevas aceptan enlaces HTTPS de audio. Para reproducirlos en el navegador deben apuntar a un recurso compatible; una página de Spotify/YouTube no equivale a un archivo de audio. El sonido inicial se genera localmente y funciona sin un proveedor externo.

## Etapas implementadas

| Etapa | Resultado |
|---|---|
| Base y acceso | Persona y Usuario en tablas separadas, perfil creado al registrar, autenticación con Spring Security, protección CSRF y control de cuentas activas. |
| Perfiles y bandas | Perfil público y biografía; bandas sin límite fijo de cuatro personas; integrantes con instrumento, solicitudes y aprobaciones. |
| Permisos y privacidad | Delegación de publicación/aprobación, transferencia a un integrante existente, publicaciones como banda, bloqueo unilateral y protección de posts privados/eliminados. |
| Interacciones | Ratings 1–5 sin autocalificación, promedio/cantidad, likes/dislikes, cambio/retiro y restricciones de unicidad. |
| Interfaz y entrega | Feed y perfil con la estética negra/dorada de las maquetas, perfil de banda, formularios existentes conectados, audio y paquete local. |

## Verificación

Suite automatizada: 25 pruebas con H2, incluyendo registros, separación de entidades, permisos, bloqueos, vistas renderizadas, CSRF, votos simultáneos, invalidación de sesión y cobertura i18n sin centinelas. Ejecución:

```powershell
.\gradlew.bat test
```

Además se comprobó el ejecutable mediante solicitudes HTTP con dos cuentas: 12 pantallas, biografía, rating, reacción, bloqueo, transferencia y reproducción del archivo local. Se reinició el ejecutable y se verificó que la biografía y la transferencia de banda se conservaran, sin recrear las cuentas.

## Modelo y decisiones del prompt original

- `persons` contiene datos personales; `users.person_id` es obligatorio y único. `profiles.user_id` también es único. Las vistas públicas muestran username y biografía, no los datos civiles ni credenciales.
- `memberships` guarda instrumento y permisos por usuario/banda. La relación existente `group_members` determina la pertenencia. Los permisos solo se aplican mientras la persona sea integrante; se eliminan al abandonar la banda.
- `profile_ratings` tiene una fila por perfil/calificador y rango 1–5. `post_reactions` tiene una fila por post/usuario y valor 1 o −1. Un valor 0 en el formulario retira la fila, no se almacena como voto.
- Las operaciones de voto bloquean transaccionalmente la cuenta autora para serializar envíos simultáneos; las restricciones únicas refuerzan la integridad. El resumen de reacciones del feed se obtiene por lote.
- `user_blocks` expresa quién bloquea a quién. El bloqueo se aplica al contenido del autor, también cuando publicó como banda. No oculta publicaciones de otros integrantes. Los enlaces externos siguen sujetos a los permisos del proveedor de audio.
- `BaseEntity` no crea tabla; `contents` sí es la base persistente de `Post` y se conserva. `tags/post_tags` permiten agregar hasta cinco etiquetas por publicación, editarlas y filtrar el feed por etiqueta. El feed está paginado y aplica la privacidad antes de limitar los resultados. Los permisos de bandas ya tienen uso funcional; los roles globales de administración quedan para una entrega posterior.

## Integración de las maquetas

Se mantiene Thymeleaf porque el proyecto ya usa controladores MVC, formularios y sesiones. Las vistas dinámicas están en `src/main/resources/templates`; el estilo compartido, en `src/main/resources/static/css/harmony.css`. La navegación compartida vive en `fragments.html`.

`HomeController` entrega datos para `home.html`, adaptado desde el feed. `CommunityController` entrega datos para `public-profile.html` y `band-profile.html`. `/profile` redirige al perfil propio y `/account` conserva las opciones de username y contraseña.

Ejemplo del perfil dinámico:

```html
<h1 th:text="${person.username}"></h1>
<p th:text="${person.bio}"></p>
<a th:href="@{/account}">Editar cuenta</a>
<link rel="stylesheet" th:href="@{/css/harmony.css}">
```

Los formularios usan `th:action`, lo que permite integrar el token CSRF. Los vínculos apuntan a rutas de la aplicación, no a archivos `.html`.

## MySQL y límites de esta entrega

**Esta beta fue verificada con H2 embebido. MySQL no estaba disponible para verificarlo en esta PC.** Se mantiene el controlador MySQL y un perfil alternativo para una base nueva `harmony_beta`:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/harmony_beta?serverTimezone=UTC'
$env:DB_USER = 'tu_usuario'
$env:DB_PASSWORD = 'tu_clave'
.\gradlew.bat bootRun --args="--spring.profiles.active=mysql"
```

El perfil MySQL no carga cuentas de demostración: registrá usuarios desde la interfaz. El perfil `demo` utiliza generación/actualización de esquema de Hibernate sobre H2. El perfil `mysql` incorpora la migración inicial Flyway `V1__beta_schema.sql` (con tipos VARCHAR alineados a las entidades JPA) y luego valida el esquema; todavía requiere verificación con un servidor MySQL 8.4 real. No migra datos del esquema anterior: usá una base nueva. La configuración por defecto sin perfil explícito exige las variables de entorno de base de datos sin defaults silenciosos.
Los scripts de prueba de Oracle/APEX fueron retirados del repositorio para evitar ejecuciones accidentales.

Quedan fuera de esta beta: publicación abierta a Internet (requiere HTTPS y cookie de sesión con flag seguro), correo de recuperación/verificación, límite de intentos de acceso (anti fuerza bruta), moderación global desde UI, respaldos automáticos, operación con alertas, matchmaking, eventos, empleos, reservas, pagos y subida directa de archivos.

La interfaz web (ES/EN) está completamente internacionalizada mediante Thymeleaf y bundles de mensajes sincronizados, cubriendo tanto navegación principal como formularios y pantallas secundarias.

La aplicación escucha únicamente en la PC local (127.0.0.1:8080). No se publicaron servicios ni se modificaron bases existentes.
