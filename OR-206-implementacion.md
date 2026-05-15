# OR-206 — Autorización por rol en endpoints de alquileres

## 1. Plan de implementación

### 1.1 Resumen
Falta componente `LeaseSecurity` + actualizar `SecurityConfig`. Patrón ya existe (`PropertySecurity`, `LeadSecurity`). Esta task agrega:
- Helper `assertLeaseAccess(userId, leaseId)` — lanza `org.springframework.security.access.AccessDeniedException`.
- Reglas de Spring Security para endpoints `/api/leases/**`.

### 1.2 Archivos a crear / modificar

| Archivo | Acción | Motivo |
|---|---|---|
| `src/main/java/com/openroof/openroof/security/LeaseSecurity.java` | CREAR | Componente con `assertLeaseAccess` y `hasLeaseAccess`. |
| `src/main/java/com/openroof/openroof/config/SecurityConfig.java` | MODIFICAR | Agregar matchers de `/api/leases/**`. |
| `src/test/java/com/openroof/openroof/security/LeaseSecurityTest.java` | CREAR | Unit tests del helper. |
| `src/test/java/com/openroof/openroof/controller/LeaseSecurityEndpointsTest.java` | CREAR | Tests de reglas en `SecurityConfig`. |

### 1.3 Diseño de `LeaseSecurity`

Reglas:
1. `userId == null` → `AccessDeniedException("Usuario no autenticado")`.
2. Usuario `ADMIN` → acceso permitido (no se carga el lease).
3. Lease no existe → `AccessDeniedException("Lease {id} no encontrado o sin acceso")` (no leak: mismo mensaje que no-acceso).
4. Usuario = `lease.landlord.id` o `lease.primaryTenant.id` → permitido.
5. Otro caso → `AccessDeniedException("Usuario {userId} no es landlord ni tenant del lease {leaseId}")`.

Método auxiliar `hasLeaseAccess(userId, leaseId)` retorna `boolean` para usar en `@PreAuthorize`.

### 1.4 Cambios en `SecurityConfig`

Agregar antes de `.anyRequest().authenticated()`:

```java
// Endpoints públicos de alquileres (catálogo, listados de propiedades en alquiler)
.requestMatchers(HttpMethod.GET, "/api/leases/public/**").permitAll()
// Endpoints protegidos: requieren auth + validación de acceso en service/controller
.requestMatchers("/api/leases/**").authenticated()
.requestMatchers("/api/rentals/**").authenticated()
.requestMatchers("/api/lease-payments/**").authenticated()
```

Notas:
- `permitAll()` solo aplica a `/api/leases/public/**` (si el feature lo necesita; remover si no).
- La autorización fina (landlord/tenant) se hace en controller vía `@PreAuthorize("@leaseSecurity.hasLeaseAccess(principal.id, #leaseId)")` o llamada directa a `assertLeaseAccess`.

### 1.5 Pasos secuenciales

1. Crear `LeaseSecurity.java` con código del Apéndice A.
2. Inyectar `LeaseRepository` + `UserRepository` (ya existen).
3. Editar `SecurityConfig.securityFilterChain` — agregar matchers del 1.4.
4. Crear `LeaseSecurityTest.java` (Apéndice B).
5. Crear `LeaseSecurityEndpointsTest.java` (Apéndice C).
6. Correr `mvn test -Dtest=LeaseSecurityTest,LeaseSecurityEndpointsTest`.
7. Correr suite completa: `mvn test`.
8. Commit con mensaje convencional: `feat(security): add LeaseSecurity + auth rules for leases (OR-206)`.

---

## 2. Descripción para Jira (OR-206)

**Título:** `[OR-206] Autorización por rol en endpoints de alquileres`

**Descripción:**

Implementar control de acceso para endpoints relacionados con `Lease`. Hoy `/api/leases/**` no tiene reglas específicas en `SecurityConfig` y no existe componente `LeaseSecurity` análogo a `PropertySecurity` / `LeadSecurity`.

