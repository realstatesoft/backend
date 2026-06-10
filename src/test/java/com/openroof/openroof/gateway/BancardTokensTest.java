package com.openroof.openroof.gateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vectores calculados externamente con md5 estándar para fijar el contrato:
 * cualquier cambio accidental en la concatenación rompe estos tests.
 */
class BancardTokensTest {

    private static final String PRIVATE_KEY = "test-private-key";

    @Test
    @DisplayName("single_buy: md5(private_key + shop_process_id + amount + currency)")
    void singleBuyToken() {
        assertThat(BancardTokens.singleBuyToken(PRIVATE_KEY, 873L, "350000.00", "PYG"))
                .isEqualTo("5372ad2f9356746a60122a1e9ef1c616");
    }

    @Test
    @DisplayName("confirm: md5(private_key + shop_process_id + \"confirm\" + amount + currency)")
    void confirmToken() {
        assertThat(BancardTokens.confirmToken(PRIVATE_KEY, "873", "350000.00", "PYG"))
                .isEqualTo("961babce3a9a0c19ae12f571be74d10d");
    }

    @Test
    @DisplayName("get_confirmation: md5(private_key + shop_process_id + \"get_confirmation\")")
    void getConfirmationToken() {
        assertThat(BancardTokens.getConfirmationToken(PRIVATE_KEY, 873L))
                .isEqualTo("755d5560fc75668d93bdec36480ca905");
    }

    @Test
    @DisplayName("rollback: md5(private_key + shop_process_id + \"rollback\" + \"0.00\")")
    void rollbackToken() {
        assertThat(BancardTokens.rollbackToken(PRIVATE_KEY, 873L))
                .isEqualTo("eb58549d8b80f62a058bdadde4e4f28b");
    }

    @Test
    @DisplayName("formatAmount: siempre 2 decimales con punto, sin notación científica")
    void formatAmount() {
        assertThat(BancardTokens.formatAmount(new BigDecimal("350000"))).isEqualTo("350000.00");
        assertThat(BancardTokens.formatAmount(new BigDecimal("1234.5"))).isEqualTo("1234.50");
        assertThat(BancardTokens.formatAmount(new BigDecimal("2500000.00"))).isEqualTo("2500000.00");
        assertThat(BancardTokens.formatAmount(new BigDecimal("0.1"))).isEqualTo("0.10");
    }

    @Test
    @DisplayName("md5 produce hex en minúsculas de 32 caracteres")
    void md5Format() {
        String hash = BancardTokens.md5("hola");
        assertThat(hash).hasSize(32).matches("[0-9a-f]+");
    }
}
