package com.openroof.openroof.common;

public final class PasswordPolicy {

    public static final String COMPLEXITY_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{10,}$";

    public static final String COMPLEXITY_MESSAGE =
            "La contraseña debe tener al menos 10 caracteres e incluir mayúsculas, minúsculas, números y un carácter especial";

    private PasswordPolicy() {}
}
