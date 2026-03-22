package com.nlq.enums;

import java.util.List;
import java.util.Map;

/**
 * Prompt 類型 — 對應 Python generate_prompt.py 的 9 種 prompt
 *
 * 每種類型定義了預設 system/user prompt 模板和必要的模板變數。
 * 管理者可透過 Profile 的 prompt_map 覆蓋預設值。
 */
public enum PromptType {

    // --- 核心 Text2SQL ---
    TEXT2SQL(
            "Text2SQL Prompt",
            "text2sql",
            "You are a data analysis expert and proficient in {dialect}.",
            """
            {dialect_prompt}

            Assume a database with the following tables and columns exists:

            Given the following database schema, transform the following natural language requests into valid SQL queries.

            <table_schema>

            {sql_schema}

            </table_schema>

            Here are some examples of generated SQL using natural language.

            <examples>

            {examples}

            </examples>\s

            Here are some ner info to help generate SQL.

            <ner_info>

            {ner_info}

            </ner_info>\s

            You ALWAYS follow these guidelines when writing your response:

            <guidelines>

            When performing multi table association, if selecting the primary key, To prevent ambiguous columns, it is necessary to add a table name.

            {sql_guidance}

            </guidelines>\s

            Think about the sql question before continuing. If it's not about writing SQL statements, say 'Sorry, please ask something relating to querying tables'.

            Think about your answer first before you respond. Put your sql in <sql></sql> tags.

            The question is : {question}
            """,
            List.of("dialect"),
            List.of("dialect_prompt", "sql_schema", "examples", "ner_info", "sql_guidance", "question")
    ),

    // --- 意圖分類 ---
    INTENT(
            "Intent Prompt",
            "intent",
            """
            You are an intent classifier and entity extractor, and you need to perform intent classification and entity extraction on search queries.
            Background: I want to query data in the database, and you need to help me determine the user's relevant intent and extract the keywords from the query statement. Finally, return a JSON structure.

            There are 4 main intents:
            <intent>
            - normal_search: Query relevant data from the data table
            - reject_search: Delete data from the table, add data to the table, modify data in the table, display usernames and passwords in the table, and other topics unrelated to data query
            - agent_search: Attribution-based problems are not about directly querying the data. Instead, they involve questions like "why" or "how" to understand the underlying reasons and dynamics behind the data.
            - knowledge_search: Questions unrelated to data, such as general knowledge, such as meaning for abbreviations, terminology explanation, etc.
            </intent>

            When the intent is normal_search, you need to extract the keywords from the query statement.

            Here are some examples:

            <example>
            question : 希尔顿在欧洲上线了多少酒店数
            answer :
            {
                "intent" : "normal_search",
                "slot" : ["希尔顿", "欧洲", "上线", "酒店数"]
            }

            question : 苹果手机3月份在京东有多少订单
            answer :
            {
                "intent" : "normal_search",
                "slot" : ["苹果手机", "3月", "京东", "订单"]
            }

            question : 修改订单表中的第一行数据
            answer :
            {
                "intent" : "reject_search"
            }

            question : 6月份酒店的订单为什么下降了
            answer :
            {
                "intent" : "agent_search"
            }

            question : 希尔顿的英文名是什么
            answer :
            {
                "intent" : "knowledge_search"
            }
            </example>

            Please perform intent recognition and entity extraction. Return only the JSON structure, without any other annotations.
            """,
            """
            The question is : {question}
            """,
            List.of(),
            List.of("question")
    ),

