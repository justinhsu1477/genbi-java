# NLQ API Flowchart & Pseudocode

> All APIs are under the prefix `/qa`
> Source: `data-transform-gen-bi/application/api/main.py`

---

## API Overview

| # | Method | Path | Description |
|---|--------|------|-------------|
| 1 | GET | `/qa/option` | Get available profiles & models |
| 2 | GET | `/qa/get_custom_question` | Get example questions for a profile |
| 3 | POST | `/qa/get_history_by_user_profile` | Get chat history by user + profile |
| 4 | POST | `/qa/get_sessions` | Get all sessions for a user |
| 5 | POST | `/qa/get_history_by_session` | Get messages in a specific session |
| 6 | POST | `/qa/delete_history_by_session` | Delete a session's history |
| 7 | POST | `/qa/user_feedback` | Submit upvote/downvote feedback |
| 8 | WebSocket | `/qa/ws` | Main NLQ query (core feature) |

---

## 1. GET /qa/option

**Purpose**: Return available database profiles and LLM model list for frontend dropdown.

**Request**:
```
GET /qa/option?id=user123   (id is optional)
```

**Pseudocode**:
```
function get_option(id):
    all_profiles = ProfileManagement.get_all_profiles_with_info()
    all_models = BEDROCK_MODEL_IDS + SageMaker_custom_models

    if ENABLE_USER_PROFILE_MAP and id is not null:
        user_map = UserProfileManagement.get_user_profile_by_id(id)
        if user_map exists:
            return { profiles: user_map.profile_name_list, models: all_models }
        else:
            return { profiles: ["No_Profile"], models: all_models }
    else:
        return { profiles: all_profiles.keys(), models: all_models }
```

**Flowchart**:
```
[Request] → Check ENABLE_USER_PROFILE_MAP?
              ├── Yes + id → Lookup user profile map
              │                ├── Found → Return user's allowed profiles
              │                └── Not found → Return ["No_Profile"]
              └── No → Return all profiles
           → Append all available model IDs
           → [Response: {data_profiles, bedrock_model_ids}]
```

---

## 2. GET /qa/get_custom_question

**Purpose**: Get example questions configured in a database profile (for UI hints).

**Request**:
```
GET /qa/get_custom_question?data_profile=demo-profile
```

**Pseudocode**:
```
function get_custom_question(data_profile):
    all_profiles = ProfileManagement.get_all_profiles_with_info()
    profile = all_profiles[data_profile]
    comments = profile["comments"]

    if "Examples:" in comments:
        examples_text = comments.split("Examples:")[1]
        questions = examples_text.split(";")
        return { custom_question: questions }
    else:
        return { custom_question: [] }
```

**Flowchart**:
```
[Request] → Get profile by name
          → Extract "comments" field
          → Contains "Examples:"?
              ├── Yes → Split by ";" → Return question list
              └── No → Return empty list
```

---

## 3. POST /qa/get_history_by_user_profile

**Purpose**: Get all chat history grouped by session for a user + profile.

**Request**:
```json
POST /qa/get_history_by_user_profile
{
    "user_id": "admin",
    "profile_name": "demo-profile",
    "log_type": "chat_history"
}
```

**Pseudocode**:
```
function get_history_by_user_profile(user_id, profile_name):
    history_list = LogManagement.get_history(user_id, profile_name)
    sessions = {}

    for item in history_list:
        session_id = item.session_id
        if session_id not in sessions:
            sessions[session_id] = []
        sessions[session_id].append(
            Message(type="human", content=item.query),
            Message(type="AI", content=parse_json(item.log_info))
        )

    return [{ session_id, messages } for each session]
```

**Flowchart**:
```
[Request] → Query DB for all logs (user_id + profile_name)
          → Group by session_id
          → For each session: pair up human/AI messages
          → [Response: [{session_id, messages[]}, ...]]
```

---

## 4. POST /qa/get_sessions

**Purpose**: Get session list for a user (sidebar navigation).

**Request**:
```json
POST /qa/get_sessions
{
    "user_id": "admin",
    "profile_name": "demo-profile",
    "log_type": "chat_history"
}
```

**Pseudocode**:
```
function get_sessions(user_id, profile_name, log_type):
    return LogManagement.get_all_sessions(user_id, profile_name, log_type)
```

