package com.installfinish.config;

import com.installfinish.security.CorsProperties;
import com.installfinish.security.InstallFinishSecurityProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Refuses to start the production profile with configuration that would weaken security: an unencrypted database
 * connection, wildcard or plain-HTTP CORS origins, or public API docs.
 */
@Component
@Profile("prod")
public class ProductionSafetyChecks {

    private static final Pattern TLS_SSLMODE = Pattern.compile("[?&]sslmode=(require|verify-ca|verify-full)(&|$)");

    public ProductionSafetyChecks(@Value("${spring.datasource.url}") String datasourceUrl,
                                  @Value("${spring.flyway.url}") String flywayUrl,
                                  CorsProperties corsProperties,
                                  InstallFinishSecurityProperties securityProperties) {
        List<String> problems = new ArrayList<>();
        if (!TLS_SSLMODE.matcher(datasourceUrl).find()) {
            problems.add("DATABASE_URL must set sslmode=require, verify-ca or verify-full");
        }
        if (!TLS_SSLMODE.matcher(flywayUrl).find()) {
            problems.add("FLYWAY_URL must set sslmode=require, verify-ca or verify-full");
        }
        corsProperties.allowedOrigins().stream()
                .filter(origin -> !origin.startsWith("https://") || origin.contains("*"))
                .forEach(origin -> problems.add("CORS origin '" + origin + "' must be an exact https:// origin"));
        if (securityProperties.publicApiDocs()) {
            problems.add("installfinish.security.public-api-docs must be false in production");
        }
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Unsafe production configuration: " + String.join("; ", problems));
        }
    }
}
