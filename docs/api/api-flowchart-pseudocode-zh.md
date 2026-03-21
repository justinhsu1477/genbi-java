# NLQ API 流程圖與偽碼（中文版）

> 所有 API 都在 `/qa` 前綴底下
> 來源：`data-transform-gen-bi/application/api/main.py`

---

## API 總覽

| # | 方法 | 路徑 | 說明 |
|---|------|------|------|
| 1 | GET | `/qa/option` | 取得可用的 Profile 和模型列表 |
| 2 | GET | `/qa/get_custom_question` | 取得某個 Profile 的範例問題 |
| 3 | POST | `/qa/get_history_by_user_profile` | 取得用戶的聊天紀錄（依 Profile 分組） |
| 4 | POST | `/qa/get_sessions` | 取得用戶的所有對話 Session |
| 5 | POST | `/qa/get_history_by_session` | 取得某個 Session 的所有訊息 |
| 6 | POST | `/qa/delete_history_by_session` | 刪除某個 Session 的紀錄 |
| 7 | POST | `/qa/user_feedback` | 用戶按讚/倒讚回饋 |
| 8 | WebSocket | `/qa/ws` | **主要 NLQ 查詢功能（核心）** |

---

## 1. GET /qa/option

**用途**：回傳前端下拉選單需要的「資料庫 Profile 列表」和「LLM 模型列表」。

**請求**：
```
GET /qa/option?id=user123   (id 可選)
```

**偽碼**：
```
function 取得選項(用戶id):
    所有profiles = 從資料庫取得所有Profile資訊()
    所有模型 = Bedrock內建模型 + SageMaker自訂模型

    如果 開啟了用戶Profile對應 且 有用戶id:
        用戶對應 = 查詢用戶允許的Profile(用戶id)
        如果 找到對應:
            回傳 { profiles: 用戶允許的列表, 模型: 所有模型 }
        否則:
            回傳 { profiles: ["No_Profile"], 模型: 所有模型 }
    否則:
        回傳 { profiles: 全部Profile, 模型: 所有模型 }
```

**流程圖**：
```
[請求] → 有開啟用戶Profile對應嗎？
            ├── 有 + 有id → 查用戶對應表
            │                 ├── 找到 → 回傳該用戶允許的 Profile
            │                 └── 沒找到 → 回傳 ["No_Profile"]
            └── 沒有 → 回傳所有 Profile
         → 附上所有可用模型 ID
         → [回應: {data_profiles, bedrock_model_ids}]
```

---

## 2. GET /qa/get_custom_question

**用途**：取得 Profile 中設定的範例問題（前端提示用戶可以問什麼）。

**請求**：
```
GET /qa/get_custom_question?data_profile=demo-profile
```

**偽碼**：
```
function 取得範例問題(profile名稱):
    所有profiles = 取得所有Profile資訊()
    profile = 所有profiles[profile名稱]
    備註欄 = profile的comments欄位

    如果 備註欄包含 "Examples:":
        範例文字 = 備註欄.切割("Examples:")[1]
        問題列表 = 範例文字.用分號切割()
        回傳 { custom_question: 問題列表 }
    否則:
        回傳 { custom_question: [] }
```

**流程圖**：
```
[請求] → 根據名稱找到 Profile
       → 取出 comments 欄位
       → 有包含 "Examples:" 嗎？
           ├── 有 → 用 ";" 切割 → 回傳問題列表
           └── 沒有 → 回傳空列表
```

---

## 3. POST /qa/get_history_by_user_profile

**用途**：取得某用戶在某 Profile 下的所有聊天紀錄，按 Session 分組。

**請求**：
```json
{
    "user_id": "admin",
    "profile_name": "demo-profile",
    "log_type": "chat_history"
}
```

**偽碼**：
```
function 取得用戶歷史紀錄(user_id, profile_name):
    紀錄列表 = 從資料庫查詢(user_id, profile_name)
    sessions = {}

    對每筆紀錄:
        session_id = 紀錄.session_id
        如果 session_id 不在 sessions 裡:
            sessions[session_id] = []
        sessions[session_id].加入(
            人類訊息(type="human", content=紀錄.query),
            AI訊息(type="AI", content=解析JSON(紀錄.log_info))
        )

    回傳 [{ session_id, messages } ...]
```

**流程圖**：
```
[請求] → 查 DB (user_id + profile_name)
       → 按 session_id 分組
       → 每組配對 human/AI 訊息
       → [回應: [{session_id, messages[]}, ...]]
```

---

