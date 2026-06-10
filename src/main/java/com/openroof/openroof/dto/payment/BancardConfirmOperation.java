package com.openroof.openroof.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Elemento operation del POST single_buy_confirm que Bancard envía a la confirmation_url.
 * shop_process_id y ticket_number pueden llegar como número o string según la versión;
 * se declaran String y Jackson coacciona. Campos desconocidos se ignoran.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BancardConfirmOperation(
        String token,

        @JsonProperty("shop_process_id")
        String shopProcessId,

        String response,

        @JsonProperty("response_details")
        String responseDetails,

        String amount,

        String currency,

        @JsonProperty("authorization_number")
        String authorizationNumber,

        @JsonProperty("ticket_number")
        String ticketNumber,

        @JsonProperty("response_code")
        String responseCode,

        @JsonProperty("response_description")
        String responseDescription,

        @JsonProperty("extended_response_description")
        String extendedResponseDescription
) {}
