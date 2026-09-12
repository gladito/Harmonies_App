# Trabajo Práctico: Verificación y Validación de Software

* **Tipo de entrega:** Trabajo Práctico Obligatorio N.º 1
* **Fecha de entrega:** 06/07/2026
* **Sistema bajo prueba (SUT):** Harmony
* **Alumno:** Agustin Navarro
* **Curso:** 3er Año
* **Materia:** Verificación y validación de software

---

### 1. Presentación del Sistema

**1.1. Nombre del sistema y descripción general**
El sistema bajo estudio y desarrollo se denomina **Harmony**. Consiste en una plataforma web orientada específicamente al ámbito musical, cuyo propósito central es proveer un entorno digital que facilite la interacción, colaboración y conexión entre músicos. Harmony permite a los usuarios registrarse en el sistema, confeccionar y gestionar perfiles individuales detallados, y disponer de herramientas para la creación y administración de agrupaciones musicales (bandas). Adicionalmente, el software proporciona funcionalidades para la publicación de contenido multimedia e informativo, así como mecanismos de vinculación para conectar a músicos que compartan intereses, habilidades o afinidades artísticas similares.

**1.2. Tecnologías y lenguajes utilizados**
Para la construcción e implementación de Harmony, se ha definido una arquitectura técnica que emplea el siguiente conjunto de lenguajes y tecnologías:
* **Lógica de Negocio y Backend:** Java y Spring Boot.
* **Interfaz de Usuario (Frontend):** Thymeleaf, HTML, CSS y JavaScript.
* **Persistencia de Datos:** MySQL.

**1.3. Usuarios o actores principales**
* **Músico (Usuario Registrado):** Actor central. Autenticación, gestión de perfil, explorar plataforma, publicar contenido y establecer conexiones. Puede fundar o unirse a bandas.
* **Administrador de Banda:** Rol específico con permisos de gestión sobre una banda. Administra el perfil de la agrupación, integrantes y publicaciones.

**1.4. Estado actual del desarrollo**
En estado de **codificación avanzada y pruebas iniciales**. Funcionalidades estructurales en proceso de implementación técnica, preparando el producto para ciclos formales de pruebas.

---

### 2. Funcionalidades Críticas del Sistema

| Funcionalidad | Descripción Breve | Justificación de Criticidad | Clasificación |
| :--- | :--- | :--- | :--- |
| **Registro y Autenticación** | Permite a los músicos crear cuenta y acceder de forma segura. | Un fallo bloquea el acceso. Vulnerabilidades exponen datos personales y credenciales. | Autenticación y Seguridad |
| **Gestión de Bandas** | Permite conformar agrupaciones, definir perfil y administrar miembros. | Núcleo de la lógica de negocio. Fallos impiden formar grupos o alteran privilegios. | Lógica de Negocio y Persistencia |
| **Búsqueda y Emparejamiento** | Búsquedas avanzadas aplicando filtros paramétricos. | Mecanismo principal de propuesta de valor. Fallas impiden conexiones entre usuarios. | Procesamiento de Datos |
| **Publicación de Contenido** | Subir, visualizar y compartir material en perfiles. | Imposibilidad de publicar anula la exhibición de talento. Riesgo de inyección de código (XSS). | Persistencia e Interfaz de Usuario |

---

### 3. Plan de Pruebas

**3.1. Alcance de las pruebas**
* **En alcance:** Registro/Autenticación, Gestión de Bandas, Búsqueda, Publicación. Validación de interfaz en navegadores modernos.
* **Fuera de alcance:** Pruebas de estrés y carga extrema, integración con pasarelas de pago, navegadores obsoletos.

**3.2. Tipos de pruebas**
* **Unitarias:** Verificar comportamiento de clases y métodos en Java.
* **Integración:** Comunicación entre Spring Boot, Thymeleaf y MySQL.
* **Funcionales:** Validar el software contra requisitos de negocio (caja negra).
* **Regresión:** Asegurar que nuevas adiciones no introducen defectos.
* **Usabilidad:** Evaluar intuición de la interfaz (HTML/CSS/JS).
* **Seguridad:** Evaluar vulnerabilidades comunes (SQL Injection, XSS).

**3.3. Técnicas de diseño**
* **Partición de Equivalencia:** Clases válidas e inválidas (ej. formatos de email).
* **Análisis de Valores Límite:** Extremos de clases (ej. longitud de contraseñas).
* **Tablas de Decisión:** Lógicas complejas dependientes de múltiples condiciones (ej. permisos de administración).

