package com.installfinish;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.installfinish.security.Role;
import com.installfinish.support.PostgresIntegrationTest;
import com.installfinish.support.TestJwts;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** Full application context against PostgreSQL: health, OpenAPI generation and exposure of operational endpoints. */
class ApplicationIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReportsUpWithLiveDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
    }

    @Test
    void openApiDocumentDescribesTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("InstallFinish Ops API"))
                .andExpect(jsonPath("$.paths['/api/v1/health'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/jobs'].post").exists())
                .andExpect(jsonPath("$.paths['/api/v1/cases/{caseId}'].get").exists())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.CreateInstallationJobRequest.required",
                        hasItem("clientAccountId")))
                // The authenticated principal is resolved from the token, never a client-supplied parameter.
                .andExpect(jsonPath("$.paths['/api/v1/jobs'].get.parameters[?(@.name == 'actor')]").isEmpty());
    }

    @Test
    void actuatorEndpointsOtherThanHealthRequireSystemAdmin() throws Exception {
        mockMvc.perform(get("/actuator/info")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/info")
                        .header(HttpHeaders.AUTHORIZATION, TestJwts.staff("op", Role.OPERATOR)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/actuator/info")
                        .header(HttpHeaders.AUTHORIZATION, TestJwts.staff("admin", Role.SYSTEM_ADMIN)))
                .andExpect(status().isOk());
    }

    /** Spring Security 7 would serve this publicly; the full application (all servlet filters) must not. */
    @Test
    void protectedResourceMetadataIsNotPublished() throws Exception {
        String root = "/.well-known/oauth-protected-resource";
        for (String path : List.of(root, root + "/api/v1")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().stringValues(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                    .andExpect(content().string(not(containsString("tls_client_certificate_bound_access_tokens"))));
            mockMvc.perform(get(path).header(HttpHeaders.AUTHORIZATION, TestJwts.staff("admin", Role.SYSTEM_ADMIN)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.resource").doesNotExist());
        }
    }

    @Test
    void pathsOutsideTheApiAreDenied() throws Exception {
        mockMvc.perform(get("/internal/anything")
                        .header(HttpHeaders.AUTHORIZATION, TestJwts.staff("admin", Role.SYSTEM_ADMIN)))
                .andExpect(status().isForbidden());
    }
}
