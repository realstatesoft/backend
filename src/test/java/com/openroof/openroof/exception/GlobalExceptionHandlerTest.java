package com.openroof.openroof.exception;

import com.openroof.openroof.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    @DisplayName("handleGeneral - No debe incluir stack trace ni mensaje interno en la respuesta")
    void handleGeneral_doesNotExposeStackTraceOrInternalDetails() {
        Exception ex = new RuntimeException("Detalle altamente confidencial del servidor");
        ex.initCause(new IllegalStateException("at com.openroof.internal.SecretService.process(SecretService.java:42)"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);
        ApiResponse<Void> body = response.getBody();

        assertNotNull(body);
        assertEquals("Error interno del servidor", body.getMessage());
        assertNull(body.getData());
        assertFalse(body.getMessage().contains("confidencial"));
        assertFalse(body.getMessage().contains("SecretService"));
        assertFalse(body.getMessage().contains("at com.openroof"));
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
    @DisplayName("handleMessageNotReadable - Debería ocultar la causa interna de Jackson")
    void handleMessageNotReadable_hidesInternalCause() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("JSON parse error: Unrecognized field \"secretColumn\"");
        when(ex.getMostSpecificCause()).thenReturn(new IllegalArgumentException("column users.secret_column does not exist"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Solicitud inválida", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("secretColumn"));
    }

    @Test
    @DisplayName("handleConstraintViolation - Debería retornar mensaje genérico de validación")
    void handleConstraintViolation_returnsGenericValidationMessage() {
        ConstraintViolationException ex = new ConstraintViolationException(
                "violates check constraint \"fk_tenant_user\"", Collections.emptySet());

        ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error de validación", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("fk_tenant_user"));
    }

    @Test
    @DisplayName("handleDataIntegrityViolation - Debería retornar 409 y mensaje genérico")
    void handleDataIntegrityViolation_returns409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "violates foreign key constraint 'fk_tenant_user'");
        ResponseEntity<ApiResponse<Void>> response = handler.handleDataIntegrityViolation(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());

        ApiResponse<Void> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Operación no permitida", body.getMessage());
        assertNull(body.getData());
    }

    @Test
    @DisplayName("handleMaxUploadSize - Debería retornar mensaje genérico de archivo")
    void handleMaxUploadSize_returnsFileProcessingMessage() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(5_000_000L);

        ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUploadSize(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error al procesar el archivo", response.getBody().getMessage());
    }

    @Test
    @DisplayName("handleIllegalArgument - Mensaje de archivo no debe filtrar detalle interno")
    void handleIllegalArgument_fileRelated_returnsGenericFileMessage() {
        IllegalArgumentException ex = new IllegalArgumentException(
                "Content-Type inválido: application/x-msdownload; tabla storage_keys");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Error al procesar el archivo", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("storage_keys"));
    }

    @Test
    @DisplayName("handleIllegalArgument - Otros argumentos inválidos retornan operación no permitida")
    void handleIllegalArgument_nonFile_returnsOperationNotAllowed() {
        IllegalArgumentException ex = new IllegalArgumentException("commission_pct es nulo; datos incompletos");

        ResponseEntity<ApiResponse<Void>> response = handler.handleIllegalArgument(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Operación no permitida", response.getBody().getMessage());
        assertFalse(response.getBody().getMessage().contains("commission_pct"));
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
