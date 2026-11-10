package com.installfinish.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Protocol-compatibility hardening for Spring Security 7. Its resource-server configurer always registers a filter
 * that serves RFC 9728 protected-resource metadata at {@code /.well-known/oauth-protected-resource}, publicly and ahead
 * of authorization, with no switch to turn it off. That document is not an authentication bypass — it grants no
 * access to any API — but we have not chosen to publish one, and the default advertises
 * {@code tls_client_certificate_bound_access_tokens: true}, a capability we do not support. This filter runs just
 * before it and answers exactly as the deny-by-default chain did on Spring Security 6: 401 with the standard problem.
 *
 * <p>Deliberately not a {@code @Component}: it is created in {@link SecurityConfig} and lives only in the security
 * filter chain, so Spring Boot never also registers it as a servlet container filter. {@link OncePerRequestFilter}
 * guarantees one execution per request.
 */
final class ProtectedResourceMetadataBlocker extends OncePerRequestFilter {

    static final String METADATA_PATH = "/.well-known/oauth-protected-resource";

    private final RequestMatcher metadataRequests = PathPatternRequestMatcher.withDefaults().matcher(METADATA_PATH + "/**");
    private final SecurityProblemHandler problemHandler;

    ProtectedResourceMetadataBlocker(SecurityProblemHandler problemHandler) {
        this.problemHandler = problemHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (metadataRequests.matches(request)) {
            problemHandler.commence(request, response,
                    new InsufficientAuthenticationException("Protected resource metadata is not published"));
            return;
        }
        chain.doFilter(request, response);
    }
}
