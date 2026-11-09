package com.installfinish.security;

import jakarta.servlet.DispatcherType;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Stateless bearer-token API. Deny by default: only the health checks (and, where enabled, the API docs) are public;
 * every {@code /api/**} call needs a recognised role; everything else is denied. Fine-grained rules live on the
 * controllers ({@code @PreAuthorize}) and client scoping lives in the services.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({InstallFinishSecurityProperties.class, CorsProperties.class})
public class SecurityConfig {

    private static final String[] API_DOC_PATHS = {"/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**"};
    private static final String[] STAFF_ROLES =
            Arrays.stream(Role.values()).filter(Role::isStaff).map(Role::name).toArray(String[]::new);
    private static final String[] ALL_ROLES = Arrays.stream(Role.values()).map(Role::name).toArray(String[]::new);

    @Bean
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
                                               JwtActorConverter jwtActorConverter,
                                               SecurityProblemHandler problemHandler,
                                               InstallFinishSecurityProperties properties) throws Exception {
        http
                // No cookies or sessions: authentication is a bearer token on every request, so CSRF does not apply.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER)))
                .authorizeHttpRequests(auth -> {
                    auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/health", "/actuator/health", "/actuator/health/**")
                            .permitAll();
                    if (properties.publicApiDocs()) {
                        auth.requestMatchers(API_DOC_PATHS).permitAll();
                    } else {
                        auth.requestMatchers(API_DOC_PATHS).hasAnyRole(STAFF_ROLES);
                    }
                    auth.requestMatchers("/actuator/**").hasRole(Role.SYSTEM_ADMIN.name());
                    auth.requestMatchers("/api/**").hasAnyRole(ALL_ROLES);
                    auth.anyRequest().denyAll();
                })
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtActorConverter))
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemHandler)
                        .accessDeniedHandler(problemHandler));
        return http.build();
    }

    /** Only explicitly configured origins may call the API from a browser; with none configured, CORS is closed. */
    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Request-Id"));
        configuration.setExposedHeaders(List.of("Location", "X-Request-Id"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
