# OR-206 — Autorización por rol en endpoints de Lease

## 1. Plan de implementación

### 1.1 Resumen

Backend Spring Security ya tiene patrón de componentes `*Security` (`PropertySecurity`, `LeadSecurity`) usados vía `@PreAuthorize`. Falta:

- Componente `LeaseSecurity` con `assertLeaseAccess(userId, leaseId)` y `hasLeaseAccess(userId, leaseId)`.
- Matchers de `/api/leases/**`, `/api/rentals/**`, `/api/lease-payments/**` en `SecurityConfig` para distinguir rutas públicas de protegidas.

`AccessDeniedException` ya está mapeada a HTTP 403 por `GlobalExceptionHandler`.

### 1.2 Archivos a crear / modificar

| Archivo | Acción | Motivo |
|---|---|---|
| `src/main/java/com/openroof/openroof/security/LeaseSecurity.java` | CREAR | Componente con `assertLeaseAccess` + `hasLeaseAccess`. |
| `src/main/java/com/openroof/openroof/config/SecurityConfig.java` | MODIFICAR | Agregar matchers para endpoints de leases / rentals / lease-payments. |
| `src/test/java/com/openroof/openroof/security/LeaseSecurityTest.java` | CREAR | Unit tests Mockito puro de las 5 reglas + variantes. |
| `src/test/java/com/openroof/openroof/controller/LeaseSecurityEndpointsTest.java` | CREAR | `@WebMvcTest` verificando 401 sin token en `/api/leases/**`. |

### 1.3 Diseño de `LeaseSecurity`

Reglas de `assertLeaseAccess(Long userId, Long leaseId)`:

1. `userId == null` → `AccessDeniedException("Usuario no autenticado")`. No consulta repos.
2. `userRepository.findById(userId).isEmpty()` → `AccessDeniedException("Usuario " + userId + " no encontrado")`. No consulta `leaseRepository`.
3. `user.role == ADMIN` → retorna sin consultar `leaseRepository`.
4. `leaseRepository.findById(leaseId).isEmpty()` → `AccessDeniedException("Lease " + leaseId + " no encontrado o sin acceso")` (mismo formato que outsider para no filtrar existencia).
5. `userId.equals(lease.landlord.id)` → permitido.
6. `userId.equals(lease.primaryTenant.id)` → permitido.
7. Otro caso → `AccessDeniedException("Usuario " + userId + " no es landlord ni tenant del lease " + leaseId)`.

`hasLeaseAccess(Long userId, Long leaseId)` → envuelve `assertLeaseAccess` en try/catch y retorna `boolean`. Usable desde `@PreAuthorize`.

### 1.4 Cambios en `SecurityConfig`

Insertar **antes** de `.anyRequest().authenticated()` dentro de `authorizeHttpRequests`:

```java
// Endpoints públicos de alquileres (catálogo)
.requestMatchers(HttpMethod.GET, "/api/leases/public/**").permitAll()

// Endpoints protegidos — autorización fina en service/controller vía LeaseSecurity
.requestMatchers("/api/leases/**").authenticated()
.requestMatchers("/api/rentals/**").authenticated()
.requestMatchers("/api/lease-payments/**").authenticated()
```

Notas:
- `.anyRequest().authenticated()` ya cubre el caso por defecto; los matchers explícitos documentan intención y permiten futuras reglas por método HTTP.
- Autorización por recurso se hace en controller con `@PreAuthorize("@leaseSecurity.hasLeaseAccess(principal.id, #leaseId)")` o en service llamando directamente a `assertLeaseAccess`.

### 1.5 Pasos secuenciales

1. Crear `LeaseSecurity.java` con código del Apéndice A.
2. Editar `SecurityConfig.securityFilterChain` — pegar bloque de 1.4 antes de `.anyRequest().authenticated()`.
3. Crear `LeaseSecurityTest.java` (Apéndice B).
4. Crear `LeaseSecurityEndpointsTest.java` (Apéndice C).
5. `./mvnw test -Dtest=LeaseSecurityTest,LeaseSecurityEndpointsTest` → verde.
6. `./mvnw test` → suite completa verde.
7. Commit: `feat(security): add LeaseSecurity + auth rules for leases (OR-206)`.
8. Push + abrir PR enlazando OR-206 en descripción.

---

## 2. Descripción para Jira (OR-206)

**Título:** `[OR-206] Autorización por rol en endpoints de Lease`

**Descripción:**

`/api/leases/**`, `/api/rentals/**` y `/api/lease-payments/**` no tienen reglas explícitas en `SecurityConfig` y no existe componente análogo a `PropertySecurity`/`LeadSecurity` para validar que el usuario autenticado sea landlord o tenant del lease. Cualquier usuario autenticado puede potencialmente acceder a leases de terceros si la autorización fina no se implementa en los servicios.

