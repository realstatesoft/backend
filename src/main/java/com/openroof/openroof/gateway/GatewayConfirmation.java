package com.openroof.openroof.gateway;

/**
 * Confirmación de una transacción según el contrato single_buy_confirm de Bancard.
 * Los montos viajan como string decimal con 2 decimales (formato del JSON de Bancard).
 */
public record GatewayConfirmation(
        String shopProcessId,
        String response,            // "S" | "N"
        String responseCode,        // "00" aprobada · "05" · "12" · "15" · "51"
        String responseDescription,
        String authorizationNumber, // solo si aprobada
        String ticketNumber,
        String amount,
        String currency,
        String token
) {
    public boolean isApproved() {
        return "00".equals(responseCode);
    }
}
