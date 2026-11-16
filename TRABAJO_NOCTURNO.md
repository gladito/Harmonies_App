# Continuación después de la entrega de la beta

> Actualización: esta continuación ya fue compilada y comprobada con 20 pruebas y recorridos de navegador. Se corrigió el retorno a español. `VERIFICACION.md` contiene el estado actual; lo que sigue registra el estado histórico previo a la revisión. MySQL permanece pendiente.

Fecha: 13/09/2026. Pedido del usuario: seguir implementando y dejar toda verificación para mañana.

## Cambios de esta pasada

- Preparación de migración inicial Flyway para MySQL 8.4 sobre una base nueva, con relaciones, restricciones e índices. El perfil MySQL pasa a validar el esquema después de migrarlo. No se conectó a MySQL ni se ejecutó SQL.
- Etiquetas en publicaciones personales y de bandas, normalizadas y limitadas a cinco por post. Edición y filtro por etiqueta en el feed.
- Feed paginado desde la consulta, aplicando privacidad y bloqueos antes del límite de resultados.
- Selector español/inglés persistido en cookie y traducciones de la navegación y las pantallas principales. El contenido escrito por usuarios conserva su idioma original. La traducción de todos los mensajes de error y formularios secundarios sigue pendiente.

## Estado

Estos cambios de código **no fueron compilados, probados ni verificados**, por instrucción del usuario. Las 15 pruebas y el recorrido HTTP informados antes corresponden al corte anterior, no a esta continuación.

El ZIP HARMONY_BETA_HOY.zip y la aplicación local que ya estaba corriendo siguen siendo el corte anterior. No se reconstruyeron ni se reinició la aplicación durante esta pasada. El código nuevo está en la rama local beta-entrega-hoy.

## Para retomar mañana

1. Compilar y ejecutar las pruebas existentes sobre esta continuación; agregar cobertura de etiquetas, paginación y cambio de idioma.
2. Revisar las pantallas con ambos idiomas y terminar las cadenas secundarias.
3. Probar Flyway y el mapeo contra MySQL 8.4 disponible en un entorno autorizado, usando una base nueva. No aplicar a una base anterior.
4. Resolver cualquier fallo antes de reconstruir el paquete para enviar.

El README_BETA.md describe el corte anterior; la sección MySQL y la exclusión de etiquetas/idioma deben actualizarse después de comprobar estas nuevas funciones.
