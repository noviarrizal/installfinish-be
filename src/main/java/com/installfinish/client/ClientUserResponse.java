package com.installfinish.client;

import com.installfinish.security.Role;
import java.time.Instant;
import java.util.UUID;

public record ClientUserResponse(
        UUID id,
        UUID clientAccountId,
        String identitySubject,
        String email,
        String fullName,
        Role role,
        boolean active,
        Instant createdAt) {

    static ClientUserResponse from(ClientUser user) {
        return new ClientUserResponse(user.getId(), user.getClientAccountId(), user.getIdentitySubject(),
                user.getEmail(), user.getFullName(), user.getRole(), user.isActive(), user.getCreatedAt());
    }
}