**Alcance:**
- Agregar componente `LeaseSecurity` con método `assertLeaseAccess(Long userId, Long leaseId)`.
- El método verifica que el usuario sea ADMIN, landlord o primary tenant del lease.
- Si no tiene acceso, lanza `org.springframework.security.access.AccessDeniedException` con mensaje descriptivo (el `GlobalExceptionHandler` ya lo mapea a HTTP 403).
- Configurar `SecurityConfig` con los nuevos matchers públicos/protegidos para `/api/leases/**`.

**Criterios de aceptación:**
- [ ] `LeaseSecurity.assertLeaseAccess` permite ADMIN sin consultar el lease.
- [ ] Permite al landlord del lease.
- [ ] Permite al primary tenant del lease.
- [ ] Bloquea a cualquier otro usuario con `AccessDeniedException`.
- [ ] Si el lease no existe → `AccessDeniedException` (no `EntityNotFoundException`, para no filtrar existencia).
- [ ] `SecurityConfig` exige autenticación en `/api/leases/**`.
- [ ] `GET /api/leases/public/**` accesible sin token (si aplica al feature).
- [ ] Unit tests en `LeaseSecurityTest` cubren los 5 escenarios anteriores.
- [ ] Test de integración en `LeaseSecurityEndpointsTest` verifica 401 sin token.
- [ ] `mvn test` pasa verde.

**Dependencias:**
- OR-197/198/199 (mergeado en `d845ec8`) — entidad `Lease`, `LeaseRepository`, DTOs.

**Notas técnicas:**
- Reusar patrón de `LeadSecurity` (anotación `@Component("leaseSecurity")`).
- Excepción ya manejada por `GlobalExceptionHandler.handleAccessDenied` → 403 con `ApiResponse.error("Acceso denegado")`.

---

## 3. Plan de pruebas QA — paso a paso

