# Prompts de OpenCode — Sprint 1

**Cómo usarlos**
1. Pega el prompt de la tarea en una sesión **nueva** de OpenCode abierta en la raíz del repositorio (`AGENTS.md` se lee solo; si tu OpenCode no lo hace, empieza con «Lee AGENTS.md y constitution.md»).
2. Rama por tarea: `s1-07-esqueleto`, `s1-08-login`, etc.
3. Cuando termine, **lee el diff completo** (o pídele a la otra persona que lo haga), ejecuta `./mvnw test` y registra la sesión en `docs/ia/bitacora.md`.
4. No pegues contraseñas, URLs de bases de datos ni datos reales en el prompt.

**Cierre común** (se repite al final de cada prompt, no hace falta pegarlo dos veces si ya está abajo):

> Responde al terminar con: archivos creados o modificados · qué AC cubren las pruebas y su resultado (rojo antes, verde después) · decisiones que tomaste · cosas fuera de alcance que viste sin tocarlas · marcadores [NECESITA ACLARACIÓN].

**Orden y dependencias:** S1-07 → S1-08 → (S1-09, S1-10, S1-11) → S1-12 y S1-13 → S1-14 y S1-15.

---

## S1-07 — Esqueleto Spring Boot, perfiles, seeder y Dockerfile (David)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/plan.md (secciones 1, 2, 3 y 8) y docs/adr/ADR-001-a-005.md.

TAREA S1-07: crear el esqueleto del proyecto BakerStock.

Haz:
1. Proyecto Maven en la raíz: Java 21, Spring Boot 3.5.x (última de la rama 3), groupId com.bakerstock, artifactId bakerstock, paquete base com.bakerstock. Incluye Maven Wrapper (mvnw).
2. Solo estas dependencias: web, thymeleaf, thymeleaf-extras-springsecurity6, security, data-jpa, validation, mysql-connector-j (runtime), h2 (runtime), starter-test, spring-security-test. Nada más (sin Lombok).
3. Paquetes vacíos con package-info.java: dominio, repositorio, web, seguridad, config.
4. Perfiles según plan.md sección 3: application.properties (común: session timeout 30m, open-in-view false, zona America/Bogota), application-dev.properties (MySQL con DB_URL, DB_USER, DB_PASSWORD desde variables de entorno, ddl-auto=update), application-cloud.properties (H2 en memoria modo MySQL, puerto desde PORT), application-test.properties (H2).
5. Una clase de prueba de contexto (BakerStockApplicationTests) con perfil test que verifica que el contexto arranca.
6. Dockerfile de dos etapas (build con Maven, ejecución con JRE 21), perfil cloud por defecto, EXPOSE desde PORT.
7. .gitignore (target, .env, IDE) y README.md con: cómo ejecutar en dev, variables de entorno y cómo correr las pruebas.
8. SecurityConfig mínima que permita todo por ahora (se completa en S1-08).

No hagas: entidades, controladores, vistas, ni paquete service. No incluyas contraseñas ni URLs reales. No modifiques specs/, constitution.md ni docs/adr/.

Verificación: ./mvnw test en verde; ./mvnw spring-boot:run con -Dspring-boot.run.profiles=cloud arranca sin errores.
```

*Prueba de aceptación manual (Ana):* clonar, `./mvnw test`, arrancar con `cloud`, construir la imagen Docker y ver el servicio en Render.

---

## S1-08 — HU-01 Inicio y cierre de sesión (David)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (REQ-BAK-015, 016, 017, 027; NFR-BAK-002, 003; AC-015.1, AC-016.1 a 016.3, AC-017.1, AC-027.1, AC-N002.1, AC-N003.1) y plan.md (secciones 4 y 5).

TAREA S1-08: inicio y cierre de sesión con Spring Security. Sin capa service.

Primero las pruebas (rojo), luego el código (verde):
1. Escribe las pruebas con @SpringBootTest + MockMvc + spring-security-test, perfil test, una por criterio listado; cada una lleva el ID del criterio en @DisplayName. Clases: AuthLoginTest, AuthLogoutTest, AuditoriaSeguridadTest, PasswordHashTest, SesionConfigTest. Ejecútalas y confirma que fallan.
2. Entidades en dominio: Usuario (id, nombre, correo único en minúsculas, contrasenaHash, rol, activo, creadoEn), enum Rol (ADMINISTRADOR, COMPRADOR, ALMACENISTA, PANADERO, CAJERO), EventoAuditoria (solo inserción) y enum AccionAuditoria con LOGIN_OK, LOGIN_FALLIDO, LOGOUT, ACCESO_DENEGADO, USUARIO_CREADO, USUARIO_DESACTIVADO, USUARIO_ACTIVADO, RECUPERACION_SOLICITADA, CONTRASENA_RESTABLECIDA, INSUMO_CREADO, PROVEEDOR_CREADO.
3. Repositorios: UsuarioRepository (findByCorreoIgnoreCase), EventoAuditoriaRepository.
4. En seguridad: UserDetailsService sobre UsuarioRepository (rol como ROLE_<ROL>, cuenta desactivada = disabled), SecurityConfig con login por formulario en /login, logout por POST en /logout, CSRF activado, BCryptPasswordEncoder, / protegida y /login público. Mensaje único «Correo o contraseña incorrectos» para correo inexistente, contraseña mala o cuenta desactivada. Un @EventListener que escribe LOGIN_OK, LOGIN_FALLIDO y LOGOUT en EventoAuditoriaRepository.
5. Vistas: login.html (Bootstrap, en español) e inicio.html mínima (el menú por rol llega en S1-09).
6. DataSeeder en config para perfiles dev y cloud: un usuario por rol con correos <rol>@bakerstock.test. La contraseña sale de BAKERSTOCK_SEED_PASSWORD; si no existe, genera una aleatoria y la imprime UNA vez en el log. Nunca la escribas en el repositorio.

No hagas: recuperación de contraseña, gestión de usuarios, menú por rol, ni servicios. No añadas dependencias.

Verificación: toda la suite en verde y inicio de sesión manual con cada rol en dev.
```

