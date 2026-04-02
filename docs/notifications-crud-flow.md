# Notifications CRUD - Flujo y estado actual

Documentacion de referencia para la tarea `[Backend] CRUD de Notifications` en OpenRoof.

Estado actual:

- backend implementado
- endpoints operativos
- tests de servicio y controller agregados
- seed SQL de prueba agregado
- colección Postman agregada

## 1. Objetivo del modulo

El modulo de notificaciones sirve para registrar eventos importantes del sistema y mostrarselos al usuario autenticado para que luego pueda:

- ver sus notificaciones
- marcar una notificacion como leida
- marcar todas como leidas
- eliminar una notificacion de su bandeja

La entidad base esta en [`Notification.java`](../src/main/java/com/openroof/openroof/model/notification/Notification.java) y soporta:

- `user`
- `type`
- `title`
- `message`
- `data` (JSONB)
- `actionUrl`
- `readAt`

Ademas:

- usa `soft delete`
- hereda `version` desde `BaseEntity`
- filtra automaticamente registros con `deleted_at IS NULL`

## 2. Flujo general

```mermaid
flowchart TD
    A["Evento del sistema<br/>visit request, contract, alert, etc."] --> B["NotificationService.create(...)"]
    B --> C["Guardar en tabla notifications"]
    C --> D["Usuario consulta GET /notifications/me"]
    D --> E["Backend devuelve lista ordenada por createdAt desc"]
    E --> F["Frontend muestra bandeja o badge"]
    F --> G["Usuario abre una notificacion"]
    G --> H["PUT /notifications/{id}/read"]
    H --> I["readAt = now()"]
    G --> J["Redireccion opcional usando actionUrl"]
```

## 3. Flujo CRUD implementado

```mermaid
flowchart LR
    C["Create"] --> C1["POST /notifications"]
    R["Read"] --> R1["GET /notifications/me"]
    R --> R2["GET /notifications/{id}"]
    U["Update"] --> U1["PUT /notifications/{id}/read"]
    U --> U2["PUT /notifications/me/read-all"]
    D["Delete"] --> D1["DELETE /notifications/{id}"]
```

## 4. Flujo de permisos

```mermaid
flowchart TD
    A["Request autenticada"] --> B{"La notificacion pertenece al usuario?"}
    B -- "Si" --> C["Permitir ver / marcar leida / eliminar"]
    B -- "No" --> D["404 Not Found para recurso ajeno o inexistente"]
    E["Admin"] --> F["Puede crear para otro usuario con userId"]
    G["Usuario comun"] --> H["Solo puede crear para si mismo"]
```

Reglas actuales del backend:

- cualquier usuario autenticado puede crear una notificacion para si mismo
- solo `ADMIN` puede crear una notificacion para otro usuario usando `userId`
- cada usuario solo puede ver, leer o eliminar sus propias notificaciones
- `DELETE` realiza borrado logico

## 5. Tipos de notificaciones sugeridos

El enum actual ya tiene estos tipos:

- `SYSTEM`
- `ALERT`
- `MESSAGE`
- `CONTRACT`
- `VISIT`
- `OFFER`
- `REVIEW`

### Como usarlos en OpenRoof

| Tipo | Cuando se dispara | Ejemplo de titulo | `data` sugerido | `actionUrl` sugerido |
|------|--------------------|-------------------|-----------------|----------------------|
| `SYSTEM` | Eventos generales del sistema | Perfil actualizado | `userId` | `/profile` |
| `ALERT` | Recordatorios o advertencias | Tienes una propiedad pendiente | `propertyId` | `/properties/me` |
| `MESSAGE` | Mensajeria futura | Nuevo mensaje del agente | `conversationId` | `/messages` |
| `CONTRACT` | Contratos o firmas | Contrato listo para revisar | `contractId` | `/contracts/12` |
| `VISIT` | Solicitudes o cambios de visita | Nueva solicitud de visita | `visitRequestId`, `propertyId` | `/visit-requests` |
| `OFFER` | Ofertas economicas | Recibiste una oferta | `offerId`, `propertyId` | `/offers/3` |
| `REVIEW` | Revisiones, feedback o moderacion | Tu propiedad fue revisada | `reviewId`, `propertyId` | `/properties/45` |

## 6. Flujo recomendado por caso de uso

### A. Solicitud de visita

```mermaid
sequenceDiagram
    actor Buyer
    participant VR as VisitRequestService
    participant NS as NotificationService
    participant DB as notifications
    actor Agent

    Buyer->>VR: Crea solicitud de visita
    VR->>NS: create(agentUserId, VISIT, ...)
    NS->>DB: INSERT notification
    Agent->>NS: GET /notifications/me
    NS-->>Agent: Lista con notificacion nueva
    Agent->>NS: PUT /notifications/{id}/read
```

