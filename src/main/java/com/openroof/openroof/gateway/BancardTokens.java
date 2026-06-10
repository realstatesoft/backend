package com.openroof.openroof.gateway;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Cálculo de tokens md5 según "Especificaciones Técnicas Single Buy v1.23" de Bancard.
 * Los números se transforman a cadenas y los montos llevan siempre dos decimales con punto.
 */
public final class BancardTokens {

    private BancardTokens() {
    }

    /** Monto como string con punto decimal y exactamente 2 decimales (ej. "350000.00"). */
    public static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** single_buy: md5(private_key + shop_process_id + amount + currency) */
    public static String singleBuyToken(String privateKey, long shopProcessId, String amount, String currency) {
        return md5(privateKey + shopProcessId + amount + currency);
    }

    /** single_buy_confirm: md5(private_key + shop_process_id + "confirm" + amount + currency) */
    public static String confirmToken(String privateKey, String shopProcessId, String amount, String currency) {
        return md5(privateKey + shopProcessId + "confirm" + amount + currency);
    }

    /** get_single_buy_confirmation: md5(private_key + shop_process_id + "get_confirmation") */
    public static String getConfirmationToken(String privateKey, long shopProcessId) {
        return md5(privateKey + shopProcessId + "get_confirmation");
    }

    /** single_buy_rollback: md5(private_key + shop_process_id + "rollback" + "0.00") */
    public static String rollbackToken(String privateKey, long shopProcessId) {
        return md5(privateKey + shopProcessId + "rollback" + "0.00");
    }

    public static String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 no disponible en el JRE", e);
        }
    }
}