**Flowchart**:
```
[Request] → Query DB for distinct sessions
          → [Response: session list]
```

---

## 5. POST /qa/get_history_by_session

**Purpose**: Get all messages in a specific session (load chat when clicking a session).

**Request**:
```json
POST /qa/get_history_by_session
{
    "session_id": "abc-123",
    "user_id": "admin",
    "profile_name": "demo-profile",
    "log_type": "chat_history"
}
```

**Pseudocode**:
```
function get_history_by_session(session_id, user_id, profile_name, log_type):
    history_list = LogManagement.get_all_history_by_session(
        profile_name, user_id, session_id, size=1000, log_type
    )
    messages = format_chat_history(history_list, log_type)
    title = history_list[0].query if history_list else ""
    return { session_id, messages, title }
```

**Flowchart**:
```
[Request] → Query DB for session messages (up to 1000)
          → Format into human/AI message pairs
          → Extract title from first query
          → [Response: {session_id, messages[], title}]
```

---

## 6. POST /qa/delete_history_by_session

**Purpose**: Delete all messages in a session.

**Request**:
```json
POST /qa/delete_history_by_session
{
    "session_id": "abc-123",
    "user_id": "admin",
    "profile_name": "demo-profile"
}
```

**Pseudocode**:
```
function delete_history_by_session(user_id, profile_name, session_id):
    return LogManagement.delete_history_by_session(user_id, profile_name, session_id)
```

**Flowchart**:
```
[Request] → Delete all logs matching (user_id + profile_name + session_id)
          → [Response: success/fail]
```

---

## 7. POST /qa/user_feedback

**Purpose**: User upvotes (adds to RAG examples) or downvotes (logs error feedback).

**Request**:
```json
POST /qa/user_feedback
{
    "feedback_type": "upvote",       // or "downvote"
    "data_profiles": "demo-profile",
    "query": "Show me total sales",
    "query_intent": "normal_search",
    "query_answer": "SELECT SUM(amount) FROM orders",
    "session_id": "abc-123",
    "user_id": "admin",
    // downvote only:
    "error_description": "",
    "error_categories": "",
    "correct_sql_reference": ""
}
```

**Pseudocode**:
```
function user_feedback(input):
    if input.feedback_type == "upvote":
        // Add Q&A pair to vector store → improves future RAG retrieval
        VectorStore.add_sample(input.data_profiles, input.query, input.query_answer)
        return True

    elif input.feedback_type == "downvote":
        error_info = {
            error_description,
            error_categories,
            correct_sql_reference
        }
        // Log error feedback for review
        FeedBackManagement.add_log_to_database(
            user_id, session_id, profile_name,
            sql=query_answer, query, intent, error_info,
            log_type="feedback_downvote"
        )
        return True
```

**Flowchart**:
```
[Request] → feedback_type?
              ├── "upvote"
              │     → VectorStore.add_sample(profile, query, sql)
              │     → [This Q&A pair will be retrieved as example in future queries]
              │
              └── "downvote"
                    → Build error_info JSON
                    → FeedBackManagement.add_log_to_database(...)
                    → [Stored for human review / model improvement]
```

---

## 8. WebSocket /qa/ws (Core Feature)

**Purpose**: Main NLQ query endpoint. User asks a natural language question → system generates SQL → executes → returns results with optional insights and visualization.

**Request** (JSON via WebSocket):
```json
{
    "query": "Show me total sales this month",
    "profile_name": "demo-profile",
    "session_id": "abc-123",
    "user_id": "admin",
    "username": "justin",
    "bedrock_model_id": "anthropic.claude-3-sonnet-20240229-v1:0",
    "use_rag_flag": true,
    "intent_ner_recognition_flag": true,
    "agent_cot_flag": true,
    "explain_gen_process_flag": true,
    "gen_suggested_question_flag": false,
    "answer_with_insights": false,
    "context_window": 5,
    "query_rewrite": "",
    "previous_intent": "",
    "entity_user_select": {},
    "entity_retrieval": []
}
```

**Response** (multiple WebSocket messages):
```
1. STATE messages (progress): {"content_type": "state", "content": {"text": "Step Name", "status": "start|end"}}
2. END message (final):       {"content_type": "end", "content": Answer}
3. EXCEPTION (if error):      {"content_type": "exception", "content": "error message"}
```

