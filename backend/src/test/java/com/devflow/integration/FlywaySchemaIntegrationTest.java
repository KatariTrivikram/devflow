package com.devflow.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Flyway Schema Integration Test")
class FlywaySchemaIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("should verify that Flyway applied V1, V2, and V3 migrations successfully")
    void shouldVerifyFlywayMigrationSuccess() {
        MigrationInfo currentMigration = flyway.info().current();
        assertThat(currentMigration).isNotNull();
        assertThat(currentMigration.getVersion().getVersion()).isEqualTo("3");
        assertThat(currentMigration.getDescription()).isEqualTo("add performance indexes");
        assertThat(currentMigration.getState().isApplied()).isTrue();

        Boolean v1Success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'",
                Boolean.class
        );
        assertThat(v1Success).isTrue();

        Boolean v2Success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '2'",
                Boolean.class
        );
        assertThat(v2Success).isTrue();

        Boolean v3Success = jdbcTemplate.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '3'",
                Boolean.class
        );
        assertThat(v3Success).isTrue();
    }

    @Test
    @DisplayName("should verify all 8 domain tables exist in public schema")
    void shouldVerifyAllDomainTablesCreated() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class
        );

        assertThat(tables).contains(
                "users",
                "projects",
                "project_members",
                "sprints",
                "issues",
                "comments",
                "activities",
                "notifications",
                "flyway_schema_history"
        );
    }
}
