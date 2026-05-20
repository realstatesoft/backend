package com.openroof.openroof.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@code ApiResponse — static factories}.
 */
@DisplayName("ApiResponse — static factories")
class ApiResponseTest {

    // ─── ok(data) ────────────────────────────────────────────────────────────

    /**
     * ok(data) sets success=true, data, and non-null timestamp.
     */
    @Test
    @DisplayName("ok(data) sets success=true, data, and non-null timestamp")
    void ok_data_setsSuccessTrue() {
        ApiResponse<String> resp = ApiResponse.ok("payload");

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo("payload");
        assertThat(resp.getTimestamp()).isNotNull();
        assertThat(resp.getMessage()).isNull();
    }

    /**
     * ok(null data) is still successful — null data is valid.
     */
    @Test
    @DisplayName("ok(null data) is still successful — null data is valid")
    void ok_nullData_isSuccessful() {
        ApiResponse<String> resp = ApiResponse.ok(null);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isNull();
    }

    /**
     * ok(data) works with any generic type — Integer.
     */
    @Test
    @DisplayName("ok(data) works with any generic type — Integer")
    void ok_data_worksWithIntegerType() {
        ApiResponse<Integer> resp = ApiResponse.ok(42);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo(42);
    }

    // ─── ok(data, message) ───────────────────────────────────────────────────

    /**
     * ok(data, message) sets success=true, data, and message.
     */
    @Test
    @DisplayName("ok(data, message) sets success=true, data, and message")
    void ok_dataAndMessage_setsAllFields() {
        ApiResponse<String> resp = ApiResponse.ok("payload", "Operación exitosa");

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getData()).isEqualTo("payload");
        assertThat(resp.getMessage()).isEqualTo("Operación exitosa");
        assertThat(resp.getTimestamp()).isNotNull();
    }

    /**
     * ok(data, message) with null message still returns success=true.
     */
    @Test
    @DisplayName("ok(data, message) with null message still returns success=true")
    void ok_dataAndNullMessage_isSuccessful() {
        ApiResponse<String> resp = ApiResponse.ok("data", null);

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isNull();
    }

    // ─── error(message) ──────────────────────────────────────────────────────

    /**
     * error(message) sets success=false and the error message.
     */
    @Test
    @DisplayName("error(message) sets success=false and the error message")
    void error_message_setsSuccessFalse() {
        ApiResponse<Object> resp = ApiResponse.error("Recurso no encontrado");

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isEqualTo("Recurso no encontrado");
        assertThat(resp.getTimestamp()).isNotNull();
        assertThat(resp.getData()).isNull();
    }

    /**
     * error(null) sets success=false with null message.
     */
    @Test
    @DisplayName("error(null) sets success=false with null message")
    void error_nullMessage_setsSuccessFalse() {
        ApiResponse<Object> resp = ApiResponse.error(null);

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isNull();
    }

    // ─── ok vs error discriminator ───────────────────────────────────────────

    /**
     * ok and error are mutually exclusive on success flag.
     */
    @Test
    @DisplayName("ok and error are mutually exclusive on success flag")
    void ok_and_error_haveOppositeSuccessFlags() {
        ApiResponse<String> ok = ApiResponse.ok("data");
        ApiResponse<Object> error = ApiResponse.error("fail");

        assertThat(ok.isSuccess()).isTrue();
        assertThat(error.isSuccess()).isFalse();
    }

    // ─── builder ─────────────────────────────────────────────────────────────

    /**
     * builder allows constructing a fully custom response.
     */
    @Test
    @DisplayName("builder allows constructing a fully custom response")
    void builder_constructsCustomResponse() {
        ApiResponse<Integer> resp = ApiResponse.<Integer>builder()
                .success(true)
                .message("Creado")
                .data(99)
                .build();

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("Creado");
        assertThat(resp.getData()).isEqualTo(99);
    }
}
