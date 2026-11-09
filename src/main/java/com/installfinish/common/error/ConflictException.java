package com.installfinish.common.error;

/**
 * The request is valid but conflicts with current state (duplicate identifier, inactive client, etc.).
 * The message is returned to the caller, so it must not contain internal details.
 */
public class ConflictException extends RuntimeException {

    private final String code;

    public ConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
