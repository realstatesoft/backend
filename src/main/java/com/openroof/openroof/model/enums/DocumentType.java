package com.openroof.openroof.model.enums;

public enum DocumentType {
    // ─── KYC Obligatorios (Paraguay) ───────────────────────────────────────────
    ID_FRONT,       // Cédula de Identidad — frente
    ID_BACK,        // Cédula de Identidad — reverso
    SELFIE,         // Foto del rostro del titular
    PROOF_OF_ADDRESS, // Comprobante de domicilio

    // ─── Adicionales / Financieros ─────────────────────────────────────────────
    ID,             // Tipo legacy (compatibilidad con datos existentes)
    PROOF_OF_INCOME,
    TAX_RETURN,
    BANK_STATEMENT,
    OTHER
}