### Pre-requisitos
- Backend corriendo en `http://localhost:8080`.
- Postman / curl.
- Tres usuarios sembrados en BD:
  - `admin@test.com` (rol ADMIN).
  - `landlord@test.com` (rol USER, dueño del lease #1).
  - `tenant@test.com` (rol USER, primary tenant del lease #1).
  - `outsider@test.com` (rol USER, no relacionado al lease #1).
- Un `Lease` con id `1` donde landlord = `landlord@test.com`, tenant = `tenant@test.com`.

### Caso 1 — Sin token (401)
1. `GET /api/leases/1` sin header `Authorization`.
2. **Esperado:** HTTP 401, body con `success: false`.

### Caso 2 — ADMIN accede a cualquier lease (200)
1. Login como `admin@test.com` → guardar `accessToken`.
2. `GET /api/leases/1` con `Authorization: Bearer {token}`.
3. **Esperado:** HTTP 200, body del lease.

### Caso 3 — Landlord accede a su lease (200)
1. Login como `landlord@test.com`.
2. `GET /api/leases/1`.
3. **Esperado:** HTTP 200.

### Caso 4 — Tenant accede a su lease (200)
1. Login como `tenant@test.com`.
2. `GET /api/leases/1`.
3. **Esperado:** HTTP 200.

### Caso 5 — Usuario ajeno bloqueado (403)
1. Login como `outsider@test.com`.
2. `GET /api/leases/1`.
3. **Esperado:** HTTP 403, mensaje `"Acceso denegado"`.

### Caso 6 — Lease inexistente (403, no 404)
1. Login como `landlord@test.com`.
2. `GET /api/leases/999999`.
3. **Esperado:** HTTP 403 (no filtra existencia).

### Caso 7 — Endpoint público accesible sin token (si aplica)
1. `GET /api/leases/public/...` sin token.
2. **Esperado:** HTTP 200.

### Caso 8 — Operación de escritura protegida
1. Login como `outsider@test.com`.
2. `PUT /api/leases/1` con body modificando renta.
3. **Esperado:** HTTP 403.

### Caso 9 — Token expirado
1. Usar token expirado o malformado.
2. `GET /api/leases/1`.
3. **Esperado:** HTTP 401.

### Reporte de bug — plantilla
- Caso fallado: #
- Request (método + URL + headers).
- Body enviado.
- Status code recibido vs. esperado.
- Body de respuesta.
- Logs del backend si HTTP 5xx.

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
     *
     * @param userId  id del usuario autenticado
     * @param leaseId id del lease a verificar
     * @throws AccessDeniedException si no tiene acceso, con mensaje descriptivo
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
                && lease.getLandlord().getId().equals(userId);
        boolean isTenant = lease.getPrimaryTenant() != null
                && lease.getPrimaryTenant().getId().equals(userId);

        if (!isLandlord && !isTenant) {
            throw new AccessDeniedException(
                    "Usuario " + userId + " no es landlord ni tenant del lease " + leaseId);
        }
    }

    /**
     * Versión boolean para @PreAuthorize.
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
import static org.mockito.Mockito.verify;
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

    // --- assertLeaseAccess --- //

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
    void assertLeaseAccess_other_throwsAccessDenied() {
        when(userRepository.findById(OTHER_ID))
                .thenReturn(Optional.of(user(OTHER_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.of(lease(LANDLORD_ID, TENANT_ID)));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(OTHER_ID, LEASE_ID));

        assertTrue(ex.getMessage().contains(String.valueOf(OTHER_ID)));
        assertTrue(ex.getMessage().contains(String.valueOf(LEASE_ID)));
    }

    @Test
    void assertLeaseAccess_leaseNotFound_throwsAccessDenied() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.of(user(LANDLORD_ID, UserRole.USER)));
        when(leaseRepository.findById(LEASE_ID))
                .thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("lease"));
    }

    @Test
    void assertLeaseAccess_userNotFound_throwsAccessDenied() {
        when(userRepository.findById(LANDLORD_ID))
                .thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));

        assertTrue(ex.getMessage().toLowerCase().contains("usuario"));
        verifyNoInteractions(leaseRepository);
    }

    @Test
    void assertLeaseAccess_nullUserId_throwsAccessDenied() {
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
        Lease lease = lease(LANDLORD_ID, TENANT_ID);
        lease.setPrimaryTenant(null);
        when(leaseRepository.findById(LEASE_ID)).thenReturn(Optional.of(lease));

        assertDoesNotThrow(() -> leaseSecurity.assertLeaseAccess(LANDLORD_ID, LEASE_ID));
    }

    // --- hasLeaseAccess --- //

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
    }

    // --- helpers --- //

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
                .leaseType(LeaseType.RESIDENTIAL)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusYears(1))
                .monthlyRent(BigDecimal.valueOf(1000))
                .build();
        l.setId(LEASE_ID);
        return l;
    }
}
```

> Nota: `LeaseType.RESIDENTIAL` puede no existir con ese nombre — sustituir por el enum válido del proyecto al ejecutar (ver `LeaseType.java`).

---

## Apéndice C — `LeaseSecurityEndpointsTest.java`

Sigue el patrón de `SecurityEndpointsTest`. Crear cuando exista el `LeaseController` (no presente aún en `controller/`).

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { /* LeaseController.class — cuando exista */ })
@Import({SecurityConfig.class, com.openroof.openroof.config.JacksonConfig.class})
class LeaseSecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.web.context.WebApplicationContext context;

    @MockitoBean
    private LeaseSecurity leaseSecurity;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean
    private PropertyViewRateLimitingFilter propertyViewRateLimitingFilter;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private com.openroof.openroof.exception.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

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
            jakarta.servlet.http.HttpServletResponse res = inv.getArgument(1);
            res.setStatus(401);
            return null;
        }).when(jwtAuthenticationEntryPoint).commence(any(), any(), any());

        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(context)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void leaseEndpoints_requireLogin() throws Exception {
        mockMvc.perform(get("/api/leases/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/leases")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/leases/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/leases/1")).andExpect(status().isUnauthorized());
    }
}
```
