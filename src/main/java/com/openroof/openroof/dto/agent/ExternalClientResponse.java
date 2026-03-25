package com.openroof.openroof.dto.agent;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal; // Added this import to make BigDecimal resolve

// [x] **Backend Infrastructure Expansion**:
//     - [x] **Entity Update**: Expanded `ExternalClient.java` with detailed fields: `birthDate`, `maritalStatus`, `occupation`, `annualIncome`, `address`, `sourceChannel`, `isSearchingProperty`, and search preferences (`budgetRange`, `bedroomRange`, `bathroomRange`, `preferredPropertyTypes`, `preferredAreas`, `desiredFeatures`).
//     - [x] **DTO Alignment**: Updated `CreateExternalClientRequest`, `UpdateExternalClientRequest`, and `ExternalClientResponse` to handle all new fields.
//     - [x] **Service Mapping**: Implemented advanced mapping in `ExternalClientService` for embedded ranges and collection fields.
//     - [x] **Security**: Enforced resource-level access control via `@agentClientSecurity.canAccessExternal`.
// - [x] **Unified Search Integration**:
//     - [x] Integrated `internalType` filtering in `ExternalClientRepository`.
//     - [x] Updated `ClientList.jsx` and `useClients.js` to allow agents to toggle between their portfolio and external prospects.
public record ExternalClientResponse(
        Long id,
        Long agentId,
        String name,
        String email,
        String phone,
        String status,
        String priority,
        String clientType,
        LocalDateTime lastContactDate,
        String origin,
        List<String> tags,
        String notes,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        Integer minBedrooms,
        Integer maxBedrooms,
        Integer minBathrooms,
        Integer maxBathrooms,
        String maritalStatus,
        java.time.LocalDate birthDate,
        String occupation,
        BigDecimal annualIncome,
        String address,
        String sourceChannel,
        List<String> preferredPropertyTypes,
        List<String> preferredAreas,
        List<String> desiredFeatures,
        Boolean isSearchingProperty,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
