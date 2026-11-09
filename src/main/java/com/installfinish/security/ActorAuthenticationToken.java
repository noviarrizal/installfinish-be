package com.installfinish.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/** Authentication whose principal is the resolved {@link Actor}; controllers obtain it via {@code @AuthenticationPrincipal}. */
public final class ActorAuthenticationToken extends AbstractAuthenticationToken {

    private final Actor actor;
    private final Jwt token;

    public ActorAuthenticationToken(Actor actor, Jwt token) {
        super(authoritiesOf(actor));
        this.actor = actor;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Actor getPrincipal() {
        return actor;
    }

    @Override
    public Jwt getCredentials() {
        return token;
    }

    @Override
    public String getName() {
        return actor.subject();
    }

    private static Collection<SimpleGrantedAuthority> authoritiesOf(Actor actor) {
        return actor.roles().stream()
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
    }
}
