# Implementación: integración de pasarela de pagos (Bancard vPOS / Mock)

> Fecha: 2026-06-10 · Plan base: `docs/payments-api.md` §9.3 · Spec: *eCommerce Bancard Compra Simple v1.23* (verificada contra el PDF oficial)
> Resultado: `./mvnw test` → **1474 tests, 0 fallos** (BUILD SUCCESS).

## 1. Qué se implementó

| Pieza | Estado |
|---|---|
| Interface `PaymentGateway` (createCheckout / getConfirmation / rollback) + selección por `payments.gateway` (`MOCK\|BANCARD`) vía env vars | ✅ |
| `MockPaymentGateway` (default dev): contrato exacto Bancard, confirma vía webhook propio con JSON real + token md5; reglas ...99→51, ...15→15, resto→00 | ✅ |
| `BancardGateway`: `single_buy`, `get_single_buy_confirmation`, `single_buy_rollback` con tokens md5 de la spec | ✅ |
| `POST /api/payments/{id}/checkout` (dueño + PENDING) → `processId` + URL del script de iframe | ✅ |
| `POST /api/payments/webhooks/bancard` público, verifica token, 200 siempre, aplica APPROVED→COMPLETED o REJECTED, guarda auth/ticket | ✅ |
| Scheduler de reconciliación (>10 min sin confirmación → consulta; sin resultado → rollback + REJECTED) | ✅ |
| Idempotencia en `POST /payments` (header `Idempotency-Key`, columna + unique, devuelve pago existente) | ✅ |
| Migración Liquibase `063` + master actualizado | ✅ |
| Docs: §9.3 marcado, endpoints nuevos en `payments-openapi.yaml` | ✅ |

## 2. Archivos

### Creados (src/main)

| Archivo | Rol |
|---|---|
| `gateway/PaymentGateway.java` | Interface de pasarela |
| `gateway/MockPaymentGateway.java` | Impl mock dev/test (default) |
| `gateway/BancardGateway.java` | Impl Bancard vPOS 2.0 (RestClient) |
| `gateway/BancardTokens.java` | Tokens md5 + formato de montos (spec §Token) |
| `gateway/GatewayCheckoutResult.java`, `GatewayConfirmation.java`, `GatewayRollbackResult.java` | Records internos del contrato |
| `model/enums/PaymentGatewayProvider.java` | `MOCK \| BANCARD` (selección y columna `payments.gateway`) |
| `config/PaymentGatewayProperties.java` | `@ConfigurationProperties("payments")` |
| `config/PaymentGatewayConfig.java` | Bean `PaymentGateway` según config |
| `service/PaymentGatewayService.java` | Checkout, procesamiento de confirmaciones, reconciliación |
| `controller/PaymentCheckoutController.java` | `POST /payments/{id}/checkout` |
| `controller/PaymentWebhookController.java` | `POST /payments/webhooks/bancard` |
| `scheduler/PaymentReconciliationScheduler.java` | Cron cada 5 min + ShedLock |
| `exception/PaymentGatewayException.java` | → 502 en `GlobalExceptionHandler` |
| `dto/payment/CheckoutRequest|CheckoutResponse|BancardConfirmRequest|BancardConfirmOperation.java` | DTOs REST |
| `resources/db/changelog/changelogs/063-add-gateway-fields-to-payments.yaml` | Migración (ver §3) |

### Modificados (src/main) — todos aditivos

| Archivo | Cambio |
|---|---|
| `model/payment/Payment.java` | + campos nullable: `idempotencyKey`, `gateway`, `gatewayProcessId`, `gatewayAuthorizationNumber`, `gatewayTicketNumber`, `gatewayResponseCode`, `checkoutStartedAt` |
| `repository/PaymentRepository.java` | + `findByUser_IdAndIdempotencyKey`, + query de reconciliación |
| `service/PaymentService.java` | Completa el TODO de `create(…, idempotencyKey)`: lookup + persistencia de la key. Resto intacto |
| `controller/PaymentController.java` | + header **opcional** `Idempotency-Key` en `POST /payments` (sin header → mismo código de siempre; contrato HTTP retrocompatible) |
| `config/SecurityConfig.java` | + 1 matcher: `POST /payments/webhooks/bancard` → `permitAll` |
| `exception/GlobalExceptionHandler.java` | + handler `PaymentGatewayException` → 502 |
| `resources/application.yml` (+ `.example`) | + bloque `payments:` (§9.1 del doc) |
| `resources/db/changelog/db.changelog-master.yaml` | + include de la 063 |

### Tests creados

