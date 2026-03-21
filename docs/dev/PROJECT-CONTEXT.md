# Lnfusion GenBI — Java Refactoring Project Context
# Lnfusion GenBI — Java 重構專案上下文

> Last updated: 2026-03-20

---

## 1. Project Overview / 專案概述

**Product**: Lnfusion — GenBI (Generative BI) module
**Origin**: Fork from [AWS Generative BI Demo](https://github.com/aws-samples/generative-bi-using-rag) (Python + FastAPI + Streamlit)
**Goal**: Refactor core backend from Python → Java 21 + Spring Boot 3.4.5, embed as Lnfusion product module
**Deadline**: Mid-May 2026

**產品**: Lnfusion — GenBI (生成式 BI) 模組
**來源**: 從 AWS 開源 Generative BI 專案 fork (Python + FastAPI + Streamlit)
**目標**: 將核心後端從 Python 重構為 Java 21 + Spring Boot 3.4.5，嵌入 Lnfusion 產品模組
**截止日期**: 2026 年 5 月中

---

## 2. Team / 團隊分工

| Person | Role | Current Focus |
|--------|------|---------------|
| **Justin (me)** | Java refactoring | Core `/qa/ws` WebSocket state machine, API restructuring, Java project setup |
| **Hazdik** (Hazdi Kurniawan) | Deployment / infra / DB | Oracle DB support, deployment scripts, Lnfusion auth integration, frontend cleanup |
| **Edo** | Frontend | React frontend adjustments |
| **Fadhil** | Backend support | Backend tasks (scope TBD) |

**Key decisions by hazdik:**
- Will create a separate repo for the refactored Java project (said "tonight or tomorrow" on 2026-03-20)
- For now, Justin can create `feature/java-refactor` branch on existing GitLab repo
- Auth: Cognito SSO removed → switching to Lnfusion authentication
- DB: Oracle + MySQL (Redshift/StarRocks/Clickhouse deprecated)

---

## 3. Git Repository / Git 倉庫

**GitLab**: `https://gitlab.com/lndata/data-transform/backend/data-transform-gen-bi`

### Branch Structure / 分支結構

| Branch | Status | Description |
|--------|--------|-------------|
| `master` | Base | Initial import (2026-02-09) |
| `develop` | Active | Main development branch, hazdik's work merged here |
| `feature/session` | Active (latest) | Frontend auth + session work by hazdik (latest: 2026-03-20) |
| `feature/java-refactor` | **TO CREATE** | Justin's Java refactoring branch |

### Recent Commit Timeline (by hazdik) / 最近提交時間線

| Date | Key Changes |
|------|-------------|
| 2026-02-09 | Initial import to master (by Alcredo Simanjuntak) |
| 2026-02-10 | Add Oracle support in DB type mappings |
| 2026-02-24~25 | Add Oracle dependency, update gitignore |
| 2026-03-06 | Deployment scripts, merge Oracle DB feature |
| 2026-03-09 | **Deprecate Redshift/StarRocks/Clickhouse**, update SQLAlchemy v2, update Bedrock model |
| 2026-03-10 | **Remove SageMaker & Cognito** components |
| 2026-03-11 | Update Bedrock model to `claude-3-5-sonnet-20241022-v2`, Oracle query fixes |
| 2026-03-18~19 | Frontend VITE variables, deployment scripts |
| 2026-03-20 | **Add Lnfusion authentication** (provider, constants, env vars) |

---

## 4. Tech Stack / 技術棧

### Python (Current / 現有)
- **Framework**: FastAPI + Streamlit (frontend)
- **LLM**: AWS Bedrock (Claude 3.5 Sonnet, Mistral, LLaMA)
- **Vector Search**: AWS OpenSearch (RAG for similar Q&A retrieval)
- **Database**: Oracle + MySQL (previously also Redshift/StarRocks/Clickhouse, now deprecated)
- **Auth**: ~~Cognito SSO~~ → Lnfusion auth

### Java (Target / 目標)
- **Java**: 21
- **Framework**: Spring Boot 3.4.5
- **DTO**: Java `record` (immutable)
- **Logging**: Lombok `@Slf4j`
- **LLM**: AWS Bedrock SDK for Java (to replace boto3)
- **Database**: MySQL + Oracle (via Spring JDBC / JPA)
- **Vector Search**: OpenSearch Java client (to replace opensearch-py)
- **Comments**: Bilingual (Chinese + English)

### Key AWS Services / 關鍵 AWS 服務
- **Bedrock**: LLM inference — model: `anthropic.claude-3-5-sonnet-20241022-v2:0`
- **Bedrock Region**: `us-west-2`
- **OpenSearch**: Vector similarity search for RAG (entity/QA/agent retrieval)

---

## 5. Architecture — Core NLQ Flow / 核心 NLQ 流程架構

### State Machine Pattern / 狀態機模式

The core `/qa/ws` WebSocket uses a **State Machine** pattern:

```
INITIAL
  ↓
QUERY_REWRITE (optional rewrite for clarity)
  ↓
INTENT_RECOGNITION → 4 paths:
  ├── normal_search  → ENTITY_RETRIEVAL → QA_RETRIEVAL → SQL_GENERATION → EXECUTE_QUERY → ANALYZE_DATA
  ├── knowledge_search → KNOWLEDGE_SEARCH → ANALYZE_DATA
  ├── reject_search → REJECT_INTENT (refuse inappropriate queries)
  └── agent_search → AGENT_TASK → AGENT_SQL_GENERATION → AGENT_DATA_ANALYSIS
  ↓
(all paths converge)
  ↓
ENTITY_SELECTION (optional — if ambiguous entities)
  ↓
DATA_VISUALIZATION (generate chart config)
  ↓
SUGGEST_QUESTION (suggest follow-up questions)
  ↓
COMPLETE
```

### 18 States / 18 個狀態
```
INITIAL, QUERY_REWRITE, INTENT_RECOGNITION,
KNOWLEDGE_SEARCH, ENTITY_RETRIEVAL, QA_RETRIEVAL,
SQL_GENERATION, EXECUTE_QUERY, ANALYZE_DATA,
REJECT_INTENT,
AGENT_TASK, AGENT_SQL_GENERATION, AGENT_DATA_ANALYSIS,
ENTITY_SELECTION, USER_SELECT_ENTITY,
DATA_VISUALIZATION, SUGGEST_QUESTION, COMPLETE
```

---

## 6. API Endpoints / API 端點

### Current Python (12 endpoints) → Proposed Java (8 endpoints)

| # | Method | Java Path | Description | Python Origin |
|---|--------|-----------|-------------|---------------|
| 1 | GET | `/health` | Health check (Actuator) | `/` + `/ping` |
| 2 | GET | `/qa/option` | Get profiles & model list | `/qa/option` |
| 3 | GET | `/qa/profiles/{name}/examples` | Example questions | `/qa/get_custom_question` (redesigned) |
| 4 | GET | `/qa/sessions` | Session list | `/qa/get_sessions` (RESTful) |
| 5 | GET | `/qa/sessions/{sessionId}` | Messages in session | `/qa/get_history_by_session` (RESTful) |
| 6 | DELETE | `/qa/sessions/{sessionId}` | Delete session | `/qa/delete_history_by_session` (RESTful) |
| 7 | POST | `/qa/feedback` | Upvote/Downvote | `/qa/user_feedback` |
| 8 | **WS** | **`/qa/ws`** | **Core NLQ query** | `/qa/ws` |

**Removed**: `/` (dup), `/test` (dev-only), `/option` (dup), `/qa/get_history_by_user_profile` (merged into #4)

> ⚠️ This API structure is pending hazdik's review. He may want to keep/modify some endpoints.

---

## 7. Java Project — Current Implementation Status / 目前實作進度

### ✅ Completed / 已完成

| Module | Files | Status |
|--------|-------|--------|
| **Project setup** | `pom.xml`, `NlqApplication.java`, `application.yml` | Done |
| **Enums** | `QueryState.java`, `ContentType.java` | Done — 18 states mapped |
| **DTOs** | `Question`, `Answer`, `ProcessingContext`, `SqlSearchResult`, `KnowledgeSearchResult`, `AgentSearchResult`, `TaskSqlSearchResult`, `AskRewriteResult`, `AskEntitySelect`, `ChartEntity`, `WsMessage`, `StateContent` | Done — all use `record` except `Answer` (mutable) |
| **Service interfaces** | `LlmService`, `DatabaseService`, `RetrievalService`, `ProfileService` | Done |
| **Mock implementations** | `MockLlmService`, `MockDatabaseService`, `MockRetrievalService`, `MockProfileService` | Done — returns fake data for demo |
| **State machine** | `QueryStateMachine.java` | Done — all 18 states + 4 intent paths |
| **WebSocket** | `WebSocketConfig.java`, `QaWebSocketHandler.java` | Done — `/qa/ws` endpoint |
| **Tests** | `QueryStateMachineTest.java` | Done — 16 test cases |
| **Docker** | `Dockerfile`, `docker-compose.yml`, `docker/mysql/init.sql` | Done — MySQL 8.0 + Spring Boot |
| **Docs** | `api-review.txt`, `api-flow.txt`, `api-flow-zh.txt`, `api-flowchart-pseudocode.md`, `api-flowchart-pseudocode-zh.md`, `api-analysis-zh.md` | Done |

### ❌ Not Yet Implemented / 尚未實作

| Module | Description | Priority |
|--------|-------------|----------|
| **AWS Bedrock SDK** | Replace `MockLlmService` with real Bedrock Java SDK calls | 🔴 High |
| **OpenSearch client** | Replace `MockRetrievalService` with real vector search | 🔴 High |
| **Real ProfileService** | Replace `MockProfileService` with DB-backed profile/DDL management | 🟡 Medium |
| **CRUD endpoints** | `/qa/option`, `/qa/sessions`, `/qa/feedback`, etc. (6 REST endpoints) | 🟡 Medium |
| **Oracle support** | `DatabaseService` dual DB support (MySQL + Oracle) | 🟡 Medium |
| **Lnfusion auth** | Integrate with Lnfusion authentication (replacing Cognito) | 🟡 Medium |
| **Row-Level Security** | Python has RLS checks — not yet in Java | 🟠 Low (phase 2) |
| **Auto SQL Correction** | Retry SQL generation on failure — not yet in Java | 🟠 Low (phase 2) |
| **Token tracking** | Track LLM token usage — not yet in Java | 🟠 Low (phase 2) |

---

## 8. Key Python Source Files Reference / Python 原始碼參考

For understanding the original implementation:

| Python File | Purpose | Java Equivalent |
|-------------|---------|-----------------|
| `application/nlq/core/state_machine.py` | State machine with all handlers | `QueryStateMachine.java` |
| `application/nlq/core/state.py` | QueryState enum | `QueryState.java` |
| `application/nlq/core/chat_context.py` | ProcessingContext dataclass | `ProcessingContext.java` |
| `application/api/schemas.py` | Pydantic models (Question, Answer, etc.) | `dto/*.java` |
| `application/api/enum.py` | ContentEnum | `ContentType.java` |
| `application/api/service.py` | `ask_websocket()` — main WS handler | `QaWebSocketHandler.java` |
| `application/api/main.py` | FastAPI routes + WS endpoint | `WebSocketConfig.java` + future controllers |
| `application/utils/llm.py` | All LLM calls (Bedrock boto3) | `LlmService.java` (interface, mock impl) |
| `application/utils/opensearch.py` | OpenSearch vector retrieval | `RetrievalService.java` (interface, mock impl) |
| `application/utils/text_search.py` | Entity/QA search wrappers | Folded into `RetrievalService` |
| `application/nlq/business/connection.py` | DB connection management | `DatabaseService.java` |
| `application/nlq/business/profile.py` | Profile CRUD | `ProfileService.java` |

---

## 9. Development Notes / 開發注意事項

### Conventions / 規範
- **DTO**: Use Java `record` for immutable data; use `class` only when state needs mutation (e.g., `Answer`)
- **Logging**: `@Slf4j` (Lombok) — no manual `LoggerFactory.getLogger()`
- **Comments**: Bilingual — Chinese summary + English detail
- **Commits**: Bilingual — `<type>(<scope>): <Chinese summary>` + English summary (see `.claude/commands/commit.md`)
- **Tests**: JUnit 5 + Mockito, test each state transition

### Known Issues / 已知問題
- Lombok `@Slf4j` requires `maven-compiler-plugin` annotationProcessorPaths config (already fixed in `pom.xml`)
- Docker compose needs Docker Desktop running
- `Answer` is a mutable class (not record) because the state machine updates it step by step

### Environment Variables (from Python .env / hazdik's config)
```
BEDROCK_MODEL_ID=anthropic.claude-3-5-sonnet-20241022-v2:0
BEDROCK_REGION=us-west-2
AOS_HOST=<OpenSearch endpoint>
AOS_PORT=443
AOS_USERNAME=<username>
AOS_PASSWORD=<password>
DB_TYPE=mysql|oracle
```

---

## 10. Suggested Development Order / 建議開發順序

```
Phase 1 (Current ✅): Core WebSocket + State Machine + Mock services + Docker
    ↓
Phase 2 (Next 🔜): AWS Bedrock SDK integration (real LLM calls)
    ↓
Phase 3: OpenSearch integration (real vector search / RAG)
    ↓
Phase 4: CRUD REST endpoints (sessions, feedback, options, profiles)
    ↓
Phase 5: Oracle DB support + Lnfusion auth integration
    ↓
Phase 6: Row-Level Security, Auto Correction, Token tracking
```

---

## 11. Communication / 溝通

- **Language**: English with hazdik/edo/fadhil; Chinese internally
- **Tools**: Slack (team chat), GitLab (code)
- **Hazdik's timezone**: Likely UTC+7 (Indonesia)
- **Slack format**: Share docs as code blocks or file attachments, not walls of text
