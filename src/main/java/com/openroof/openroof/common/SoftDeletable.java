package com.openroof.openroof.common;

import java.time.LocalDateTime;

/**
 * Interfaz para entidades con borrado lógico (soft delete).
 */
public interface SoftDeletable {

    LocalDateTime getDeletedAt();

    void setDeletedAt(LocalDateTime deletedAt);

    default void softDelete() {
        setDeletedAt(LocalDateTime.now());
    }

    default boolean isDeleted() {
        return getDeletedAt() != null;
    }

    default void restore() {
        setDeletedAt(null);
    }
}