- `gateway/BancardTokensTest` — vectores md5 fijos (calculados externamente) para las 4 fórmulas + formato de montos.
- `gateway/MockPaymentGatewayTest` — reglas determinísticas, shape del JSON `single_buy_confirm`, token válido, scheduling del webhook.
- `service/PaymentGatewayServiceTest` — checkout (dueño/PENDING/repetido/forbidden), webhook (aprobado, rechazado, token inválido, retry idempotente, pago inexistente), reconciliación (confirma/reversa/continúa ante error).
- `controller/PaymentWebhookControllerTest` — endpoint público sin JWT, mapeo del JSON real de Bancard, 200 ante token inválido y ante excepción interna.
- `service/PaymentServiceTest` — + nested `create() con idempotencyKey` (persiste, devuelve existente, ignora blank).

### Tests modificados (fix de suite preexistente — ver §5)

`TestSecurityMocksConfig`, `SliceSecurityBeans` y 15 controller tests (`PaymentControllerTest`, `AdminSettings…`, `AgentProfile…`, `Lead…`, etc.).

## 3. Modelo de datos (migración 063)

Columnas nuevas en `payments` (todas nullable → cero impacto en filas/flujos existentes):

- `idempotency_key` VARCHAR(255) + unique `(user_id, idempotency_key)` (NULLs no chocan).
- `gateway` VARCHAR(20) — `MOCK|BANCARD`; **NULL = flujo manual admin** (approve/reject siguen igual).
- `gateway_process_id` VARCHAR(50) (+índice), `gateway_authorization_number` VARCHAR(10), `gateway_ticket_number` VARCHAR(20), `gateway_response_code` VARCHAR(5), `checkout_started_at` TIMESTAMP (+índice).

## 4. Decisiones tomadas

1. **`shop_process_id` = `payments.id`** (entero ≤15 dígitos, como pide la spec). El webhook lo recibe como string o número (Jackson coacciona).
2. **Token del webhook verificado con los valores crudos del payload** (`shop_process_id`, `amount`, `currency` tal como llegan) — es exactamente lo que Bancard usó para generarlo.
3. **Webhook responde `200 {"status":"success"}` SIEMPRE** (token inválido o error interno incluidos): es el contrato de Bancard (<30 s); el estado real lo asegura la reconciliación. No usa `ApiResponse` porque el consumidor es Bancard.
4. **Pago aprobado por pasarela queda `COMPLETED`**: el webhook reusa `PaymentService.approvePayment()` (mismos efectos secundarios highlight/suscripción, sin pasar por el endpoint admin) y luego `completePayment()` — la transición `PENDING→APPROVED→COMPLETED` ya existía.
5. **Retry idempotente del webhook**: pago no-PENDING → 200 sin reprocesar (Bancard reintenta y el mock también podría).
6. **Checkout repetido devuelve el mismo `process_id`** sin llamar de nuevo a la pasarela (Bancard rechaza `shop_process_id` duplicados).
7. **Mock confirma vía HTTP real al webhook propio** (`payments.mock.webhook-url`), no por llamada interna: ejercita el endpoint completo (ruta pública + verificación de token) tal como lo hará Bancard.
8. **Reglas del mock sobre la parte entera del monto** (PYG no usa decimales): `entero % 100 == 99 → "51"`, `== 15 → "15"`, resto `"00"`.
9. **Rollback**: `PaymentNotFoundError` y `AlreadyRollbackedError` cuentan como reversa efectiva (la spec lo indica para el primero); `TransactionAlreadyConfirmed` deja el pago PENDING para que el próximo ciclo lo resuelva vía `get_confirmation`.
10. **Controllers nuevos separados** (`PaymentCheckoutController`, `PaymentWebhookController`) para no tocar la firma ni el contexto de test del `PaymentController` existente.
11. **Idempotency-Key scopeada por usuario** (mismo criterio que no exista cruce entre usuarios); el mismo patrón de header que ya usa `POST /rentals/installments/{id}/payments`.
12. **`PaymentGatewayProvider` nuevo** en vez de reusar `PaymentGatewayType` (que es de `lease_payments`, no tiene `MOCK` y sigue reservado).

## 5. Nota: estado de la suite de tests

La suite en `dev` estaba **rota antes de este trabajo**: 247 tests fallando (verificado con `git stash` + `./mvnw test`). Causa raíz: `AuthRateLimitingFilter` ganó la dependencia `AuthRateLimiter` y ningún `@WebMvcTest` slice (salvo `VisitRequestControllerTest`) fue actualizado; segunda capa: filtros mockeados sin stub passthrough cortaban la `FilterChain`. Se arregló:

