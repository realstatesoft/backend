# Supabase Storage — Guía para desarrolladores

Guía para configurar Supabase Storage y probar la subida de imágenes desde la API.

> **Todos los devs comparten el mismo proyecto Supabase** (DB + Storage).
> No se necesitan credenciales individuales ni buckets separados.

---

## Arquitectura del equipo

```
┌──────────────────────────────────────────────┐
│           Supabase (proyecto compartido)     │
│                                              │
│  ┌─────────────┐    ┌────────────────────┐   │
│  │ PostgreSQL   │    │ Storage            │   │
│  │ (DB única)   │    │ bucket:            │   │
│  │              │    │ openroof-images    │   │
│  └─────────────┘    │                    │   │
│                      │ /properties/...    │   │
│                      │ /avatars/...       │   │
│                      │ /general/...       │   │
│                      └────────────────────┘   │
└──────────────────────────────────────────────┘
        ▲          ▲          ▲          ▲
        │          │          │          │
      Dev 1      Dev 2     Dev 3  ... Dev 10
    (branch A) (branch B) (branch C)
```

### ¿Por qué un solo bucket para todos?

- Los archivos se guardan con nombres **UUID** (`properties/550e8400-e29b-41d4-a716-446655440000.jpg`), así que **no hay colisiones** entre devs.
- La base de datos es compartida, por lo que las imágenes deben estar en el mismo bucket para que las URLs sean consistentes.
- Cada dev trabaja en su **rama de Git** normalmente. El storage no depende de la rama.
- Es un proyecto universitario — mantener 10 buckets separados sería complejidad innecesaria.

---

## 1. Configuración (una sola vez)

### Paso A — Copiar el template

```powershell
Copy-Item src/main/resources/application.yml.example src/main/resources/application.yml
```

### Paso B — No hay Paso B 🎉

El `application.yml.example` ya viene con toda la config de Supabase lista.
Solo copiá y listo. Las credenciales compartidas ya están en el template.

> **IMPORTANTE**: `application.yml` está en `.gitignore` — no se commitea.
> El archivo `application.yml.example` es el template que SÍ se commitea.

---

## 2. Levantar el backend

```powershell
.\mvnw.cmd spring-boot:run
```

Verificar en los logs:
- `Started OpenRoofApplication` → todo OK
- `Liquibase ... UPDATE SUMMARY` → DB conectada
- Error de conexión → verificar que el `application.yml` es correcto

---

## 3. Probar la API de imágenes

### 3.1 Login

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@openroof.com","password":"Test1234!"}'
```

Copiar el `accessToken` de la respuesta.

### 3.2 Subir imagen a una propiedad

```bash
curl -X POST "http://localhost:8080/api/properties/1/images" \
  -H "Authorization: Bearer TU_TOKEN" \
  -F "files=@./foto.jpg" \
  -F "isPrimary=true"
```

### 3.3 Subir imagen genérica (endpoint directo)

```bash
curl -X POST "http://localhost:8080/api/images/upload" \
  -H "Authorization: Bearer TU_TOKEN" \
  -F "file=@./foto.jpg" \
  -F "folder=properties"
```

### 3.4 Ver imágenes de una propiedad

```bash
curl -s "http://localhost:8080/api/properties/1/images" \
  -H "Authorization: Bearer TU_TOKEN"
```

### 3.5 Ver imagen principal

```bash
curl -s "http://localhost:8080/api/properties/1/images/primary" \
  -H "Authorization: Bearer TU_TOKEN"
```

### 3.6 Cambiar imagen principal

```bash
curl -X PATCH "http://localhost:8080/api/properties/1/images/ID_MEDIA/primary" \
  -H "Authorization: Bearer TU_TOKEN"
```

### 3.7 Eliminar imagen

```bash
curl -X DELETE "http://localhost:8080/api/properties/1/images/ID_MEDIA" \
  -H "Authorization: Bearer TU_TOKEN"
```

También podés probar desde **Swagger UI**: `http://localhost:8080/api/swagger-ui/index.html`

---

## 4. Verificar en Supabase Dashboard

1. Ir a [supabase.com/dashboard](https://supabase.com/dashboard)
2. Seleccionar el proyecto **OpenRoof**
3. Menú izquierdo → **Storage**
4. Entrar al bucket `openroof-images`
5. Navegar por las carpetas (`properties/`, `avatars/`, `general/`)

Las imágenes subidas aparecen ahí con su UUID y podés verlas directamente.

**URL pública directa** (el bucket es público):

```
https://etpxedhtursffyoeapqr.supabase.co/storage/v1/object/public/openroof-images/properties/UUID.jpg
```

---

## 5. Errores comunes

| Error | Solución |
|-------|----------|
| `No autorizado` / 401 | Hacer login de nuevo y copiar el token |
| `El archivo está vacío` | Verificar que el archivo existe en la ruta |
| `Tipo de archivo no permitido` | Solo se aceptan `jpeg`, `png`, `webp` |
| `Límite de imágenes excedido` | Máximo 20 por propiedad |
| `Error al subir archivo a Supabase Storage` | Verificar que el bucket `openroof-images` existe en el dashboard |
| Error de conexión a DB | Verificar `application.yml` tiene la config correcta |

---

## 6. Flujo técnico (para entender el código)

```
POST /api/images/upload (o /api/properties/{id}/images)
         │
         ▼
  ImageUploadController / PropertyImageController
         │
         ▼
  StorageService.upload(file, folder)    ← interfaz
         │
         ▼
  SupabaseStorageService                 ← implementación
         │
         ▼
  POST https://{ref}.supabase.co/storage/v1/object/{bucket}/{uuid}.jpg
         │  (usa service-role-key como Bearer token)
         │
         ▼
  Devuelve URL pública + guarda metadatos en tabla `images`
```

**Archivos relevantes:**

| Archivo | Descripción |
|---------|-------------|
| `service/StorageService.java` | Interfaz de storage |
| `service/SupabaseStorageService.java` | Implementación con Supabase Storage API |
| `controller/ImageUploadController.java` | Endpoint `/images/upload` |
| `service/PropertyImageService.java` | Lógica de imágenes de propiedades |

---

## 7. Reglas para el equipo

1. **No crear buckets adicionales** — todos usan `openroof-images`.
2. **No modificar el service-role-key** — es compartido y ya está configurado.
3. **No commitear `application.yml`** — solo el `.example`.
4. **Trabajar en ramas de Git normalmente** — el storage es independiente de las ramas.
5. **Las imágenes de prueba quedan en el bucket** — no pasa nada, se limpian al final del proyecto si es necesario.

---

## Equipo

| Desarrollador | Acceso |
|---------------|--------|
| Ángel Ramírez | Proyecto Supabase compartido |
| Ayumu Sonoda | Proyecto Supabase compartido |
| Carlos Martínez | Proyecto Supabase compartido |
| Enrique Chyrnia | Proyecto Supabase compartido |
| Manuel Ayala G. | Proyecto Supabase compartido |
| Nicolás Ortiz | Proyecto Supabase compartido |
| Otto Enzler | Proyecto Supabase compartido |
| Rodrigo Ibarra | Proyecto Supabase compartido |
| Tania Schulz | Proyecto Supabase compartido |
| Víctor Casco | Proyecto Supabase compartido |