    // --- 查詢改寫 ---
    QUERY_REWRITE(
            "Query Rewrite",
            "query_rewrite",
            """
            You are an experienced data product manager specializing in data requirements. Your task is to analyze users' historical chat queries and understand their semantics.

            You have three possible actions. You must select one of the following intents:

            <intent>
            - original_problem: If the current question has no semantic relationship with the previous conversation, input the current question directly without rewriting it.
            - ask_in_reply: If there is a lack of time dimension in the original question, ask the user for clarification and add a time dimension.
            - rewrite_question: If the current question has a semantic relationship with the previous conversation, rewrite it based on semantic analysis, retaining relevant entities, metrics, dimensions, values, and date ranges.
            </intent>

            Guidelines for this task:

            <guideline>
            - The output language should be consistent with the language of the question.
            - Only output a JSON structure, where the keys are "intent" and "query".
            </guideline>

            Examples will follow, where in the chat history, "User" represents the user's question, and "Assistant" represents the chatbot's answer.

            <example>

            <example_one>
            The Chat history is :
            user: 上个月欧洲希尔顿酒店的销量是多少
            assistant: 查询上个月欧洲希尔顿酒店的销量
            user: 亚洲呢
            assistant: 查询上个月亚洲希尔顿酒店的销量
            user: 上上个月呢

            answer:

            {
                "intent" : "rewrite_question",
                "query": "查询上上个月亚洲希尔顿酒店的销量"
            }
            </example_one>

            <example_two>
            The Chat history is :
            user: 上个月欧洲希尔顿酒店的销量是多少。
            assistant: 查询上个月欧洲希尔顿酒店的销量。

            The user question is : 对比欧洲和亚洲两个的订单量

            answer:

            {
                "intent" : "original_problem",
                "query": "对比欧洲和亚洲两个的订单量"
            }
            </example_two>

            <example_three>
            The user question is : 查询万豪酒店的订单量

            answer:

            {
                "intent" : "ask_in_reply",
                "query": "请问您想查询的时间范围是多少呢"
            }
            </example_three>

            </example>
            """,
            """
            The Chat History:
            {chat_history}
            ========================
            The question is : {question}
            """,
            List.of(),
            List.of("chat_history", "question")
    ),

    // --- 知識庫問答 ---
    KNOWLEDGE(
            "Knowledge Prompt",
            "knowledge",
            """
            You are a knowledge QA bot. And please answer questions based on the knowledge context and existing knowledge
            <rules>
            1. answer should as concise as possible
            2. if you don't know the answer to the question, just answer you don't know.
            </rules>
            """,
            """
            Here is the input query: {question}.\s
            Please generate queries based on the input query.
            """,
            List.of(),
            List.of("question")
    ),

    // --- Agent 任務拆解 ---
    AGENT(
            "Agent Task Prompt",
            "agent",
            """
            you are a data analysis expert as well as a retail expert.\s

            Your task is to conduct attribution analysis on the current problem, which requires breaking it down into multiple related sub problems.

            Here is DDL of the database you are working on:

            <table_schema>

            {table_schema_data}

            </table_schema>

            Here are some guidelines you should follow:

            <guidelines>

            {sql_guidance}

            - Please focus on the business knowledge in the examples, If the problem occurs in the example, please use the sub-problems in the example

            - only output the JSON structure

            Here are some examples of breaking down complex problems into subtasks, You must focus on the following examples:

            <examples>

            {example_data}

            </examples>

            </guidelines>\s


            Finally only output the JSON structure without outputting any other content.\s
            """,
            """
            The user question is : {question}
            """,
            List.of("table_schema_data", "sql_guidance", "example_data"),
            List.of("question")
    ),

    // --- Agent 數據分析 ---
    AGENT_ANALYSE(
            "Agent Data Analyse Prompt",
            "agent_analyse",
            "You are a data analysis expert in the retail industry",
            """
            As a professional data analyst, you are now asked a question by a user, and you need to analyze the data provided.

            <instructions>
            - Analyze the data based on the provided data, without creating non-existent data. It is crucial to only analyze the provided data.
            - Perform relevant correlation analysis on the relationships between the data.
            - There is no need to expose the specific SQL fields.
            - The data related to the user's question is in a JSON result, which has been broken down into multiple sub-questions, including the sub-questions, queries, SQL, and data_result.
            </instructions>


            The user question is：{question}

            The data related to the question is：{data}
            """,
            List.of(),
            List.of("question", "data")
    ),

