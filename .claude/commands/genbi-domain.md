# GenBI Domain Knowledge 領域知識

## What is GenBI? 什麼是 GenBI?

GenBI (Generative BI) 是一套 **自然語言查詢 (NLQ)** 系統，讓使用者用自然語言提問，系統自動產生 SQL 查詢、執行、並回傳結果。
核心能力：NL → Intent → SQL → Execute → Visualize → Suggest

## Core Concepts 核心概念

### 1. Query Flow 查詢流程（狀態機）

```
使用者提問
  ↓
[Query Rewrite] — 根據歷史對話改寫問題
  ↓
[Intent Classification] — 分類意圖
  ├─ normal_search  → Text2SQL → Execute → Data Summary → Visualization → Suggestion
  ├─ agent_search   → Task Decomposition → 多個 Text2SQL → Agent Analyse
  ├─ knowledge_search → Knowledge QA (LLM 直接回答)
  └─ reject_search  → 拒絕回應（刪改資料等危險操作）
```

### 2. Four Intent Paths 四種意圖路徑

| Intent | Description | Example |
|--------|-------------|---------|
| `normal_search` | 直接查詢資料表 | "上個月營收多少？" |
| `agent_search` | 歸因分析（為什麼/如何） | "為什麼六月訂單下降了？" |
| `knowledge_search` | 通用知識問答 | "MTD 是什麼意思？" |
| `reject_search` | 拒絕危險操作 | "刪除訂單表第一行" |

### 3. RAG (Retrieval-Augmented Generation) 向量搜尋增強

三個 OpenSearch 向量索引：

| Index | Purpose | Data |
|-------|---------|------|
| `uba` (sql_index) | SQL 範例 few-shot | question + sql pairs |
| `uba_ner` (ner_index) | 實體識別範例 | entity + comment (e.g., "台積電" → "TSMC, stock_code=2330") |
| `uba_agent` (agent_index) | Agent COT 範例 | query + decomposition examples |

**流程**: 使用者問題 → Embedding (Titan 1536-dim) → KNN cosine similarity → top-K results → 作為 few-shot examples 注入 prompt

### 4. Prompt System 提示詞系統 (9 types)

| Type | Purpose | Key Variables |
|------|---------|---------------|
| `text2sql` | NL→SQL 核心轉換 | dialect, dialect_prompt, sql_schema, examples, ner_info, sql_guidance, question |
| `intent` | 意圖分類 + NER 槽位提取 | question |
| `query_rewrite` | 歷史對話改寫 | chat_history, question |
| `knowledge` | 知識庫問答 | question |
| `agent` | Agent 任務拆解 (COT) | table_schema_data, sql_guidance, example_data, question |
| `agent_analyse` | Agent 數據分析 | question, data |
| `data_summary` | 數據摘要 | question, data |
| `data_visualization` | 圖表類型選擇 | question, data |
| `suggestion` | 推薦問題 | question |

### 5. SQL Dialects 資料庫方言 (8 types)

每種方言有專屬的 dialect_prompt，描述該 DB 的語法特性：

| Dialect | Key Syntax Differences |
|---------|----------------------|
| MySQL | backticks `` ` ``, `CURDATE()`, no alias |
| PostgreSQL | double quotes `"`, `CURRENT_DATE` |
| Redshift | AWS specific date functions, no quotes |
| StarRocks | no quotes around table names |
| ClickHouse | double quotes, `current_date()`, Chinese alias in `""` |
| Hive | backticks, `concat()` instead of `\|\|` |
| BigQuery | backticks, `CURRENT_DATE()` |
| Default | generic SQL |

### 6. Profile 資料庫連線設定

每個 Profile 包含：
- **Connection info**: DB URL, credentials, db_type
- **Schema**: `tables_info` (DDL), `hints` (業務規則)
- **Prompt map**: 自訂的 9 種 prompt 模板（覆蓋預設值）
- **RLS**: Row-Level Security 設定

### 7. Auto Correction 自動修正

SQL 執行失敗時，系統會：
1. 將原始 SQL + 錯誤訊息回傳給 LLM
2. LLM 根據錯誤修正 SQL
3. 最多重試 N 次

### 8. Data Visualization 數據可視化

LLM 根據查詢結果選擇最佳圖表類型：
- `table` — 超過 3 欄或預設
- `bar` — 分類比較
- `pie` — 佔比分析
- `line` — 趨勢分析

## Architecture Layers 架構分層

```
Controller (REST/WebSocket)
  ↓
StateMachine (QueryStateMachine — 狀態流轉)
  ↓
Service Layer
  ├── LlmService         — AI 模型呼叫 (Bedrock Claude)
  ├── RetrievalService   — RAG 向量搜尋 (OpenSearch KNN)
  ├── EmbeddingService   — 文本向量化 (Bedrock Titan)
  ├── DatabaseService    — SQL 執行
  ├── ProfileService     — 連線設定管理
  └── SampleManagementService — 範例 CRUD
  ↓
Infrastructure
  ├── AWS Bedrock (LLM + Embedding)
  ├── OpenSearch (KNN 向量搜尋)
  ├── MySQL/PostgreSQL (使用者資料庫)
  └── JPA (系統資料庫)
```

## Python → Java Mapping 對照

| Python | Java |
|--------|------|
| `prompt_map` dict in DynamoDB | `DbProfile.promptMap` TEXT (JSON) in MySQL |
| `generate_prompt.py` 2400 行 | `PromptType` enum + `PromptService` |
| `prompt.py` dialect prompts | `SqlDialect` enum |
| `check_prompt.py` validation | `PromptType.getRequiredVariables()` |
| `llm.py` generate_*_prompt() | `PromptService.buildPrompt()` |
