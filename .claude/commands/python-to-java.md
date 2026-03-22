# Python → Java 轉換指南 (GenBI Refactoring Guide)

將 Python (FastAPI) 開源 GenBI 轉換為 Java (Spring Boot) 時的對照規則。

## 架構對照表

| Python | Java | 說明 |
|--------|------|------|
| Pydantic BaseModel | `record` | 不可變 DTO |
| dataclass | `record` 或 `@Builder class` | 看是否需要 mutable |
| FastAPI router | `@RestController` | REST 端點 |
| WebSocket endpoint | `TextWebSocketHandler` | WebSocket 處理 |
| boto3 bedrock client | `BedrockRuntimeClient` (AWS SDK v2) | LLM 呼叫 |
| OpenSearch client (`opensearch-py`) | `RestHighLevelClient` (opensearch-rest-high-level-client) | RAG 向量搜索 |
| DynamoDB | JPA + MySQL/Oracle | Profile 儲存 |
| Streamlit pages | 不需要 | 前端由其他團隊負責 |
| `@app.get/post` | `@GetMapping/@PostMapping` | 路由 |
| `async def` | `CompletableFuture` 或同步 | 非同步 |
| `logger = getLogger()` | `@Slf4j` (Lombok) | Logging |

## 狀態機對照

Python `state_machine.py` → Java `QueryStateMachine.java`

| Python 方法 | Java 方法 | 注意事項 |
|-------------|-----------|---------|
| `handle_initial()` | `handleInitial()` | 包含 query rewrite 邏輯 |
| `handle_intent_recognition()` | `handleIntentRecognition()` | 4 種意圖分流 |
| `_generate_sql()` | `generateSql()` + `handleSqlGeneration()` | SQL 提取用 `<sql>` 標籤 |
| `_generate_sql_again()` | `handleAutoCorrection()` | 帶上錯誤訊息重試 |
| `_apply_row_level_security_for_sql()` | `RlsService.applyRowLevelSecurity()` | 待 auth 接入 |
| `handle_execute_query()` | `handleExecuteQuery()` | 含 auto correction 分支 |

## LLM 呼叫對照

Python `utils/llm.py` → Java `LlmService` interface

| Python 函數 | Java 方法 | 回傳 |
|-------------|-----------|------|
| `invoke_model_claude3()` | `BedrockLlmService.invokeModel()` | 底層呼叫 |
| `get_query_intent()` | `getQueryIntent()` | `Map` (intent + slot) |
| `text_to_sql()` | `textToSql()` | String (含 `<sql>` 標籤) |
| `text_to_sql()` + additional_info | `textToSqlWithCorrection()` | Auto Correction |
| `get_query_rewrite()` | `getQueryRewrite()` | `Map` (intent + query) |
| `knowledge_search()` | `knowledgeSearch()` | String |
| `data_analyse_tool()` | `dataAnalyse()` | String |
| `get_agent_cot_task()` | `getAgentCotTask()` | `Map` (task_1, task_2...) |
| `data_visualization()` | `dataVisualization()` | `Map` (showType + chartType) |

## Profile / 資料管理對照

| Python | Java | 說明 |
|--------|------|------|
| DynamoDB `ProfileEntity` | `DbProfile` (JPA Entity) | Profile 儲存 |
| `ProfileManagement` | `DbProfileService` | Profile CRUD |
| `ConnectionManagement` | `DatabaseService` | DB 連線 |
| `LogManagement` | `SessionService.saveMessage()` | 查詢記錄 |
| `ConnectConfig` dataclass | `DbProfile` Entity 的欄位 | 連線設定 |

## RAG / 向量搜尋對照 (Phase 4)

Python `vector_store.py` + `opensearch.py` → Java `service/impl/`

### Embedding (向量化)
| Python | Java | 說明 |
|--------|------|------|
| `create_vector_embedding_with_bedrock()` | `BedrockEmbeddingService.createEmbedding()` | Titan Embedding API |
| `embedding_info["embedding_name"]` | `amazon.titan-embed-text-v1` (常數) | 模型 ID |
| `boto3.client('bedrock-runtime')` | `BedrockRuntimeClient` (AWS SDK v2) | 共用 Bedrock client |

