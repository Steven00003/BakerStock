# BakerStock

Sistema web de inventario para una panadería (proyecto académico UTS). Java 21, Spring Boot 3.5.x, Spring Security, Spring Data JPA y Thymeleaf. La base de datos es MySQL en todos los perfiles.

## Requisitos

- Java 21 (o superior)
- MySQL local (con las dos bases de datos indicadas abajo)
- No se necesita Maven instalado: el proyecto incluye Maven Wrapper (`mvnw`)

## Crear las bases de datos

```sql
CREATE DATABASE bakerstock_db CHARACTER SET utf8mb4;
CREATE DATABASE bakerstock_test CHARACTER SET utf8mb4;
```

## Variables de entorno

| Variable | Descripción | Ejemplo local |
|----------|-------------|---------------|
| `DB_URL` | URL JDBC de la base de desarrollo (`dev`) | `jdbc:mysql://localhost:3306/bakerstock_db` |
| `DB_USER` | Usuario de la base de datos; por defecto `root` | `root` |
| `DB_PASSWORD` | Contraseña de la base de datos; por defecto vacía. Con el root de XAMPP sin contraseña **no hace falta definirla**. En `cloud` es obligatoria | |
| `DB_TEST_URL` | URL JDBC de la base de pruebas (`test`); si falta se usa `jdbc:mysql://localhost:3306/bakerstock_test` | |
| `BAKERSTOCK_SEED_PASSWORD` | (opcional) contraseña de los usuarios sembrados; si falta se genera una aleatoria y se imprime una sola vez en el log | |

Ninguna de estas variables se guarda en el repositorio.

## Perfiles

| Perfil | Base de datos | Uso |
|--------|---------------|-----|
| `dev` | MySQL `bakerstock_db` (`DB_URL`, `DB_USER`, `DB_PASSWORD`) | Desarrollo; `ddl-auto=update` |
| `cloud` | MySQL con `DB_URL`, `DB_USER`, `DB_PASSWORD` (obligatorias) y puerto desde `PORT` | Despliegue |
| `test` | MySQL `bakerstock_test` (`DB_TEST_URL`), mismo usuario y contraseña que `dev`; `ddl-auto=create-drop` | Pruebas automáticas |

## Ejecutar en desarrollo (perfil `dev`)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

En Windows (PowerShell):

```powershell
# Con el root de XAMPP sin contraseña basta con esta variable (DB_USER=root y DB_PASSWORD vacía por defecto)
$env:DB_URL = "jdbc:mysql://localhost:3306/bakerstock_db"
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Al arrancar, Hibernate crea las tablas en `bakerstock_db` (`ddl-auto=update`).

## Pruebas

```bash
./mvnw test                          # toda la suite
./mvnw test -Dtest=AuthLoginTest     # una clase
```

Las pruebas usan el perfil `test` contra MySQL (`bakerstock_test`, esquema creado y destruido en cada ejecución con `ddl-auto=create-drop`). Con el root de XAMPP sin contraseña basta con tener las bases creadas; no hace falta definir `DB_USER` ni `DB_PASSWORD` (por defecto `root` / vacía).

Para los futuros `@DataJpaTest`, anotar cada clase con `@AutoConfigureTestDatabase(replace = Replace.NONE)` (import `org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase`) para que Spring no sustituya el DataSource de MySQL por uno embebido.

## Docker

```bash
docker build -t bakerstock .
docker run -p 8080:8080 -e DB_URL=... -e DB_USER=... -e DB_PASSWORD=... bakerstock
```

La imagen ejecuta con el perfil `cloud` por defecto; necesita `DB_URL`, `DB_USER` y `DB_PASSWORD` en el entorno del contenedor.
