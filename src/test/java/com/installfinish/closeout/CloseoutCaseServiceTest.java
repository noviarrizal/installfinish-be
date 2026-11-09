package com.installfinish.closeout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.installfinish.audit.AuditService;
import com.installfinish.common.error.ResourceNotFoundException;
import com.installfinish.security.Actor;
import com.installfinish.security.Role;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CloseoutCaseServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-09T15:00:00Z");

    @Mock
    private CloseoutCaseRepository closeoutCaseRepository;
    @Mock
    private AuditService auditService;

    private CloseoutCaseService service;

    @BeforeEach
    void setUp() {
        service = new CloseoutCaseService(closeoutCaseRepository, auditService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void clientUserLookupIsAlwaysScopedToTheirAccount() {
        UUID clientA = UUID.randomUUID();
        UUID caseOfB = UUID.randomUUID();
        Actor viewerOfA = Actor.clientUser("u", Set.of(Role.CUSTOMER_VIEWER), clientA);
        when(closeoutCaseRepository.findByIdAndClientAccountId(caseOfB, clientA)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(viewerOfA, caseOfB)).isInstanceOf(ResourceNotFoundException.class);
        verify(closeoutCaseRepository, never()).findById(any());
    }

    @Test
    void jobSummariesExcludeCasesOutsideTheActorsScope() {
        UUID clientA = UUID.randomUUID();
        UUID clientB = UUID.randomUUID();
        CloseoutCase caseA = CloseoutCase.received(clientA, UUID.randomUUID(), NOW);
        CloseoutCase caseB = CloseoutCase.received(clientB, UUID.randomUUID(), NOW);
        List<UUID> jobIds = List.of(caseA.getInstallationJobId(), caseB.getInstallationJobId());
        when(closeoutCaseRepository.findByInstallationJobIdIn(jobIds)).thenReturn(List.of(caseA, caseB));

        Actor viewerOfA = Actor.clientUser("u", Set.of(Role.CUSTOMER_VIEWER), clientA);

        assertThat(service.summariesByJobId(viewerOfA, jobIds)).containsOnlyKeys(caseA.getInstallationJobId());
    }

    @Test
    void newCasesStartReceivedWithNormalPriority() {
        CloseoutCase closeoutCase = CloseoutCase.received(UUID.randomUUID(), UUID.randomUUID(), NOW);

        assertThat(closeoutCase.getStatus()).isEqualTo(CloseoutCaseStatus.RECEIVED);
        assertThat(closeoutCase.getPriority()).isEqualTo(CasePriority.NORMAL);
        assertThat(closeoutCase.getStatusChangedAt()).isEqualTo(NOW);
    }
}
