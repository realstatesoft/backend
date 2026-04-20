package com.openroof.openroof.model.enums;

/**
 * Acciones registradas en {@code audit_logs.action} (VARCHAR 50).
 */
public enum AuditAction {
    REGISTER,
    LOGIN,
    CREATE,
    UPDATE,
    DELETE,
    TRASH,
    RESTORE,
    STATUS_CHANGE,
    CLEAR_TRASH
}
