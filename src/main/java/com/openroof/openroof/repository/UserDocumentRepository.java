package com.openroof.openroof.repository;

import com.openroof.openroof.model.document.UserDocument;
import com.openroof.openroof.model.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {

    /** Todos los documentos activos del usuario, ordenados del más reciente al más antiguo. */
    List<UserDocument> findByUser_IdOrderByCreatedAtDesc(Long userId);

    /** Buscar un documento específico asegurando que pertenezca al usuario (seguridad). */
    Optional<UserDocument> findByIdAndUser_Id(Long id, Long userId);

    /**
     * Verifica si el usuario ya tiene un documento activo (no borrado) de cierto tipo.
     * <p>La consulta derivada respeta la restricción de soft delete definida en la entidad
     * ({@code @SQLRestriction("deleted_at IS NULL")}), por lo que solo considera documentos
     * cuyo campo {@code deleted_at} sea nulo. Como consecuencia, si un documento fue borrado
     * lógicamente el usuario podrá volver a subir uno del mismo tipo.
     */
    boolean existsByUser_IdAndDocumentType(Long userId, DocumentType documentType);
}