### Full Pseudocode

```
async function websocket_endpoint(websocket):
    accept connection

    while True:
        data = receive_text()
        question = parse_json(data) as Question

        // --- Authentication ---
        if not skipAuthentication:
            tokens = extract(X-Access-Token, X-Id-Token, X-Refresh-Token)
            auth_response = authenticate(tokens)
            if auth failed:
                send END with {X-Status-Code: 401}
                continue

        // --- Build Context ---
        all_profiles = ProfileManagement.get_all_profiles_with_info()
        db_profile = all_profiles[question.profile_name]

        if db_profile.db_url is empty:
            db_profile.db_url = ConnectionManagement.get_db_url(db_profile.conn_name)
            db_profile.db_type = ConnectionManagement.get_db_type(db_profile.conn_name)

        history = []
        if context_window > 0:
            history = LogManagement.get_history_by_session(profile, user, session, size)
            history.append("user:" + query)

        context = ProcessingContext(question, db_profile, history, ...)
        state_machine = QueryStateMachine(context)

        // --- State Machine Loop ---
        while state != COMPLETE and state != ERROR:
            send STATE(current_state_label, "start")
            state_machine.execute_current_state()
            send STATE(current_state_label, "end")

        // --- Post-processing ---
        if gen_suggested_question_flag and intent != "entity_select":
            state_machine.handle_suggest_question()

        if state == COMPLETE:
            state_machine.handle_data_visualization()
            state_machine.handle_add_to_log(log_id)

        // --- Send Final Result ---
        send END(answer)
```

### State Machine Flowchart

```
                              ┌──────────┐
                              │ INITIAL  │
                              └────┬─────┘
                                   │
                        context_window > 0?
                       ┌───Yes───┐     │No
                       ▼         │     │
                ┌─────────────┐  │     │
                │QUERY REWRITE│  │     │
                └──────┬──────┘  │     │
                       │         │     │
                  ask_in_reply?  │     │
                  ┌──Yes──┐     │     │
                  ▼       │     │     │
              COMPLETE    └──┬──┘     │
              (反問用戶)      │        │
                             ▼        ▼
                      ┌────────────────────┐
                      │ INTENT_RECOGNITION │
                      │ (LLM classifies)   │
                      └────────┬───────────┘
                               │
              ┌────────┬───────┼────────┐
              ▼        ▼       ▼        ▼
         ┌────────┐┌───────┐┌──────┐┌───────┐
         │REJECT  ││KNOWL- ││NORMAL││AGENT  │
         │INTENT  ││EDGE   ││SEARCH││SEARCH │
         └───┬────┘│SEARCH │└──┬───┘└───┬───┘
             │     └───┬───┘   │        │
             ▼         ▼       ▼        ▼
         COMPLETE  COMPLETE  ┌──────────────┐
                             │   ENTITY     │
                             │  RETRIEVAL   │
                             │(vector search│
                             │  for NER)    │
                             └──────┬───────┘
                                    │
                            same-name entity?
                           ┌──Yes──┐    │No
                           ▼       │    │
                    ┌──────────┐   │    │
                    │ASK_ENTITY│   │    │
                    │ SELECT   │   │    │
                    └────┬─────┘   │    │
                         │         │    │
                     COMPLETE      │    │
                  (intent=entity   │    │
                   _select)        │    ▼
                                   │  ┌──────────────┐
                                   │  │ QA_RETRIEVAL  │
                                   │  │(find similar  │
                                   │  │ Q&A examples) │
                                   │  └──────┬───────┘
                                   │         │
                                   │         ▼
                                   │  ┌──────────────┐
                                   │  │SQL_GENERATION │
                                   │  │(LLM text→SQL) │
                                   │  │+ row-level    │
                                   │  │  security     │
                                   │  └──────┬───────┘
                                   │         │
                                   │   visualize?
                                   │   ┌─No──┐  │Yes
                                   │   ▼     │  ▼
                                   │ COMPLETE │ ┌─────────────┐
                                   │         │ │EXECUTE_QUERY │
                                   │         │ │(run SQL on DB)│
                                   │         │ └──────┬───────┘
                                   │         │        │
                                   │         │   status_code?
                                   │         │  ┌─200─┐  │500
                                   │         │  │     │  │
                                   │         │  │  analyse? auto_correct?
                                   │         │  │  ┌Yes┐  ┌Yes──────────────┐
                                   │         │  │  ▼   │  ▼                 │
                                   │         │  │ ┌────┴────┐ RE-GENERATE   │
                                   │         │  │ │ANALYZE  │ SQL with error│
                                   │         │  │ │DATA     │ info → retry  │
                                   │         │  │ │(LLM     │ execute       │
                                   │         │  │ │insights)│    ├─200→ OK  │
                                   │         │  │ └────┬────┘    └─500→ERROR│
                                   │         │  │      │                    │
                                   │         │  │      ▼                    │
                                   │         │  │  COMPLETE                 │
                                   │         │  │                           │
                                   │         │  └─No analyse→ COMPLETE      │
                                   │         │                              │
                                   │         └─No auto_correct────→ ERROR   │
                                   │
                                   │ (AGENT path)
                                   ▼
                            ┌──────────────┐
                            │  AGENT_TASK  │
                            │(LLM splits   │
                            │ into subtasks)│
                            └──────┬───────┘
                                   │
                                   ▼
                            ┌──────────────┐
                            │ AGENT_SEARCH │
                            │(generate SQL │
                            │ per subtask) │
                            └──────┬───────┘
                                   │
                                   ▼
                            ┌──────────────────┐
                            │AGENT_DATA_SUMMARY│
                            │(execute each SQL,│
                            │ LLM summarizes)  │
                            └──────┬───────────┘
                                   │
                                   ▼
                               COMPLETE


    ┌────────────────── After COMPLETE ──────────────────┐
    │                                                     │
    │  1. SUGGEST_QUESTION (if flag on + search/agent)   │
    │  2. DATA_VISUALIZATION (LLM picks chart type)      │
    │  3. ADD_LOG (save to database)                     │
    │  4. Send END message with full Answer              │
    └─────────────────────────────────────────────────────┘
```

