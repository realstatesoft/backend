# Módulo de Gestión de Alquileres — Capa JPA (Entidades y Enums)

Implementación completa de la capa de dominio (modelos JPA) para el módulo de alquileres de OpenRoof.
Cubre los tickets **OR-192, OR-193, OR-194, OR-212, OR-228, OR-246, OR-247, OR-262, OR-291**.

---

## Resumen de cambios

### Nuevos paquetes creados

| Paquete | Contenido |
|---------|-----------|
| `model/rental/` | `Lease`, `RentalApplication`, `RentalInstallment`, `LeasePayment` |
| `model/screening/` | `TenantScreening` |
| `model/maintenance/` | `Vendor`, `MaintenanceRequest`, `WorkOrder` |
| `model/conversation/` | `Conversation`, `ConversationMessage` |
| `model/ledger/` | `LedgerEntry` |

### Archivos existentes modificados

| Archivo | Cambio |
|---------|--------|
| `model/notification/Notification.java` | +5 campos de canal/delivery + método `markAsRead()` |

### Nuevos archivos en paquetes existentes

| Archivo | Propósito |
|---------|-----------|
| `model/notification/NotificationPreference.java` | Preferencias de notificación por usuario/canal |
| `model/notification/NotificationPreferenceId.java` | Clave compuesta embeddable |

---

## Enums nuevos (`model/enums/`)

### Módulo Lease

| Enum | Valores |
|------|---------|
| `LeaseStatus` | `DRAFT`, `PENDING_SIGNATURE`, `ACTIVE`, `EXPIRING_SOON`, `EXPIRED`, `TERMINATED`, `RENEWED` |
| `LeaseType` | `FIXED_TERM`, `MONTH_TO_MONTH` |
| `BillingFrequency` | `WEEKLY`, `MONTHLY`, `BIMONTHLY`, `QUARTERLY`, `SEMIANNUAL`, `ANNUAL` |
| `DepositStatus` | `HELD`, `PARTIALLY_RETURNED`, `RETURNED`, `FORFEITED` |
| `LateFeeType` | `PERCENTAGE`, `FIXED_AMOUNT`, `DAILY_FIXED`, `TIERED` |

### Módulo Aplicación / Screening

| Enum | Valores |
|------|---------|
| `RentalApplicationStatus` | `SUBMITTED`, `UNDER_REVIEW`, `SCREENING_IN_PROGRESS`, `APPROVED`, `REJECTED`, `WITHDRAWN` |
| `EmploymentStatus` | `EMPLOYED`, `SELF_EMPLOYED`, `UNEMPLOYED`, `RETIRED`, `STUDENT`, `OTHER` |
| `ScreeningProvider` | `TRANSUNION`, `EXPERIAN`, `INTERNAL`, `MANUAL` |
| `BackgroundCheckStatus` | `CLEAR`, `FLAGGED`, `FAILED` |
| `ScreeningRecommendation` | `APPROVE`, `REVIEW`, `REJECT` |

### Módulo Pagos

| Enum | Valores |
|------|---------|
| `InstallmentStatus` | `PENDING`, `PARTIAL`, `PAID`, `OVERDUE`, `WAIVED`, `IN_DISPUTE` |
| `PaymentMethod` | `ACH`, `CARD`, `CASH`, `BANK_TRANSFER`, `CHECK`, `OTHER` |
| `LeasePaymentStatus` | `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`, `REFUNDED`, `DISPUTED` |
| `LeasePaymentType` | `RENT`, `LATE_FEE`, `DEPOSIT`, `APPLICATION_FEE`, `MAINTENANCE`, `OTHER` |
| `PaymentGatewayType` | `STRIPE`, `BANCARD`, `PAGOPAR`, `MERCADOPAGO`, `MANUAL` |

> **Nota:** `PaymentMethod` reemplaza a `LeasePaymentMethod` para coincidir con el nombre del ticket OR-212.
> El enum `PaymentStatus` (PENDING, APPROVED, REJECTED) existía previamente para la tabla `payments` de reservas y no fue modificado.

### Módulo Mantenimiento

