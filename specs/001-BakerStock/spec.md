# BakerStock — Especificación v1.0

**Estado:** BORRADOR para revisión de Ana y David, y aprobación del Product Owner en la Puerta 1
**Fecha:** 2026-11-13 · **Nivel SDD:** spec-anchored
**Product Owner:** Julián Barney · **Redacta y valida:** Ana Sofía Gómez Arguello (redacta), David Steven Patiño Rueda (valida)

> Esta especificación responde al **qué** y al **porqué**. El **cómo** está en `plan.md` y el **en qué orden** en `tasks.md`. La versión v1.1 será la v1.0 más el delta de CR-001 (`changes/CR-001`).

---

## 0. Decisiones para la Puerta 1

Estos puntos estaban abiertos al redactar. Cada uno lleva una propuesta por defecto en la que se basan los requisitos de abajo; el Product Owner la acepta o la cambia. **Ningún marcador `[NECESITA ACLARACIÓN]` puede quedar abierto al aprobar.**

| ID | Tema | Por qué es una duda | Propuesta por defecto |
|----|------|---------------------|------------------------|
| D-01 | Recuperación de contraseña (HU-02) | No se define cómo llega el enlace; enviar correo exige una biblioteca de correo y un servidor SMTP | Token de un solo uso con vencimiento de 30 minutos; el enlace se escribe en el log de la aplicación (sin SMTP) |
| D-02 | Quién consulta insumos, lotes y kardex (HU-07, HU-08, HU-13) | Las HU dicen «cualquier rol», pero la Tabla 8 limita al Cajero al stock de producto terminado y asigna el kardex al Almacenista | El Cajero **no** consulta insumos ni lotes; el kardex lo ven Almacenista y Administrador |
| D-03 | Quién consulta proveedores | La Tabla 8 solo asigna el registro al Comprador | Comprador registra y consulta; Administrador consulta (regla de «todas las consultas») |
| D-04 | Categorías de insumo | HU-05 pide categoría sin decir si es lista fija o catálogo editable | Lista fija: HARINAS, LACTEOS_Y_HUEVOS, GRASAS, LEVADURAS_Y_AGENTES, AZUCARES, RELLENOS_Y_FRUTAS, EMPAQUES, OTROS |
| D-05 | Producto terminado y precio de venta | El dominio del documento no lista la entidad Producto y las ventas y CR-001 (10 % del valor de la venta) necesitan un precio | Incluir la entidad `Producto` (nombre, precio de venta, stock) ligada 1:1 a su `Receta` desde el modelo de datos de v1.0; se programa en Sprint 2 |
| D-06 | Base de datos en la nube | Render no ofrece MySQL y no se usará H2 | MySQL en todos los perfiles (decisión del equipo): `dev` y `test` en MySQL local; `cloud` con un MySQL en línea de otro proveedor al que se conecta la aplicación desplegada en Render |
| D-07 | Capa de servicios | El documento describe controladores → servicios → repositorios; el docente indicó el 4 de noviembre que con JPA no debe haber capa service | Sin capa service (ADR-004); las reglas viven en el dominio y en transacciones |
| D-08 | Días para alerta de vencimiento (REQ-BAK-007) | El documento ya lo marca «por confirmar» | 3 días o menos |

---

## 1. Contexto y objetivo

Una panadería pequeña lleva el inventario en cuaderno u hoja de cálculo. Eso genera saldos que no coinciden con la bodega, insumos perecederos que se vencen porque nadie sabe qué lote usar primero y costos calculados «a ojo». BakerStock es un sistema de **movimientos auditables**: sabe qué entró, de qué proveedor, en qué lote, cuándo vence, en qué receta se consumió y quién lo registró.

**Product Goal:** Que ningún insumo ni producto de la panadería se mueva sin registro: toda entrada, producción y venta queda registrada por la persona que su rol autoriza, el stock nunca es negativo y cada operación queda auditada.

### 1.1 Alcance

Gestión de insumos y proveedores, compras con recepción por lotes, recetas, producción, producto terminado, ventas de mostrador, alertas, costos de producción y auditoría.

### 1.2 No-objetivos

Facturación electrónica ante la DIAN, contabilidad, pago en línea, domicilios, aplicación móvil, pruebas de carga y microservicios.

---

## 2. Actores y permisos

