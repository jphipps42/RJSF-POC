package com.egs.rjsf.transformer.util;

import java.util.regex.Pattern;

public final class IdentifierValidator {
    private static final Pattern VALID_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,62}$");

    private IdentifierValidator() {}

    public static String validate(String identifier) {
        if (identifier == null || !VALID_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + identifier);
        }
        return identifier;
    }
}
