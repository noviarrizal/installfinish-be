package com.installfinish;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.installfinish.support.PostgresIntegrationTest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies the migrated schema against PostgreSQL: Flyway history, Hibernate validation (the context only starts if
 * entities match the schema), and the database-level guarantees the application relies on.
 */
class DatabaseSchemaIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private Flyway flyway;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allMigrationsAreAppliedSuccessfully() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(applied).extracting(info -> info.getVersion().getVersion())
                .containsExactly("1", "2", "3", "4", "5");
        assertThat(Arrays.stream(applied).map(MigrationInfo::getState)).containsOnly(MigrationState.SUCCESS);
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    }

    @Test
    void foundationTablesExist() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = current_schema()",
                String.class);

        assertThat(tables).contains("client_account", "client_user", "service_location", "municipality", "playbook",
                "playbook_checklist_item", "installation_job", "closeout_case", "document", "task", "approval",
                "audit_event");
    }

    @Test
    void auditEventsAreAppendOnly() {
        UUID eventId = jdbc.queryForObject("""
                insert into audit_event (actor_type, action, entity_type, source_system)
                values ('SYSTEM', 'test.event', 'test', 'test') returning id""", UUID.class);

        assertThatThrownBy(() -> jdbc.update("update audit_event set action = 'tampered' where id = ?", eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("delete from audit_event where id = ?", eventId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.execute("truncate audit_event"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void compositeForeignKeysPreventCrossClientReferences() {
        UUID clientA = insertClient();
        UUID clientB = insertClient();
        UUID jobOfA = insertJob(clientA);

        assertThatThrownBy(() -> jdbc.update(
                "insert into closeout_case (client_account_id, installation_job_id) values (?, ?)", clientB, jobOfA))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into installation_job (client_account_id, service_location_id, job_type, intake_channel, created_by)
                values (?, (select service_location_id from installation_job where id = ?), 'HVAC', 'MANUAL', 'test')""",
                clientB, jobOfA))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void approvedPlaybookVersionsAreFrozen() {
        UUID client = insertClient();
        UUID municipality = jdbc.queryForObject("""
                insert into municipality (authority_name, state_code, timezone)
                values (?, 'FL', 'America/New_York') returning id""", UUID.class, "Authority " + UUID.randomUUID());
        UUID playbook = jdbc.queryForObject("""
                insert into playbook (client_account_id, municipality_id, job_type, version_number)
                values (?, ?, 'HVAC_REPLACEMENT', 1) returning id""", UUID.class, client, municipality);
        jdbc.update("""
                insert into playbook_checklist_item (client_account_id, playbook_id, item_key, label, item_kind, owner_party, sort_order)
                values (?, ?, 'equipment_serial', 'Equipment serial number', 'DATA_FIELD', 'CUSTOMER', 1)""",
                client, playbook);

        assertThatThrownBy(() -> jdbc.update("update playbook set status = 'APPROVED' where id = ?", playbook))
                .as("approval must record who approved it and when it takes effect")
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("""
                update playbook set status = 'APPROVED', approved_by = 'Office Manager', approved_at = now(),
                                    effective_date = current_date
                where id = ?""", playbook);

        assertThatThrownBy(() -> jdbc.update("update playbook set job_type = 'OTHER' where id = ?", playbook))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update playbook_checklist_item set required = false where playbook_id = ?",
                playbook))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("""
                insert into playbook_checklist_item (client_account_id, playbook_id, item_key, label, item_kind, owner_party, sort_order)
                values (?, ?, 'late_item', 'Late item', 'DOCUMENT', 'CUSTOMER', 2)""", client, playbook))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("delete from playbook where id = ?", playbook))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.update("update playbook set status = 'RETIRED' where id = ?", playbook)).isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("update playbook set status = 'APPROVED' where id = ?", playbook))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void approvalPayloadHashIsComputedByDatabaseAndDecisionsAreImmutable() {
        UUID client = insertClient();
        UUID caseId = jdbc.queryForObject(
                "insert into closeout_case (client_account_id, installation_job_id) values (?, ?) returning id",
                UUID.class, client, insertJob(client));
        UUID approval = jdbc.queryForObject("""
                insert into approval (client_account_id, closeout_case_id, action_type, payload_snapshot, payload_sha256, requested_by)
                values (?, ?, 'PERMIT_SUBMISSION', '{"permitType": "MECHANICAL", "fee": "125.00"}'::jsonb, ?, 'operator-1')
                returning id""", UUID.class, client, caseId, "0".repeat(64));

        Boolean hashMatches = jdbc.queryForObject("""
                select payload_sha256 = encode(sha256(convert_to(payload_snapshot::text, 'UTF8')), 'hex')
                from approval where id = ?""", Boolean.class, approval);
        assertThat(hashMatches).isTrue();

        assertThatThrownBy(() -> jdbc.update(
                "update approval set payload_snapshot = '{\"fee\": \"0\"}'::jsonb where id = ?", approval))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("""
                update approval set status = 'APPROVED', approver_name = 'Office Manager', decision_channel = 'EMAIL',
                                    decided_at = now()
                where id = ?""", approval);

        assertThatThrownBy(() -> jdbc.update("update approval set decision_comment = 'edited' where id = ?", approval))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("delete from approval where id = ?", approval))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UUID insertClient() {
        return jdbc.queryForObject("""
                insert into client_account (legal_name, display_name, timezone)
                values ('Test HVAC LLC', 'Test HVAC', 'America/New_York') returning id""", UUID.class);
    }

    private UUID insertJob(UUID client) {
        UUID location = jdbc.queryForObject("""
                insert into service_location (client_account_id, address_line1, city, state_code, postal_code)
                values (?, '1 Main St', 'Austin', 'TX', '78701') returning id""", UUID.class, client);
        return jdbc.queryForObject("""
                insert into installation_job (client_account_id, service_location_id, job_type, intake_channel, created_by)
                values (?, ?, 'HVAC_REPLACEMENT', 'MANUAL', 'test') returning id""", UUID.class, client, location);
    }
}