| Capacidad | Admin. | Comprador | Almacenista | Panadero | Cajero |
|-----------|:------:|:---------:|:-----------:|:--------:|:------:|
| Iniciar/cerrar sesión, recuperar contraseña | ✔ | ✔ | ✔ | ✔ | ✔ |
| Gestionar usuarios y roles | ✔ | | | | |
| Registrar proveedores | | ✔ | | | |
| Consultar proveedores (D-03) | ✔ | ✔ | | | |
| Registrar insumos | | | ✔ | | |
| Consultar insumos, stock y lotes (D-02) | ✔ | ✔ | ✔ | ✔ | |
| Registrar órdenes de compra | | ✔ | | | |
| Recepciones, lotes, conteos y ajustes | | | ✔ | | |
| Consultar kardex | ✔ | | ✔ | | |
| Registrar recetas y producciones | | | | ✔ | |
| Consultar costo de producción | ✔ | | | ✔ | |
| Registrar ventas | | | | | ✔ |
| Consultar stock de producto terminado | ✔ | | | ✔ | ✔ |
| Consultar alertas | ✔ | ✔ | ✔ | ✔ | ✔ |
| Consultar auditoría | ✔ | | | | |

*Nota.* «Administrador… puede realizar todas las consultas» no le da permiso de registrar movimientos; solo de consultarlos. El kardex lo consultan el Almacenista y el Administrador; los demás roles ven el stock, pero no el detalle de movimientos.

---

## 3. Glosario

Insumo, lote, FEFO, stock mínimo, receta, kardex y conteo físico tienen las definiciones de la Tabla 2 del documento del docente. Términos propios de esta especificación:

- **Stock total de un insumo:** suma de `cantidad_actual` de todos sus lotes.
- **Stock utilizable:** suma de `cantidad_actual` de los lotes **no vencidos**. Producción y venta solo pueden usar este stock.
- **Lote vencido:** lote cuya fecha de vencimiento es **anterior** a la fecha actual. Un lote que vence hoy aún es utilizable.

---

## 4. Reglas de negocio

| ID | Regla |
|----|-------|
| RN-01 | Toda cantidad se maneja en la unidad base del insumo (gramos, mililitros o unidades); las conversiones se hacen al registrar |
| RN-02 | Los insumos salen por FEFO: se consume primero el lote con vencimiento más próximo |
| RN-03 | El stock de un insumo o producto nunca puede quedar negativo |
| RN-04 | Un lote vencido no se puede usar en producción ni en venta; se retira del inventario con un ajuste y su causa |
| RN-05 | Toda salida distinta de producción o venta (ajuste) exige una causa registrada |
| RN-06 | Un movimiento de inventario no se edita ni se elimina; se corrige con un movimiento contrario |
| RN-07 | Cada receta define su rendimiento; la producción registra la cantidad realmente obtenida |

*Reservada:* RN-08 (descuentos en ventas) entra por CR-001 en Sprint 3.

---

## 5. Requisitos (EARS)

*Nota.* REQ-BAK-014 está reservado para CR-001 (delta del Sprint 3).

### 5.1 Requisitos de inventario, producción y ventas

