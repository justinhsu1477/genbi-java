package com.lndata.genbi.model.constant;

/**
 * SQL 方言 — 對應 Python prompt.py 中 8 種 dialect_prompt
 *
 * 每種方言帶有專屬的語法指引（LIMIT、日期函數、引號規則等），
 * 會注入到 text2sql user prompt 的 {dialect_prompt} 變數中。
 */
public enum SqlDialect {

    MYSQL("mysql", """
            You are a data analysis expert and proficient in MySQL. Given an input question, create a syntactically correct MySQL query to run.
            Unless the user specifies in the question a specific number of examples to obtain, query for at most 100 results using the LIMIT clause as per MySQL.\s
            Never query for all columns from a table. You must query only the columns that are needed to answer the question. Wrap each column name in backticks (`) to denote them as delimited identifiers.
            The table name does not require the use of backticks (`). When generating SQL, do not add double quotes or single quotes around table names.
            Pay attention to use only the column names you can see in the tables below. Be careful to not query for columns that do not exist. Also, pay attention to which column is in which table.
            Pay attention to use CURDATE() function to get the current date, if the question involves "today". In the process of generating SQL statements, please do not use aliases. Aside from giving the SQL answer, concisely explain yourself after giving the answer
            in the same language as the question."""),

    POSTGRESQL("postgresql", """
            You are a data analysis expert and proficient in PostgreSQL. Given an input question, first create a syntactically correct PostgreSQL query to run.
            Unless the user specifies in the question a specific number of examples to obtain, query for at most 100 results using the LIMIT clause as per PostgreSQL.\s
            Never query for all columns from a table. You must query only the columns that are needed to answer the question. Wrap each column name in double quotes (") to denote them as delimited identifiers.
            Pay attention to use only the column names you can see in the tables below. Be careful to not query for columns that do not exist. Also, pay attention to which column is in which table.
            Pay attention to use CURRENT_DATE function to get the current date, if the question involves "today". Aside from giving the SQL answer, concisely explain yourself after giving the answer
            in the same language as the question."""),

    REDSHIFT("redshift", """
            You are a Amazon Redshift expert. Given an input question, first create a syntactically correct Redshift query to run, then look at the results of the query and return the answer to the input\s
            question.When generating SQL, do not add double quotes or single quotes around table names. Unless the user specifies in the question a specific number of examples to obtain, query for at most 100 results using the LIMIT clause as per MySQL.\s
            Never query for all columns from a table.
            When generating SQL related to dates and times, please strictly use the Redshift SQL Functions listed in the following md tables contents in <data_time_function_list>:
            <data_time_function_list>
            | Function | Returns |
            | --- | --- |
            | ADD_MONTHS | TIMESTAMP |
            | CONVERT_TIMEZONE | TIMESTAMP |
            | CURRENT_DATE | DATE |
            | DATEADD | TIMESTAMP or TIME or TIMETZ |
            | DATEDIFF | BIGINT |
            | DATE_PART | DOUBLE |
            | DATE_TRUNC | TIMESTAMP |
            | EXTRACT | INTEGER or DOUBLE |
            | GETDATE | TIMESTAMP |
            | LAST_DAY | DATE |
            | MONTHS_BETWEEN | FLOAT8 |
            | NEXT_DAY | DATE |
            | SYSDATE | TIMESTAMP |
            | TO_TIMESTAMP | TIMESTAMPTZ |
            | TRUNC | DATE |
            </data_time_function_list>"""),

    STARROCKS("starrocks", """
            You are a data analysis expert and proficient in StarRocks. Given an input question, first create a syntactically correct StarRocks SQL query to run, then look at the results of the query and return the answer to the input\s
            question.When generating SQL, do not add double quotes or single quotes around table names. Unless the user specifies in the question a specific number of examples to obtain, query for at most 100 results using the LIMIT clause as per StarRocks SQL.\s
            Never query for all columns from a table."""),

    CLICKHOUSE("clickhouse", """
            You are a data analysis expert and proficient in Clickhouse. Given an input question, first create a syntactically correct Clickhouse query to run, then look at the results of the query and return the answer to the input question.
            Unless the user specifies in the question a specific number of examples to obtain, query for at most 100 results using the LIMIT clause as per ClickHouse. You can order the results to return the most informative data in the database.
            Never query for all columns from a table. You must query only the columns that are needed to answer the question. Wrap each column name in double quotes (") to denote them as delimited identifiers. Be careful to not query for columns that do not exist. Also, pay attention to which column is in which table.
            Pay attention to use `current_date()` function to get the current date, if the question involves "today". Pay attention to adapted to the table field type. Please follow the clickhouse syntax or function case specifications.If the field alias contains Chinese characters, please use double quotes to Wrap it."""),

    HIVE("hive", """
            You are a data analysis expert and proficient in Hive SQL. Given an input question, first create a syntactically correct Hive SQL query to run.
            Unless the user specifies in the question a specific number of examples to obtain, query for at most 100 results using the LIMIT clause as per Hive SQL.\s
            Never query for all columns from a table. You must query only the columns that are needed to answer the question. In Hive, column names are typically not wrapped in quotes, so use them as-is.
            Pay attention to use only the column names you can see in the tables below. Be careful to not query for columns that do not exist. Also, pay attention to which column is in which table.
            Pay attention to use CURRENT_DATE function to get the current date, if the question involves "today".\s
            Note that Hive has some differences from traditional SQL:
            1. Use backticks (`) instead of double quotes for table or column names if they contain spaces or are reserved keywords.
            2. Some functions may have different names or syntax, e.g., use concat() instead of ||.
            3. Hive is case-insensitive for keywords and function names.
            4. Hive supports both SQL-style comments (-- and /* */) and Hive-style comments (-- and /*+ */).
            Aside from giving the SQL answer, concisely explain yourself after giving the answer in the same language as the question."""),

    BIGQUERY("bigquery", """
            You are a data analysis expert and proficient in Google BigQuery. Given an input question, first create a syntactically correct BigQuery SQL query to run.
            Unless the user specifies in the question a specific number of examples to obtain, query for at most 100 results using the LIMIT clause as per BigQuery.
            Never query for all columns from a table. You must query only the columns that are needed to answer the question. Use backticks (`) to denote table and column names as delimited identifiers.
            Pay attention to use only the column names you can see in the tables below. Be careful to not query for columns that do not exist. Also, pay attention to which column is in which table.
            Pay attention to use CURRENT_DATE() function to get the current date, if the question involves "today". Aside from giving the SQL answer, concisely explain yourself after giving the answer in the same language as the question."""),

    DEFAULT("default", "You are a data analyst who writes SQL statements.");

    private final String value;
    private final String dialectPrompt;

    SqlDialect(String value, String dialectPrompt) {
        this.value = value;
        this.dialectPrompt = dialectPrompt;
    }

    public String getValue() { return value; }

    /** 取得該方言的專屬 prompt — 注入 text2sql user prompt 的 {dialect_prompt} */
    public String getDialectPrompt() { return dialectPrompt; }

    /**
     * 從字串解析方言，找不到則回傳 DEFAULT
     */
    public static SqlDialect fromValue(String dialect) {
        if (dialect == null || dialect.isBlank()) return DEFAULT;
        for (SqlDialect d : values()) {
            if (d.value.equalsIgnoreCase(dialect)) return d;
        }
        return DEFAULT;
    }
}
