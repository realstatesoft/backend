package com.openroof.openroof.service;

import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.contract.ContractSignature;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.ContractRepository;
import com.openroof.openroof.repository.ContractSignatureRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractPdfServiceTest {

    @Mock
    private ContractRepository contractRepository;
    @Mock
    private ContractSignatureRepository signatureRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ContractService contractService;

    @InjectMocks
    private ContractPdfService contractPdfService;

    private User buyer;
    private User seller;
    private Property property;
    private Contract contract;

    @BeforeEach
    void setUp() {
        buyer = User.builder().email("buyer@test.com").name("Buyer Test").build();
        buyer.setId(1L);
        
        seller = User.builder().email("seller@test.com").name("Seller Test").build();
        seller.setId(2L);
        
        property = Property.builder().title("Test Property").build();
        property.setId(1L);
        
        contract = Contract.builder()
                .buyer(buyer)
                .seller(seller)
                .property(property)
                .amount(new BigDecimal("150000.00"))
                .currencyCode("PYG")
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();
        contract.setId(1L);
    }

    @Test
    @DisplayName("Debe generar PDF exitosamente cuando los datos son correctos")
    void generatePdf_Success() {
        // Arrange
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(seller));
        when(contractService.canAccess(any(), any())).thenReturn(true);
        when(signatureRepository.findByContractIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        // Act
        byte[] pdfBytes = contractPdfService.generatePdf(1L, "admin@test.com");

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        verify(contractService).canAccess(any(), any());
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el usuario no tiene acceso")
    void generatePdf_NoAccess() {
        // Arrange
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmail("stranger@test.com")).thenReturn(Optional.of(buyer));
        when(contractService.canAccess(any(), any())).thenReturn(false);

        // Act & Assert
        assertThrows(BadRequestException.class, () -> 
            contractPdfService.generatePdf(1L, "stranger@test.com")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando el contrato está incompleto (falta comprador)")
    void generatePdf_IncompleteContract() {
        // Arrange
        contract.setBuyer(null);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(seller));
        when(contractService.canAccess(any(), any())).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
            contractPdfService.generatePdf(1L, "admin@test.com")
        );
        assertTrue(ex.getMessage().contains("falta el comprador"), 
            "Debe indicar específicamente que falta el comprador");
    }

    @Test
    @DisplayName("Debe lanzar excepción cuando la propiedad es nula")
    void generatePdf_NullProperty() {
        // Arrange
        contract.setProperty(null);
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(seller));
        when(contractService.canAccess(any(), any())).thenReturn(true);

        // Act & Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> 
            contractPdfService.generatePdf(1L, "admin@test.com")
        );
        assertTrue(ex.getMessage().contains("falta la propiedad"),
            "Debe indicar específicamente que falta la propiedad");
    }

    @Test
    @DisplayName("Debe generar PDF exitosamente con variantes de cláusulas (masculinos, compuestos, etc)")
    void generatePdf_ClauseVariants_Success() {
        // Arrange
        contract.setTerms("PRIMERO. El precio...\nDÉCIMO PRIMERO. Otros términos...\n12) Jurisdicción...");
        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(seller));
        when(contractService.canAccess(any(), any())).thenReturn(true);
        when(signatureRepository.findByContractIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        // Act
        byte[] pdfBytes = contractPdfService.generatePdf(1L, "admin@test.com");

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