---

## S1-09 — HU-04 Menú por rol, 403 y auditoría de accesos denegados (Ana)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (REQ-BAK-008, 021, 027; AC-008.1, AC-008.2, AC-021.1, AC-021.2; sección 2 con la matriz de permisos) y plan.md (sección 5 con la tabla de rutas).

TAREA S1-09: menú según el rol y rechazo de accesos no permitidos. Sin capa service. Ya existen Usuario, Rol, EventoAuditoria, SecurityConfig y login (S1-08); no cambies su comportamiento.

Primero las pruebas (rojo), luego el código (verde):
1. MenuPorRolTest y AccesoPorRolTest: una prueba por criterio con el ID en @DisplayName. Para AC-008.1 comprueba 403, la página «Acceso denegado» y el evento ACCESO_DENEGADO con usuario, fecha-hora y recurso. Para AC-008.2 comprueba la redirección a /login.
2. fragmentos/menu.html con sec:authorize, incluido desde las demás plantillas. Opciones del Sprint 1 por rol: Administrador (Usuarios, Proveedores, Insumos), Comprador (Proveedores, Insumos), Almacenista (Insumos), Panadero (Insumos), Cajero (solo Inicio). Muestra cada enlace solo si el rol tiene permiso.
3. En SecurityConfig, reglas por ruta según la tabla de plan.md sección 5 (rutas aún sin controlador: déjalas declaradas igualmente). Un AccessDeniedHandler que redirija a /acceso-denegado con estado 403 y plantilla error/403.html.
4. Listener de AuthorizationDeniedEvent (o AccessDeniedHandler) que escribe ACCESO_DENEGADO en EventoAuditoriaRepository con usuario, fecha-hora y ruta.

No hagas: controladores de usuarios, insumos ni proveedores (llegan en otras tareas), ni capa service.

Verificación: toda la suite en verde; prueba manual con el Cajero abriendo /insumos/nuevo.
```

---

## S1-10 — HU-03 Gestión de usuarios (Ana)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (REQ-BAK-019, 020, 028; NFR-BAK-004; AC-019.1, 019.2, 020.1, 028.1, N004.1) y plan.md (secciones 4 y 5).

TAREA S1-10: el Administrador crea usuarios con un rol, desactiva y reactiva cuentas. Sin capa service. Ya existen Usuario, UsuarioRepository, EventoAuditoria y seguridad (S1-08, S1-09).

Primero las pruebas (rojo), luego el código (verde):
1. UsuarioAdminTest con una prueba por criterio (ID en @DisplayName). Incluye el caso de borde: el único Administrador no puede desactivarse a sí mismo (AC-028.1) ni quitarse su rol.
2. Controlador web UsuarioAdminController en /admin/usuarios (lista), /admin/usuarios/nuevo (GET/POST), /admin/usuarios/{id}/desactivar y /activar (POST). Usa UsuarioRepository directamente.
3. Formulario con Bean Validation: nombre obligatorio, correo válido y único (DATO_DUPLICADO), rol uno de los 5, contraseña mínimo 8 caracteres con al menos una letra y un número (DATO_INVALIDO). Contraseña guardada con el PasswordEncoder del sistema (BCrypt).
4. Desactivar no borra: pone activo=false y conserva el historial. Auditoría con USUARIO_CREADO, USUARIO_DESACTIVADO y USUARIO_ACTIVADO usando EventoAuditoriaRepository.
5. Plantillas admin/usuarios/lista.html y form.html con Bootstrap en español.

No hagas: recuperación de contraseña, eliminar usuarios, cambiar el rol de usuarios existentes (solo crear), ni capa service.

Verificación: toda la suite en verde; prueba manual creando un usuario por rol e intentando entrar con él.
```

