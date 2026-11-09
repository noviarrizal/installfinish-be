package com.installfinish.support;

import com.installfinish.security.Role;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Mints real RS256 tokens with a key pair generated per test run, and the matching decoder (issuer and audience
 * validated, as in production). No key material is stored in the repository.
 */
public final class TestJwts {

    public static final String ISSUER = "https://issuer.test.installfinish.invalid";
    public static final String AUDIENCE = "installfinish-api";
    public static final String STAFF_ROLES_CLAIM = "installfinish_roles";

    private static final KeyPair KEY_PAIR = generateKeyPair();
    private static final JwtEncoder ENCODER = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(
            new RSAKey.Builder((RSAPublicKey) KEY_PAIR.getPublic()).privateKey(KEY_PAIR.getPrivate()).build())));

    private TestJwts() {
    }

    public static JwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) KEY_PAIR.getPublic()).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(ISSUER),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(AUDIENCE))));
        return decoder;
    }

    /** Bearer header value for a staff member holding the given roles. */
    public static String staff(String subject, Role... roles) {
        return "Bearer " + encode(subject, Arrays.stream(roles).map(Role::name).toList(), AUDIENCE);
    }

    /** Bearer header value for a person whose access is determined by their client_user record. */
    public static String user(String subject) {
        return "Bearer " + encode(subject, List.of(), AUDIENCE);
    }

    public static String withAudience(String subject, String audience, Role... roles) {
        return "Bearer " + encode(subject, Arrays.stream(roles).map(Role::name).toList(), audience);
    }

    private static String encode(String subject, List<String> roles, String audience) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(subject)
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim(STAFF_ROLES_CLAIM, roles)
                .build();
        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        return ENCODER.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
