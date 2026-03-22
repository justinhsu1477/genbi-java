package com.lndata.genbi.enums;

import com.lndata.genbi.model.constant.SqlDialect;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlDialect 列舉測試
 */
class SqlDialectTest {

    @Test
    @DisplayName("應有 8 種方言")
    void shouldHaveEightDialects() {
        assertEquals(8, SqlDialect.values().length);
    }

    @Test
    @DisplayName("fromValue 正確解析")
    void fromValue_correct() {
        assertEquals(SqlDialect.MYSQL, SqlDialect.fromValue("mysql"));
        assertEquals(SqlDialect.POSTGRESQL, SqlDialect.fromValue("postgresql"));
        assertEquals(SqlDialect.REDSHIFT, SqlDialect.fromValue("redshift"));
        assertEquals(SqlDialect.CLICKHOUSE, SqlDialect.fromValue("clickhouse"));
        assertEquals(SqlDialect.HIVE, SqlDialect.fromValue("hive"));
        assertEquals(SqlDialect.BIGQUERY, SqlDialect.fromValue("bigquery"));
        assertEquals(SqlDialect.STARROCKS, SqlDialect.fromValue("starrocks"));
    }

    @Test
    @DisplayName("fromValue 大小寫不敏感")
    void fromValue_caseInsensitive() {
        assertEquals(SqlDialect.MYSQL, SqlDialect.fromValue("MySQL"));
        assertEquals(SqlDialect.POSTGRESQL, SqlDialect.fromValue("PostgreSQL"));
    }

    @Test
    @DisplayName("fromValue 空值或未知返回 DEFAULT")
    void fromValue_fallbackToDefault() {
        assertEquals(SqlDialect.DEFAULT, SqlDialect.fromValue(null));
        assertEquals(SqlDialect.DEFAULT, SqlDialect.fromValue(""));
        assertEquals(SqlDialect.DEFAULT, SqlDialect.fromValue("oracle"));
    }

    @Test
    @DisplayName("MySQL dialect prompt 包含 CURDATE 和 backticks")
    void mysql_dialectPrompt() {
        String prompt = SqlDialect.MYSQL.getDialectPrompt();
        assertTrue(prompt.contains("CURDATE()"));
        assertTrue(prompt.contains("backticks"));
    }

    @Test
    @DisplayName("PostgreSQL dialect prompt 包含 CURRENT_DATE 和 double quotes")
    void postgresql_dialectPrompt() {
        String prompt = SqlDialect.POSTGRESQL.getDialectPrompt();
        assertTrue(prompt.contains("CURRENT_DATE"));
        assertTrue(prompt.contains("double quotes"));
    }

    @Test
    @DisplayName("Redshift dialect prompt 包含日期函數列表")
    void redshift_dialectPrompt() {
        String prompt = SqlDialect.REDSHIFT.getDialectPrompt();
        assertTrue(prompt.contains("DATEADD"));
        assertTrue(prompt.contains("DATEDIFF"));
        assertTrue(prompt.contains("data_time_function_list"));
    }

    @Test
    @DisplayName("所有方言的 dialectPrompt 不為空")
    void allDialects_notBlank() {
        for (SqlDialect dialect : SqlDialect.values()) {
            assertFalse(dialect.getDialectPrompt().isBlank(), dialect.name() + " prompt is blank");
        }
    }
}