---

## S1-11 — HU-02 Recuperar contraseña (David)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (§0 decisión D-01, REQ-BAK-018, AC-018.1 a 018.4, NFR-BAK-004) y plan.md (secciones 4 y 5).

TAREA S1-11: recuperación de contraseña por token, sin enviar correos reales. Sin capa service. Ya existen Usuario, UsuarioRepository, auditoría y login.

Primero las pruebas (rojo), luego el código (verde):
1. RecuperacionContrasenaTest con una prueba por criterio (ID en @DisplayName): mensaje neutro siempre igual exista o no el correo; token de un solo uso con vencimiento de 30 minutos; token usado o vencido rechazado; contraseña nueva válida con BCrypt e invalidación del token.
2. Entidad TokenRecuperacion (usuario, tokenHash, expiraEn, usado) y su repositorio. Guarda solo el SHA-256 del token, nunca el token en claro. El token se genera con SecureRandom (32 bytes, Base64 URL).
3. Controlador RecuperacionController: GET/POST /recuperar y GET/POST /restablecer?token=. El enlace se escribe en el log con nivel INFO (sin SMTP, sin dependencia de correo): "Enlace de recuperación para <correo>: <url>".
4. Auditoría: RECUPERACION_SOLICITADA (solo si la cuenta existe y está activa, pero el mensaje al usuario es el mismo) y CONTRASENA_RESTABLECIDA.
5. Plantillas recuperar.html y restablecer.html, en español, con el enlace «¿Olvidaste tu contraseña?» en login.html.
6. Validación de la contraseña nueva igual que en S1-10 (mínimo 8, letra y número).

No hagas: envío de correos, límite de intentos, preguntas secretas, ni capa service. No añadas dependencias.

Verificación: toda la suite en verde; prueba manual completa leyendo el enlace del log.
```

---

## S1-12 — HU-05 Registrar insumos (David)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (§0 decisión D-04, REQ-BAK-022, 023, 027; RN-01; AC-022.1 a 022.3, AC-023.1) y plan.md (secciones 4 y 5).

TAREA S1-12: el Almacenista registra insumos. Sin capa service. Ya existen seguridad por rol y menú (S1-09).

Primero las pruebas (rojo), luego el código (verde):
1. InsumoRegistroTest con una prueba por criterio (ID en @DisplayName) y un @DataJpaTest para la unicidad del nombre sin distinguir mayúsculas.
2. Entidad Insumo en dominio: nombre, unidadBase (enum G, ML, UNIDAD), categoria (enum HARINAS, LACTEOS_Y_HUEVOS, GRASAS, LEVADURAS_Y_AGENTES, AZUCARES, RELLENOS_Y_FRUTAS, EMPAQUES, OTROS), stockMinimo (BigDecimal, >= 0), activo. Stock total y utilizable son métodos del dominio que suman los lotes (el cálculo completo llega en S1-15, deja los métodos stockTotal() devolviendo cero si no hay lotes).
3. Entidad Lote mínima (insumo, numeroLote, cantidadInicial, cantidadActual >= 0, costoUnitario, fechaRecepcion, fechaVencimiento) con el método estaVencido(LocalDate hoy) = vencimiento anterior a hoy. Solo lectura por ahora; no hay pantalla para crearlos.
4. InsumoRepository con existsByNombreIgnoreCase.
5. InsumoController: GET /insumos/nuevo y POST /insumos/nuevo, solo ALMACENISTA. Validación con Bean Validation y mensajes en español: «Use g, ml o unidad» para unidad inválida (DATO_INVALIDO), nombre repetido (DATO_DUPLICADO), stock mínimo negativo. Al crear: stock 0 y evento INSUMO_CREADO.
6. Plantilla insumos/form.html con selectores de unidad y categoría.
7. En el DataSeeder de dev/cloud, agrega 8 insumos ficticios y algunos lotes de demostración, incluido un lote vencido y uno que vence en 3 días.

No hagas: listado, búsqueda ni detalle (S1-14 y S1-15), edición ni eliminación de insumos, ni capa service.

Verificación: toda la suite en verde; prueba manual registrando insumos como Almacenista y viendo el 403 como Cajero.
```

---

## S1-13 — HU-06 Proveedores (Ana)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (§0 decisión D-03, REQ-BAK-024, 027; AC-024.1, 024.2) y plan.md (secciones 4 y 5).

