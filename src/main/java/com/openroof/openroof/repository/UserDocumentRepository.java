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

    /** Verificar si el usuario ya tiene un documento de cierto tipo (sin borrado lógico). */
    boolean existsByUser_IdAndDocumentType(Long userId, DocumentType documentType);
}