### B. Cambio de estado de propiedad

```mermaid
sequenceDiagram
    participant PS as PropertyService
    participant NS as NotificationService
    participant Owner as Usuario propietario

    PS->>NS: create(ownerId, ALERT, "Estado actualizado")
    NS-->>Owner: Disponible en bandeja
```

### C. Contrato disponible

```mermaid
sequenceDiagram
    participant CS as ContractService
    participant NS as NotificationService
    actor User

    CS->>NS: create(userId, CONTRACT, ...)
    User->>NS: GET /notifications/me
    User->>NS: Abre actionUrl
```

## 7. Endpoints implementados

| Metodo | Endpoint | Uso |
|--------|----------|-----|
| `POST` | `/notifications` | Crear notificacion |
| `GET` | `/notifications/me` | Listar mis notificaciones |
| `GET` | `/notifications/me/unread-count` | Contar no leidas |
| `GET` | `/notifications/{id}` | Ver una notificacion propia |
| `PUT` | `/notifications/{id}/read` | Marcar una como leida |
| `PUT` | `/notifications/me/read-all` | Marcar todas como leidas |
| `DELETE` | `/notifications/{id}` | Eliminar logicamente una notificacion |

### Resumen de respuestas esperadas

- `POST /notifications` -> `201 Created`
- `GET /notifications/me` -> `200 OK`
- `GET /notifications/me/unread-count` -> `200 OK`
- `GET /notifications/{id}` -> `200 OK` o `404`
- `PUT /notifications/{id}/read` -> `200 OK` o `404`
- `PUT /notifications/me/read-all` -> `200 OK`
- `DELETE /notifications/{id}` -> `204 No Content` o `404`

## 8. Modelo mental del frontend futuro

Hoy el frontend no tiene aun un modulo visible de notificaciones, pero este backend ya puede dejar listo el contrato para:

- un badge en navbar
- un dropdown de notificaciones
- una pagina tipo inbox

```mermaid
flowchart TD
    A["Navbar"] --> B["Badge con unreadCount"]
    A --> C["Dropdown ultimas 5"]
    C --> D["Ver todas"]
    D --> E["Pagina /notifications"]
    E --> F["Filtro: todas / no leidas / por tipo"]
```

## 9. Artefactos entregados

Implementacion backend:

- [`NotificationController.java`](../src/main/java/com/openroof/openroof/controller/NotificationController.java)
- [`NotificationService.java`](../src/main/java/com/openroof/openroof/service/NotificationService.java)
- [`NotificationRepository.java`](../src/main/java/com/openroof/openroof/repository/NotificationRepository.java)
- [`CreateNotificationRequest.java`](../src/main/java/com/openroof/openroof/dto/notification/CreateNotificationRequest.java)
- [`NotificationResponse.java`](../src/main/java/com/openroof/openroof/dto/notification/NotificationResponse.java)

Tests:

- [`NotificationServiceTest.java`](../src/test/java/com/openroof/openroof/service/NotificationServiceTest.java)
- [`NotificationControllerTest.java`](../src/test/java/com/openroof/openroof/controller/NotificationControllerTest.java)

Datos de prueba:

- [`004-notifications-test-data.sql`](../seeds/004-notifications-test-data.sql)

Postman:

- [`notifications-crud.postman_collection.json`](./postman/notifications-crud.postman_collection.json)

## 10. Estado de cobertura

La suite actual cubre:

- creacion propia
- creacion para otro usuario por `ADMIN`
- rechazo para usuario comun creando a terceros
- listado de notificaciones
- obtencion por `id`
- marcado individual como leida
- marcado masivo como leidas
- conteo de no leidas
- borrado logico
- `400` de validacion
- `404` en escenarios principales de recurso faltante

No cubre completamente:

- integracion real con base de datos
- JWT end-to-end real
- concurrencia con `@Version`
- tiempo real o websockets

## 11. Nota tecnica importante

La entidad `Notification` usa `@SQLDelete` y hereda `@Version` desde `BaseEntity`.
Por eso el borrado logico debe contemplar `id` y `version`.

Implementacion actual:

```java
@SQLDelete(sql = "UPDATE notifications SET deleted_at = CURRENT_TIMESTAMP, version = version + 1 WHERE id = ? AND version = ?")
```

Esto evita errores de binding al ejecutar `DELETE`.

## 12. Donde ver este flujo visualmente

Opciones simples:

- Abrir este archivo en VS Code o Cursor con Markdown Preview
- Subirlo a GitHub para que renderice los diagramas Mermaid
- Copiar el bloque Mermaid a [Mermaid Live Editor](https://mermaid.live/)

Si queres una presentacion mas "tipo web interna", este mismo documento puede convertirse despues en:

- una pagina en `backend/docs`
- una wiki de GitHub
- una pagina de documentacion en Swagger complementada con ejemplos
