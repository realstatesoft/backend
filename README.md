# OpenRoof — Backend

API REST para la plataforma inmobiliaria **OpenRoof**.

## Tech Stack

| Tecnología | Versión |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.3 |
| Spring Security | 7.x + JWT |
| PostgreSQL | 16+ |
| Liquibase | 5.x |
| Maven | Wrapper incluido |
| Springdoc OpenAPI | 2.8.6 |
| Lombok | latest |

## Requisitos previos

- **Java 25** — [Descargar](https://jdk.java.net/25/)
- **PostgreSQL 16+** — instalado localmente **o** Docker
- **Docker** (opcional) — solo si usás `docker-compose` para la BD

> **Nota:** No necesitás instalar Maven. El proyecto incluye `mvnw`/`mvnw.cmd` (Maven Wrapper).

## Inicio rápido

### 1. Clonar el repositorio

```bash
git clone <url-del-repo>
cd OpenRoof
```

### 2. Levantar la base de datos

**Opción A — Docker (recomendado):**

```bash
docker compose up -d
```

Esto crea y levanta un contenedor PostgreSQL con la BD `openroof_db` lista para usar.

**Opción B — PostgreSQL local:**

Conectate como superusuario y ejecutá:

```sql
CREATE USER openroof WITH PASSWORD 'openroof123';
CREATE DATABASE openroof_db OWNER openroof;
```

### 3. Ejecutar la aplicación

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
.\mvnw.cmd spring-boot:run
```

La app arranca en `http://localhost:8080/api`.

> Si el puerto 8080 está ocupado, usá otro:
> ```bash
> # Linux / macOS
> SERVER_PORT=8082 ./mvnw spring-boot:run
>
> # Windows PowerShell
> $env:SERVER_PORT="8082"; .\mvnw.cmd spring-boot:run
> ```

### 4. Verificar que funciona

```bash
curl http://localhost:8080/api/health
```

Debería devolver:

```json
{
  "status": "UP",
  "message": "OpenRoof API is running"
}
```

## Documentación de la API

Una vez levantada la app:

| Recurso | URL |
|---|---|
| Swagger UI | [http://localhost:8080/api/swagger-ui.html](http://localhost:8080/api/swagger-ui.html) |
| OpenAPI JSON | [http://localhost:8080/api/v3/api-docs](http://localhost:8080/api/v3/api-docs) |

## Variables de entorno

Todas tienen valores por defecto para desarrollo local. Podés sobreescribirlas según tu entorno:

| Variable | Default | Descripción |
|---|---|---|
| `DB_HOST` | `localhost` | Host de PostgreSQL |
| `DB_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_NAME` | `openroof_db` | Nombre de la base de datos |
| `DB_USERNAME` | `openroof` | Usuario de la BD |
| `DB_PASSWORD` | `openroof123` | Contraseña de la BD |
| `SERVER_PORT` | `8080` | Puerto del servidor |
| `JWT_SECRET` | (base64 dev key) | Clave secreta para tokens JWT |

## Estructura del proyecto

```
src/main/java/com/openroof/openroof/
├── common/                  # BaseEntity, embeddables, interfaces
│   └── embeddable/          # GeoLocation, MoneyRange, etc.
├── config/                  # OpenAPI, seguridad
├── exception/               # Manejo global de errores
├── model/                   # Entidades JPA
│   ├── admin/               # AdminReview, PropertyFlag, AuditLog
│   ├── agent/               # AgentProfile, AgentClient, etc.
│   ├── contract/            # Contract, ContractTemplate, Signature
│   ├── enums/               # Enums con state machines
│   ├── interaction/         # Favorite, Visit, Offer, AgentAgenda
│   ├── lead/                # Lead, LeadStatus, LeadInteraction
│   ├── messaging/           # Message
│   ├── notification/        # Notification
│   ├── property/            # Property, Location, Features, etc.
│   ├── reservation/         # Reservation, DocumentUploaded
│   ├── search/              # SearchPreference, Alert
│   └── user/                # User, UserSession
└── security/                # JWT, filtros, UserDetailsService

src/main/resources/
├── application.yml
└── db/changelog/            # Liquibase changelogs
    ├── db.changelog-master.yaml
    └── changelogs/
        └── 001-initial-schema.yaml
```

## Migraciones de base de datos (Liquibase)

Las migraciones se ejecutan automáticamente al iniciar la app. Los changelogs están en `src/main/resources/db/changelog/`.

**Generar un changelog desde la BD actual:**

```bash
.\mvnw.cmd liquibase:generateChangeLog
```

**Para agregar cambios futuros:**

1. Crear un nuevo archivo `changelogs/002-descripcion.yaml`
2. Agregarlo en `db.changelog-master.yaml`:

```yaml
databaseChangeLog:
  - include:
      file: changelogs/001-initial-schema.yaml
      relativeToChangelogFile: true
  - include:
      file: changelogs/002-descripcion.yaml
      relativeToChangelogFile: true
```

## Compilar y ejecutar tests

```bash
# Compilar
.\mvnw.cmd compile

# Ejecutar tests
.\mvnw.cmd test

# Empaquetar JAR
.\mvnw.cmd package -DskipTests
```