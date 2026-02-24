# API de Propiedades — OpenRoof

Documentación de los endpoints REST para la gestión de propiedades inmobiliarias.

**Base URL:** `http://localhost:8080/api`  
**Swagger UI:** `http://localhost:8080/api/swagger-ui.html`

---

## Endpoints

| Método | URI | Descripción | Auth |
|--------|-----|-------------|------|
| `POST` | `/properties` | Crear propiedad | Público* |
| `GET` | `/properties/{id}` | Obtener por ID | Público |
| `GET` | `/properties` | Listar todas (paginado) | Público |
| `GET` | `/properties/owner/{ownerId}` | Listar por propietario | Público |
| `GET` | `/properties/search?q=` | Buscar por texto | Público |
| `PUT` | `/properties/{id}` | Actualizar propiedad | Público* |
| `DELETE` | `/properties/{id}` | Eliminar (soft delete) | Público* |
| `PATCH` | `/properties/{id}/status` | Cambiar estado | Público* |

> \* Estos endpoints serán restringidos a usuarios autenticados en una futura versión.

---

## Formato de Respuesta

Todas las respuestas están envueltas en `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Propiedad creada exitosamente",
  "data": { ... },
  "timestamp": "2026-02-22T22:30:00"
}
```

En caso de error:

```json
{
  "success": false,
  "message": "Propiedad no encontrada con ID: 99",
  "data": null,
  "timestamp": "2026-02-22T22:30:00"
}
```

---

## 1. Crear Propiedad

```
POST /properties
Content-Type: application/json
```

### Body (completo)

```json
{
  "title": "Casa moderna en Barrio Herrera",
  "description": "Hermosa casa de 3 plantas con acabados de primera calidad.",
  "propertyType": "HOUSE",
  "category": "SALE",
  "address": "Av. Mcal. López 1234, Asunción",
  "lat": -25.2867,
  "lng": -57.5803,
  "locationId": 1,
  "price": 350000.00,
  "bedrooms": 4,
  "bathrooms": 3.0,
  "halfBathrooms": 1,
  "fullBathrooms": 2,
  "surfaceArea": 450.00,
  "builtArea": 320.00,
  "parkingSpaces": 2,
  "floorsCount": 3,
  "ownerId": 1,
  "constructionYear": 2023,
  "constructionStatus": "NEW",
  "structureMaterial": "Hormigón armado",
  "wallsMaterial": "Ladrillo",
  "floorMaterial": "Porcelanato",
  "roofMaterial": "Losa",
  "waterConnection": "ESSAP",
  "sanitaryInstallation": "Red cloacal",
  "electricityInstallation": "ANDE monofásica",
  "availability": "IMMEDIATE",
  "rooms": [
    {
      "name": "Dormitorio principal",
      "area": 25.5,
      "interiorFeatureIds": [1, 2]
    },
    {
      "name": "Cocina",
      "area": 18.0,
      "interiorFeatureIds": []
    }
  ],
  "media": [
    {
      "type": "PHOTO",
      "url": "https://example.com/casa-frente.jpg",
      "thumbnailUrl": "https://example.com/casa-frente-thumb.jpg",
      "isPrimary": true,
      "orderIndex": 0,
      "title": "Fachada principal"
    }
  ],
  "exteriorFeatureIds": [1, 2, 3]
}
```

### Body (mínimo — solo campos obligatorios)

```json
{
  "title": "Terreno en Luque",
  "propertyType": "LAND",
  "address": "Ruta 2 km 15, Luque",
  "price": 85000.00,
  "ownerId": 1
}
```

### Campos obligatorios

| Campo | Tipo | Validación |
|-------|------|------------|
| `title` | String | No vacío, máx 255 chars |
| `propertyType` | Enum | `HOUSE`, `APARTMENT`, `LAND`, `OFFICE`, `WAREHOUSE`, `FARM` |
| `address` | String | No vacío, máx 500 chars |
| `price` | BigDecimal | Mayor a 0 |
| `ownerId` | Long | Debe existir en la tabla `users` |

### Respuesta: `201 Created`

---

## 2. Obtener Propiedad por ID

```
GET /properties/{id}
```

### Respuesta: `200 OK`