**3.4. Entorno de pruebas**
* **Hardware/Software:** Estaciones de trabajo locales con Windows (Entorno de desarrollo local). Navegadores modernos.
* **Frameworks/Herramientas:** MySQL (para validación de persistencia y estado de datos) y Postman (para pruebas de integración y validación de endpoints HTTP). Las pruebas sobre la interfaz (Frontend) se ejecutarán de forma manual.
* **Datos de Prueba:** Conjunto de datos sintéticos (*mock data*).

**3.5. Criterios de aceptación**
* **Aprobada:** Comportamiento real coincide con esperado, sin excepciones ni degradación.
* **Fallida:** Desviación funcional, vulneración de datos, *crash* o tiempos de respuesta inaceptables.

**3.6. Riesgos del plan**
* **Inestabilidad del entorno:** Mitigación mediante Integración Continua (CI).
* **Retrasos de desarrollo:** Mitigación mediante Pruebas Basadas en Riesgos (*Risk-Based Testing*).

---

### 4. Casos de Prueba

| ID del Caso de Prueba | CP-01 |
| :--- | :--- |
| **Nombre** | Autenticación Exitosa |
| **Módulo / Funcionalidad** | Autenticación |
| **Prioridad** | Alta |
| **Precondiciones** | El usuario está registrado y su cuenta se encuentra activa. |
| **Condiciones de Entrada** | Email: 'musico@test.com' \| Contraseña: 'Password123!' |
| **Pasos de Ejecución** | 1. Acceder a la pantalla de inicio de sesión.<br>2. Ingresar el email.<br>3. Ingresar la contraseña.<br>4. Hacer clic en 'Iniciar sesión'. |
| **Resultado Esperado** | El sistema autentica al usuario correctamente y redirige a la pantalla principal (dashboard). |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario exitoso (Happy Path) |
| **Riesgo Cubierto** | Fallo de autenticación que impide el acceso de usuarios al sistema. |

<br>

| ID del Caso de Prueba | CP-02 |
| :--- | :--- |
| **Nombre** | Credenciales Inválidas |
| **Módulo / Funcionalidad** | Autenticación |
| **Prioridad** | Alta |
| **Precondiciones** | El usuario existe en la base de datos. |
| **Condiciones de Entrada** | Email: 'musico@test.com' \| Contraseña: 'ClaveIncorrecta' |
| **Pasos de Ejecución** | 1. Acceder a la pantalla de inicio de sesión.<br>2. Ingresar un email correcto.<br>3. Ingresar una contraseña incorrecta.<br>4. Hacer clic en 'Iniciar sesión'. |
| **Resultado Esperado** | El sistema no autentica al usuario. Muestra un mensaje de error: 'Credenciales inválidas'. |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario de error |
| **Riesgo Cubierto** | Acceso no autorizado al sistema por validación insuficiente de credenciales. |

<br>

| ID del Caso de Prueba | CP-03 |
| :--- | :--- |
| **Nombre** | Creación de Banda Exitosa |
| **Módulo / Funcionalidad** | Gestión y Administración de Bandas |
| **Prioridad** | Alta |
| **Precondiciones** | El usuario ha iniciado sesión y se encuentra en el módulo 'Mis Bandas'. |
| **Condiciones de Entrada** | Nombre: 'The Rockers' \| Género: 'Rock' \| Descripción: 'Banda tributo' |
| **Pasos de Ejecución** | 1. Hacer clic en el botón 'Crear Nueva Banda'.<br>2. Completar todos los campos obligatorios.<br>3. Hacer clic en 'Guardar Banda'. |
| **Resultado Esperado** | El sistema guarda la banda y asigna el rol de Administrador al creador. |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario exitoso (Happy Path) |
| **Riesgo Cubierto** | Incapacidad de conformar comunidades o agrupaciones. |

<br>

| ID del Caso de Prueba | CP-04 |
| :--- | :--- |
| **Nombre** | Creación de Banda con Campos Faltantes |
| **Módulo / Funcionalidad** | Gestión y Administración de Bandas |
| **Prioridad** | Media |
| **Precondiciones** | El usuario ha iniciado sesión y está en el formulario de creación de banda. |
| **Condiciones de Entrada** | Nombre: (Vacío) \| Género: 'Jazz' |
| **Pasos de Ejecución** | 1. Dejar en blanco el campo 'Nombre'.<br>2. Completar el resto de campos.<br>3. Hacer clic en 'Guardar Banda'. |
| **Resultado Esperado** | El sistema no guarda la banda, resalta el campo en rojo y muestra el error de validación. |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario de error |
| **Riesgo Cubierto** | Inconsistencia de datos en la base de datos (registros sin nombre). |

<br>

