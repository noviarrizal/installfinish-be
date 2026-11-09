package com.installfinish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.installfinish.audit.AuditEvent;
import com.installfinish.audit.AuditEventRepository;
import com.installfinish.closeout.CloseoutCaseRepository;
import com.installfinish.job.InstallationJobRepository;
import com.installfinish.security.Role;
import com.installfinish.support.PostgresIntegrationTest;
import com.installfinish.support.TestJwts;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * End-to-end client isolation: real tokens, security filter chain, services, repositories and PostgreSQL.
 * Every test creates its own client accounts, so tests are independent without truncating (audit is append-only).
 */
class TenantIsolationIntegrationTest extends PostgresIntegrationTest {

    private static final String ADMIN = TestJwts.staff("admin-1", Role.SYSTEM_ADMIN);
    private static final String OPERATOR = TestJwts.staff("operator-1", Role.OPERATOR);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private InstallationJobRepository installationJobRepository;
    @Autowired
    private CloseoutCaseRepository closeoutCaseRepository;
    @Autowired
    private AuditEventRepository auditEventRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID clientA;
    private UUID clientB;
    private String approverOfA;
    private String approverSubject;
    private Created jobA;
    private Created jobB;

    @BeforeEach
    void setUp() throws Exception {
        clientA = createClient("Alpha Heating & Air");
        clientB = createClient("Bravo Mechanical");
        approverSubject = "idp|" + UUID.randomUUID();
        perform(post("/api/v1/client-accounts/" + clientA + "/users"), ADMIN, """
                { "identitySubject": "%s", "email": "office@alpha.example", "fullName": "Alex Office",
                  "role": "CUSTOMER_APPROVER" }""".formatted(approverSubject))
                .andExpect(status().isCreated());
        approverOfA = TestJwts.user(approverSubject);
        jobA = createJob(clientA, "A-" + UUID.randomUUID());
        jobB = createJob(clientB, "B-" + UUID.randomUUID());
    }

    @Test
    void clientUserSeesOnlyTheirOwnJobsAndCases() throws Exception {
        MvcResult cases = perform(get("/api/v1/cases?size=100"), approverOfA, null)
                .andExpect(status().isOk()).andReturn();
        List<String> caseClients = JsonPath.read(cases.getResponse().getContentAsString(), "$.items[*].clientAccountId");
        List<String> caseIds = JsonPath.read(cases.getResponse().getContentAsString(), "$.items[*].id");
        assertThat(caseClients).containsOnly(clientA.toString());
        assertThat(caseIds).contains(jobA.caseId.toString()).doesNotContain(jobB.caseId.toString());

        MvcResult jobs = perform(get("/api/v1/jobs?size=100"), approverOfA, null)
                .andExpect(status().isOk()).andReturn();
        List<String> jobClients = JsonPath.read(jobs.getResponse().getContentAsString(), "$.items[*].clientAccountId");
        assertThat(jobClients).containsOnly(clientA.toString());
    }

