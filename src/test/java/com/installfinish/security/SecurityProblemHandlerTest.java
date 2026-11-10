package com.installfinish.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the {@code resource_metadata} stripping to what Spring Security actually emits. The stripping only removes a
 * trailing parameter, so the order Spring Security uses is asserted here explicitly.
 */
class SecurityProblemHandlerTest {

    private static final String RFC6750 = "https://tools.ietf.org/html/rfc6750#section-3.1";

    private final SecurityProblemHandler handler = new SecurityProblemHandler(JsonMapper.builder().build());

    @Test
    void springSecurityAppendsResourceMetadataAsTheLastChallengeParameter() throws Exception {
        MockHttpServletResponse raw = new MockHttpServletResponse();
        new BearerTokenAuthenticationEntryPoint().commence(request(), raw, invalidTokenWithScope());

        assertThat(raw.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer error=\"invalid_token\", "
                + "error_description=\"Token expired\", error_uri=\"" + RFC6750 + "\", scope=\"jobs:read\", "
                + "resource_metadata=\"https://api.example/.well-known/oauth-protected-resource\"");
    }

    @Test
    void invalidTokenChallengeKeepsEveryOtherParameterInOrder() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.commence(request(), response, invalidTokenWithScope());

        assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).containsExactly("Bearer error=\"invalid_token\", "
                + "error_description=\"Token expired\", error_uri=\"" + RFC6750 + "\", scope=\"jobs:read\"");
        assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHORIZED\"").doesNotContain("Token expired");
    }

    @Test
    void missingTokenChallengeIsPlainBearer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.commence(request(), response, new InsufficientAuthenticationException("no token"));

        assertThat(response.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).containsExactly("Bearer");
    }

    @Test
    void accessDeniedChallengeIsUnchanged() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.handle(request(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        // The 403 handler never adds resource_metadata; it is not wrapped, and the challenge is passed through as is.
        assertThat(response.getHeaders(HttpHeaders.WWW_AUTHENTICATE)).containsExactly("Bearer");
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
    }

    @Test
    void onlyATrailingResourceMetadataParameterIsRemoved() {
        assertThat(SecurityProblemHandler.withoutResourceMetadata("Bearer resource_metadata=\"https://a/m\""))
                .isEqualTo("Bearer");
        assertThat(SecurityProblemHandler.withoutResourceMetadata(
                "Bearer error=\"invalid_token\", resource_metadata=\"https://a/m\""))
                .isEqualTo("Bearer error=\"invalid_token\"");
        // Text that merely mentions the name, or a parameter that is not last, is never rewritten.
        String mentions = "Bearer error=\"invalid_token\", error_description=\"bad resource_metadata=\\\"x\\\" value\"";
        assertThat(SecurityProblemHandler.withoutResourceMetadata(mentions)).isEqualTo(mentions);
        String notLast = "Bearer resource_metadata=\"https://a/m\", error=\"invalid_token\"";
        assertThat(SecurityProblemHandler.withoutResourceMetadata(notLast)).isEqualTo(notLast);
        assertThat(SecurityProblemHandler.withoutResourceMetadata("Bearer realm=\"api\"")).isEqualTo("Bearer realm=\"api\"");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/jobs");
        request.setScheme("https");
        request.setServerName("api.example");
        request.setServerPort(443);
        return request;
    }

    private static OAuth2AuthenticationException invalidTokenWithScope() {
        return new OAuth2AuthenticationException(
                new BearerTokenError("invalid_token", HttpStatus.UNAUTHORIZED, "Token expired", RFC6750, "jobs:read"));
    }
}