### User Entity Select Flow (Re-entry)

When `previous_intent = "entity_select"`, the user has selected an entity:

```
[WebSocket message with previous_intent="entity_select"]
    → state_machine starts at USER_SELECT_ENTITY
    → Process user's selection
    → Filter entity_slot based on selection
    → Continue to QA_RETRIEVAL → SQL_GENERATION → ... (normal flow)
```

### Answer Object Structure

```json
{
    "query": "original user question",
    "query_rewrite": "LLM rewritten question",
    "query_intent": "normal_search | agent_search | knowledge_search | reject_search | entity_select | ask_in_reply",

    "sql_search_result": {
        "sql": "SELECT ...",
        "sql_data": [["col1","col2"], [val1, val2], ...],
        "data_show_type": "table | bar | line | pie",
        "sql_gen_process": "explanation of how SQL was generated",
        "data_analyse": "LLM insights about the data",
        "sql_data_chart": [{"chart_type": "bar", "chart_data": [...]}]
    },

    "knowledge_search_result": {
        "knowledge_response": "direct LLM answer (no SQL)"
    },

    "agent_search_result": {
        "agent_sql_search_result": [
            {"sub_task_query": "subtask 1", "sql_search_result": {...}},
            {"sub_task_query": "subtask 2", "sql_search_result": {...}}
        ],
        "agent_summary": "LLM summary of all subtask results"
    },

    "ask_rewrite_result": {
        "query_rewrite": "LLM follow-up question"
    },

    "suggested_question": ["question 1", "question 2", "question 3"],

    "ask_entity_select": {
        "entity_select_info": {"entity_name": [{"table_name", "column_name", "value"}]},
        "entity_retrieval": [...]
    },

    "error_log": {}
}
```

---

## Dependencies Between APIs

```
Frontend Flow:

1. Page Load → GET /qa/option → populate profile & model dropdowns
2. Select Profile → GET /qa/get_custom_question → show example questions
3. Load Sessions → POST /qa/get_sessions → show session list in sidebar
4. Click Session → POST /qa/get_history_by_session → load chat messages
5. Ask Question → WebSocket /qa/ws → main NLQ flow
6. Like/Dislike → POST /qa/user_feedback → improve RAG / log errors
7. Delete Session → POST /qa/delete_history_by_session → cleanup
```
