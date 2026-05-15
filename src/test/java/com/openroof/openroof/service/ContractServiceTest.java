package com.openroof.openroof.service;

import com.openroof.openroof.dto.contract.SignContractRequest;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.contract.ContractSignature;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.model.enums.*;
import com.openroof.openroof.mapper.ContractMapper;
import com.openroof.openroof.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractSignatureRepository contractSignatureRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PropertyRepository propertyRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private EmailService emailService;
    @Mock
    private ContractMapper contractMapper;

    @InjectMocks
    private ContractService contractService;

    @Test
    @DisplayName("Al completar todas las firmas de un contrato de SALE, la propiedad cambia a SOLD")
    void sign_updatesPropertyToSoldWhenAllSigned() {
        // Setup
        User buyer = User.builder().email("buyer@test.com").build(); buyer.setId(1L);
        User seller = User.builder().email("seller@test.com").build(); seller.setId(2L);
        
        Property property = Property.builder().status(PropertyStatus.PUBLISHED).owner(seller).build();
        property.setId(100L);

        Contract contract = Contract.builder()
                .property(property)
                .buyer(buyer)
                .seller(seller)
                .contractType(ContractType.SALE)
                .status(ContractStatus.PARTIALLY_SIGNED)
                .build();
        contract.setId(10L);

        SignContractRequest signRequest = new SignContractRequest(
                SignatureType.DIGITAL, SignatureRole.SELLER, "data"
        );

        // Mocking
        when(contractRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmail("seller@test.com")).thenReturn(Optional.of(seller));
        
        // Simular que falta solo la firma del vendedor
        ContractSignature buyerSig = ContractSignature.builder().role(SignatureRole.BUYER).build();
        ContractSignature sellerSig = ContractSignature.builder().role(SignatureRole.SELLER).build();
        
        when(contractSignatureRepository.findByContractIdAndDeletedAtIsNull(10L))
                .thenReturn(List.of(buyerSig, sellerSig));
        
        when(contractRepository.save(contract)).thenReturn(contract);

        // Execute
        contractService.sign(10L, signRequest, "seller@test.com", "127.0.0.1");

        // Verify
        assertThat(contract.getStatus()).isEqualTo(ContractStatus.SIGNED);
        assertThat(property.getStatus()).isEqualTo(PropertyStatus.SOLD);
        assertThat(property.getBuyer()).isEqualTo(buyer);
        verify(propertyRepository).save(property);
    }
}