| ID del Caso de Prueba | CP-05 |
| :--- | :--- |
| **Nombre** | Búsqueda Exitosa por Instrumento |
| **Módulo / Funcionalidad** | Motor de Búsqueda y Emparejamiento |
| **Prioridad** | Alta |
| **Precondiciones** | Existen al menos 3 músicos registrados que tocan la batería. |
| **Condiciones de Entrada** | Filtro Instrumento: 'Batería' \| Filtro Nivel: 'Profesional' |
| **Pasos de Ejecución** | 1. Acceder al módulo 'Buscar Músicos'.<br>2. Seleccionar los filtros correspondientes.<br>3. Hacer clic en 'Buscar'. |
| **Resultado Esperado** | Muestra lista paginada exclusivamente con músicos que cumplen los criterios. |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario exitoso |
| **Riesgo Cubierto** | Falla en el mecanismo de emparejamiento que impide conectar músicos. |

<br>

| ID del Caso de Prueba | CP-06 |
| :--- | :--- |
| **Nombre** | Búsqueda sin Resultados Coincidentes |
| **Módulo / Funcionalidad** | Motor de Búsqueda y Emparejamiento |
| **Prioridad** | Baja |
| **Precondiciones** | El usuario ha iniciado sesión. |
| **Condiciones de Entrada** | Filtro Instrumento: 'Arpa' \| Filtro Género: 'Heavy Metal' |
| **Pasos de Ejecución** | 1. Acceder al módulo 'Buscar Músicos'.<br>2. Aplicar filtros improbables.<br>3. Hacer clic en 'Buscar'. |
| **Resultado Esperado** | El sistema ejecuta la consulta sin fallar, mostrando el mensaje 'No se encontraron resultados'. |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario alternativo |
| **Riesgo Cubierto** | Excepciones no controladas al no encontrar registros coincidentes. |

<br>

| ID del Caso de Prueba | CP-07 |
| :--- | :--- |
| **Nombre** | Publicación de Imagen en Perfil |
| **Módulo / Funcionalidad** | Publicación y Almacenamiento de Contenido |
| **Prioridad** | Media |
| **Precondiciones** | El usuario ha iniciado sesión y se encuentra en su muro. |
| **Condiciones de Entrada** | Archivo: 'concierto.jpg' (2MB) \| Texto: 'Gran show' |
| **Pasos de Ejecución** | 1. Hacer clic en 'Nueva Publicación'.<br>2. Escribir texto y adjuntar archivo.<br>3. Hacer clic en 'Publicar'. |
| **Resultado Esperado** | Sube la imagen y actualiza el muro con la nueva publicación. |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario exitoso |
| **Riesgo Cubierto** | Fallo en la persistencia de imágenes y visualización en UI. |

<br>

| ID del Caso de Prueba | CP-08 |
| :--- | :--- |
| **Nombre** | Carga de Archivo No Soportado |
| **Módulo / Funcionalidad** | Publicación y Almacenamiento de Contenido |
| **Prioridad** | Alta |
| **Precondiciones** | El usuario está en la vista de nueva publicación. |
| **Condiciones de Entrada** | Archivo: 'script_malicioso.exe' |
| **Pasos de Ejecución** | 1. Hacer clic en 'Nueva Publicación'.<br>2. Intentar adjuntar el archivo .exe.<br>3. Hacer clic en 'Publicar'. |
| **Resultado Esperado** | Rechaza la operación en el frontend y muestra mensaje de extensión no permitida. |
| **Tipo de Prueba** | Funcional — Caja Negra — Escenario de error |
| **Riesgo Cubierto** | Subida de archivos maliciosos (seguridad y persistencia). |

---

### 5. Matriz de Trazabilidad

A continuación, se presenta la matriz de trazabilidad que vincula cada caso de prueba diseñado con la funcionalidad crítica correspondiente, asegurando la cobertura de todos los procesos críticos identificados.

| ID de Funcionalidad | Nombre de la Funcionalidad | ID del Caso de Prueba | Tipo de Escenario Cubierto |
| :--- | :--- | :--- | :--- |
| **F-01** | Registro y Autenticación | CP-01 | Exitoso (Happy Path) |
| **F-01** | Registro y Autenticación | CP-02 | Error |
| **F-02** | Gestión de Bandas | CP-03 | Exitoso (Happy Path) |
| **F-02** | Gestión de Bandas | CP-04 | Error |
| **F-03** | Búsqueda y Emparejamiento | CP-05 | Exitoso (Happy Path) |
| **F-03** | Búsqueda y Emparejamiento | CP-06 | Alternativo |
| **F-04** | Publicación de Contenido | CP-07 | Exitoso (Happy Path) |
| **F-04** | Publicación de Contenido | CP-08 | Error |