| ID | Patrón | Requisito | HU | Sprint |
|----|--------|-----------|----|:------:|
| REQ-BAK-001 | Ubicuo | El sistema deberá registrar usuario, fecha-hora y motivo de cada movimiento de inventario | HU-10, 12, 14, 15 | 2 |
| REQ-BAK-002 | Evento | Cuando el Almacenista registra la recepción de una compra, el sistema deberá crear un lote con cantidad, costo y fecha de vencimiento, y sumar la cantidad al stock del insumo | HU-10 | 2 |
| REQ-BAK-003 | Evento | Cuando el Panadero registra una producción, el sistema deberá descontar los insumos de la receta aplicando FEFO y sumar al stock la cantidad obtenida del producto terminado | HU-12 | 2 |
| REQ-BAK-004 | No deseado | Si el stock de un insumo es insuficiente para una producción, entonces el sistema deberá rechazarla e indicar el insumo y la cantidad faltante | HU-12 | 2 |
| REQ-BAK-005 | No deseado | Si un lote está vencido, entonces el sistema deberá excluirlo de la producción y de la venta | HU-12, 15 | 2 |
| REQ-BAK-006 | Estado | Mientras el stock de un insumo sea menor o igual a su stock mínimo, el sistema deberá mostrarlo en la lista de alertas | HU-16 | 3 |
| REQ-BAK-007 | Evento | Cuando un lote esté a 3 días o menos de vencer, el sistema deberá mostrarlo en la lista de alertas por vencimiento (D-08) | HU-16 | 3 |
| REQ-BAK-009 | Ubicuo | El sistema deberá impedir la edición y eliminación de movimientos de inventario ya registrados | HU-13 | 2 |
| REQ-BAK-010 | Evento | Cuando el Cajero registra una venta, el sistema deberá descontar las unidades vendidas del stock de producto terminado | HU-15 | 3 |
| REQ-BAK-011 | Opcional | Donde el producto tenga receta con costos, el sistema deberá calcular su costo de producción por unidad con el costo de los lotes consumidos | HU-17 | 3 |
| REQ-BAK-012 | Evento | Cuando el Almacenista registra un conteo físico con diferencia, el sistema deberá generar un ajuste con su causa | HU-14 | 2 |
| REQ-BAK-013 | Evento | Cuando el Almacenista retira un lote vencido mediante un ajuste, el sistema deberá descontar la cantidad del lote y registrar la causa | HU-14 | 2 |

### 5.2 Requisitos de acceso, catálogos y consulta (Sprint 1)

| ID | Patrón | Requisito | HU | Sprint |
|----|--------|-----------|----|:------:|
| REQ-BAK-008 | No deseado | Si un usuario intenta una operación que su rol no permite, entonces el sistema deberá rechazarla y registrar el intento | HU-04 | 1 |
| REQ-BAK-015 | Evento | Cuando un usuario con cuenta activa envía su correo y contraseña válidos, el sistema deberá iniciar su sesión y mostrar el menú de su rol | HU-01 | 1 |
| REQ-BAK-016 | No deseado | Si el correo no existe, la contraseña no coincide o la cuenta está desactivada, entonces el sistema deberá rechazar el acceso con un mensaje único que no revele cuál dato falló, y registrar el intento | HU-01 | 1 |
| REQ-BAK-017 | Evento | Cuando el usuario cierra sesión, el sistema deberá invalidar su sesión | HU-01 | 1 |
| REQ-BAK-018 | Evento | Cuando un usuario solicita recuperar su contraseña con su correo, el sistema deberá mostrar siempre el mismo mensaje y, si la cuenta existe y está activa, generar un token de un solo uso que vence a los 30 minutos (D-01) | HU-02 | 1 |
| REQ-BAK-019 | Evento | Cuando el Administrador crea un usuario, el sistema deberá guardarlo con exactamente uno de los 5 roles y con la contraseña cifrada con BCrypt | HU-03 | 1 |
| REQ-BAK-020 | Evento | Cuando el Administrador desactiva una cuenta, el sistema deberá impedir su inicio de sesión y conservar su historial | HU-03 | 1 |
| REQ-BAK-021 | Estado | Mientras haya una sesión activa, el sistema deberá mostrar en el menú solo las opciones permitidas para el rol del usuario | HU-04 | 1 |
| REQ-BAK-022 | Evento | Cuando el Almacenista registra un insumo con nombre, unidad base, categoría y stock mínimo válidos, el sistema deberá crearlo con stock 0 | HU-05 | 1 |
| REQ-BAK-023 | No deseado | Si el nombre del insumo ya existe, la unidad no es g, ml o unidad, o el stock mínimo es negativo, entonces el sistema deberá rechazar el registro e indicar el motivo | HU-05 | 1 |
| REQ-BAK-024 | Evento | Cuando el Comprador registra un proveedor con nombre, NIT y un dato de contacto, el sistema deberá guardarlo y rechazar un NIT repetido | HU-06 | 1 |
| REQ-BAK-025 | Evento | Cuando un usuario autorizado busca insumos por nombre o categoría, el sistema deberá mostrar las coincidencias parciales sin distinguir mayúsculas | HU-07 | 1 |
| REQ-BAK-026 | Evento | Cuando un usuario autorizado consulta un insumo, el sistema deberá mostrar su stock total, su stock utilizable y sus lotes ordenados por vencimiento | HU-08 | 1 |
| REQ-BAK-027 | Ubicuo | El sistema deberá registrar en la auditoría usuario, fecha-hora, acción y resultado de los inicios y cierres de sesión, los cambios de cuentas, las altas de insumos y proveedores, y las operaciones rechazadas | HU-04 | 1 |
| REQ-BAK-028 | No deseado | Si el Administrador intenta desactivar su propia cuenta o quitarse su propio rol, entonces el sistema deberá rechazarlo | HU-03 | 1 |

