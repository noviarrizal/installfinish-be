package com.installfinish.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.installfinish.audit.AuditService;
import com.installfinish.common.error.ConflictException;
import com.installfinish.common.error.ResourceNotFoundException;
import com.installfinish.security.Actor;
import com.installfinish.security.Role;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ClientAccountServiceTest {

    @Mock
    private ClientAccountRepository clientAccountRepository;
    @Mock
    private AuditService auditService;

    private ClientAccountService service;

    @BeforeEach
    void setUp() {
        service = new ClientAccountService(clientAccountRepository, auditService);
    }

    @Test
    void clientUserCannotReachAnotherAccountAndDoesNotLearnWhetherItExists() {
        Actor approverOfA = Actor.clientUser("u", Set.of(Role.CUSTOMER_APPROVER), UUID.randomUUID());
        UUID accountB = UUID.randomUUID();

        assertThatThrownBy(() -> service.get(approverOfA, accountB)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.requireActiveAccount(approverOfA, accountB))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(clientAccountRepository);
    }

    @Test
    void newWorkIsRejectedForSuspendedAccount() {
        UUID id = UUID.randomUUID();
        ClientAccount suspended = new ClientAccount("Acme HVAC LLC", "Acme HVAC", "America/Chicago", null, null);
        ReflectionTestUtils.setField(suspended, "status", ClientAccount.Status.SUSPENDED);
        when(clientAccountRepository.findById(id)).thenReturn(Optional.of(suspended));

        Actor operator = Actor.staff("op", Set.of(Role.OPERATOR));

        assertThatThrownBy(() -> service.requireActiveAccount(operator, id))
                .isInstanceOfSatisfying(ConflictException.class,
                        e -> assertThat(e.code()).isEqualTo("CLIENT_ACCOUNT_NOT_ACTIVE"));
    }

    @Test
    void onlyStaffCanListAccounts() {
        Actor viewer = Actor.clientUser("u", Set.of(Role.CUSTOMER_VIEWER), UUID.randomUUID());

        assertThatThrownBy(() -> service.list(viewer, Pageable.unpaged())).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(clientAccountRepository);
    }
}
