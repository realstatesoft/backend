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

## Deploy en Render (producción con Docker)

### Prerequisitos
- Cuenta en [Render](https://render.com) (plan free disponible)
- Repositorio en GitHub/GitLab conectado a Render
- Base de datos en **Supabase** (ya configurada en este proyecto)

### Paso 1 — Subir cambios al repo

Asegurate de que los siguientes archivos estén commiteados:

```
Dockerfile
.dockerignore
render.yaml
src/main/resources/application-prod.yml
```

```bash
git add Dockerfile .dockerignore render.yaml src/main/resources/application-prod.yml
git commit -m "feat: add Render deployment config"
git push origin main
```

### Paso 2 — Crear el servicio en Render

1. Ir a [dashboard.render.com](https://dashboard.render.com) → **New** → **Web Service**
2. Conectar tu repositorio de GitHub/GitLab
3. Render detectará el `render.yaml` automáticamente (o configurar manualmente):
   - **Environment:** `Docker`
   - **Branch:** `main` (producción) o `test` (staging)
   - **Dockerfile Path:** `./Dockerfile`

### Paso 3 — Configurar variables de entorno

En **Render Dashboard → tu servicio → Environment**, agregar:

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | `jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:6543/postgres?prepareThreshold=0` |
| `DB_USERNAME` | *(usuario de Supabase)* |
| `DB_PASSWORD` | *(contraseña de Supabase)* |
| `JWT_SECRET` | *(generado automáticamente por Render)* |
| `SUPABASE_URL` | `https://<tu-proyecto>.supabase.co` |
| `SUPABASE_SERVICE_ROLE_KEY` | *(service role key de Supabase)* |

> Los valores de `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` los encontrás en:
> **Supabase Dashboard → Project Settings → Database → Connection string**

### Paso 4 — Deploy

Render hace el build automáticamente al hacer push a la rama configurada.

Para forzar un redeploy manual:

```bash
# Render Dashboard → Manual Deploy → Deploy latest commit
```

O via Render CLI:

```bash
render deploy --service openroof-backend
```

### URLs del servicio desplegado

| Recurso | URL |
|---|---|
| API Base | `https://openroof-backend.onrender.com/api` |
| Swagger UI | `https://openroof-backend.onrender.com/api/swagger-ui.html` |
| OpenAPI JSON | `https://openroof-backend.onrender.com/api/v3/api-docs` |

> El nombre exacto depende del nombre que elijas en Render.

### Notas sobre el plan free de Render

- El servicio se **suspende tras 15 min de inactividad** (cold start ~30s al primer request).
- Para evitarlo: hacer upgrade a **Starter plan** (~$7/mes).
- La base de datos Supabase (free tier) tiene límite de 500 MB y 2 conexiones activas simultáneas.

### Construcción local de la imagen (verificación)

```bash
# Construir imagen
docker build -t openroof-backend .

# Correr localmente con perfil prod
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://..." \
  -e DB_USERNAME="..." \
  -e DB_PASSWORD="..." \
  -e JWT_SECRET="mi-secreto-local" \
  -e SUPABASE_URL="https://..." \
  -e SUPABASE_SERVICE_ROLE_KEY="..." \
  openroof-backend
```

---

## Despliegue y entornos

### Ramas y entornos recomendados
- `dev` : desarrollo diario y ramas feature (PRs). Aquí se valida código antes de pasar a staging.
- `test` : entorno de **staging/testing**. Despliegue automático desde `test` para QA y demos.
- `main` : entorno de **producción**. Despliegues controlados y aprobados.

Mapea cada rama a su entorno (dev → preview, test → staging, main → production) y usa pipelines (GitHub Actions / GitLab CI) para build/test/deploy.

### Uso de la base de datos desplegada (managed PostgreSQL)
- Recomiendo usar una instancia administrada (AWS RDS / Azure Database for PostgreSQL / Google Cloud SQL) separada para `test` y `production`.
- Configura las credenciales en tu CI/CD como secretos (GitHub Secrets, ADO variable groups). No guardes contraseñas en el repo.
- Variables de entorno que deben apuntar a la BD desplegada: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.
- Ejecutá migraciones con Liquibase en un paso controlado del pipeline antes de desplegar la nueva versión en `test` y `main`.

Ejemplo (variables de entorno en el servidor o en el contenedor):

```powershell
$env:DB_HOST="mi-db-host.prod.example"
$env:DB_PORT="5432"
$env:DB_NAME="openroof_db"
$env:DB_USERNAME="openroof"
$env:DB_PASSWORD="<SECRET_FROM_VAULT>"
```

### Recomendaciones de despliegue — Backend (Spring Boot)
- Empaquetá la aplicación como JAR o como imagen Docker (recomendado). Usar `./mvnw package` o `docker build`.
- Opciones de hosting según escala:
  - Pequeño/rápido: Azure App Service / Heroku / AWS Elastic Beanstalk (son sencillos para JARs).
  - Escalado y control: Kubernetes (EKS/AKS/GKE) con imágenes Docker y despliegue mediante Helm/ArgoCD.
  - Contenedores gestionados: Azure Container Apps, AWS Fargate.
- Registry: empujá la imagen a GitHub Container Registry, Docker Hub o ACR.
- Pipeline (resumen): Build → Tests → Build image → Push image → Run migrations (Liquibase) → Deploy to env.

### Recomendaciones de despliegue — Frontend (React + Vite)
- Generá el bundle estático con `npm run build` (o `yarn build`).
- Hosting recomendado para assets estáticos:
  - Vercel / Netlify (rápido y con previews automáticos por PR).
  - AWS S3 + CloudFront, o Azure Static Web Apps para producción con CDN.
- Para integración: cada PR puede desplegar una preview en Vercel/Netlify automáticamente.

### Buenas prácticas finales
- Mantener credenciales en un vault (Secrets, Key Vault, Parameter Store).
- Separar BDs entre `test` y `prod` y usar backups automáticos en producción.
- Tener un paso de smoke tests en `test` antes de promover a `main`.
- Registrar logs y métricas (Prometheus/Grafana o un servicio gestionado) y alertas para producción.

---
