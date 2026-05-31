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
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

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

    @Test
    @DisplayName("handleValidation - Con violaciones normales de validación debería retornar HTTP 400")
    void handleValidation_withNormalViolations_returns400() {
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        
        FieldError fieldError = new FieldError("objectName", "fieldName", "El campo es requerido");
        Mockito.when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Error de validación", response.getBody().getMessage());
        assertEquals("El campo es requerido", response.getBody().getData().get("fieldName"));
    }

    @Test
    @DisplayName("handleValidation - Con violaciones de MaxDigits debería retornar HTTP 422")
    void handleValidation_withMaxDigitsViolations_returns422() {
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        
        FieldError fieldError = new FieldError("objectName", "amount", null, false, new String[]{"MaxDigits"}, null, "El campo no puede tener más de 20 dígitos enteros");
        
        Mockito.when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("El campo numérico excede la longitud máxima permitida", response.getBody().getMessage());
        assertEquals("El campo no puede tener más de 20 dígitos enteros", response.getBody().getData().get("amount"));
    }

    @Test
    @DisplayName("handleValidation - Con violaciones de Digits debería retornar HTTP 422")
    void handleValidation_withDigitsViolations_returns422() {
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        
        FieldError fieldError = new FieldError("objectName", "amount", null, false, new String[]{"Digits"}, null, "El monto excede el límite permitido");
        
        Mockito.when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("El campo numérico excede la longitud máxima permitida", response.getBody().getMessage());
        assertEquals("El monto excede el límite permitido", response.getBody().getData().get("amount"));
    }

    @Test
    @DisplayName("handleValidation - Con códigos prefijados de MaxDigits (como MaxDigits.contrato.monto) debería retornar HTTP 422")
    void handleValidation_withMaxDigitsPrefixViolations_returns422() {
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        
        FieldError fieldError = new FieldError("objectName", "amount", null, false, new String[]{"MaxDigits.contract.amount", "MaxDigits"}, null, "El campo no puede tener más de 20 dígitos enteros");
        
        Mockito.when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("El campo numérico excede la longitud máxima permitida", response.getBody().getMessage());
        assertEquals("El campo no puede tener más de 20 dígitos enteros", response.getBody().getData().get("amount"));
    }

    @Test
    @DisplayName("handleValidation - Con códigos prefijados de Digits (como Digits.monto) debería retornar HTTP 422")
    void handleValidation_withDigitsPrefixViolations_returns422() {
        MethodArgumentNotValidException ex = Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = Mockito.mock(BindingResult.class);
        
        FieldError fieldError = new FieldError("objectName", "amount", null, false, new String[]{"Digits.amount", "Digits.java.math.BigDecimal", "Digits"}, null, "El monto excede el límite permitido");
        
        Mockito.when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));
        Mockito.when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiResponse<Map<String, String>>> response = handler.handleValidation(ex);

        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("El campo numérico excede la longitud máxima permitida", response.getBody().getMessage());
        assertEquals("El monto excede el límite permitido", response.getBody().getData().get("amount"));
    }
}
