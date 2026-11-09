package com.installfinish.security;

import com.installfinish.client.ClientAccount;
import com.installfinish.client.ClientAccountRepository;
import com.installfinish.client.ClientUser;
import com.installfinish.client.ClientUserRepository;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turns a validated JWT into an {@link Actor}.
 *
 * <ul>
 *   <li>If the token subject is linked to a {@code client_user}, the actor is that client user: role and client
 *       account come from the database, and any role claims in the token are ignored. Inactive users and users of
 *       non-active client accounts get no roles, so every API call is denied.</li>
 *   <li>Otherwise the actor is InstallFinish staff, with staff roles read from the configured claim. Unknown or
 *       customer role names in the claim are ignored.</li>
 * </ul>
 */
@Component
public class JwtActorConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final Logger log = LoggerFactory.getLogger(JwtActorConverter.class);

    private final ClientUserRepository clientUserRepository;
    private final ClientAccountRepository clientAccountRepository;
    private final String staffRolesClaim;

    public JwtActorConverter(ClientUserRepository clientUserRepository,
                             ClientAccountRepository clientAccountRepository,
                             InstallFinishSecurityProperties properties) {
        this.clientUserRepository = clientUserRepository;
        this.clientAccountRepository = clientAccountRepository;
        this.staffRolesClaim = properties.staffRolesClaim();
    }

    @Override
    @Transactional(readOnly = true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidBearerTokenException("Token has no subject");
        }
        Actor actor = clientUserRepository.findByIdentitySubject(subject)
                .map(this::clientUserActor)
                .orElseGet(() -> Actor.staff(subject, staffRoles(jwt)));
        return new ActorAuthenticationToken(actor, jwt);
    }

    private Actor clientUserActor(ClientUser user) {
        boolean accountActive = clientAccountRepository.findById(user.getClientAccountId())
                .map(ClientAccount::isActive)
                .orElse(false);
        if (!user.isActive() || !accountActive) {
            log.info("Denying access for inactive client user {} (account active: {})", user.getId(), accountActive);
            return Actor.clientUser(user.getIdentitySubject(), Set.of(), user.getClientAccountId());
        }
        return Actor.clientUser(user.getIdentitySubject(), Set.of(user.getRole()), user.getClientAccountId());
    }

    private Set<Role> staffRoles(Jwt jwt) {
        Set<Role> roles = EnumSet.noneOf(Role.class);
        for (String name : claimValues(jwt.getClaim(staffRolesClaim))) {
            parseRole(name)
                    .filter(Role::isStaff)
                    .ifPresentOrElse(roles::add,
                            () -> log.debug("Ignoring unsupported role '{}' for subject {}", name, jwt.getSubject()));
        }
        return roles;
    }

    private static Collection<String> claimValues(Object claim) {
        if (claim instanceof Collection<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        if (claim instanceof String value) {
            return List.of(value.trim().split("[\\s,]+"));
        }
        return List.of();
    }

    private static Optional<Role> parseRole(String name) {
        try {
            return Optional.of(Role.valueOf(name.trim()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
