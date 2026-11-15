# BakerStock — Tareas del Sprint 1

**Sprint 1:** 11 al 17 de noviembre de 2026 · **Puntos:** 13 (HU-01 a HU-08)
**Estado:** BORRADOR para la Puerta 3 · **Fecha:** 2026-11-13

**Sprint Goal:** Al terminar el sprint, la especificación v1.0 de BakerStock está aprobada con su plan y tareas, y cada uno de los 5 roles puede iniciar sesión, recuperar su contraseña y ver solo sus opciones, mientras el Almacenista registra insumos y el Comprador registra proveedores en el entorno de pruebas.

**Tablero Kanban:** Por hacer · En curso (máx. 2) · En revisión (máx. 1) · Bloqueado · Hecho.
**Regla:** quien hace la tarea nunca es quien la valida. El código puede empezar tras la Puerta 0, pero ninguna historia se da por terminada hasta que la especificación tenga su aprobación en la Puerta 1.

## Tarjetas (16)

| ID | Tarea | HU / REQ | Hace | Valida | Depende de | Horas |
|----|-------|----------|------|--------|------------|:-----:|
| S1-01 | Sprint Planning (2 h) y tablero Kanban con las 16 tarjetas | — | Ana (SM) | David | — | 2 |
| S1-02 | Constitución, `AGENTS.md`, contexto, actores y reglas de la especificación (**Puerta 0**) | Spec §0-4 | Ana | David | S1-01 | 4 |
| S1-03 | Requisitos EARS (mínimo 15) y marcadores de decisiones D-01…D-08 | REQ-BAK-001…028, NFR | Ana | David | S1-02 | 3 |
| S1-04 | Criterios Given/When/Then (mínimo 30, con casos de borde) y **Puerta 1** con el PO | AC-xxx | Ana | David | S1-03 | 3 |
| S1-05 | `plan.md` y ADR-001 a ADR-005 (**Puerta 2**) | Plan | David | Ana | S1-04 | 3 |
| S1-06 | `tasks.md`, tablero y matriz de trazabilidad inicial (**Puerta 3**) | Tasks | Ana | David | S1-05 | 1 |
| S1-07 | Esqueleto Spring Boot, perfiles `dev`/`cloud`/`test`, `DataSeeder`, `Dockerfile` y esquema MySQL de pruebas | Plan §1-3, 8 | David | Ana | S1-02 | 4 |
| S1-08 | HU-01: inicio y cierre de sesión con Spring Security (BCrypt, mensaje único, auditoría) | REQ-015, 016, 017, 027; NFR-002, 003 | David | Ana | S1-07 | 4 |
| S1-09 | HU-04: menú por rol, 403 y auditoría de accesos denegados | REQ-008, 021, 027 | Ana | David | S1-08 | 2 |
| S1-10 | HU-03: crear usuarios con rol, desactivar/activar cuentas y protección del propio Administrador | REQ-019, 020, 028; NFR-004 | Ana | David | S1-08 | 3 |
| S1-11 | HU-02: recuperación de contraseña con token de un solo uso | REQ-018 | David | Ana | S1-08 | 3 |
| S1-12 | HU-05: registrar insumos con unidad, categoría y stock mínimo | REQ-022, 023 | David | Ana | S1-09 | 3 |
| S1-13 | HU-06: registrar y consultar proveedores | REQ-024 | Ana | David | S1-09 | 3 |
| S1-14 | HU-07: buscar insumos por nombre o categoría | REQ-025 | Ana | David | S1-12 | 3 |
| S1-15 | HU-08: consultar stock de un insumo y sus lotes | REQ-026; NFR-001 | David | Ana | S1-12 | 3 |
| S1-16 | Sprint Review (1 h), Retrospective (45 min), actas y decisión del PO | — | Ana (SM) | David | S1-15 | 2 |

### Horas por persona (coinciden con la Tabla 15 del documento)