| Enum | Valores |
|------|---------|
| `MaintenanceCategory` | `PLUMBING`, `ELECTRICAL`, `HVAC`, `APPLIANCE`, `STRUCTURAL`, `PEST_CONTROL`, `LANDSCAPING`, `CLEANING`, `OTHER` |
| `MaintenancePriority` | `LOW`, `MEDIUM`, `HIGH`, `EMERGENCY` |
| `MaintenanceStatus` | `SUBMITTED`, `ACKNOWLEDGED`, `IN_PROGRESS`, `ON_HOLD`, `COMPLETED`, `CANCELLED` |
| `CostBilledTo` | `LANDLORD`, `TENANT`, `WARRANTY`, `INSURANCE` |
| `WorkOrderStatus` | `PENDING`, `QUOTED`, `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `DISPUTED` |

### Módulo Ledger

| Enum | Valores |
|------|---------|
| `LedgerEntryType` | `DEBIT`, `CREDIT` |
| `LedgerEntryCategory` | `RENT`, `LATE_FEE`, `DEPOSIT`, `MAINTENANCE`, `UTILITY`, `INSURANCE`, `TAX`, `OTHER` |

### Módulo Notificaciones (extensión)

| Enum | Valores |
|------|---------|
| `NotificationChannel` | `IN_APP`, `EMAIL`, `SMS` |
| `NotificationDeliveryStatus` | `PENDING`, `SENT`, `DELIVERED`, `FAILED`, `READ` |
| `NotificationEventType` | `PAYMENT_DUE`, `PAYMENT_RECEIVED`, `MAINTENANCE_UPDATE`, `LEASE_EXPIRING`, `LEASE_SIGNED`, `LEASE_ACTIVATED`, `APPLICATION_APPROVED`, `APPLICATION_REJECTED`, `INSPECTION_SCHEDULED`, `SLA_BREACH` |

> **Nota:** `NotificationChannel` es distinto de `NotifyChannel` (EMAIL, IN_APP, BOTH) que ya existía para el flujo de notificaciones de agentes. Ambos coexisten para no romper código existente.

---

## Entidades

### OR-192 — `Lease` (`model/rental/Lease.java`)

**Tabla:** `leases` | **Migración:** `038-create-leases-table.yaml`

```text
Lease
├── property (ManyToOne → Property)
├── landlord (ManyToOne → User)
├── primaryTenant (ManyToOne → User)
├── parentLeaseId (Long, nullable) — referencia a renovaciones anteriores
├── status (LeaseStatus)
├── leaseType (LeaseType)
├── startDate / endDate (LocalDate)
├── monthlyRent / currency
├── billingFrequency (BillingFrequency)
├── dueDay / gracePeriodDays
├── lateFeeType (LateFeeType) / lateFeeValue / maxLateFeeCap
├── securityDeposit / depositStatus (DepositStatus)
├── documentUrl / autoRenew
└── e-firma: signatureTokenLandlord, signatureTokenTenant (UUID),
           signatureTokenExpiresAt, signedByLandlordAt, signedByTenantAt,
           signatureAuditTrail (JSONB)
