package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.document.UpdateDocumentStatusRequest;
import com.openroof.openroof.dto.document.UserDocumentResponse;
import com.openroof.openroof.model.enums.DocumentType;
import com.openroof.openroof.service.UserDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.security.Principal;
import java.util.List;

/**
 * Endpoints para gestión de documentos personales del usuario autenticado.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Documents", description = "Gestión de documentos personales del usuario")
public class UserDocumentController {

    private final UserDocumentService documentService;

    // ─── GET /users/me/documents ───────────────────────────────────────────────

    @Operation(
            summary = "Listar mis documentos",
            description = "Retorna todos los documentos personales del usuario autenticado."
    )
    @GetMapping("/me/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<UserDocumentResponse>>> getMyDocuments(Principal principal) {
        List<UserDocumentResponse> docs = documentService.getMyDocuments(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    // ─── POST /users/me/documents ──────────────────────────────────────────────

    @Operation(
            summary = "Subir documento personal",
            description = "Sube un nuevo documento personal (PDF, JPG, PNG, WebP — máx. 10 MB). "
                    + "El estado inicial siempre es PENDING hasta revisión del administrador."
    )
    @PostMapping(value = "/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDocumentResponse>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("documentType") DocumentType documentType,
            Principal principal
    ) {
        UserDocumentResponse doc = documentService.upload(principal.getName(), file, documentType);
        URI location = URI.create("/users/me/documents/" + doc.getId());
        return ResponseEntity.created(location).body(ApiResponse.ok(doc, "Documento subido exitosamente"));
    }

    // ─── PUT /users/me/documents/{id} ──────────────────────────────────────────

    @Operation(
            summary = "Reemplazar documento",
            description = "Reemplaza el archivo de un documento existente. Restablece el estado a PENDING."
    )
    @PutMapping(value = "/me/documents/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDocumentResponse>> replace(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        UserDocumentResponse doc = documentService.replace(principal.getName(), id, file);
        return ResponseEntity.ok(ApiResponse.ok(doc, "Documento reemplazado exitosamente"));
    }

    // ─── DELETE /users/me/documents/{id} ──────────────────────────────────────

    @Operation(
            summary = "Eliminar documento",
            description = "Realiza un borrado lógico del documento. Solo el dueño puede eliminarlo."
    )
    @DeleteMapping("/me/documents/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            Principal principal
    ) {
        documentService.delete(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Documento eliminado exitosamente"));
    }

    // ─── GET /users/documents (ADMIN) ──────────────────────────────────────────

    @Operation(
            summary = "Listar todos los documentos (Admin)",
            description = "Retorna todos los documentos de todos los usuarios en el sistema para su revisión."
    )
    @GetMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDocumentResponse>>> getAllDocuments() {
        List<UserDocumentResponse> docs = documentService.getAllDocuments();
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    // ─── PATCH /users/documents/{id}/status (ADMIN) ───────────────────────────

    @Operation(
            summary = "Actualizar estado del documento (Admin)",
            description = "Permite a un administrador aprobar o rechazar un documento con notas opcionales."
    )
    @PatchMapping("/documents/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDocumentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDocumentStatusRequest request
    ) {
        UserDocumentResponse doc = documentService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(doc, "Estado actualizado exitosamente"));
    }
}
