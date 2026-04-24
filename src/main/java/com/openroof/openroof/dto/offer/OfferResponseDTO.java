package com.openroof.openroof.dto.offer;

import com.openroof.openroof.model.enums.OfferStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferResponseDTO {
    private Long id;
    private Long propertyId;
    private String propertyTitle;
    private Long buyerId;
    private String buyerName;
    private String buyerEmail;
    private String buyerPhone;
    private BigDecimal amount;
    private OfferStatus status;
    private String message;
    private String rejectionReason;
    private BigDecimal counterOfferAmount;
    private LocalDateTime createdAt;
}
