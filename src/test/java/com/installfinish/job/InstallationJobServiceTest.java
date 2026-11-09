package com.installfinish.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.installfinish.audit.AuditAction;
import com.installfinish.audit.AuditService;
import com.installfinish.client.ClientAccount;
import com.installfinish.client.ClientAccountService;
import com.installfinish.closeout.CloseoutCaseService;
import com.installfinish.closeout.CloseoutCaseStatus;
import com.installfinish.closeout.CloseoutCaseSummary;
import com.installfinish.common.error.ConflictException;
import com.installfinish.job.CreateInstallationJobRequest.ServiceLocationRequest;
import com.installfinish.security.Actor;
import com.installfinish.security.Role;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InstallationJobServiceTest {

    private static final Actor OPERATOR = Actor.staff("operator-1", Set.of(Role.OPERATOR));

    @Mock
    private InstallationJobRepository installationJobRepository;
    @Mock
    private ServiceLocationRepository serviceLocationRepository;
    @Mock
    private ClientAccountService clientAccountService;
    @Mock
    private CloseoutCaseService closeoutCaseService;
    @Mock
    private AuditService auditService;
    @Captor
    private ArgumentCaptor<Map<String, Object>> auditSnapshot;

    private InstallationJobService service;
    private UUID clientId;

    @BeforeEach
    void setUp() {
        service = new InstallationJobService(installationJobRepository, serviceLocationRepository,
                clientAccountService, closeoutCaseService, auditService);
        ClientAccount account = new ClientAccount("Acme HVAC LLC", "Acme HVAC", "America/New_York", null, null);
        clientId = UUID.randomUUID();
        ReflectionTestUtils.setField(account, "id", clientId);
        when(clientAccountService.requireActiveAccount(OPERATOR, clientId)).thenReturn(account);
    }

    @Test
    void rejectsDuplicateExternalJobIdWithoutWritingAnything() {
        when(installationJobRepository.existsByClientAccountIdAndExternalJobId(clientId, "ST-1001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(OPERATOR, request(" ST-1001 ")))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("DUPLICATE_EXTERNAL_JOB_ID"));

        verify(serviceLocationRepository, never()).save(any());
        verify(installationJobRepository, never()).save(any());
        verifyNoInteractions(closeoutCaseService, auditService);
    }

    @Test
    void intakeCreatesJobOpensCaseAndAuditsWithoutHomeownerDataOrSerials() {
        when(serviceLocationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(installationJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CloseoutCaseSummary openedCase = new CloseoutCaseSummary(UUID.randomUUID(), "IF-100001",
                CloseoutCaseStatus.RECEIVED);
        when(closeoutCaseService.openCaseForJob(eq(OPERATOR), eq(clientId), any())).thenReturn(openedCase);

        InstallationJobResponse response = service.create(OPERATOR, request("ST-1001"));

        assertThat(response.closeoutCase()).isEqualTo(openedCase);
        assertThat(response.intakeChannel()).isEqualTo(IntakeChannel.MANUAL);
        assertThat(response.externalJobId()).isEqualTo("ST-1001");
        assertThat(response.equipment()).hasSize(1);

        verify(auditService).record(eq(OPERATOR), eq(clientId), eq(AuditAction.INSTALLATION_JOB_CREATED), any(),
                auditSnapshot.capture());
        assertThat(auditSnapshot.getValue())
                .containsEntry("externalJobId", "ST-1001")
                .containsEntry("equipmentCount", 1)
                .doesNotContainKeys("homeownerName", "homeownerEmail", "homeownerPhone", "equipment");
        assertThat(auditSnapshot.getValue().values().toString())
                .doesNotContain("pat@example.com", "SN-998877", "Pat Homeowner");
    }

    @Test
    void jobWithoutExternalIdSkipsDuplicateCheckButIsStillAudited() {
        when(serviceLocationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(installationJobRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(OPERATOR, request(null));

        verify(installationJobRepository, never()).existsByClientAccountIdAndExternalJobId(any(), any());
        verify(auditService).record(eq(OPERATOR), eq(clientId), eq(AuditAction.INSTALLATION_JOB_CREATED), any(),
                anyMap());
    }

    private CreateInstallationJobRequest request(String externalJobId) {
        return new CreateInstallationJobRequest(clientId, externalJobId, "HVAC_REPLACEMENT", null, "ServiceTitan",
                new ServiceLocationRequest("1 Main St", null, "Deerfield Beach", "FL", "33441", "Broward"),
                "Pat Homeowner", "pat@example.com", "555-0100", LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 20), "Crew 3",
                List.of(new EquipmentItem("CONDENSER", "Carrier", "24ACC636", "SN-998877")));
    }
}