```

**Métodos de dominio:**
- `isSigned()` — true si ambas partes firmaron
- `isActive()` — true si status == ACTIVE

---

### OR-193 — `RentalApplication` (`model/rental/RentalApplication.java`)

**Tabla:** `rental_applications` | **Migración:** `040-create-rental-applications-table.yaml`

```text
RentalApplication
├── property (ManyToOne → Property)
├── applicant (ManyToOne → User)
├── status (RentalApplicationStatus)
├── desiredMoveInDate (LocalDate)
├── monthlyIncome / incomeToRentRatio
├── employmentStatus (EmploymentStatus) / employerName
├── previousAddresses (JSONB: List<Map>)
├── tenantReferences (JSONB: List<Map>)
├── hasPets / screeningConsent / screeningConsentAt
├── submittedAt / decidedAt
└── rejectionReason (TEXT)
```

---

### OR-194 — `RentalInstallment` (`model/rental/RentalInstallment.java`)

**Tabla:** `rental_installments` | **Migración:** `041-create-rental-installments-table.yaml`

```text
RentalInstallment
├── lease (ManyToOne → Lease)
├── installmentNumber
├── periodStart / periodEnd / dueDate (LocalDate)
├── baseRent / otherCharges (JSONB) / totalAmount
├── paidAmount / balance / lateFee
├── status (InstallmentStatus)
└── invoiceNumber / invoicePdfUrl
```

**Métodos de dominio:**
- `getBalance()` — totalAmount - paidAmount (calculado)
- `isOverdue()` — dueDate antes de hoy y no PAID/WAIVED

---

### OR-212 — `LeasePayment` (`model/rental/LeasePayment.java`)

**Tabla:** `lease_payments` | **Migración:** `042-create-lease-payments-table.yaml`

> Se usa tabla `lease_payments` separada de `payments` (reservas/contratos) para evitar conflicto de dominio.

```text
LeasePayment
├── lease (ManyToOne → Lease)
├── installment (ManyToOne → RentalInstallment, nullable)
├── payer (ManyToOne → User)
├── amount / currency
├── method (PaymentMethod)
├── gateway (String) / gatewayTransactionId
├── status (LeasePaymentStatus)
├── type (LeasePaymentType)
├── processingFee / netAmount
├── receiptNumber / receiptPdfUrl
├── paidAt (LocalDateTime)
└── idempotencyKey (@Column unique=true)
```

**Métodos de dominio:**
- `isCompleted()` — status == COMPLETED
- `isRefundable()` — true si status == COMPLETED y paidAt != null

---

### OR-228 — `TenantScreening` (`model/screening/TenantScreening.java`)

**Tabla:** `tenant_screenings` | **Migración:** `043-create-tenant-screenings-table.yaml`

```text
TenantScreening
├── application (OneToOne → RentalApplication, unique FK)
├── provider (ScreeningProvider)
├── creditScore / creditReportUrl
├── backgroundCheckStatus (BackgroundCheckStatus) / backgroundReportUrl
├── evictionHistory (JSONB: List<Map>)
├── criminalRecords (JSONB: List<Map>)
├── incomeVerified / identityVerified
├── recommendation (ScreeningRecommendation)
└── expiresAt / runAt
```

**Métodos de dominio:**
- `isExpired()` — expiresAt antes de ahora
- `autoRecommend()` — reglas: FAILED background → REJECT; FLAGGED o creditScore < 580 → REVIEW; else → APPROVE

---

### OR-246 — Mantenimiento (3 entidades)

#### `Vendor` (`model/maintenance/Vendor.java`)
**Tabla:** `vendors` | **Migración:** `046-create-vendors-table.yaml`

```text
Vendor
├── companyName / contactName / email / phone / taxId
├── specialties (JSONB: List<MaintenanceCategory>)
├── rating / totalJobs / isActive
├── insuranceExpiresAt (LocalDate)
├── hourlyRate
└── serviceAreas (JSONB: List<String>)
```

#### `MaintenanceRequest` (`model/maintenance/MaintenanceRequest.java`)
**Tabla:** `maintenance_requests` | **Migración:** `047-create-maintenance-requests-table.yaml`

```text
MaintenanceRequest
├── property (ManyToOne → Property)
├── lease (ManyToOne → Lease, nullable)
├── tenant (ManyToOne → User)
├── title / description / category (MaintenanceCategory)
├── priority (MaintenancePriority)
├── status (MaintenanceStatus)
├── images (JSONB: List<String>)
├── assignedVendor (ManyToOne → Vendor, nullable)
├── estimatedCost
├── actualCost / costBilledTo (CostBilledTo)
└── scheduledDate / completedDate / closedAt
```

#### `WorkOrder` (`model/maintenance/WorkOrder.java`)
**Tabla:** `work_orders` | **Migración:** `048-create-work-orders-table.yaml`

```text
WorkOrder
├── maintenanceRequest (ManyToOne → MaintenanceRequest)
├── vendor (ManyToOne → Vendor)
├── status (WorkOrderStatus)
├── quotedAmount / finalAmount
├── scheduledDate / completedDate
├── invoiceUrl
├── beforePhotos (JSONB: List<String>)
├── afterPhotos (JSONB: List<String>)
└── warrantyDays
```

---

### OR-247 — Mensajería (2 entidades)

#### `Conversation` (`model/conversation/Conversation.java`)
**Tabla:** `conversations` | **Migración:** `049-create-conversations-table.yaml`

```text
Conversation
├── subject
├── relatedLease (ManyToOne → Lease, nullable)
├── lastMessageAt
├── isArchived
└── participants (ManyToMany → User via conversation_participants)
```

#### `ConversationMessage` (`model/conversation/ConversationMessage.java`)
**Tabla:** `conversation_messages` | **Migración:** `049-create-conversations-table.yaml`

> **No extiende BaseEntity** — no tiene soft delete por diseño (mensajes son inmutables).

```text
ConversationMessage
├── id (@Id @GeneratedValue)
├── conversation (ManyToOne → Conversation)
├── sender (ManyToOne → User)
├── body (TEXT)
├── sentAt (LocalDateTime)
├── attachments (JSONB: List<Map>)
└── readBy (JSONB: Map<String, Object>) — clave = userId como String
```

**Métodos de dominio:**
- `isReadBy(Long userId)` — verifica si la clave userId existe en el mapa readBy

---

### OR-262 — `LedgerEntry` (`model/ledger/LedgerEntry.java`)

**Tabla:** `ledger_entries` | **Migración:** `050-create-ledger-entries-table.yaml`

> Registros contables — inmutables por convención. Se crea pero nunca se actualiza ni elimina.

```text
LedgerEntry
├── lease (ManyToOne → Lease)
├── property (ManyToOne → Property)
├── date (LocalDate)
├── type (LedgerEntryType)
├── category (LedgerEntryCategory)
├── amount / currency
├── description (TEXT)
├── relatedPayment (ManyToOne → LeasePayment, nullable)
├── relatedWorkOrder (ManyToOne → WorkOrder, nullable)
├── attachmentUrl
├── isTaxDeductible
└── createdBy (ManyToOne → User, nullable)
```

**Factory methods estáticos:**
- `LedgerEntry.fromPayment(LeasePayment)` — crea entrada CREDIT/RENT desde un pago completado
- `LedgerEntry.fromWorkOrder(WorkOrder)` — crea entrada DEBIT/MAINTENANCE desde una orden de trabajo

---

### OR-291 — Notificaciones extendidas

#### Modificación: `Notification.java`

Campos añadidos (changeset `051-add-notification-preferences.yaml`):

| Campo | Tipo | Columna |
|-------|------|---------|
| `channel` | `NotificationChannel` | `channel` VARCHAR(20) |
| `deliveryStatus` | `NotificationDeliveryStatus` | `delivery_status` VARCHAR(20) |
| `sentAt` | `LocalDateTime` | `sent_at` |
| `relatedEntityType` | `String` | `related_entity_type` VARCHAR(50) |
| `relatedEntityId` | `Long` | `related_entity_id` |

**Método añadido:**
```java
public void markAsRead() {
    this.readAt = LocalDateTime.now();
    this.deliveryStatus = NotificationDeliveryStatus.READ;
}
```

#### Nueva entidad: `NotificationPreference` (`model/notification/NotificationPreference.java`)

**Tabla:** `notification_preferences` | **Migración:** `051-add-notification-preferences.yaml`

> No extiende `BaseEntity`. Usa clave primaria compuesta (user_id, event_type, channel).

```text
NotificationPreference
├── id: NotificationPreferenceId @EmbeddedId
│   ├── userId (Long)
│   ├── eventType (NotificationEventType)
│   └── channel (NotificationChannel)
└── isEnabled (Boolean)
```

`NotificationPreferenceId` es un `@Embeddable` que implementa `Serializable` con `@EqualsAndHashCode` de Lombok.

---

## Decisiones de diseño

### 1. `lease_payments` vs `payments`
La tabla `payments` ya existía para reservas y contratos (con `PaymentStatus`: PENDING/APPROVED/REJECTED). Se creó `lease_payments` con semántica diferente (COMPLETED, REFUNDED, DISPUTED) para no romper el módulo de reservas ni forzar una migración sobre datos existentes.

### 2. `ConversationMessage` sin `BaseEntity`
La especificación del ticket indica "No soft delete en mensajes". La entidad tiene su propio `@Id @GeneratedValue` sin heredar `BaseEntity`, evitando columnas innecesarias en la tabla.

### 3. JSONB como `List<Map<String, Object>>`
Para campos estructurados variables (historial de domicilios, referencias, fotos, etc.) se usa `@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"` con colecciones Java. Esto permite evolución del esquema sin migraciones adicionales.

### 4. `parentLeaseId` como `Long` en `Lease`
Para evitar referencia circular (`Lease → Lease`), se almacena solo el ID del contrato padre en renovaciones en lugar de una relación JPA.

### 5. `LedgerEntry` inmutable por convención
Aunque tiene `deleted_at` (hereda de `BaseEntity`), la convención del equipo es nunca llamar a delete ni update sobre registros contables. Los factory methods garantizan consistencia al crear entradas.

### 6. `NotificationChannel` vs `NotifyChannel`
- `NotifyChannel` (existente): `EMAIL`, `IN_APP`, `BOTH` — para notificaciones de agentes
- `NotificationChannel` (nuevo): `IN_APP`, `EMAIL`, `SMS` — para el módulo de alquileres

Coexisten para no romper el código existente del módulo de notificaciones de agentes.
