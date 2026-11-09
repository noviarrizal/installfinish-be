package com.installfinish.security;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;

/**
 * The authenticated caller, as resolved by {@link JwtActorConverter}. Services receive the actor explicitly and use
 * {@link #clientScope()} to restrict every read and write to the data the caller may see.
 *
 * <p>Staff actors (InstallFinish employees) carry no client scope and hold only staff roles. Client-user actors are
 * pinned to exactly one client account, taken from the {@code client_user} table — never from the request.
 */
public record Actor(String subject, Set<Role> roles, UUID clientAccountId) {

    public Actor {
        Objects.requireNonNull(subject, "subject");
        roles = roles.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(roles));
        boolean expectStaff = clientAccountId == null;
        if (roles.stream().anyMatch(role -> role.isStaff() != expectStaff)) {
            throw new IllegalArgumentException(expectStaff
                    ? "Staff actors cannot hold customer roles"
                    : "Client-user actors cannot hold staff roles");
        }
    }

    public static Actor staff(String subject, Set<Role> roles) {
        return new Actor(subject, roles, null);
    }

    public static Actor clientUser(String subject, Set<Role> roles, UUID clientAccountId) {
        return new Actor(subject, roles, Objects.requireNonNull(clientAccountId, "clientAccountId"));
    }

    public boolean isStaff() {
        return clientAccountId == null;
    }

    /** Empty for staff (all clients); otherwise the only client account this actor may access. */
    public Optional<UUID> clientScope() {
        return Optional.ofNullable(clientAccountId);
    }

    public boolean canAccessClient(UUID clientAccountId) {
        return isStaff() || this.clientAccountId.equals(clientAccountId);
    }

    /**
     * The client filter to apply to a collection query. Staff may filter by any client or none; client users are
     * always restricted to their own account and may not ask for another one.
     *
     * @param requested the client account id supplied by the caller, or {@code null}
     */
    public Optional<UUID> resolveClientFilter(UUID requested) {
        if (isStaff()) {
            return Optional.ofNullable(requested);
        }
        if (requested != null && !requested.equals(clientAccountId)) {
            throw new AccessDeniedException("Client users can only query their own client account");
        }
        return Optional.of(clientAccountId);
    }
}