## 4. POST /qa/get_sessions

**用途**：取得用戶的 Session 列表（側邊欄導航用）。

**請求**：
```json
{
    "user_id": "admin",
    "profile_name": "demo-profile",
    "log_type": "chat_history"
}
```

**偽碼**：
```
function 取得sessions(user_id, profile_name, log_type):
    回傳 LogManagement.取得所有sessions(user_id, profile_name, log_type)
```

**流程圖**：
```
[請求] → 查 DB 取得不重複的 session 列表
       → [回應: session 列表]
```

---

## 5. POST /qa/get_history_by_session

**用途**：取得某個 Session 的所有訊息（點選 Session 時載入聊天記錄）。

**請求**：
```json
{
    "session_id": "abc-123",
    "user_id": "admin",
    "profile_name": "demo-profile",
    "log_type": "chat_history"
}
```

**偽碼**：
```
function 取得session紀錄(session_id, user_id, profile_name, log_type):
    紀錄列表 = 從DB查詢(profile, user, session, 最多1000筆, log_type)
    訊息 = 格式化聊天紀錄(紀錄列表, log_type)
    標題 = 紀錄列表[0].query  // 第一個問題當標題
    回傳 { session_id, messages: 訊息, title: 標題 }
```

**流程圖**：
```
[請求] → 查 DB 該 session 的紀錄（最多 1000 筆）
       → 格式化成 human/AI 訊息對
       → 取第一個問題當標題
       → [回應: {session_id, messages[], title}]
```

---

## 6. POST /qa/delete_history_by_session

**用途**：刪除某個 Session 的所有紀錄。

**請求**：
```json
{
    "session_id": "abc-123",
    "user_id": "admin",
    "profile_name": "demo-profile"
}
```

**偽碼**：
```
function 刪除session紀錄(user_id, profile_name, session_id):
    回傳 LogManagement.刪除session紀錄(user_id, profile_name, session_id)
```

**流程圖**：
```
[請求] → 刪除 DB 中符合 (user_id + profile_name + session_id) 的所有紀錄
       → [回應: 成功/失敗]
```

---

## 7. POST /qa/user_feedback

**用途**：用戶對查詢結果按讚或倒讚。按讚會把這組 Q&A 加入向量庫（改善未來 RAG 檢索品質）；倒讚會記錄錯誤回饋。

**請求**：
```json
{
    "feedback_type": "upvote",       // 或 "downvote"
    "data_profiles": "demo-profile",
    "query": "今個月總銷售額多少",
    "query_intent": "normal_search",
    "query_answer": "SELECT SUM(amount) FROM orders",
    "session_id": "abc-123",
    "user_id": "admin",
    // 倒讚才需要:
    "error_description": "",
    "error_categories": "",
    "correct_sql_reference": ""
}
```

**偽碼**：
```
function 用戶回饋(input):
    如果 feedback_type == "upvote":
        // 加入向量庫 → 未來查詢會檢索到這組 Q&A 作為範例
        VectorStore.新增範例(profile, query, sql)
        回傳 成功

    如果 feedback_type == "downvote":
        錯誤資訊 = {
            error_description,      // 錯誤描述
            error_categories,       // 錯誤分類
            correct_sql_reference   // 正確 SQL 參考
        }
        // 記錄到回饋表，供人工審查
        FeedBackManagement.寫入DB(user_id, session_id, profile, sql, query, 錯誤資訊)
        回傳 成功
```

**流程圖**：
```
[請求] → 回饋類型？
           ├── "upvote" (按讚)
           │     → VectorStore.新增範例(profile, query, sql)
           │     → [這組 Q&A 以後會被 RAG 檢索到，改善 SQL 品質]
           │
           └── "downvote" (倒讚)
                 → 組合錯誤資訊 JSON
                 → 寫入回饋DB
                 → [供人工審查 / 模型改善]
```

---

## 8. WebSocket /qa/ws（核心功能）

**用途**：主要 NLQ 查詢端點。用戶用自然語言問問題 → 系統生成 SQL → 執行 → 回傳結果，附帶可選的數據分析和圖表。

