package com.installfinish.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class ActorTest {

    private static final UUID CLIENT_A = UUID.randomUUID();
    private static final UUID CLIENT_B = UUID.randomUUID();

    @Test
    void staffMayFilterByAnyClientOrNone() {
        Actor operator = Actor.staff("op-1", Set.of(Role.OPERATOR));

        assertThat(operator.resolveClientFilter(null)).isEmpty();
        assertThat(operator.resolveClientFilter(CLIENT_B)).contains(CLIENT_B);
        assertThat(operator.canAccessClient(CLIENT_A)).isTrue();
    }

    @Test
    void clientUserIsAlwaysScopedToOwnAccount() {
        Actor viewer = Actor.clientUser("viewer-1", Set.of(Role.CUSTOMER_VIEWER), CLIENT_A);

        assertThat(viewer.resolveClientFilter(null)).contains(CLIENT_A);
        assertThat(viewer.resolveClientFilter(CLIENT_A)).contains(CLIENT_A);
        assertThat(viewer.canAccessClient(CLIENT_A)).isTrue();
        assertThat(viewer.canAccessClient(CLIENT_B)).isFalse();
    }

    @Test
    void clientUserCannotRequestAnotherAccount() {
        Actor viewer = Actor.clientUser("viewer-1", Set.of(Role.CUSTOMER_VIEWER), CLIENT_A);

        assertThatThrownBy(() -> viewer.resolveClientFilter(CLIENT_B)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void staffAndCustomerRolesCannotBeMixed() {
        assertThatThrownBy(() -> Actor.staff("x", Set.of(Role.OPERATOR, Role.CUSTOMER_APPROVER)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Actor.clientUser("x", Set.of(Role.OPERATOR), CLIENT_A))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