| Persona | Tarjetas | Horas |
|---------|----------|:-----:|
| Ana Gómez (Scrum Master) | S1-01, S1-16 | 4 |
| Ana Gómez (Developer) | S1-02, 03, 04, 06, 09, 10, 13, 14 | 22 |
| David Patiño (Developer) | S1-05, 07, 08, 11, 12, 15 | 20 |

## Lista de verificación por tarjeta

**S1-02**
- [ ] `constitution.md` y `AGENTS.md` en la raíz del repositorio
- [ ] Spec §0-4 completas (contexto, actores, glosario, RN)
- [ ] David lee y comenta (Puerta 0 con al menos un comentario por escrito)

**S1-03 / S1-04**
- [ ] ≥ 15 requisitos EARS con patrón correcto y sin palabras vagas
- [ ] ≥ 30 criterios Given/When/Then y al menos un caso de borde por regla RN-03
- [ ] Cada REQ tiene al menos un AC (script de cruce)
- [ ] Decisiones D-01…D-08 llevadas al PO y resueltas; ningún `[NECESITA ACLARACIÓN]` abierto
- [ ] Spec aprobada por el PO (Puerta 1) y registrada en el historial

**S1-05**
- [ ] Plan coherente con la spec aprobada (rutas, roles, tablas)
- [ ] ADR-001 a ADR-005 revisados; ADR-004 confirmado por el PO
- [ ] Ana lee el diff completo contra el alcance (Puerta 2)

**S1-06**
- [ ] 16 tarjetas con responsable, validador, dependencias, horas y checklist
- [ ] `docs/trazabilidad.md` con una fila por requisito
- [ ] Puerta 3 revisada

**S1-07**
- [ ] `./mvnw test` pasa con un contexto vacío
- [ ] Arranca con perfil `dev` (MySQL local); `./mvnw test` usa el esquema `bakerstock_test`
- [ ] `DataSeeder` crea 5 usuarios, insumos y lotes de demostración sin contraseña en el repositorio
- [ ] Imagen Docker construida (el despliegue en Render espera a tener MySQL en línea, ADR-005)
- [ ] PR revisado por Ana, entrada en la bitácora de IA

**S1-08 a S1-15 (patrón común)**
- [ ] Pruebas de los AC escritas **antes** del código y vistas en rojo
- [ ] Pruebas en verde, toda la suite en verde
- [ ] Diff sin funcionalidad ni dependencias no pedidas ni capa `service`
- [ ] PR enlaza la tarea y los REQ, revisado por la otra persona
- [ ] Matriz de trazabilidad y bitácora de IA actualizadas
- [ ] Desplegado en pruebas

## Calendario sugerido (4 días y medio de sprint)

| Día | Plan |
|-----|------|
| Vie 13 | S1-02, S1-03 y S1-04 (borrador). Ana y David revisan la spec y preparan las 8 decisiones para el PO |
| Sáb 14 | Ajustes, **Puerta 1** con el PO si está disponible, S1-05 y S1-06. David arranca S1-07 en paralelo (no depende de la aprobación del contenido funcional) |
| Dom 15 | S1-07 y S1-08 (David); S1-09 y S1-10 (Ana) |
| Lun 16 | S1-11, S1-12 (David); S1-13 (Ana). Verifica si el lunes 16 es festivo (el 11 de noviembre se traslada al lunes) y ajusta |
| Mar 17 | S1-14 y S1-15, revisión cruzada de PR, despliegue de prueba, S1-16 (Review y Retrospective) |

Si el 15 se ve apretado, las tarjetas S1-14 y S1-15 (HU-07 y HU-08) son las primeras candidatas a pasar al Sprint 2: se decide con el PO en la Review, nunca a mitad del sprint sin pasar por el Product Backlog.

## Puertas SDD del Sprint 1

| Puerta | Revisa | Quién aprueba | Duración |
|--------|--------|---------------|----------|
| 0 | Constitución y `AGENTS.md` | Ana y David | 20 min |
| 1 | Especificación v1.0 y decisiones D-01…D-08 | Product Owner | 45 min |
| 2 | Plan técnico y ADR | Ana y David (ADR-004 con el PO) | 30 min |
| 3 | Tareas y trazabilidad | Ana y David | 20 min |