### 5.3 Requisitos no funcionales

| ID | Atributo (ISO/IEC 25010) | Requisito |
|----|--------------------------|-----------|
| NFR-BAK-001 | Eficiencia | El sistema deberá responder la consulta de stock de un insumo en menos de 2 segundos, medidos en el navegador |
| NFR-BAK-002 | Seguridad | El sistema deberá almacenar las contraseñas cifradas con BCrypt |
| NFR-BAK-003 | Seguridad | El sistema deberá cerrar la sesión tras 30 minutos de inactividad |
| NFR-BAK-004 | Seguridad | El sistema deberá exigir contraseñas de al menos 8 caracteres con al menos una letra y un número |

---

## 6. Criterios de aceptación (Given/When/Then)

Formato: **Given** contexto · **When** acción · **Then** resultado observable. Los casos de borde se marcan con ◆.

### 6.1 Inventario, producción y ventas (Sprints 2 y 3)

| ID | Given | When | Then |
|----|-------|------|------|
| AC-001.1 | El Panadero está autenticado y hay stock suficiente | Registra una producción | Cada movimiento queda con usuario, fecha-hora y motivo `PRODUCCION` |
| AC-001.2 | El Almacenista registra un ajuste | Deja la causa vacía | El sistema rechaza con «La causa es obligatoria» y no crea el movimiento (RN-05) |
| AC-002.1 | Orden de compra de 5 000 g de harina a 3,20 COP/g; stock actual de harina 0 g | El Almacenista registra la recepción con vencimiento 2027-01-15 | Se crea un lote de 5 000 g con costo 3,20 y vencimiento 2027-01-15, y el stock de harina queda en 5 000 g |
| AC-002.2 ◆ | Orden de compra abierta | El Almacenista registra una recepción con cantidad 0 | El sistema rechaza y no crea lote |
| AC-003.1 | Dos lotes de harina: A vence el 20 de diciembre y B el 10 de diciembre | Se registra una producción que consume harina | El sistema descuenta primero del lote B |
| AC-003.2 | Receta de pan: 1 000 g de harina y 20 g de levadura, con stock suficiente de ambos | El Panadero registra la producción con 10 unidades obtenidas | Se descuentan ambos insumos y el stock del producto sube 10 unidades |
| AC-004.1 | Hay 2 000 g de harina y la receta exige 2 500 g | El Panadero registra la producción | El sistema rechaza con el código `STOCK_INSUFICIENTE` y muestra «Faltan 500 g de harina» |
| AC-004.2 ◆ | Hay 2 500 g de harina y la receta exige 2 500 g | El Panadero registra la producción | El sistema la acepta y el stock de harina queda en 0 g |
| AC-004.3 ◆ | Hay 2 500 g de harina y 0 g de levadura; la receta exige ambos | El Panadero registra la producción | El sistema rechaza por la levadura y la harina sigue en 2 500 g (la operación es atómica) |
| AC-005.1 | Un lote de levadura venció ayer y tiene 500 g | Se registra una producción que usa levadura | El sistema no descuenta del lote vencido e indica que está vencido |
| AC-005.2 ◆ | El único lote de levadura venció ayer y la receta exige 20 g | El Panadero registra la producción | El sistema rechaza con `STOCK_INSUFICIENTE` porque el stock utilizable es 0 |
| AC-006.1 ◆ | Insumo con stock mínimo 5 000 g y stock total 5 000 g | Se consulta la lista de alertas | El insumo aparece en las alertas |
| AC-006.2 ◆ | Insumo con stock mínimo 5 000 g y stock total 5 001 g | Se consulta la lista de alertas | El insumo no aparece |
| AC-007.1 ◆ | Un lote vence dentro de 3 días | Se consulta la lista de alertas por vencimiento | El lote aparece |
| AC-007.2 ◆ | Un lote vence dentro de 4 días | Se consulta la lista de alertas por vencimiento | El lote no aparece |
| AC-009.1 | Existe un movimiento registrado | Cualquier usuario intenta editarlo o eliminarlo (incluso con una petición manual) | El sistema rechaza y el movimiento queda intacto |
| AC-009.2 | Existe un movimiento de salida erróneo de 100 g | El Almacenista lo corrige | Se crea un movimiento contrario de +100 g con causa y el original permanece |
| AC-010.1 | Hay 20 panes en stock de producto terminado | El Cajero registra una venta de 5 | El stock queda en 15 |
| AC-010.2 ◆ | Hay 3 panes en stock | El Cajero intenta vender 4 | El sistema rechaza con `STOCK_INSUFICIENTE` y el stock sigue en 3 |
| AC-011.1 | Producción que consumió 1 000 g de harina a 3,20 COP/g y 20 g de levadura a 40 COP/g, con 10 unidades obtenidas | Se consulta el costo del producto | El costo por unidad es 400 COP ((3 200 + 800) / 10) |
| AC-012.1 | El sistema dice 1 000 g de harina | El Almacenista registra un conteo físico de 950 g con causa «Merma» | Se crea un ajuste de −50 g con la causa y el stock queda en 950 g |
| AC-012.2 ◆ | El sistema dice 1 000 g de harina | El Almacenista registra un conteo físico de 1 000 g | No se crea ajuste |
| AC-012.3 | El sistema dice 1 000 g de harina | El Almacenista registra un conteo de 950 g sin causa | El sistema rechaza (RN-05) |
| AC-013.1 | Un lote de levadura vencido tiene 500 g | El Almacenista lo retira con causa `VENCIMIENTO` | El lote queda en 0 g y se crea el movimiento con la causa |

