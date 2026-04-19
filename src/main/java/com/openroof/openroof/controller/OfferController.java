package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.offer.OfferRequestDTO;
import com.openroof.openroof.dto.offer.OfferResponseDTO;
import com.openroof.openroof.dto.offer.UpdateOfferStatusDTO;
import com.openroof.openroof.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/offers")
@RequiredArgsConstructor
@Tag(name = "Offers", description = "Gestión de ofertas sobre propiedades")
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Realizar una oferta")
    public ResponseEntity<ApiResponse<OfferResponseDTO>> createOffer(
            @Valid @RequestBody OfferRequestDTO request,
            Principal principal) {
        OfferResponseDTO response = offerService.createOffer(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Oferta enviada con éxito"));
    }

    @GetMapping("/me/buyer")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis ofertas realizadas")
    public ResponseEntity<ApiResponse<Page<OfferResponseDTO>>> getMyOffersAsBuyer(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        Page<OfferResponseDTO> offers = offerService.getMyOffersAsBuyer(principal.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(offers));
    }

    @GetMapping("/me/received")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'AGENT', 'ADMIN')")
    @Operation(summary = "Ofertas recibidas")
    public ResponseEntity<ApiResponse<Page<OfferResponseDTO>>> getReceivedOffers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        Page<OfferResponseDTO> offers = offerService.getReceivedOffers(principal.getName(), pageable);
        return ResponseEntity.ok(ApiResponse.ok(offers));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'AGENT', 'ADMIN')")
    @Operation(summary = "Actualizar estado de una oferta")
    public ResponseEntity<ApiResponse<OfferResponseDTO>> updateOfferStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOfferStatusDTO request,
            Principal principal) {
        OfferResponseDTO response = offerService.updateOfferStatus(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Estado de oferta actualizado"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Editar una oferta (Comprador)")
    public ResponseEntity<ApiResponse<OfferResponseDTO>> updateOffer(
            @PathVariable Long id,
            @Valid @RequestBody OfferRequestDTO request,
            Principal principal) {
        OfferResponseDTO response = offerService.updateOffer(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Oferta editada con éxito"));
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ofertas de una propiedad específica")
    public ResponseEntity<ApiResponse<List<OfferResponseDTO>>> getOffersByProperty(
            @PathVariable Long propertyId,
            Principal principal) {
        List<OfferResponseDTO> offers = offerService.getOffersByProperty(propertyId, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(offers));
    }
}
