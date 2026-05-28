package com.openroof.openroof.exception;

import com.openroof.openroof.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleGeneral - Debería retornar mensaje genérico y ocultar detalles del error")
    void handleGeneral_returnsGenericMessage() {
        Exception ex = new RuntimeException("Detalle altamente confidencial del servidor");
        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Error interno del servidor", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    @DisplayName("handleStorage - Debería retornar mensaje genérico de almacenamiento y ocultar detalles")
    void handleStorage_returnsGenericMessage() {
        StorageException ex = new StorageException("No se pudo conectar a Supabase token vencido");
        ResponseEntity<ApiResponse<Void>> response = handler.handleStorage(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());

        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Error en el servicio de almacenamiento", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    @DisplayName("handleInvalidConfiguration - Debería retornar mensaje genérico y ocultar detalles de configuración")
    void handleInvalidConfiguration_returnsGenericMessage() {
        InvalidConfigurationException ex = new InvalidConfigurationException("Clave API de Supabase nula");
        ResponseEntity<ApiResponse<Void>> response = handler.handleInvalidConfiguration(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Error interno del servidor", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    @DisplayName("handleDataIntegrityViolation - Debería retornar 409 y mensaje genérico de conflicto de datos")
    void handleDataIntegrityViolation_returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("violates foreign key constraint 'fk_tenant_user'");
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Operación no permitida: conflicto de datos", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    @DisplayName("handleTransactionSystem - Con ConstraintViolationException interna debería retornar 400 y mensaje de validación")
    void handleTransactionSystem_withConstraintViolation() {
        ConstraintViolationException cve = new ConstraintViolationException("El email no es válido", Collections.emptySet());
        TransactionSystemException ex = new TransactionSystemException("Transaction rolled back", cve);

        ResponseEntity<ApiResponse<Void>> response = handler.handleTransactionSystem(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());

        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Error de validación", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    @DisplayName("handleTransactionSystem - Con otra causa interna debería retornar 500 y mensaje genérico")
    void handleTransactionSystem_withOtherCause() {
        RuntimeException otherCause = new RuntimeException("Fallo de red en la transacción");
        TransactionSystemException ex = new TransactionSystemException("Could not commit JDBC transaction", otherCause);

        ResponseEntity<ApiResponse<Void>> response = handler.handleTransactionSystem(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());

        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Error interno del servidor", body.getMessage());
        assertNull(body.getData());
    }
}