**Alcance:**

- Crear `com.openroof.openroof.security.LeaseSecurity` (`@Component("leaseSecurity")`) con:
  - `void assertLeaseAccess(Long userId, Long leaseId)` — lanza `AccessDeniedException` con mensaje descriptivo si no aplica.
  - `boolean hasLeaseAccess(Long userId, Long leaseId)` — versión no-lanzante para `@PreAuthorize`.
- Modificar `SecurityConfig` agregando matchers de `/api/leases/public/**` (permitAll), `/api/leases/**`, `/api/rentals/**`, `/api/lease-payments/**` (authenticated).
- Tests unitarios + de seguridad de endpoint.

**Criterios de aceptación:**

- [ ] `LeaseSecurity.assertLeaseAccess` permite a usuarios con rol `ADMIN` sin consultar `leaseRepository`.
- [ ] Permite al `landlord` del lease.
- [ ] Permite al `primaryTenant` del lease.
- [ ] Bloquea a cualquier otro usuario lanzando `org.springframework.security.access.AccessDeniedException` con mensaje que incluye `userId` y `leaseId`.
- [ ] Si el lease no existe → `AccessDeniedException` (NO `EntityNotFoundException`), para no filtrar existencia a terceros.
- [ ] Si `userId == null` → `AccessDeniedException("Usuario no autenticado")` sin tocar repos.
- [ ] `SecurityConfig` exige autenticación en `/api/leases/**`, `/api/rentals/**`, `/api/lease-payments/**`.
- [ ] `GET /api/leases/public/**` accesible sin token.
- [ ] `hasLeaseAccess` retorna `boolean` (true ok, false en cualquier denegación, incluso `null`).
- [ ] Unit tests `LeaseSecurityTest` cubren los 5 escenarios + null + variantes `hasLeaseAccess`.
- [ ] Test de integración `LeaseSecurityEndpointsTest` verifica 401 en GET/POST/PUT/DELETE sin token.
- [ ] `./mvnw test` pasa en verde.

**Dependencias:**

- Entidad `Lease` + `LeaseRepository` (OR-197/198/199, ya mergeado).
- `GlobalExceptionHandler.handleAccessDenied` — mapea `AccessDeniedException` a HTTP 403.

**Notas técnicas:**

- Reutilizar patrón de `LeadSecurity` (`@Component("leaseSecurity")` para uso en `@PreAuthorize`).
- Mensajes de excepción en español, sin filtrar existencia del recurso.
- No mockear contexto Spring en `LeaseSecurityTest`: Mockito puro con `@ExtendWith(MockitoExtension.class)`.

---

## 3. Plan de pruebas QA — paso a paso

### Pre-requisitos

- Backend corriendo en `http://localhost:8080`.
- Postman o `curl`.
- Cuatro usuarios sembrados en BD:
  - `admin@test.com` — rol `ADMIN`.
  - `landlord@test.com` — rol `USER`, `landlord_id` del lease #1.
  - `tenant@test.com` — rol `USER`, `primary_tenant_id` del lease #1.
  - `outsider@test.com` — rol `USER`, no relacionado al lease #1.
- Un `Lease` con `id = 1`, `landlord_id` = usuario landlord, `primary_tenant_id` = usuario tenant, `status = ACTIVE`.

### Caso 1 — Sin token (401)

1. `GET http://localhost:8080/api/leases/1` sin header `Authorization`.
2. **Esperado:** HTTP 401, body JSON con `success: false`.

### Caso 2 — ADMIN accede a cualquier lease (200)

1. `POST /auth/login` con `admin@test.com` → guardar `accessToken`.
2. `GET /api/leases/1` con header `Authorization: Bearer <token>`.
3. **Esperado:** HTTP 200, body con el lease #1.

### Caso 3 — Landlord accede a su lease (200)

1. Login como `landlord@test.com`.
2. `GET /api/leases/1` con su token.
3. **Esperado:** HTTP 200.

### Caso 4 — Tenant accede a su lease (200)

1. Login como `tenant@test.com`.
2. `GET /api/leases/1` con su token.
3. **Esperado:** HTTP 200.

### Caso 5 — Usuario ajeno bloqueado (403)

1. Login como `outsider@test.com`.
2. `GET /api/leases/1` con su token.
3. **Esperado:** HTTP 403, body con mensaje `"Acceso denegado"` (mensaje genérico — el detallado va al log del backend).

### Caso 6 — Lease inexistente (403, NO 404)

