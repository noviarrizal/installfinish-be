package com.installfinish.common.error;

import java.util.UUID;

/**
 * The resource does not exist or is outside the caller's client scope. Both cases produce the same response so the
 * API never reveals whether another client's record exists.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, UUID id) {
        super(resourceName + " " + id + " not found");
    }
}
