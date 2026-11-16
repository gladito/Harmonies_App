# Harmony — Guía de Pasos Siguientes y Paso a Producción

Esta guía detalla cómo llevar Harmony desde la versión actual (Beta local portable) hacia producción, cubrir los requisitos faltantes y las alternativas para compartirla online hoy mismo sin pagar servidores ni instalar nada.

---

## 1. Cómo usar y mostrar el paquete portable actual (Beta hoy)

El archivo `HARMONY_BETA_HOY.zip` contiene todo lo necesario para correr la app de forma 100% autónoma.

### Requisito único:
- Tener instalado **Java 17 o superior** (verificar en consola con `java -version`).

### Pasos para ejecutar:
1. Descomprimir `HARMONY_BETA_HOY.zip` en cualquier carpeta.
2. Hacer doble clic sobre `INICIAR.bat`.
3. Se abrirá una ventana de consola. Mantenerla abierta.
4. Abrir en el navegador: [http://127.0.0.1:8080](http://127.0.0.1:8080)
5. Para cerrar la app: presionar `Ctrl+C` en la consola o cerrarla.

### Datos y cuentas incluidas:
- **Gladito:** `gladito@example.test` / `Harmony123` (Creador de *Nebula Waves*, toca sintetizador).
- **Pepito:** `pepito@example.test` / `Harmony123` (Integrante de *Nebula Waves*, toca bajo, con permisos para publicar).

### Flujo de prueba recomendado:
- Ingresar como Gladito: ver el feed con reproductor de audio integrado.
- Ir a *Mis bandas* → *Nebula Waves*: ver la lista de integrantes e instrumentos.
- Modificar el instrumento o permisos de Pepito.
- Crear una nueva publicación propia o en nombre de la banda.
- Registrar un usuario nuevo desde la pantalla de login.
- Con el nuevo usuario: buscar la banda, solicitar unirse, y aprobarlo con la cuenta de Gladito asignándole instrumento.
- Calificar perfiles (1 a 5 estrellas) y reaccionar a publicaciones (likes).

> **Reinicio de fábrica:** La app guarda los datos en la carpeta local `data/`. Si querés reiniciar la base de datos a su estado original, cerrá la app, borrá la carpeta `data/` y volvé a iniciar.

---

## 2. Compartir online hoy mismo sin instalar nada ni pagar un VPS ($0)

Si tu amigo u otra persona quiere probar la app desde su computadora o celular **sin descargar el ZIP ni instalar Java**:

### Opción A: Cloudflare Tunnel ($0, seguro y sin abrir puertos en el router)
1. Descargar el ejecutable liviano `cloudflared` para Windows desde [GitHub Releases](https://github.com/cloudflare/cloudflared/releases) (`cloudflared-windows-amd64.exe`).
2. Renombrarlo a `cloudflared.exe`.
3. Con Harmony corriendo en tu PC (`INICIAR.bat`), abrir una consola en la carpeta de `cloudflared.exe` y ejecutar:
   ```powershell
   .\cloudflared.exe tunnel --url http://127.0.0.1:8080
   ```
4. Cloudflare generará una URL pública segura con HTTPS (ejemplo: `https://palabra-aleatoria-123.trycloudflare.com`).
5. Pasale ese enlace a tu amigo y puede entrar de inmediato desde cualquier navegador o teléfono.
6. Al cerrar la consola de cloudflared, el túnel se cierra de inmediato.

### Opción B: ngrok
1. Descargar ngrok desde [ngrok.com](https://ngrok.com).
2. Ejecutar: `ngrok http 8080`.
3. Copiar la URL pública HTTPS provista.

---

## 3. Paso a Base de Datos Real (MySQL 8.4)

La versión portable utiliza la base de datos embebida H2 (en modo archivo) para que no requiera instalar MySQL. El código de Harmony ya incluye el soporte para MySQL y el script Flyway listo.

### Cómo activar MySQL:
1. Instalar MySQL 8.0+ o levantarlo con Docker.
2. Crear una base de datos vacía:
   ```sql
   CREATE DATABASE harmony_beta CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. Configurar las variables de entorno:
   ```powershell
   $env:SPRING_PROFILES_ACTIVE = "mysql"
   $env:DB_URL = "jdbc:mysql://localhost:3306/harmony_beta?serverTimezone=UTC"
   $env:DB_USER = "tu_usuario"
   $env:DB_PASSWORD = "tu_password"
   ```
4. Ejecutar el JAR:
   ```powershell
   java -jar harmony.jar --spring.profiles.active=mysql
   ```

### Migraciones Flyway automáticas:
- Al arrancar con el perfil `mysql`, Flyway ejecuta automáticamente `V1__beta_schema.sql`.
- Crea las 16 tablas relacionales con claves foráneas, índices de rendimiento y restricciones de unicidad.

---

## 4. Despliegue en la Nube Gratuito (PaaS — Cero Mantenimiento)

Para tener la app online 24/7 sin administrar servidores Linux:

### Render.com / Railway.app:
1. Subir el repositorio a GitHub.
2. Crear una cuenta en [Render](https://render.com).
3. Crear una base de datos (PostgreSQL o MySQL con Supabase/Aiven en tier gratuito).
4. Crear un *Web Service* en Render vinculado al repositorio de GitHub:
   - **Build Command:** `./gradlew bootJar`
   - **Start Command:** `java -jar build/libs/Harmony-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql`
5. En *Environment Variables*, definir `DB_URL`, `DB_USER` y `DB_PASSWORD`.
6. Render compila, levanta y otorga un subdominio HTTPS gratuito (ej: `https://harmony-app.onrender.com`).

---

## 5. Despliegue en Servidor Propio (VPS con Docker + Caddy SSL)

Si eligen contratar un VPS (como Hetzner Cloud por ~4 a 5 USD/mes):

### `Dockerfile`:
```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/Harmony-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `docker-compose.yml`:
```yaml
services:
  app:
    build: .
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=mysql
      - DB_URL=jdbc:mysql://db:3306/harmony_beta?serverTimezone=UTC
      - DB_USER=harmony
      - DB_PASSWORD=${DB_PASSWORD}
    depends_on:
      - db

  db:
    image: mysql:8.4
    restart: unless-stopped
    environment:
      - MYSQL_DATABASE=harmony_beta
      - MYSQL_USER=harmony
      - MYSQL_PASSWORD=${DB_PASSWORD}
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASSWORD}
    volumes:
      - mysql_data:/var/lib/mysql

  caddy:
    image: caddy:alpine
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - caddy_data:/data
    depends_on:
      - app

volumes:
  mysql_data:
  caddy_data:
```

### `Caddyfile` (SSL automático con Let's Encrypt):
```caddy
tu-dominio.com {
    reverse_proxy app:8080
}
```

---

## 6. Seguridad Requerida para Producción

1. **Cookies de sesión sobre HTTPS:**
   ```properties
   server.servlet.session.cookie.secure=true
   server.servlet.session.cookie.http-only=true
   server.servlet.session.cookie.same-site=strict
   ```
2. **Protección anti-fuerza bruta en `/login`:**
   - Filtro con Caffeine Cache o Bucket4j: bloquear temporalmente por 15 minutos tras 5 intentos fallidos consecutivos.

---

## 7. Servicio de Correo y Recuperación de Contraseña ($0)

- **Proveedor gratuito:** [Resend](https://resend.com) (3.000 emails/mes gratis) o Brevo (300 emails/día).
- **Flujo:**
  1. Tabla `password_reset_tokens(id, token, user_id, expiry_date)`.
  2. Endpoint `/forgot-password` (formulario de email).
  3. Token firmado con validez de 20 minutos enviado por email.
  4. Endpoint `/reset-password?token=...` para redefinir contraseña.

---

## 8. Panel de Moderación y Administración

1. Asignar rol `ROLE_ADMIN` y proteger rutas `/admin/**`.
2. Buscador de usuarios y botón *Suspender cuenta* (`user.enabled = false`): expulsión inmediata de sesión gracias a `ActiveAccountFilter`.
3. Listado de posts/comentarios para dar de baja lógica (`deleted = true`).

---

## 9. Política de Copias de Seguridad (Backups RPO 4h) y Monitoreo 24/7

1. **RPO 4 Horas:**
   - Cron de respaldo cada 4 horas:
     ```bash
     mysqldump -u harmony -p${DB_PASSWORD} harmony_beta | gzip > /backups/harmony_$(date +%Y%m%d_%H%M).sql.gz
     ```
   - Retención de 7 días.
2. **Monitoreo con UptimeRobot ($0):**
   - Monitorear `/actuator/health` cada 5 minutos.
   - Alertas automáticas a Telegram / Email ante cualquier anomalía.
   - RTO garantizado menor a 6 horas.

---

## 10. Tabla Comparativa de Modalidades

| Modalidad | Costo Mensual | Mantenimiento | Ideal Para... |
|---|:---:|:---:|---|
| **1. ZIP Portable (H2)** | **$0** | Cero | Enviar al amigo para probar hoy con 1 clic |
| **2. Túnel Cloudflare (PC local)** | **$0** | Solo tener la PC prendida | Mostrar la app en celular/remoto sin instalar nada |
| **3. PaaS Nube (Render / Railway)** | **$0** | Cero servidores Linux | Beta pública permanente sin costo |
| **4. VPS Propio (Hetzner + Docker)** | **~$4 - $5 USD** | Actualizaciones del SO | Producción final con dominio propio |
