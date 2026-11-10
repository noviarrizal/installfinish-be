package com.installfinish.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.installfinish.security.CorsProperties;
import com.installfinish.security.InstallFinishSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** Binds real properties through Spring Boot, so a change in profile handling or binding fails here. */
class ProductionSafetyChecksTest {

    private static final String SAFE_DB = "jdbc:postgresql://db.example/installfinish?sslmode=require";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(Properties.class, ProductionSafetyChecks.class);

    @Test
    void safeProductionConfigurationStarts() {
        prod().withPropertyValues(
                        "spring.datasource.url=" + SAFE_DB,
                        "spring.flyway.url=" + SAFE_DB,
                        "installfinish.cors.allowed-origins=https://app.installfinish.example")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(ProductionSafetyChecks.class));
    }

    @Test
    void unsafeProductionConfigurationRefusesToStart() {
        prod().withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://db.example/installfinish",
                        "spring.flyway.url=" + SAFE_DB,
                        "installfinish.cors.allowed-origins=https://app.installfinish.example, http://insecure.example,https://*.example",
                        "installfinish.security.public-api-docs=true")
                .run(context -> assertThat(context).hasFailed().getFailure().rootCause()
                        .hasMessageContaining("DATABASE_URL must set sslmode")
                        .hasMessageContaining("'http://insecure.example'")
                        .hasMessageContaining("'https://*.example'")
                        .hasMessageContaining("public-api-docs must be false")
                        .hasMessageNotContaining("FLYWAY_URL"));
    }

    @Test
    void checksDoNotRunOutsideTheProductionProfile() {
        runner.withPropertyValues("spring.datasource.url=jdbc:postgresql://localhost/db", "spring.flyway.url=x")
                .run(context -> assertThat(context).hasNotFailed().doesNotHaveBean(ProductionSafetyChecks.class));
    }

    private ApplicationContextRunner prod() {
        return runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties({CorsProperties.class, InstallFinishSecurityProperties.class})
    static class Properties {
    }
}