1. Login como `landlord@test.com`.
2. `GET /api/leases/999999`.
3. **Esperado:** HTTP 403. Si responde 404, falla — filtra existencia.

### Caso 7 — Endpoint público accesible sin token

1. `GET /api/leases/public/featured` (o ruta pública disponible) sin header `Authorization`.
2. **Esperado:** HTTP 200 (o 404 si la ruta concreta no tiene handler aún — pero NO 401).

### Caso 8 — Operación de escritura por outsider (403)

1. Login como `outsider@test.com`.
2. `PUT /api/leases/1` con body modificando `monthlyRent`.
3. **Esperado:** HTTP 403.

### Caso 9 — Token expirado / malformado (401)

1. Usar token expirado o cadena aleatoria como `Bearer`.
2. `GET /api/leases/1`.
3. **Esperado:** HTTP 401.

### Caso 10 — `/api/rentals/**` requiere auth (401 sin token)

1. `GET /api/rentals/whatever` sin token.
2. **Esperado:** HTTP 401.

### Caso 11 — `/api/lease-payments/**` requiere auth (401 sin token)

1. `GET /api/lease-payments/whatever` sin token.
2. **Esperado:** HTTP 401.

### Plantilla de reporte de bug

```text
Caso fallado: #
Request:
  Método: GET/POST/PUT/DELETE
  URL: ...
  Headers: Authorization: Bearer ...
Body enviado: ...
Status esperado: ...
Status recibido: ...
Body de respuesta: ...
Logs del backend (si 5xx): ...
```

---

## Apéndice A — `LeaseSecurity.java`

```java
package com.openroof.openroof.security;

import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Componente de seguridad para validar acceso a recursos de leases.
 * Usado vía @PreAuthorize("@leaseSecurity.hasLeaseAccess(...)") o
 * llamada directa a assertLeaseAccess desde servicios.
 */
@Component("leaseSecurity")
@RequiredArgsConstructor
public class LeaseSecurity {

    private final LeaseRepository leaseRepository;
    private final UserRepository userRepository;

    /**
     * Verifica que el usuario sea ADMIN, landlord o primary tenant del lease.
     * Lanza AccessDeniedException con mensaje descriptivo si no aplica.
     *
     * @param userId  id del usuario autenticado
     * @param leaseId id del lease a verificar
     * @throws AccessDeniedException si no tiene acceso
     */
    public void assertLeaseAccess(Long userId, Long leaseId) {
        if (userId == null) {
            throw new AccessDeniedException("Usuario no autenticado");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Usuario " + userId + " no encontrado"));

        if (user.getRole() == UserRole.ADMIN) {
            return;
        }

        Lease lease = leaseRepository.findById(leaseId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Lease " + leaseId + " no encontrado o sin acceso"));

        boolean isLandlord = lease.getLandlord() != null
                && lease.getLandlord().getId() != null
                && lease.getLandlord().getId().equals(userId);
        boolean isTenant = lease.getPrimaryTenant() != null
                && lease.getPrimaryTenant().getId() != null
                && lease.getPrimaryTenant().getId().equals(userId);

        if (!isLandlord && !isTenant) {
            throw new AccessDeniedException(
                    "Usuario " + userId + " no es landlord ni tenant del lease " + leaseId);
        }
    }

    /**
     * Versión boolean para @PreAuthorize. No lanza.
     */
    public boolean hasLeaseAccess(Long userId, Long leaseId) {
        try {
            assertLeaseAccess(userId, leaseId);
            return true;
        } catch (AccessDeniedException ex) {
            return false;
        }
    }
}
```

---

## Apéndice B — `LeaseSecurityTest.java`