### OpenSearch 向量搜尋
| Python | Java | 說明 |
|--------|------|------|
| `OpenSearchDao.__init__()` | `OpenSearchConfig` (@Bean) | 建立連線 |
| `search_sample_with_embedding()` | `OpenSearchRetrievalService.knnSearch()` | 核心 KNN 搜尋 |
| `retrieve_samples()` | `OpenSearchSampleManagementService.retrieveAllByProfile()` | 列出範例 |
| `add_sample()` / `add_entity_sample()` | `OpenSearchSampleManagementService.addSqlSample()` / `addEntitySample()` | 新增範例 |
| `delete_sample()` | `OpenSearchSampleManagementService.deleteDocument()` | 刪除範例 |
| `search_same_query()` (score == 1.0 去重) | `deleteDuplicateIfExists()` | 新增前去重 |
| `opensearch_index_init()` | `OpenSearchIndexInitializer` (@EventListener) | 啟動時自動建立索引 |

### OpenSearch 三個索引
| 索引名稱 | 用途 | Python 入口 | Java 入口 |
|----------|------|------------|-----------|
| `uba` (sql_index) | SQL 問答範例，few-shot 用 | `VectorStore.add_sample()` | `SampleManagementService.addSqlSample()` |
| `uba_ner` (ner_index) | Entity/NER 實體，消歧 + WHERE 條件 | `VectorStore.add_entity_sample()` | `SampleManagementService.addEntitySample()` |
| `uba_agent` (agent_index) | Agent COT 複雜查詢拆解範例 | `VectorStore.add_agent_cot_sample()` | `SampleManagementService.addAgentCotSample()` |

### 範例管理 REST 端點 (取代 Streamlit 管理頁面)
| 端點 | 說明 |
|------|------|
| `GET /api/v1/samples/sql?profile_name=xxx` | 列出 SQL 範例 |
| `POST /api/v1/samples/sql` | 新增 SQL 範例 |
| `DELETE /api/v1/samples/sql/{docId}` | 刪除 SQL 範例 |
| `GET /api/v1/samples/entities?profile_name=xxx` | 列出 Entity 範例 |
| `POST /api/v1/samples/entities` | 新增 Entity 範例 |
| `DELETE /api/v1/samples/entities/{docId}` | 刪除 Entity 範例 |
| `GET /api/v1/samples/agents?profile_name=xxx` | 列出 Agent COT 範例 |
| `POST /api/v1/samples/agents` | 新增 Agent COT 範例 |
| `DELETE /api/v1/samples/agents/{docId}` | 刪除 Agent COT 範例 |

## 轉換注意事項

1. **Python dict → Java Map**: Python 大量用 dict，Java 盡量用具體型別 (record/class)
2. **Python None → Java null**: 用 `Optional` 或 null check，record 欄位給預設值
3. **Python list comprehension → Java Stream**: `[x for x in list]` → `.stream().map().toList()`
4. **Python f-string → Java format**: `f"hello {name}"` → `"hello %s".formatted(name)` 或 `String.format()`
5. **Python try/except → Java try/catch**: 異常用 `BusinessException` 包裝
6. **Pandas DataFrame → List<Map>**: 查詢結果從 DataFrame 改成 List<Map<String, Object>>
7. **Python 的 `auto_correction_flag` → Java 的 `ProcessingContext.autoCorrection()`**

## 還未移植的 Python 功能

| 功能 | Python 位置 | 優先級 | 狀態 |
|------|------------|--------|------|
| OpenSearch RAG 向量搜尋 | `vector_store.py` + `opensearch.py` | 高 | ✅ Phase 4 完成 |
| Bedrock Titan Embedding | `utils/llm.py` embedding | 高 | ✅ Phase 4 完成 |
| 範例管理 CRUD | Streamlit Index/Entity/Agent 頁面 | 高 | ✅ Phase 4 完成 (REST API) |
| Prompt 模板管理 | `utils/prompts/` | 高 | **Phase 5 待做** |
| 多資料庫方言 | `utils/prompt.py` (6 種 dialect) | 中 | **Phase 5 待做** |
| Row Level Security | `datasource/base.py` | 中 | 介面已預留，待 auth 整合 |
| 多 DB 動態連線 | `ConnectionManagement` + factory | 中 | **Phase 6 待做** |
| 多租戶 User Profile | `user_profile.py` | 中 | **Phase 7 待做** |
| Token 追蹤 | `state_machine.py` token_info | 低 | 未做 |
| Entity 同名消歧 UI | `state_machine.py` ASK_ENTITY_SELECT | 低 | 狀態機已有 |
| Streamlit 管理頁面 | `pages/` | 不需要 | 前端另做 |
| SageMaker Endpoint | `utils/llm.py` | 低 | 只用 Bedrock |

## 使用方式

轉換新功能時，先讀 Python 原始碼:
```
1. 讀 Python 的函數/class
2. 對照上面的表找 Java 對應位置
3. 用 record 建 DTO，用 Service 建邏輯
4. 寫測試驗證行為一致
5. 用 /commit 產生 commit message
```
