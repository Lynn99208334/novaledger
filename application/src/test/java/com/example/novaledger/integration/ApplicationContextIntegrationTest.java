package com.example.novaledger.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * 基礎整合測試
 *
 * 目的：
 * 1. 確認 Spring context 可以完整啟動
 * 2. 確認 DB 連線正常
 * 3. 確認 Flyway migration 全部跑完（core tables 存在）
 *
 * 本機執行：mvn clean test "-Dspring.profiles.active=dev" "-Dskip.npm=true"
 * CI 執行：自動用 test profile（ci.yml 設定 SPRING_PROFILES_ACTIVE=test）
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("基礎整合測試 - Spring Context + DB 連線")
class ApplicationContextIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Spring context 應可完整啟動")
    void springContext_shouldLoadSuccessfully() {
        assertThat(dataSource).isNotNull();
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    @DisplayName("DB 連線應正常")
    void dataSource_shouldBeConnectable() {
        assertThatNoException().isThrownBy(() -> {
            try (var connection = dataSource.getConnection()) {
                assertThat(connection.isValid(3)).isTrue();
            }
        });
    }

    @Test
    @DisplayName("Flyway migration 應完成 - users 表存在")
    void flyway_shouldHaveMigrated_usersTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'users'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Flyway migration 應完成 - tenants 表存在")
    void flyway_shouldHaveMigrated_tenantsTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'tenants'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Flyway migration 應完成 - user_accounts 表存在")
    void flyway_shouldHaveMigrated_userAccountsTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'user_accounts'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("Flyway migration 應完成 - system_configs 表存在（V19）")
    void flyway_shouldHaveMigrated_systemConfigsTable() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables " +
                "WHERE table_schema = DATABASE() AND table_name = 'system_configs'",
                Integer.class
        );
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("system_configs seed data 應正確寫入")
    void systemConfigs_shouldHaveSeedData() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_configs",
                Integer.class
        );
        assertThat(count).isGreaterThan(0);
    }
}