Devuelve un `PropertyResponse` con todos los campos incluyendo rooms, media, features, datos de construcción y servicios.

### Error: `404 Not Found` si no existe.

---

## 3. Listar Propiedades (Paginado)

```
GET /properties?page=0&size=10&sort=createdAt,desc
```

### Parámetros de paginación

| Parámetro | Default | Descripción |
|-----------|---------|-------------|
| `page` | `0` | Número de página (0-indexed) |
| `size` | `10` | Elementos por página |
| `sort` | `createdAt,desc` | Campo y dirección de ordenamiento |

### Respuesta: `200 OK`

Devuelve `Page<PropertySummaryResponse>` con campos resumidos (id, title, price, type, address, imagen principal, bedrooms, bathrooms, surfaceArea, status, location).

---

## 4. Listar por Propietario

```
GET /properties/owner/{ownerId}?page=0&size=10
```

Misma paginación y formato de respuesta que el listado general.

---

## 5. Buscar Propiedades

```
GET /properties/search?q=casa&page=0&size=10
```

Busca coincidencias en `title` y `description` (case-insensitive). Si `q` está vacío, devuelve todas.

---

## 6. Actualizar Propiedad

```
PUT /properties/{id}
Content-Type: application/json
```

**Actualización parcial**: solo se modifican los campos enviados. Los campos ausentes (`null`) se mantienen sin cambios.

```json
{
  "title": "Casa RENOVADA en Barrio Herrera",
  "price": 375000.00,
  "bedrooms": 5,
  "visibility": "PUBLIC"
}
```

> **Nota sobre colecciones**: si se envía `rooms`, `media` o `exteriorFeatureIds`, se **reemplazan completamente** (no se agregan, se sustituyen).

### Respuesta: `200 OK`

---

## 7. Eliminar Propiedad (Soft Delete)

```
DELETE /properties/{id}
```

No elimina el registro de la base de datos, solo marca el campo `deletedAt` con la fecha actual.

### Respuesta: `204 No Content`

---

## 8. Cambiar Estado

```
PATCH /properties/{id}/status
Content-Type: application/json
```

```json
{
  "newStatus": "APPROVED"
}
```

### Máquina de Estados

El cambio de estado sigue transiciones válidas. Si se intenta una transición inválida, devuelve `400 Bad Request`.

```
PENDING → APPROVED → PUBLISHED → SOLD
                               → RENTED
                               → ARCHIVED
```

| Estado | Descripción | Terminal |
|--------|-------------|---------|
| `PENDING` | Pendiente de revisión | No |
| `APPROVED` | Aprobada por admin | No |
| `PUBLISHED` | Publicada y visible | No |
| `SOLD` | Vendida | Sí |
| `RENTED` | Alquilada | Sí |
| `ARCHIVED` | Archivada | Sí |

> Al pasar a `PUBLISHED`, se registra automáticamente la fecha en `publishedAt`.

---

## Enums Disponibles

### PropertyType
`HOUSE` · `APARTMENT` · `LAND` · `OFFICE` · `WAREHOUSE` · `FARM`

### PropertyCategory
`SALE` · `RENT` · `SALE_OR_RENT`

### PropertyStatus
`PENDING` · `APPROVED` · `PUBLISHED` · `SOLD` · `RENTED` · `ARCHIVED`

### Visibility
`PRIVATE` · `PUBLIC` · `HIDDEN`

### Availability
`IMMEDIATE` · `IN_30_DAYS` · `IN_60_DAYS` · `TO_NEGOTIATE`

### ConstructionStatus
`NEW` · `USED` · `UNDER_CONSTRUCTION` · `TO_RENOVATE`

### MediaType
`PHOTO` · `FLOOR_PLAN` · `VIDEO` · `VIRTUAL_TOUR_3D`

### ExteriorFeatureCategory
`AMENITY` · `SECURITY` · `OUTDOOR` · `OTHER`

### InteriorFeatureCategory
`APPLIANCE` · `FURNITURE` · `FINISH` · `OTHER`

---

## Códigos de Error

| Código | Causa |
|--------|-------|
| `400` | Validación fallida, transición de estado inválida, o features no encontradas |
| `404` | Propiedad, usuario, agente, ubicación o feature no encontrado |
| `500` | Error interno del servidor |
