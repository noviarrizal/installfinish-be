package com.installfinish.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.installfinish.client.ClientAccount;
import com.installfinish.client.ClientAccountRepository;
import com.installfinish.client.ClientUser;
import com.installfinish.client.ClientUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtActorConverterTest {

    private static final String CLAIM = "installfinish_roles";

    @Mock
    private ClientUserRepository clientUserRepository;
    @Mock
    private ClientAccountRepository clientAccountRepository;

    private JwtActorConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JwtActorConverter(clientUserRepository, clientAccountRepository,
                new InstallFinishSecurityProperties(CLAIM, false));
    }

    @Test
    void staffRolesComeFromTheClaimAndCustomerOrUnknownRolesAreIgnored() {
        when(clientUserRepository.findByIdentitySubject("staff-1")).thenReturn(Optional.empty());

        Actor actor = convert(jwt("staff-1", List.of("OPERATOR", "QUALITY_REVIEWER", "CUSTOMER_APPROVER", "ROOT")));

        assertThat(actor.isStaff()).isTrue();
        assertThat(actor.roles()).containsExactlyInAnyOrder(Role.OPERATOR, Role.QUALITY_REVIEWER);
    }

    @Test
    void spaceSeparatedRoleClaimIsSupported() {
        when(clientUserRepository.findByIdentitySubject("staff-2")).thenReturn(Optional.empty());

        Actor actor = convert(jwt("staff-2", "SYSTEM_ADMIN OPERATOR"));

        assertThat(actor.roles()).containsExactlyInAnyOrder(Role.SYSTEM_ADMIN, Role.OPERATOR);
    }

    @Test
    void linkedClientUserGetsDatabaseRoleAndAccountAndTokenRolesAreIgnored() {
        UUID clientId = UUID.randomUUID();
        when(clientUserRepository.findByIdentitySubject("user-1"))
                .thenReturn(Optional.of(clientUser(clientId, "user-1", Role.CUSTOMER_APPROVER)));
        when(clientAccountRepository.findById(clientId)).thenReturn(Optional.of(account(ClientAccount.Status.ACTIVE)));

        Actor actor = convert(jwt("user-1", List.of("SYSTEM_ADMIN")));

        assertThat(actor.isStaff()).isFalse();
        assertThat(actor.clientAccountId()).isEqualTo(clientId);
        assertThat(actor.roles()).containsExactly(Role.CUSTOMER_APPROVER);
    }

    @Test
    void userOfSuspendedAccountGetsNoRoles() {
        UUID clientId = UUID.randomUUID();
        when(clientUserRepository.findByIdentitySubject("user-2"))
                .thenReturn(Optional.of(clientUser(clientId, "user-2", Role.CUSTOMER_VIEWER)));
        when(clientAccountRepository.findById(clientId))
                .thenReturn(Optional.of(account(ClientAccount.Status.SUSPENDED)));

        Actor actor = convert(jwt("user-2", List.of()));

        assertThat(actor.roles()).isEmpty();
        assertThat(actor.clientAccountId()).isEqualTo(clientId);
    }

    @Test
    void deactivatedUserGetsNoRolesEvenWithStaffClaims() {
        UUID clientId = UUID.randomUUID();
        ClientUser user = clientUser(clientId, "user-3", Role.CUSTOMER_APPROVER);
        ReflectionTestUtils.setField(user, "active", false);
        when(clientUserRepository.findByIdentitySubject("user-3")).thenReturn(Optional.of(user));
        when(clientAccountRepository.findById(clientId)).thenReturn(Optional.of(account(ClientAccount.Status.ACTIVE)));

        Actor actor = convert(jwt("user-3", List.of("OPERATOR")));

        assertThat(actor.roles()).isEmpty();
        assertThat(actor.isStaff()).isFalse();
    }

    @Test
    void tokenWithoutSubjectIsRejected() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").claim(CLAIM, List.of("OPERATOR"))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();

        assertThatThrownBy(() -> converter.convert(jwt)).isInstanceOf(InvalidBearerTokenException.class);
    }

    private Actor convert(Jwt jwt) {
        return ((ActorAuthenticationToken) converter.convert(jwt)).getPrincipal();
    }

    private static Jwt jwt(String subject, Object roles) {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject(subject)
                .claim(CLAIM, roles)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
    }

    private static ClientUser clientUser(UUID clientId, String subject, Role role) {
        return new ClientUser(clientId, subject, subject + "@example.com", "Test User", role);
    }

    private static ClientAccount account(ClientAccount.Status status) {
        ClientAccount account = new ClientAccount("Acme HVAC LLC", "Acme HVAC", "America/New_York", null, null);
        ReflectionTestUtils.setField(account, "status", status);
        return account;
    }
}
