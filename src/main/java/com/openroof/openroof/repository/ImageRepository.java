package com.openroof.openroof.repository;

import com.openroof.openroof.model.image.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para la entidad Image.
 */
@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByUploadedByOrderByCreatedAtDesc(String uploadedBy);
}
