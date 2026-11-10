package com.installfinish.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.installfinish.common.health.HealthController;
import com.installfinish.support.ApiSliceTest;
import com.installfinish.support.TestJwts;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.server.resource.web.OAuth2ProtectedResourceMetadataFilter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Guards behaviour that Spring Security 7 changed by default: the always-on public protected-resource metadata
 * endpoint, the {@code resource_metadata} challenge parameter, and DPoP-scheme requests. Uses the real security chain.
 */
@WebMvcTest(HealthController.class)
@ApiSliceTest
class SecurityChainCompatibilityTest {

    /** Protected (requires a role) but unmapped in this slice: authenticated callers get 404, anonymous ones 401. */
    private static final String PROTECTED_PATH = "/api/v1/compat-probe";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private HealthEndpoint healthEndpoint;

    @Test
    void protectedResourceMetadataIsNotPublished() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-protected-resource"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.resource").doesNotExist());
    }

    @Test
    void protectedResourceMetadataIsNotPublishedToAuthenticatedCallersOrForOtherMethods() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-protected-resource")
                        .header(HttpHeaders.AUTHORIZATION, TestJwts.staff("admin-1", Role.SYSTEM_ADMIN)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().stringValues(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(content().string(not(containsString("tls_client_certificate_bound_access_tokens"))))
                .andExpect(jsonPath("$.resource").doesNotExist());
        mockMvc.perform(post("/.well-known/oauth-protected-resource"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(head("/.well-known/oauth-protected-resource"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Registered once, inside the security chain, ahead of Spring Security's metadata filter — and never as a
     * servlet container filter (it is not a bean), so it cannot run twice or outside the chain.
     */
    @Test
    void blockerIsRegisteredOnceAheadOfTheMetadataFilterAndOnlyInTheSecurityChain() {
        List<Class<?>> filters = securityFilterChain.getFilters().stream().<Class<?>>map(Object::getClass).toList();

        assertThat(filters).filteredOn(ProtectedResourceMetadataBlocker.class::equals).hasSize(1);
        assertThat(filters.indexOf(ProtectedResourceMetadataBlocker.class))
                .isGreaterThan(filters.indexOf(CorsFilter.class))
                .isLessThan(filters.indexOf(OAuth2ProtectedResourceMetadataFilter.class))
                .isLessThan(filters.indexOf(BearerTokenAuthenticationFilter.class));
        assertThat(context.getBeansOfType(ProtectedResourceMetadataBlocker.class)).isEmpty();
        assertThat(context.getBeansOfType(FilterRegistrationBean.class).values())
                .noneMatch(registration -> registration.getFilter() instanceof ProtectedResourceMetadataBlocker);
    }

    @Test
    void publicHealthIsUnaffected() throws Exception {
        HealthDescriptor up = up();
        when(healthEndpoint.health()).thenReturn(up);

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void protectedResourceMetadataSubPathsAreNotPublishedEvenWithAValidToken() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-protected-resource/api")
                        .header(HttpHeaders.AUTHORIZATION, TestJwts.staff("admin-1", Role.SYSTEM_ADMIN)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.resource").doesNotExist());
    }

    @Test
    void missingTokenChallengeHasNoResourceMetadataPointer() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void invalidTokenChallengeKeepsErrorParametersWithoutResourceMetadata() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer error=\"invalid_token\"")))
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, not(containsString("resource_metadata"))))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void validBearerTokenStillAuthenticates() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, TestJwts.staff("op-1", Role.OPERATOR)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dpopSchemeWithoutProofIsRejected() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH).header(HttpHeaders.AUTHORIZATION, dpop(TestJwts.staff("op-1", Role.OPERATOR))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dpopProofCannotBindATokenThatWasNotIssuedAsDpopBound() throws Exception {
        String token = TestJwts.staff("op-1", Role.OPERATOR).substring("Bearer ".length());

        mockMvc.perform(get(PROTECTED_PATH)
                        .header(HttpHeaders.AUTHORIZATION, "DPoP " + token)
                        .header("DPoP", proof(token, "GET", "http://localhost" + PROTECTED_PATH)))
                .andExpect(status().isUnauthorized());
    }

    private static HealthDescriptor up() {
        CompositeHealthDescriptor descriptor = mock(CompositeHealthDescriptor.class);
        when(descriptor.getStatus()).thenReturn(Status.UP);
        return descriptor;
    }

    private static String dpop(String bearerHeader) {
        return "DPoP " + bearerHeader.substring("Bearer ".length());
    }

    /** A well-formed DPoP proof for the request; the access token carries no {@code cnf} claim, so it is unbound. */
    private static String proof(String accessToken, String method, String uri) throws Exception {
        ECKey key = new ECKeyGenerator(Curve.P_256).generate();
        String ath = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(accessToken.getBytes(StandardCharsets.US_ASCII)));
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.ES256).type(new JOSEObjectType("dpop+jwt")).jwk(key.toPublicJWK()).build(),
                new JWTClaimsSet.Builder()
                        .jwtID(UUID.randomUUID().toString())
                        .claim("htm", method)
                        .claim("htu", uri)
                        .claim("ath", ath)
                        .issueTime(new Date())
                        .build());
        jwt.sign(new ECDSASigner(key));
        return jwt.serialize();
    }
}
