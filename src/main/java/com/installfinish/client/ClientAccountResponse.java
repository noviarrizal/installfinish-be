package com.installfinish.client;

import java.time.Instant;
import java.util.UUID;

public record ClientAccountResponse(
        UUID id,
        String legalName,
        String displayName,
        String timezone,
        String operatingArea,
        String contractTier,
        ClientAccount.Status status,
        Instant createdAt,
        Instant updatedAt,
        long version) {

    static ClientAccountResponse from(ClientAccount account) {
        return new ClientAccountResponse(account.getId(), account.getLegalName(), account.getDisplayName(),
                account.getTimezone(), account.getOperatingArea(), account.getContractTier(), account.getStatus(),
                account.getCreatedAt(), account.getUpdatedAt(), account.getVersion());
    }
}