**請求**（透過 WebSocket 送 JSON）：
```json
{
    "query": "這個月的總銷售額是多少",
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

**回應**（多條 WebSocket 訊息）：
```
1. STATE 訊息（進度）: {"content_type": "state", "content": {"text": "步驟名稱", "status": "start|end"}}
2. END 訊息（最終結果）: {"content_type": "end", "content": Answer物件}
3. EXCEPTION（錯誤）: {"content_type": "exception", "content": "錯誤訊息"}
```

### 完整偽碼

```
async function WebSocket端點(websocket):
    接受連線

    持續監聽:
        資料 = 接收文字()
        問題 = 解析JSON(資料) → Question物件

        // --- 認證 ---
        如果 需要認證:
            token = 取出(X-Access-Token, X-Id-Token, X-Refresh-Token)
            認證結果 = authenticate(token)
            如果 認證失敗:
                推送 END {X-Status-Code: 401}
                繼續監聽

        // --- 建立上下文 ---
        所有profiles = 取得所有Profile資訊()
        db_profile = 所有profiles[問題.profile_name]

        如果 db_profile.db_url 是空的:
            db_profile.db_url = 用連線名稱查DB連線URL(db_profile.conn_name)
            db_profile.db_type = 用連線名稱查DB類型(db_profile.conn_name)

        歷史紀錄 = []
        如果 context_window > 0:
            歷史紀錄 = 查session歷史(profile, user, session, 筆數)
            歷史紀錄.加入("user:" + query)

        上下文 = ProcessingContext(問題, db_profile, 歷史紀錄, ...)
        狀態機 = QueryStateMachine(上下文)

        // --- 狀態機迴圈 ---
        當 狀態 != 完成 且 狀態 != 錯誤:
            推送 STATE(當前步驟名稱, "start")
            狀態機.執行當前狀態()
            推送 STATE(當前步驟名稱, "end")

        // --- 後處理 ---
        如果 要生成建議問題 且 意圖 != "entity_select":
            狀態機.生成建議問題()

        如果 狀態 == 完成:
            狀態機.處理數據可視化()
            狀態機.儲存紀錄(log_id)

        // --- 送出最終結果 ---
        推送 END(answer)
```

### 狀態機流程圖

```
                              ┌──────────┐
                              │  初始狀態  │
                              │ INITIAL  │
                              └────┬─────┘
                                   │
                        context_window > 0？
                       ┌───是───┐      │否
                       ▼        │      │
                ┌─────────────┐ │      │
                │  查詢改寫    │ │      │
                │QUERY REWRITE│ │      │
                └──────┬──────┘ │      │
                       │        │      │
                  要反問用戶？    │      │
                  ┌──是──┐      │      │
                  ▼      │      │      │
              完成       └──┬───┘      │
            (LLM反問)       │           │
                            ▼           ▼
                     ┌────────────────────┐
                     │    意圖識別         │
                     │ INTENT_RECOGNITION │
                     │  (LLM 分類查詢)     │
                     └────────┬───────────┘
                              │
             ┌────────┬───────┼────────┐
             ▼        ▼       ▼        ▼
        ┌────────┐┌───────┐┌──────┐┌───────┐
        │ 拒絕   ││ 知識  ││ 正常 ││ Agent │
        │ 回答   ││ 搜索  ││ 搜索 ││ 搜索  │
        │REJECT  ││KNOWL- ││NORMAL││AGENT  │
        └───┬────┘│EDGE   │└──┬───┘└───┬───┘
            │     └───┬───┘   │        │
            ▼         ▼       │        │
         完成      完成       │        │
       (不回答)  (LLM直接     │        │
                  回答)       ▼        │
                      ┌──────────────┐ │
                      │   實體檢索    │ │
                      │  ENTITY      │ │
                      │  RETRIEVAL   │ │
                      │(向量搜索NER) │ │
                      └──────┬───────┘ │
                             │         │
                     有同名實體？       │
                    ┌──有──┐    │無    │
                    ▼      │    │     │
             ┌──────────┐  │    │     │
             │請用戶選擇 │  │    │     │
             │ASK_ENTITY│  │    │     │
             │ SELECT   │  │    │     │
             └────┬─────┘  │    │     │
                  │        │    │     │
               完成        │    │     │
            (等用戶選)      │    ▼     │
                           │ ┌──────────────┐
                           │ │  QA 檢索     │
                           │ │ QA_RETRIEVAL │
                           │ │(找相似問答對) │
                           │ └──────┬───────┘
                           │        │
                           │        ▼
                           │ ┌──────────────┐
                           │ │  SQL 生成    │
                           │ │SQL_GENERATION│
                           │ │(LLM 文字→SQL)│
                           │ │+ 行級安全    │
                           │ └──────┬───────┘
                           │        │
                           │  要執行嗎？
                           │  ┌─否─┐  │是
                           │  ▼    │  ▼
                           │ 完成  │ ┌─────────────┐
                           │      │ │  執行 SQL    │
                           │      │ │EXECUTE_QUERY │
                           │      │ │(對DB跑SQL)   │
                           │      │ └──────┬───────┘
                           │      │        │
                           │      │   執行結果？
                           │      │  ┌成功─┐   │失敗
                           │      │  │     │   │
                           │      │  │  要分析？ 自動修正？
                           │      │  │  ┌是┐   ┌是────────────┐
                           │      │  │  ▼  │   ▼              │
                           │      │  │┌────┴───┐ 帶錯誤訊息    │
                           │      │  ││數據分析 │ 重新生成SQL   │
                           │      │  ││ANALYZE │ → 再執行一次  │
                           │      │  ││DATA    │   ├成功→ 完成  │
                           │      │  │└───┬────┘   └失敗→ 錯誤  │
                           │      │  │    ▼                     │
                           │      │  │  完成                    │
                           │      │  │                          │
                           │      │  └─不分析→ 完成             │
                           │      │                             │
                           │      └─不修正──────→ 錯誤          │
                           │
                           │ （Agent 路徑）
                           ▼
                    ┌──────────────┐
                    │ Agent 任務拆解│
                    │  AGENT_TASK  │
                    │(LLM拆成子任務)│
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │ Agent SQL生成 │
                    │ AGENT_SEARCH │
                    │(每個子任務    │
                    │ 各生成一條SQL)│
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────────┐
                    │ Agent 數據匯總    │
                    │AGENT_DATA_SUMMARY│
                    │(執行各SQL,       │
                    │ LLM 統整分析)    │
                    └──────┬───────────┘
                           │
                           ▼
                        完成


    ┌───────────── 完成之後 ──────────────────┐
    │                                          │
    │  1. 建議問題（如果開啟且是搜索/Agent意圖） │
    │  2. 數據可視化（LLM選圖表類型）           │
    │  3. 儲存紀錄到DB                         │
    │  4. 推送 END 訊息，包含完整 Answer        │
    └──────────────────────────────────────────┘