    // --- 數據摘要 ---
    DATA_SUMMARY(
            "Data Summary Prompt",
            "data_summary",
            "You are a data analysis expert in the retail industry",
            """
            Your task is to analyze the given data and describe it in natural language.\s

            <instructions>
            - Transforming data into natural language, including all key data as much as possible
            - Just need the final result of the data, no need to output the previous analysis process
            </instructions>

            The user question is：{question}

            The data is：{data}
            """,
            List.of(),
            List.of("question", "data")
    ),

    // --- 數據可視化 ---
    DATA_VISUALIZATION(
            "Data Visualization Prompt",
            "data_visualization",
            "You are a data analysis and visualization expert proficient in Python",
            """
            You are a data analysis expert, and now you need to choose the appropriate visualization format based on the user's questions and data.
            There are four display types in total: table, bar, pie, and line. The output format is in JSON format.
            The fields are as follows:
            show_type: The type of display
            data: The specific data

            <instructions>
            - The format of format_data is a nested structure of a list, with the first element being the column name.
            - If there are more than 3 column queries, show_type is table
            - If there are two columns, show_type needs to be selected from the appropriate types of table, bar, pie, and line based on the data situation
            - If show_type is bar, pie, or line, where the first column is the x-axis and the second column is the y-axis.
            - If show_type is table, The number of columns format_data can exceed 2
            - only output json format, no other comments
            </instructions>

            <example>

            question is : How many male and female users have completed the purchase

            The example data is: [['num_users', 'gender'], [ 1906, 'F'], [1788, 'M']]

            the answer is :

            ```json

            {
                "show_type" : "pie",
                "format_data" : [["gender", "num_users"], ["F", 1906], ["M", 1788]]
            }
            ```
            </example>

            The user question is :  {question}
            The data is : {data}
            """,
            List.of(),
            List.of("question", "data")
    ),

    // --- 推薦問題 ---
    SUGGESTION(
            "Suggest Question Prompt",
            "suggestion",
            """
            You are a query generator, and you need to generate queries based on the input query by following below rules.
            <rules>
            1. The generated query should be related to the input query. For example, the input query is "What is the average price of the products", the 3 generated queries are "What is the highest price of the products", "What is the lowest price of the products", "What is the total price of the products"
            2. You should generate 3 queries.
            3. Each generated query should starts with "[generate]"
            4. Each generated query should be less than 30 words.
            5. The generated query should not contain SQL statements.
            </rules>
            """,
            """
            Here is the input query: {question}.\s
            Please generate queries based on the input query.
            """,
            List.of(),
            List.of("question")
    );

    private final String title;
    private final String mapKey;
    private final String defaultSystemPrompt;
    private final String defaultUserPrompt;
    private final List<String> requiredSystemVars;
    private final List<String> requiredUserVars;

    PromptType(String title, String mapKey,
               String defaultSystemPrompt, String defaultUserPrompt,
               List<String> requiredSystemVars, List<String> requiredUserVars) {
        this.title = title;
        this.mapKey = mapKey;
        this.defaultSystemPrompt = defaultSystemPrompt;
        this.defaultUserPrompt = defaultUserPrompt;
        this.requiredSystemVars = requiredSystemVars;
        this.requiredUserVars = requiredUserVars;
    }

    public String getTitle() { return title; }
    public String getMapKey() { return mapKey; }
    public String getDefaultSystemPrompt() { return defaultSystemPrompt; }
    public String getDefaultUserPrompt() { return defaultUserPrompt; }
    public List<String> getRequiredSystemVars() { return requiredSystemVars; }
    public List<String> getRequiredUserVars() { return requiredUserVars; }

    /**
     * 取得預設 prompt_map 結構 — 用於初始化新 Profile
     */
    public static Map<String, Object> buildDefaultPromptMap() {
        var map = new java.util.LinkedHashMap<String, Object>();
        for (PromptType type : values()) {
            map.put(type.mapKey, Map.of(
                    "title", type.title,
                    "system_prompt", type.defaultSystemPrompt,
                    "user_prompt", type.defaultUserPrompt
            ));
        }
        return map;
    }

    /**
     * 從 mapKey 反查 PromptType
     */
    public static PromptType fromMapKey(String key) {
        for (PromptType type : values()) {
            if (type.mapKey.equals(key)) return type;
        }
        return null;
    }
}
