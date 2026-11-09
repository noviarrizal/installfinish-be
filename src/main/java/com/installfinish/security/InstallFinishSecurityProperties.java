package com.installfinish.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param staffRolesClaim JWT claim that lists InstallFinish staff roles
 * @param publicApiDocs   whether the OpenAPI document and Swagger UI are reachable without authentication
 */
@ConfigurationProperties("installfinish.security")
public record InstallFinishSecurityProperties(
        @DefaultValue("installfinish_roles") String staffRolesClaim,
        boolean publicApiDocs) {
}