```

### 用戶選擇實體的重入流程

當 `previous_intent = "entity_select"` 時，代表用戶已經選好了實體：

```
[WebSocket 訊息，previous_intent="entity_select"]
    → 狀態機從 USER_SELECT_ENTITY 開始
    → 處理用戶的選擇
    → 根據選擇過濾實體
    → 繼續到 QA_RETRIEVAL → SQL_GENERATION → ...（正常流程）
```

### Answer 物件結構

```json
{
    "query": "用戶原始問題",
    "query_rewrite": "LLM 改寫後的問題",
    "query_intent": "normal_search | agent_search | knowledge_search | reject_search | entity_select | ask_in_reply",

    "sql_search_result": {
        "sql": "SELECT ...",
        "sql_data": [["欄位1","欄位2"], [值1, 值2], ...],
        "data_show_type": "table | bar | line | pie",
        "sql_gen_process": "SQL 生成過程說明",
        "data_analyse": "LLM 數據分析結論",
        "sql_data_chart": [{"chart_type": "bar", "chart_data": [...]}]
    },

    "knowledge_search_result": {
        "knowledge_response": "LLM 直接回答（不走 SQL）"
    },

    "agent_search_result": {
        "agent_sql_search_result": [
            {"sub_task_query": "子任務1", "sql_search_result": {...}},
            {"sub_task_query": "子任務2", "sql_search_result": {...}}
        ],
        "agent_summary": "LLM 對所有子任務結果的統整分析"
    },

    "ask_rewrite_result": {
        "query_rewrite": "LLM 反問用戶的問題"
    },

    "suggested_question": ["建議問題1", "建議問題2", "建議問題3"],

    "ask_entity_select": {
        "entity_select_info": {"實體名": [{"table_name", "column_name", "value"}]},
        "entity_retrieval": [...]
    },

    "error_log": {}
}
```

---

## API 之間的依賴關係

```
前端使用流程：

1. 頁面載入 → GET /qa/option → 填入 Profile 和模型下拉選單
2. 選擇 Profile → GET /qa/get_custom_question → 顯示範例問題
3. 載入 Sessions → POST /qa/get_sessions → 側邊欄顯示對話列表
4. 點選 Session → POST /qa/get_history_by_session → 載入聊天訊息
5. 提問 → WebSocket /qa/ws → 執行 NLQ 主流程
6. 按讚/倒讚 → POST /qa/user_feedback → 改善 RAG / 記錄錯誤
7. 刪除 Session → POST /qa/delete_history_by_session → 清理
```