    @Test
    void clientUserGetsNotFoundForAnotherClientsRecords() throws Exception {
        perform(get("/api/v1/jobs/" + jobB.jobId), approverOfA, null).andExpect(status().isNotFound());
        perform(get("/api/v1/cases/" + jobB.caseId), approverOfA, null).andExpect(status().isNotFound());
        perform(get("/api/v1/client-accounts/" + clientB), approverOfA, null).andExpect(status().isNotFound());

        perform(get("/api/v1/jobs/" + jobA.jobId), approverOfA, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closeoutCase.id").value(jobA.caseId.toString()));
        perform(get("/api/v1/client-accounts/" + clientA), approverOfA, null).andExpect(status().isOk());
    }

    @Test
    void clientUserCannotFilterByAnotherClient() throws Exception {
        perform(get("/api/v1/cases?clientAccountId=" + clientB), approverOfA, null)
                .andExpect(status().isForbidden());
        perform(get("/api/v1/jobs?clientAccountId=" + clientB), approverOfA, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void clientUserCannotWriteOrUseStaffEndpoints() throws Exception {
        perform(post("/api/v1/jobs"), approverOfA, jobBody(clientA, "X-" + UUID.randomUUID()))
                .andExpect(status().isForbidden());
        perform(get("/api/v1/client-accounts"), approverOfA, null).andExpect(status().isForbidden());
    }

    @Test
    void scopedRepositoryLookupsNeverCrossClients() {
        assertThat(installationJobRepository.findWithLocationByIdAndClientAccountId(jobB.jobId, clientA)).isEmpty();
        assertThat(installationJobRepository.findWithLocationByIdAndClientAccountId(jobA.jobId, clientA)).isPresent();
        assertThat(closeoutCaseRepository.findByIdAndClientAccountId(jobB.caseId, clientA)).isEmpty();
        assertThat(closeoutCaseRepository.findByIdAndClientAccountId(jobA.caseId, clientA)).isPresent();
    }

    @Test
    void deactivatedClientUserLosesAccessImmediately() throws Exception {
        jdbc.update("update client_user set active = false where identity_subject = ?", approverSubject);

        perform(get("/api/v1/jobs/" + jobA.jobId), approverOfA, null).andExpect(status().isForbidden());
    }

    @Test
    void suspendedClientCannotReceiveNewJobs() throws Exception {
        jdbc.update("update client_account set status = 'SUSPENDED' where id = ?", clientB);

        perform(post("/api/v1/jobs"), OPERATOR, jobBody(clientB, "B-" + UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CLIENT_ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void duplicateExternalJobIdIsRejectedPerClient() throws Exception {
        String externalId = "DUP-" + UUID.randomUUID();
        createJob(clientA, externalId);

        perform(post("/api/v1/jobs"), OPERATOR, jobBody(clientA, externalId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_EXTERNAL_JOB_ID"));
        // The same external id at a different client is a different job.
        createJob(clientB, externalId);
    }

    @Test
    void intakeIsAuditedWithCorrelationIdAndWithoutHomeownerData() throws Exception {
        MvcResult result = perform(post("/api/v1/jobs").header("X-Request-Id", "audit-test-0001"), OPERATOR,
                jobBody(clientA, "AUD-" + UUID.randomUUID()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.closeoutCase.caseReference").value(startsWith("IF-")))
                .andExpect(jsonPath("$.closeoutCase.status").value("RECEIVED"))
                .andReturn();
        UUID jobId = UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));

        List<AuditEvent> events = auditEventRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtAsc("installation_job", jobId);
        assertThat(events).singleElement().satisfies(event -> {
            assertThat(event.getAction()).isEqualTo("installation_job.created");
            assertThat(event.getActorSubject()).isEqualTo("operator-1");
            assertThat(event.getActorType()).isEqualTo(AuditEvent.ActorType.STAFF);
            assertThat(event.getClientAccountId()).isEqualTo(clientA);
            assertThat(event.getRequestId()).isEqualTo("audit-test-0001");
            assertThat(event.getChanges().toString()).doesNotContain("pat@example.com", "SN-998877");
        });
    }

    private UUID createClient(String name) throws Exception {
        MvcResult result = perform(post("/api/v1/client-accounts"), ADMIN, """
                { "legalName": "%s LLC", "displayName": "%s", "timezone": "America/New_York" }"""
                .formatted(name, name))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        return UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(), "$.id"));
    }

    private Created createJob(UUID clientId, String externalJobId) throws Exception {
        MvcResult result = perform(post("/api/v1/jobs"), OPERATOR, jobBody(clientId, externalJobId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new Created(UUID.fromString(JsonPath.read(body, "$.id")),
                UUID.fromString(JsonPath.read(body, "$.closeoutCase.id")));
    }

    private static String jobBody(UUID clientId, String externalJobId) {
        return """
                {
                  "clientAccountId": "%s",
                  "externalJobId": "%s",
                  "jobType": "HVAC_REPLACEMENT",
                  "serviceLocation": {
                    "addressLine1": "1 Main St", "city": "Deerfield Beach", "stateCode": "FL", "postalCode": "33441"
                  },
                  "homeownerName": "Pat Homeowner",
                  "homeownerEmail": "pat@example.com",
                  "equipment": [ { "equipmentType": "CONDENSER", "serialNumber": "SN-998877" } ]
                }""".formatted(clientId, externalJobId);
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, String bearer, String body)
            throws Exception {
        request.header(HttpHeaders.AUTHORIZATION, bearer);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(body);
        }
        return mockMvc.perform(request);
    }

    private record Created(UUID jobId, UUID caseId) {
    }
}
