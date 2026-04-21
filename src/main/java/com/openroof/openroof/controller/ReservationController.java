package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.reservation.CancelReservationRequest;
import com.openroof.openroof.dto.reservation.CreateReservationRequest;
import com.openroof.openroof.dto.reservation.ReservationResponse;
import com.openroof.openroof.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservations", description = "Reserva de propiedades en línea")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Reservar una propiedad")
    public ResponseEntity<ApiResponse<ReservationResponse>> create(
            @Valid @RequestBody CreateReservationRequest request,
            Principal principal) {
        ReservationResponse res = reservationService.createReservation(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(res, "Reserva creada"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Ver una reserva")
    public ResponseEntity<ApiResponse<ReservationResponse>> getById(
            @PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getById(id, principal.getName())));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mis reservas")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> myReservations(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.getMyReservations(principal.getName(), pageable)));
    }

    @GetMapping("/owner")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reservas sobre propiedades del dueño autenticado")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> ownerReservations(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.getReservationsAsOwner(principal.getName(), pageable)));
    }

    @GetMapping("/my/property/{propertyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mi reserva activa para una propiedad (si existe)")
    public ResponseEntity<ApiResponse<ReservationResponse>> myReservationForProperty(
            @PathVariable Long propertyId, Principal principal) {
        return reservationService.getMyReservationForProperty(propertyId, principal.getName())
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    @GetMapping("/property/{propertyId}")
    @PreAuthorize("@propertySecurity.canModify(#propertyId, principal)")
    @Operation(summary = "Reservas de una propiedad")
    public ResponseEntity<ApiResponse<List<ReservationResponse>>> byProperty(
            @PathVariable Long propertyId, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.getByProperty(propertyId, principal.getName())));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Confirmar reserva (owner/agent/ADMIN)")
    public ResponseEntity<ApiResponse<ReservationResponse>> confirm(
            @PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.confirmReservation(id, principal.getName()),
                "Reserva confirmada"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancelar reserva")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelReservationRequest request,
            Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.cancelReservation(id, request, principal.getName()),
                "Reserva cancelada"));
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Convertir reserva a contrato")
    public ResponseEntity<ApiResponse<ReservationResponse>> convert(
            @PathVariable Long id, Principal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.convertToContract(id, principal.getName()),
                "Reserva convertida a contrato"));
    }
}