### 6.2 Acceso, catálogos y consulta (Sprint 1)

| ID | Given | When | Then |
|----|-------|------|------|
| AC-008.1 | El Cajero está autenticado | Intenta registrar un insumo (`POST /insumos/nuevo`) | El sistema responde 403, muestra «Acceso denegado» y guarda un evento `ACCESO_DENEGADO` con usuario, fecha-hora y recurso |
| AC-008.2 | Un visitante sin sesión | Abre `/insumos` | El sistema lo redirige a `/login` |
| AC-015.1 | Existe un Almacenista activo con contraseña válida | Inicia sesión con su correo y contraseña | Entra a `/` y el menú muestra Insumos y no muestra Usuarios ni Proveedores |
| AC-016.1 | Un usuario activo | Inicia sesión con contraseña incorrecta | El sistema muestra «Correo o contraseña incorrectos» y guarda un evento `LOGIN_FALLIDO` |
| AC-016.2 | Una cuenta desactivada con contraseña correcta | Intenta iniciar sesión | El sistema muestra el mismo mensaje «Correo o contraseña incorrectos» |
| AC-016.3 | Un correo que no existe | Intenta iniciar sesión | El sistema muestra el mismo mensaje «Correo o contraseña incorrectos» y guarda un evento `LOGIN_FALLIDO` |
| AC-017.1 | Un usuario con sesión iniciada | Cierra sesión y luego vuelve atrás o abre `/` | El sistema lo redirige a `/login` |
| AC-018.1 | Una cuenta activa | Solicita recuperar la contraseña con su correo | El sistema muestra el mensaje neutro, crea un token que vence a los 30 minutos y escribe el enlace en el log |
| AC-018.2 | Un correo que no existe | Solicita recuperar la contraseña | El sistema muestra el mismo mensaje neutro y no crea token |
| AC-018.3 ◆ | Un token usado o vencido | Intenta restablecer la contraseña con él | El sistema lo rechaza y la contraseña no cambia |
| AC-018.4 | Un token válido | Envía una contraseña nueva válida | La contraseña cambia (BCrypt), el token queda invalidado y se guarda el evento |
| AC-019.1 | El Administrador autenticado | Crea un usuario con rol PANADERO | El usuario existe con ese único rol y su contraseña guardada empieza con `$2` (BCrypt) |
| AC-019.2 | Ya existe un usuario con ese correo | El Administrador crea otro con el mismo correo | El sistema rechaza con `DATO_DUPLICADO` |
| AC-020.1 | Una cuenta activa con historial | El Administrador la desactiva | La cuenta no puede iniciar sesión y su historial permanece |
| AC-021.1 | Un Comprador autenticado | Abre `/` | El menú muestra Proveedores e Insumos y no muestra Usuarios |
| AC-021.2 | El Administrador autenticado | Abre `/` | El menú muestra Usuarios, Proveedores e Insumos (consulta) |
| AC-022.1 | El Almacenista autenticado | Registra «Harina de trigo», unidad `g`, categoría HARINAS, stock mínimo 5 000 | El insumo se crea con stock 0 |
| AC-022.2 ◆ | El Almacenista autenticado | Registra un insumo con stock mínimo −1 | El sistema rechaza con `DATO_INVALIDO` |
| AC-022.3 ◆ | Existe «Harina de trigo» | El Almacenista registra «harina de TRIGO» | El sistema rechaza con `DATO_DUPLICADO` (sin distinguir mayúsculas) |
| AC-023.1 | El Almacenista autenticado | Registra un insumo con unidad `kg` | El sistema rechaza con «Use g, ml o unidad» (RN-01) |
| AC-024.1 | El Comprador autenticado | Registra el proveedor «Distribuidora Ficticia S.A.S.» con NIT 900000001-1 y teléfono | El proveedor se guarda |
| AC-024.2 ◆ | Existe un proveedor con NIT 900000001-1 | El Comprador registra otro con el mismo NIT | El sistema rechaza con `DATO_DUPLICADO` |
| AC-025.1 | Existen «Harina de trigo», «Harina integral» y «Azúcar» | Un usuario autorizado busca «har» | Aparecen las dos harinas y no el azúcar |
| AC-025.2 | Existen insumos de varias categorías | Un usuario autorizado filtra por LACTEOS_Y_HUEVOS sin texto | Aparecen solo los de esa categoría |
| AC-025.3 ◆ | Ningún insumo contiene «zzz» | Un usuario autorizado busca «zzz» | El sistema muestra «No se encontraron insumos» |
| AC-026.1 | Harina con lote A (20 dic, 1 000 g) y lote B (10 dic, 1 500 g), ambos vigentes | Un usuario autorizado consulta la harina | Muestra stock total 2 500 g y el lote B antes que el A |
| AC-026.2 ◆ | Harina con un lote vencido de 500 g y un lote vigente de 1 000 g | Un usuario autorizado consulta la harina | Stock total 1 500 g, stock utilizable 1 000 g y el lote vencido marcado «Vencido» |
| AC-026.3 ◆ | Un insumo sin lotes | Un usuario autorizado lo consulta | Muestra stock 0 y «Sin lotes registrados» |
| AC-027.1 | Un usuario activo | Inicia sesión con éxito | Se guarda un evento `LOGIN_OK` con usuario y fecha-hora |
| AC-028.1 ◆ | El único Administrador autenticado | Intenta desactivar su propia cuenta | El sistema rechaza y la cuenta sigue activa |

