# AGENTS.md — Instrucciones para la IA en BakerStock

Lee primero `constitution.md`. Este archivo dice **cómo** trabajar; la especificación dice **qué** construir.

## Qué es el proyecto

BakerStock es un sistema web de inventario para una panadería: insumos, proveedores, compras con lotes y vencimiento (FEFO), recetas, producción, kardex, conteo físico, ventas, alertas, costos y auditoría. Cinco roles: ADMINISTRADOR, COMPRADOR, ALMACENISTA, PANADERO, CAJERO. Proyecto académico (UTS, Programación en Java), datos ficticios.

## Fuentes de verdad (en este orden)

1. `specs/001-BakerStock/spec.md` — requisitos `REQ-BAK-xxx`, reglas `RN-xx`, criterios `AC-xxx.x`.
2. `specs/001-BakerStock/plan.md` — arquitectura, modelo de datos, rutas, seguridad.
3. `specs/001-BakerStock/tasks.md` — la tarea que te piden ejecutar.
4. `docs/adr/` — decisiones de arquitectura.

Si algo contradice a la especificación, gana la especificación. Si algo no está en la especificación, **no lo inventes**: escribe `[NECESITA ACLARACIÓN]` en tu respuesta, detente y pregunta.

## Pila tecnológica (no añadir otras sin pedirlo)

- Java 21, Maven, Spring Boot 3.5.x (rama 3).
- Spring Web MVC, Thymeleaf, Bootstrap (CDN), `thymeleaf-extras-springsecurity6`.
- Spring Data JPA y **MySQL en todos los perfiles** (`dev`, `cloud` y `test`). **Prohibido H2** u otra base embebida.
- Spring Security con BCrypt.
- Bean Validation (`spring-boot-starter-validation`).
- Pruebas: JUnit 5, Mockito, Spring Boot Test, `spring-security-test`, contra un esquema MySQL aparte (`bakerstock_test`). Las pruebas `@DataJpaTest` llevan `@AutoConfigureTestDatabase(replace = Replace.NONE)` para no sustituir MySQL.
- Despliegue: Docker y Render.
- **Prohibido sin autorización del Product Owner:** Lombok, MapStruct, Flyway/Liquibase, bibliotecas de correo, JWT, cualquier dependencia nueva.

## Estructura de paquetes (base `com.bakerstock`)

```
com.bakerstock
├── dominio        // entidades JPA + enums + reglas del dominio (métodos de las entidades)
├── repositorio    // interfaces Spring Data JPA (únicas que tocan la base de datos)
├── web            // controladores MVC, un archivo por módulo, + DTOs de formulario
├── seguridad      // SecurityConfig, UserDetailsService, listeners de eventos de seguridad
└── config         // perfiles, DataSeeder (solo dev/cloud), propiedades
```

Plantillas en `src/main/resources/templates/<modulo>/`. Pruebas en `src/test/java` con el mismo árbol de paquetes.

## Reglas de arquitectura (ADR-004)

- **No crear paquete ni clases `service`/`servicio`, ni `@Service`.** Los controladores inyectan los repositorios y los usan directamente.
- Las reglas de negocio van en métodos de las entidades (por ejemplo `Lote.estaVencido(hoy)`, `Insumo.stockTotal()`) y en validaciones Bean Validation.
- Las operaciones de varios pasos (Sprint 2: producción, recepción, conteo) se anotan con `@Transactional` en el método del controlador o en un fragmento de repositorio propio, nunca en una clase de servicio.
- Inyección por constructor. Sin `@Autowired` en campos.
- La auditoría se escribe con `EventoAuditoriaRepository` desde los controladores y desde `@EventListener` de eventos de Spring Security. No se crea una clase de servicio para esto.

## Reglas de trabajo

1. **Pruebas primero.** Para cada tarea: (a) escribe las pruebas derivadas de los `AC-xxx.x` indicados, (b) ejecútalas y confirma que fallan, (c) implementa lo mínimo, (d) ejecuta toda la suite y confirma que todo está en verde. Informa el resultado de cada paso.
2. **Alcance cerrado.** Implementa solo la tarea pedida. No refactorices ni "mejores" archivos de otras tareas. Si ves un problema fuera de alcance, repórtalo al final en lugar de corregirlo.
3. **No modificar `specs/`, `constitution.md` ni `docs/adr/`.** Si ves un error o una mejora, propónla al final de tu respuesta para el Product Owner.
4. **Diff pequeño y revisable.** Una tarea por rama (`s1-08-login`) y por pull request. Máximo lo necesario; sin código muerto ni comentarios decorativos.
5. **Commits:** `S1-08: HU-01 inicio de sesión (REQ-BAK-015, REQ-BAK-016)`. Cada pull request enlaza la tarea y los identificadores de requisito, y se revisa por la otra persona del equipo.
6. **Seguridad:** contraseñas solo con BCrypt; CSRF activado; nada de secretos, URLs de bases de datos reales ni contraseñas en el repositorio (usa variables de entorno); los mensajes de error de inicio de sesión no revelan si el correo existe.
7. **Mensajes y textos de interfaz en español.** Nombres de dominio en español (`Insumo`, `Lote`, `Proveedor`); sufijos técnicos en inglés estándar (`Repository`, `Controller`).
8. **Códigos de error de negocio:** `STOCK_INSUFICIENTE`, `LOTE_VENCIDO`, `ACCESO_DENEGADO`, `DATO_DUPLICADO`, `DATO_INVALIDO`. Las respuestas de rechazo indican la causa y, si aplica, el faltante.
9. **Cantidades** siempre en la unidad base del insumo (g, ml o unidad) usando `BigDecimal` (nunca `double`).
10. **Fechas:** `LocalDate` y `LocalDateTime` con zona `America/Bogota`; el vencimiento se compara contra la fecha actual del servidor.
11. Enums: nunca columnas ENUM nativas de MySQL. Usa @Enumerated(EnumType.STRING) con @JdbcTypeCode(SqlTypes.VARCHAR) y @Column(length = 40), para que añadir valores no exija ALTER TABLE.

## Comandos

```bash
./mvnw test                         # toda la suite
./mvnw test -Dtest=AuthLoginTest    # una clase
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Variables de entorno (nunca en el repositorio): `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_TEST_URL` (esquema `bakerstock_test`, mismo usuario), `BAKERSTOCK_SEED_PASSWORD` (opcional; si falta, el seeder genera una contraseña aleatoria y la imprime una sola vez en el log).

## Definition of Done (resumen; completa en la especificación y el documento del docente)

Especificación aprobada sin marcadores · cada AC con prueba automatizada (incluidos casos de borde) · pruebas escritas antes del código y en verde · pull request revisado por la otra persona · sin funcionalidad ni dependencias no pedidas · movimientos de inventario auditados · contraseñas cifradas y sin secretos · matriz de trazabilidad y bitácora de IA actualizadas · desplegado en pruebas.

## Al terminar cada tarea, responde con

1. Archivos creados o modificados.
2. Qué AC cubren las pruebas y su resultado (rojo antes, verde después).
3. Decisiones que tomaste y por qué.
4. Cosas fuera de alcance que viste (sin haberlas tocado).
5. Marcadores `[NECESITA ACLARACIÓN]` si los hay.
