package com.openroof.openroof.service;

import com.openroof.openroof.dto.document.UpdateDocumentStatusRequest;
import com.openroof.openroof.dto.document.UserDocumentResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.document.UserDocument;
import com.openroof.openroof.model.enums.DocumentStatus;
import com.openroof.openroof.model.enums.DocumentType;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserDocumentRepository;
import com.openroof.openroof.repository.UserRepository;
import jakarta.transaction.Transactional;
import com.openroof.openroof.model.enums.DocumentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio para gestionar documentos personales del usuario.
 * Valida tipos y tamaños de archivo, sube a Supabase Storage y persiste metadatos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDocumentService {

    private final UserDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final EmailService emailService;

    @Value("${upload.documents.max-file-size:10MB}")
    private String maxFileSizeRaw;

    @Value("${upload.documents.allowed-types:application/pdf,image/jpeg,image/png,image/webp}")
    private String allowedTypesRaw;

    // ─── Lectura ───────────────────────────────────────────────────────────────

    /**
     * Retorna todos los documentos activos del usuario autenticado.
     */
    @Transactional
    public List<UserDocumentResponse> getMyDocuments(String email) {
        User user = findUser(email);
        return documentRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(UserDocumentResponse::from)
                .toList();
    }

    /**
     * Retorna todos los documentos de todos los usuarios (para Admin).
     */
    @Transactional
    public List<UserDocumentResponse> getAllDocuments() {
        return documentRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(UserDocumentResponse::from)
                .toList();
    }

    // ─── Carga ─────────────────────────────────────────────────────────────────

    /**
     * Sube un nuevo documento personal para el usuario autenticado.
     * Valida tipo y tamaño antes de subir al storage.
     */
    @Transactional
    public UserDocumentResponse upload(String email, MultipartFile file, DocumentType documentType) {
        validateFile(file);
        User user = findUser(email);

        String folder = "documents/" + user.getId();
        StorageService.UploadResult result = storageService.upload(file, folder);

        UserDocument doc = UserDocument.builder()
                .user(user)
                .documentType(documentType)
                .documentStatus(DocumentStatus.PENDING)
                .url(result.url())
                .filename(result.filename())
                .contentType(result.contentType())
                .size(result.size())
                .build();

        doc = documentRepository.save(doc);
        log.info("Documento {} subido por {} (tipo={})", doc.getId(), email, documentType);
        return UserDocumentResponse.from(doc);
    }

    // ─── Reemplazo ─────────────────────────────────────────────────────────────

    /**
     * Reemplaza el archivo de un documento existente.
     * Sube el nuevo archivo, actualiza metadatos y resetea el estado a PENDING.
     */
    @Transactional
    public UserDocumentResponse replace(String email, Long docId, MultipartFile file) {
        validateFile(file);
        User user = findUser(email);

        UserDocument doc = documentRepository.findByIdAndUser_Id(docId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + docId));

        String folder = "documents/" + user.getId();
        StorageService.UploadResult result = storageService.upload(file, folder);

        doc.setUrl(result.url());
        doc.setFilename(result.filename());
        doc.setContentType(result.contentType());
        doc.setSize(result.size());
        doc.setDocumentStatus(DocumentStatus.PENDING);
        doc.setNotes(null);

        doc = documentRepository.save(doc);
        log.info("Documento {} reemplazado por {}", docId, email);
        return UserDocumentResponse.from(doc);
    }

    // ─── Eliminación ──────────────────────────────────────────────────────────

    /**
     * Realiza borrado lógico del documento (soft delete).
     * Verifica que el documento pertenezca al usuario autenticado.
     */
    @Transactional
    public void delete(String email, Long docId) {
        User user = findUser(email);

        UserDocument doc = documentRepository.findByIdAndUser_Id(docId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + docId));

        doc.setDeletedAt(LocalDateTime.now());
        documentRepository.save(doc);
        log.info("Documento {} eliminado (soft delete) por {}", docId, email);
    }

    // ─── Admin: cambio de estado ──────────────────────────────────────────────

    /**
     * Permite a un administrador aprobar o rechazar un documento, con notas opcionales.
     */
    @Transactional
    public UserDocumentResponse updateStatus(Long docId, UpdateDocumentStatusRequest request) {
        UserDocument doc = documentRepository.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado: " + docId));

        doc.setDocumentStatus(request.getDocumentStatus());
        if (request.getNotes() != null) {
            doc.setNotes(request.getNotes());
        }

        doc = documentRepository.save(doc);
        log.info("Documento {} cambió a estado {} por admin", docId, request.getDocumentStatus());

        // Enviar notificación por email al usuario
        String userEmail = doc.getUser().getEmail();
        String userName  = doc.getUser().getName();
        String docType   = doc.getDocumentType().name();

        if (request.getDocumentStatus() == DocumentStatus.APPROVED) {
            emailService.sendDocumentApprovedEmail(userEmail, userName, docType);
        } else if (request.getDocumentStatus() == DocumentStatus.REJECTED) {
            emailService.sendDocumentRejectedEmail(userEmail, userName, docType, request.getNotes());
        }

        return UserDocumentResponse.from(doc);
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("El archivo está vacío o no fue proporcionado.");
        }

        long maxBytes = DataSize.parse(maxFileSizeRaw).toBytes();
        if (file.getSize() > maxBytes) {
            throw new BadRequestException(
                    "El archivo supera el tamaño máximo permitido de " + maxFileSizeRaw + ".");
        }

        String contentType = file.getContentType();
        List<String> allowed = List.of(allowedTypesRaw.split(","));
        if (contentType == null || !allowed.contains(contentType.trim())) {
            throw new BadRequestException(
                    "Tipo de archivo no permitido: " + contentType
                            + ". Tipos aceptados: PDF, JPG, PNG, WebP.");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }
}
