package com.openroof.openroof.service;

import com.openroof.openroof.dto.contract.ContractRequest;
import com.openroof.openroof.dto.contract.ContractSummaryResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.ContractMapper;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.AgentProfileRepository;
import com.openroof.openroof.repository.ContractRepository;
import com.openroof.openroof.repository.ContractTemplateRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AgentProfileRepository agentProfileRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private ContractTemplateRepository contractTemplateRepository;
    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractService contractService;

    @Nested
    @DisplayName("getByProperty()")
    class GetByPropertyTests {

        @Test
        @DisplayName("Admin puede ver contratos de la propiedad")
        void getByProperty_adminCanAccess() {
            User admin = User.builder().email("admin@test.com").role(UserRole.ADMIN).build();
            admin.setId(1L);

            Contract contract = Contract.builder().build();
            contract.setId(10L);

            ContractSummaryResponse summary = new ContractSummaryResponse(
                    10L, null, null, null, null, null, null,
                    ContractType.SALE, ContractStatus.DRAFT, null, null, null, null
            );

            when(propertyRepository.existsById(100L)).thenReturn(true);
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
            when(contractRepository.findByProperty_Id(100L)).thenReturn(List.of(contract));
            when(contractMapper.toSummaryResponse(contract)).thenReturn(summary);

            List<ContractSummaryResponse> result = contractService.getByProperty(100L, "admin@test.com");

            assertThat(result).containsExactly(summary);
        }

        @Test
        @DisplayName("Agente participante puede ver contratos de la propiedad")
        void getByProperty_participantCanAccess() {
            User agentUser = User.builder().email("agent@test.com").role(UserRole.AGENT).build();
            agentUser.setId(2L);

            AgentProfile agentProfile = AgentProfile.builder().user(agentUser).build();
            agentProfile.setId(20L);

            Contract contract = Contract.builder().listingAgent(agentProfile).build();
            contract.setId(10L);

            ContractSummaryResponse summary = new ContractSummaryResponse(
                    10L, null, null, null, null, null, null,
                    ContractType.SALE, ContractStatus.DRAFT, null, null, null, null
            );

            when(propertyRepository.existsById(100L)).thenReturn(true);
            when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agentUser));
            when(contractRepository.findByProperty_Id(100L)).thenReturn(List.of(contract));
            when(agentProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(agentProfile));
            when(contractMapper.toSummaryResponse(contract)).thenReturn(summary);

            List<ContractSummaryResponse> result = contractService.getByProperty(100L, "agent@test.com");

            assertThat(result).containsExactly(summary);
        }

        @Test
        @DisplayName("Usuario sin participación no puede ver contratos de la propiedad")
        void getByProperty_nonParticipantThrows() {
            User user = User.builder().email("user@test.com").role(UserRole.AGENT).build();
            user.setId(3L);

            AgentProfile otherAgent = AgentProfile.builder().build();
            otherAgent.setId(30L);

            AgentProfile requesterAgent = AgentProfile.builder().user(user).build();
            requesterAgent.setId(31L);

            Contract contract = Contract.builder().listingAgent(otherAgent).build();

            when(propertyRepository.existsById(100L)).thenReturn(true);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(contractRepository.findByProperty_Id(100L)).thenReturn(List.of(contract));
            when(agentProfileRepository.findByUser_Id(3L)).thenReturn(Optional.of(requesterAgent));

            assertThatThrownBy(() -> contractService.getByProperty(100L, "user@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No tiene permiso");
        }

        @Test
        @DisplayName("Propiedad inexistente lanza ResourceNotFoundException")
        void getByProperty_propertyNotFound() {
            when(propertyRepository.existsById(100L)).thenReturn(false);

            assertThatThrownBy(() -> contractService.getByProperty(100L, "user@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Propiedad no encontrada");
        }
    }

        @Nested
        @DisplayName("create()")
        class CreateTests {

        @Test
        @DisplayName("Rechaza commissionPct negativo")
        void create_negativeCommissionPctThrows() {
            Property property = Property.builder().build();
            property.setId(100L);

            User buyer = User.builder().email("buyer@test.com").build();
            buyer.setId(200L);

            User seller = User.builder().email("seller@test.com").build();
            seller.setId(300L);

            ContractRequest request = new ContractRequest(
                100L,
                200L,
                300L,
                null,
                null,
                ContractType.SALE,
                new BigDecimal("100000"),
                new BigDecimal("-1.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                null,
                null
            );

            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(userRepository.findById(200L)).thenReturn(Optional.of(buyer));
            when(userRepository.findById(300L)).thenReturn(Optional.of(seller));
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));

            assertThatThrownBy(() -> contractService.create(request, "requester@test.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("commissionPct debe estar entre 0 y 100");
        }

        @Test
        @DisplayName("Rechaza listingAgentCommissionPct mayor a 100")
        void create_listingPctGreaterThanHundredThrows() {
            Property property = Property.builder().build();
            property.setId(100L);

            User buyer = User.builder().email("buyer@test.com").build();
            buyer.setId(200L);

            User seller = User.builder().email("seller@test.com").build();
            seller.setId(300L);

            AgentProfile listingAgent = AgentProfile.builder().build();
            listingAgent.setId(400L);

            ContractRequest request = new ContractRequest(
                100L,
                200L,
                300L,
                400L,
                null,
                ContractType.SALE,
                new BigDecimal("100000"),
                    new BigDecimal("100.00"),
                new BigDecimal("101.00"),
                BigDecimal.ZERO,
                null,
                null,
                null,
                null
            );

            when(propertyRepository.findById(100L)).thenReturn(Optional.of(property));
            when(userRepository.findById(200L)).thenReturn(Optional.of(buyer));
            when(userRepository.findById(300L)).thenReturn(Optional.of(seller));
            when(agentProfileRepository.findById(400L)).thenReturn(Optional.of(listingAgent));
            when(userRepository.findByEmail("buyer@test.com")).thenReturn(Optional.of(buyer));

            assertThatThrownBy(() -> contractService.create(request, "requester@test.com"))
                .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("listingAgentCommissionPct debe estar entre 0 y 100");
        }
        }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Participante puede hacer soft delete")
        void delete_participantCanSoftDelete() {
            User agentUser = User.builder().email("agent@test.com").role(UserRole.AGENT).build();
            agentUser.setId(2L);

            AgentProfile agentProfile = AgentProfile.builder().user(agentUser).build();
            agentProfile.setId(20L);

            Contract contract = Contract.builder().listingAgent(agentProfile).build();
            contract.setId(10L);

            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(userRepository.findByEmail("agent@test.com")).thenReturn(Optional.of(agentUser));
            when(agentProfileRepository.findByUser_Id(2L)).thenReturn(Optional.of(agentProfile));

            contractService.delete(10L, "agent@test.com");

            assertThat(contract.getDeletedAt()).isNotNull();
            verify(contractRepository).save(contract);
        }

        @Test
        @DisplayName("Usuario sin acceso no puede borrar contrato")
        void delete_nonParticipantThrows() {
            User user = User.builder().email("user@test.com").role(UserRole.AGENT).build();
            user.setId(3L);

            AgentProfile otherAgent = AgentProfile.builder().build();
            otherAgent.setId(30L);

            AgentProfile requesterAgent = AgentProfile.builder().user(user).build();
            requesterAgent.setId(31L);

            Contract contract = Contract.builder().listingAgent(otherAgent).build();
            contract.setId(10L);

            when(contractRepository.findById(10L)).thenReturn(Optional.of(contract));
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(agentProfileRepository.findByUser_Id(3L)).thenReturn(Optional.of(requesterAgent));

            assertThatThrownBy(() -> contractService.delete(10L, "user@test.com"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("No tiene permiso para modificar este contrato");
        }
    }
}