- Bean mock de `AuthRateLimiter` en `TestSecurityMocksConfig` y `SliceSecurityBeans` (fix compartido).
- Stubs passthrough de filtros en los tests afectados (patrón ya canonizado en `VisitRequestControllerTest`).
- 4 asserts que contradecían el comportamiento real de src/main (no modificable por restricción): 3 en `LeadControllerTest` (sin auth la app devuelve **401**, no 403 — coincide con §2.4 del doc) y 1 en `ContractControllerTest` (PATCH status permite USER por diseño del `@PreAuthorize`; queda como **pregunta de producto** si debería restringirse).

Cero regresiones: ningún test verde en baseline quedó rojo. Resultado final: **1474/1474 verdes**.

## 6. Cómo probar manualmente (curl)

```bash
BASE=http://localhost:8080/api

# 1. Login (usuario dev)
TOKEN=$(curl -s -X POST $BASE/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"comprador1@openroof.com","password":"Test1234!"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["data"]["accessToken"])')

# 2. Crear pago (con idempotencia)
KEY=$(uuidgen)
curl -s -X POST $BASE/payments -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: $KEY" -H 'Content-Type: application/json' \
  -d '{"type":"RESERVATION","amount":350000,"concept":"Prueba gateway"}'
# → anotar data.id (PAY_ID). Repetir el mismo curl → devuelve el MISMO pago (id idéntico, sin duplicar).

# 3. Iniciar checkout (gateway MOCK por defecto)
curl -s -X POST $BASE/payments/$PAY_ID/checkout -H "Authorization: Bearer $TOKEN"
# → {"processId":"mock-<id>-…","checkoutScriptUrl":"…bancard-checkout-4.0.0.js","gateway":"MOCK"}

# 4. Esperar ~3 s (el mock confirma vía webhook propio) y verificar estado
sleep 3 && curl -s $BASE/payments/$PAY_ID -H "Authorization: Bearer $TOKEN"
# → status COMPLETED (monto normal). Con monto terminado en 99 (ej. 150099) → REJECTED (fondos insuficientes);
#   terminado en 15 (ej. 350015) → REJECTED (tarjeta inválida).

# 5. Webhook a mano (simular a Bancard; calcular token md5)
#    token = md5(private_key + shop_process_id + "confirm" + amount + currency); con private_key vacía en dev:
TOKEN_MD5=$(python3 -c "import hashlib;print(hashlib.md5(f'${PAY_ID}confirm350000.00PYG'.encode()).hexdigest())")
curl -s -X POST $BASE/payments/webhooks/bancard -H 'Content-Type: application/json' -d "{
  \"operation\": {\"token\":\"$TOKEN_MD5\",\"shop_process_id\":$PAY_ID,\"response\":\"S\",
  \"amount\":\"350000.00\",\"currency\":\"PYG\",\"authorization_number\":\"123456\",
  \"ticket_number\":\"123456789\",\"response_code\":\"00\",\"response_description\":\"Aprobada\"}}"
# → {"status":"success"} siempre (también con token inválido, pero entonces no cambia nada)

# 6. Flujo manual admin (sigue intacto para pagos sin gateway)
# POST $BASE/payments/{id}/approve | /reject con token de admin@openroof.com
```

Para probar contra **staging de Bancard**: `PAYMENT_GATEWAY=BANCARD`, `BANCARD_PUBLIC_KEY`/`BANCARD_PRIVATE_KEY` del portal de comercios, `confirmation_url` pública apuntando a `/api/payments/webhooks/bancard`. Datos de prueba: cédula `9661000`, Zimple `0981123456`/OTP `1234`.

## 7. Qué quedó pendiente

- **Estado `REFUNDED` / reversas en `PaymentStatus`** (último ítem de §9.3): hoy la reversa queda `REJECTED`. Requiere decisión de producto + migración.
- **Certificación Bancard** (operativo): completar la "lista de test" del portal y el ciclo de certificación antes de producción (§9.2 del doc).
- **Caso borde**: si el webhook aprueba pero `approvePayment` falla (metadata incompleta / plan inactivo), el pago queda `PENDING` con log de error y la reconciliación reintenta; si el error persiste, requiere intervención manual (el dinero fue cobrado). Considerar alerta/notificación admin.
- **Restricción de `PATCH /contracts/{id}/status` para rol USER**: el test esperaba 403 pero el endpoint lo permite por diseño — confirmar con el equipo si es intencional.
- **`/payments/{id}/checkout` para ADMIN**: hoy es estrictamente del dueño (decisión conservadora); si el panel admin necesita iniciar checkouts de terceros, ampliar la regla.