TAREA S1-13: el Comprador registra y consulta proveedores; el Administrador solo consulta. Sin capa service.

Primero las pruebas (rojo), luego el código (verde):
1. ProveedorRegistroTest con una prueba por criterio (ID en @DisplayName): alta correcta y NIT repetido rechazado con DATO_DUPLICADO. Añade una prueba de que el Administrador ve la lista pero recibe 403 al registrar.
2. Entidad Proveedor (nombre, nit único, telefono, correo, direccion, activo). Exige nombre, NIT y al menos un dato de contacto (teléfono o correo).
3. ProveedorRepository (existsByNit) y ProveedorController: GET /proveedores (COMPRADOR y ADMINISTRADOR), GET/POST /proveedores/nuevo (solo COMPRADOR). Evento PROVEEDOR_CREADO en la auditoría.
4. Plantillas proveedores/lista.html y form.html con Bootstrap en español.
5. En el DataSeeder agrega 3 proveedores ficticios (nombres y NIT inventados, con dominio .test).

No hagas: edición ni eliminación, órdenes de compra (Sprint 2), ni capa service.

Verificación: toda la suite en verde; prueba manual como Comprador y como Administrador.
```

---

## S1-14 — HU-07 Buscar insumos (Ana)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (§0 decisión D-02, REQ-BAK-025; AC-025.1 a 025.3) y plan.md (secciones 4, 5 y 7).

TAREA S1-14: buscar insumos por nombre o categoría. Sin capa service. Ya existen Insumo e InsumoRepository (S1-12).

Primero las pruebas (rojo), luego el código (verde):
1. InsumoBusquedaTest: una prueba por criterio (ID en @DisplayName) y un @DataJpaTest para la consulta del repositorio.
2. En InsumoRepository, un método con @Query que reciba texto (opcional) y categoría (opcional): coincidencia parcial sin distinguir mayúsculas en el nombre, filtro exacto por categoría, ordenado por nombre. Sin texto ni categoría lista todos.
3. InsumoController: GET /insumos con parámetros q y categoria, permitido a ADMINISTRADOR, COMPRADOR, ALMACENISTA y PANADERO; el CAJERO recibe 403.
4. Plantilla insumos/lista.html con formulario de búsqueda, tabla (nombre, categoría, unidad, stock mínimo, enlace al detalle) y el mensaje «No se encontraron insumos» cuando no hay resultados.

No hagas: la pantalla de detalle (S1-15), paginación, ni capa service.

Verificación: toda la suite en verde; prueba manual con «har» y con un filtro de categoría.
```

---

## S1-15 — HU-08 Stock de un insumo y sus lotes (David)

```
Lee AGENTS.md, constitution.md, specs/001-BakerStock/spec.md (§0 decisión D-02, §3 glosario, REQ-BAK-026; NFR-BAK-001; AC-026.1 a 026.3, AC-N001.1) y plan.md (secciones 4, 5 y 7).

TAREA S1-15: consultar un insumo con su stock total, su stock utilizable y sus lotes. Sin capa service. Ya existen Insumo, Lote y los repositorios (S1-12).

Primero las pruebas (rojo), luego el código (verde):
1. Pruebas de dominio con JUnit 5 sin Spring: Insumo.stockTotal() suma cantidadActual de todos los lotes; Insumo.stockUtilizable(hoy) suma solo los no vencidos; Lote.estaVencido(hoy) es verdadero solo si el vencimiento es anterior a hoy (un lote que vence hoy es utilizable). Un test @DisplayName por AC-026.x.
2. InsumoDetalleTest (MockMvc): GET /insumos/{id} muestra stock total, stock utilizable y los lotes ordenados por fecha de vencimiento ascendente (FEFO), marca «Vencido» en los vencidos y muestra «Sin lotes registrados» cuando no hay.
3. Implementa los métodos de dominio y la consulta del repositorio que traiga los lotes ordenados por vencimiento sin provocar N+1 (usa join fetch o @EntityGraph).
4. InsumoController: GET /insumos/{id}, permitido a ADMINISTRADOR, COMPRADOR, ALMACENISTA y PANADERO; el CAJERO recibe 403. Un id inexistente devuelve 404.
5. Plantilla insumos/detalle.html con Bootstrap en español; cantidades con la unidad base (g, ml, unidad).
6. Con el seeder de dev, crea 50 insumos y 200 lotes de prueba bajo un perfil o propiedad opcional para medir NFR-BAK-001 (menos de 2 segundos en el navegador); esa medición es manual.

No hagas: crear ni editar lotes (Sprint 2), kardex, alertas, ni capa service.

Verificación: toda la suite en verde; mide el tiempo en la pestaña Red del navegador y anótalo en la tarjeta.
```
