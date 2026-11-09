package com.installfinish.job;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.installfinish.closeout.CloseoutCaseStatus;
import com.installfinish.closeout.CloseoutCaseSummary;
import com.installfinish.security.Role;
import com.installfinish.support.ApiSliceTest;
import com.installfinish.support.TestJwts;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.data.util.TypeInformation;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InstallationJobController.class)
@ApiSliceTest
class InstallationJobControllerTest {

    private static final String OPERATOR = TestJwts.staff("operator-1", Role.OPERATOR);
    private static final String REVIEWER = TestJwts.staff("reviewer-1", Role.QUALITY_REVIEWER);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstallationJobService installationJobService;

    @Test
    void requestWithoutTokenIsRejectedWithProblemResponse() throws Exception {
        mockMvc.perform(get("/api/v1/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.requestId").exists());
        verifyNoInteractions(installationJobService);
    }

    @Test
    void tokenForAnotherAudienceIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, TestJwts.withAudience("op", "other-api", Role.OPERATOR)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void staffWithoutAnyRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/jobs").header(HttpHeaders.AUTHORIZATION, TestJwts.staff("nobody")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void qualityReviewerCannotReceiveJobs() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, REVIEWER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(UUID.randomUUID())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(installationJobService);
    }

    @Test
    void invalidFieldsAreReportedIndividually() throws Exception {
        String body = """
                {
                  "jobType": "replacement",
                  "serviceLocation": { "addressLine1": "1 Main St", "city": "Austin", "stateCode": "Texas", "postalCode": "7870" }
                }
                """;

        mockMvc.perform(post("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, OPERATOR)
                        .header("X-Request-Id", "test-request-0001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.requestId").value("test-request-0001"))
                .andExpect(jsonPath("$.errors[*].field").value(containsInAnyOrder(
                        "clientAccountId", "jobType", "serviceLocation.stateCode", "serviceLocation.postalCode")));
        verifyNoInteractions(installationJobService);
    }

    @Test
    void malformedJsonIsBadRequestWithoutParserDetails() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, OPERATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientAccountId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Failed to read request"));
    }

    @Test
    void unknownPropertiesAreRejected() throws Exception {
        String body = validBody(UUID.randomUUID()).replaceFirst("\\{", "{ \"clientAccountStatus\": \"ACTIVE\",");

        mockMvc.perform(post("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, OPERATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(installationJobService);
    }

    @Test
    void operatorReceivesJobAndGetsLocationOfNewResource() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(installationJobService.create(any(), any())).thenReturn(response(jobId, clientId));

        mockMvc.perform(post("/api/v1/jobs")
                        .header(HttpHeaders.AUTHORIZATION, OPERATOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(clientId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/jobs/" + jobId))
                .andExpect(jsonPath("$.closeoutCase.caseReference").value("IF-100001"))
                .andExpect(jsonPath("$.closeoutCase.status").value("RECEIVED"))
                .andExpect(jsonPath("$.createdAt").value("2026-11-09T15:00:00Z"));

        verify(installationJobService).create(
                argThat(actor -> actor.subject().equals("operator-1") && actor.roles().contains(Role.OPERATOR)),
                argThat(request -> request.clientAccountId().equals(clientId)));
    }

    @Test
    void invalidPathIdentifierIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/jobs/not-a-uuid").header(HttpHeaders.AUTHORIZATION, OPERATOR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    void unknownSortPropertyIsBadRequest() throws Exception {
        when(installationJobService.list(any(), any(), any())).thenThrow(new PropertyReferenceException(
                "password", TypeInformation.of(InstallationJob.class), List.of()));

        mockMvc.perform(get("/api/v1/jobs?sort=password").header(HttpHeaders.AUTHORIZATION, OPERATOR))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("sort"));
    }

    private static String validBody(UUID clientId) {
        return """
                {
                  "clientAccountId": "%s",
                  "externalJobId": "ST-1001",
                  "jobType": "HVAC_REPLACEMENT",
                  "serviceLocation": {
                    "addressLine1": "1 Main St", "city": "Deerfield Beach", "stateCode": "FL", "postalCode": "33441"
                  },
                  "installedDate": "2026-10-20",
                  "equipment": [ { "equipmentType": "CONDENSER", "manufacturer": "Carrier" } ]
                }
                """.formatted(clientId);
    }

    private static InstallationJobResponse response(UUID jobId, UUID clientId) {
        return new InstallationJobResponse(jobId, clientId, "ST-1001", "HVAC_REPLACEMENT", IntakeChannel.MANUAL,
                null, new InstallationJobResponse.ServiceLocationResponse(UUID.randomUUID(), "1 Main St", null,
                "Deerfield Beach", "FL", "33441", null, null),
                null, null, null, null, null, null, List.of(),
                new CloseoutCaseSummary(UUID.randomUUID(), "IF-100001", CloseoutCaseStatus.RECEIVED),
                Instant.parse("2026-11-09T15:00:00Z"), Instant.parse("2026-11-09T15:00:00Z"), 0);
    }
}