```java
package com.openroof.openroof.security;

import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.LeaseRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaseSecurityTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long LANDLORD_ID = 2L;
    private static final Long TENANT_ID = 3L;
    private static final Long OTHER_ID = 4L;
    private static final Long LEASE_ID = 100L;

    @Mock
    private LeaseRepository leaseRepository;

    @Mock
    private UserRepository userRepository;

    private LeaseSecurity leaseSecurity;

    @BeforeEach
    void setUp() {
        leaseSecurity = new LeaseSecurity(leaseRepository, userRepository);
    }

    // ---------- assertLeaseAccess ----------

    @Test
    void assertLeaseAccess_admin_allowed_withoutLoadingLease() {
        when(userRepository.findById(ADMIN_ID))
                .thenReturn(Optional.of(user(ADMIN_ID, UserRole.ADMIN)));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(ADMIN_ID, LEASE_ID));

        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_landlord_allowed() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    @Test
    void assertLeaseAccess_tenant_allowed() {
        when(userRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(user(TENANT_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(TENANT_ID, LEASE_ID));
    }

    @Test
    void assertLeaseAccess_outsider_throwsAccessDenied_withDescriptiveMessage() {
        when(userRepository.findById(OTHER_ID))
                .thenReturn(Optional.of(user(OTHER_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(OTHER_ID, LEASE_ID));

        assertTrue(ex.getMessage().contains(String.valueOf(OTHER_ID)));
        assertTrue(ex.getMessage().contains(String.valueOf(LEASE_ID)));
        assertTrue(ex.getMessage().toLowerCase().contains("landlord"));
        assertTrue(ex.getMessage().toLowerCase().contains("tenant"));
    }

    @Test
    void assertLeaseAccess_leaseNotFound_throwsAccessDenied_notEntityNotFound() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("lease"));
        assertTrue(ex.getMessage().contains(String.valueOf(LEASE_ID)));
    }

    @Test
    void assertLeaseAccess_userNotFound_throwsAccessDenied_withoutTouchingLeaseRepo() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("usuario"));
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_nullUserId_throwsAccessDenied_withoutTouchingAnyRepo() {
        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(null, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("autenticad"));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_landlordOnLeaseWithNullTenant_allowed() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        Lease l = lease(LANDLORD_ID, TENANT_ID);
        l.setPrimaryTenant(null);
        when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(l));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    @Test
    void assertLeaseAccess_tenantOnLeaseWithNullLandlord_allowed() {
        when(userRepository.findById(TENANT_ID))
                .thenReturn(Optional.of(user(TENANT_ID, UserRole.USER)));
        Lease l = lease(LANDLORD_ID, TENANT_ID);
        l.setLandlord(null);
        when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(l));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(TENANT_ID, LEASE_ID));
    }

    // ---------- hasLeaseAccess ----------

    @Test
    void hasLeaseAccess_landlord_returnsTrue() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertTrue(leaseSecurity.hasLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    @Test
    void hasLeaseAccess_outsider_returnsFalse() {
        when(userRepository.findById(OTHER_ID))
                .thenReturn(Optional.of(user(OTHER_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        assertFalse(leaseSecurity.hasLeaseAccess(OTHER_ID, LEASE_ID));
    }

    @Test
    void hasLeaseAccess_nullUserId_returnsFalse() {
        assertFalse(leaseSecurity.hasLeaseAccess(null, LEASE_ID));
        verifyNoInteractions(userRepository);
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void hasLeaseAccess_leaseNotFound_returnsFalse() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.empty());

        assertFalse(leaseSecurity.hasLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    // ---------- helpers ----------

    private User user(Long id, UserRole role) {
        User u = User.builder()
                .email(role.name().toLowerCase() + id + "@test.com")
                .passwordHash("secret")
                .role(role)
                .build();
        u.setId(id);
        return u;
    }

    private Lease lease(Long landlordId, Long tenantId) {
        Lease l = Lease.builder()
                .landlord(user(landlordId, UserRole.USER))
                .primaryTenant(user(tenantId, UserRole.USER))
                .status(LeaseStatus.ACTIVE)
                .leaseType(LeaseType.FIXED_TERM)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .monthlyRent(BigDecimal.valueOf(1000))
                .build();
        l.setId(LEASE_ID);
        return l;
    }
}
```

---

## Apéndice C — `LeaseSecurityEndpointsTest.java`

Sigue patrón de `SecurityEndpointsTest`. Si `LeaseController` no existe aún, sustituir por uno de los controllers ya cargados en el contexto y dejar el test marcado como `@Disabled` hasta que el controller llegue (OR-200+).

```java
package com.openroof.openroof.controller;

import com.openroof.openroof.config.SecurityConfig;
import com.openroof.openroof.security.JwtAuthenticationFilter;
import com.openroof.openroof.security.LeaseSecurity;
import com.openroof.openroof.security.PropertyViewRateLimitingFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { /* LeaseController.class — sustituir cuando exista */ })
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class})
class LeaseSecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockitoBean private LeaseSecurity leaseSecurity;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @MockitoBean private com.openroof.openroof.config.SecurityHeadersFilter securityHeadersFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(propertyViewRateLimitingFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            ServletRequest req = inv.getArgument(0);
            ServletResponse res = inv.getArgument(1);
            FilterChain chain = inv.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(securityHeadersFilter).doFilter(any(), any(), any());

        doAnswer(inv -> {
            jakarta.servlet.http.HttpServletResponse res = inv.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void leaseEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/api/leases/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/leases")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/leases/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/leases/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void rentalEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/api/rentals/anything")).andExpect(status().isUnauthorized());
    }

    @Test
    void leasePaymentEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/api/lease-payments/anything")).andExpect(status().isUnauthorized());
    }
}
```