### 6.3 No funcionales

| ID | Given | When | Then |
|----|-------|------|------|
| AC-N001.1 | Datos de prueba con 50 insumos y 200 lotes | Se consulta `/insumos/{id}` en el navegador | El tiempo hasta la respuesta completa es menor de 2 s (pestaña Red) |
| AC-N002.1 | Cualquier usuario creado o con contraseña cambiada | Se inspecciona la columna `contrasena_hash` | Empieza con `$2a$`, `$2b$` o `$2y$` y no contiene la contraseña en claro |
| AC-N003.1 | `server.servlet.session.timeout` de la aplicación | Se verifica la configuración | Vale 30 minutos |
| AC-N004.1 ◆ | El Administrador crea un usuario | Usa la contraseña «abc» | El sistema rechaza con `DATO_INVALIDO` |

---

## 7. Fuera del alcance de v1.0

- REQ-BAK-014 y RN-08 (CR-001, Sprint 3).
- Envío real de correos, recuperación por SMS o doble factor.
- Importación masiva de insumos desde hojas de cálculo.

## 8. Historial

| Versión | Fecha | Cambio | Aprobó |
|---------|-------|--------|--------|
| 1.0 borrador | 2026-11-13 | Primera redacción con base en el documento del docente | Pendiente (Puerta 1) |
