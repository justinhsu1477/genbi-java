# Java 開發規範 (Java Coding Conventions)

適用於 GenBI Java 專案，遵循 AGENTS.md 架構規範。

## Package 結構
```
com.lndata.genbi
├── config          — Spring 和基礎設施配置
├── controller      — HTTP / WebSocket 端點
├── exception       — BusinessException + GlobalExceptionHandler
├── model
│   ├── constant    — 共用 Enum（QueryState, ContentType, PromptType, SqlDialect）
│   ├── dto         — Request / Response / 傳輸物件
│   ├── entity      — JPA Entity（繼承 BaseEntity）
│   └── response    — API 回應包裝（BaseSingleResponse, BaseListResponse, BaseRestResponse）
├── repository      — Spring Data JPA Repository
├── service         — 業務邏輯介面 + 實作
│   ├── impl        — @Profile("!dev") 正式環境實作
│   └── mock        — @Profile("dev") Mock 實作
└── statemachine    — 狀態機（QueryStateMachine）
```

## 依賴注入
- 使用 `@RequiredArgsConstructor`（Lombok），**不用** `@Autowired`
- 所有被注入的欄位宣告為 `private final`

```java
@Service
@RequiredArgsConstructor
public class SessionService {
    private final ChatSessionRepository sessionRepository;  // final + constructor injection
}
```

## DTO
- **不可變資料** → 用 `record`（Question, ProcessingContext...）
- **需要逐步修改** → 用 `class` + `@Getter`（如 Answer，狀態機會逐步修改）
- Controller 的 Request/Response 一律用 record
- **不要** 在 Entity 和 DTO 之間共用同一個 class

## Entity — 繼承 BaseEntity + @Getter @Setter

所有 Entity 繼承 `BaseEntity`（id + createdAt + updatedAt 由 JPA Auditing 自動管理）。

### 基本結構
```java
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession extends BaseEntity {

    @Column(name = "session_id", nullable = false, unique = true, length = 64)
    private String sessionId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    // Domain method（語義化的修改方法）
    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }
}
```

### 建立 Entity → new + setter
```java
// ✅ 正確
ChatMessage msg = new ChatMessage();
msg.setSessionId("s1");
msg.setQuery("show orders");
repository.save(msg);
```

### 修改 Entity → Domain Method（推薦）或直接 setter
```java
// ✅ 推薦：語義化方法
profile.updateFrom(request);

// ✅ 也可以：直接 setter
session.setTitle("新標題");
```

### 時間戳管理
- `id`, `createdAt`, `updatedAt` 繼承自 `BaseEntity`
- 時間型別統一用 `Instant`（UTC）
- `@CreatedDate` / `@LastModifiedDate` 由 JPA Auditing 自動設定
- **不需要** `@PrePersist` / `@PreUpdate`

## API 回傳格式
統一使用 `model.response` 下的包裝類別：

```java
// 回傳單一物件
BaseSingleResponse.success("OK", data)    // { success: true, message: "OK", data: ... }

// 回傳列表
BaseListResponse.success("OK", list)      // { success: true, message: "OK", total: N, data: [...] }

// 無資料回傳（刪除、更新等）
BaseRestResponse.success("OK")            // { success: true, message: "OK" }

// 錯誤
BaseRestResponse.failure("error message") // { success: false, message: "..." }
```

## Logging
- 統一使用 Lombok `@Slf4j`
- 格式: `log.info("[模組名] 動作: key={}, key={}", val, val)`
- 範例: `log.info("[Session] deleteSession: sessionId={}", sessionId)`
- **不要** 用 `System.out.println`
- **不要** 手動 `LoggerFactory.getLogger()`
- **不要** 在 log 中印密碼、API Key

## 異常處理
- 業務錯誤 → 拋 `BusinessException`（自動被 GlobalExceptionHandler 捕獲）
- 工廠方法: `BusinessException.notFound("Session xxx")` → 404
- 工廠方法: `BusinessException.badRequest("Invalid input")` → 400
- Controller 不需要 try-catch，交給全域處理
- SQL 執行錯誤不暴露內部訊息給前端

## Validation
- Request DTO 上加 `@NotBlank`, `@Pattern` 等 Jakarta Validation
- Controller 方法參數加 `@Valid`
- 驗證失敗自動回傳 400 + 錯誤訊息

## 命名規範
- Package: `com.lndata.genbi.{layer}`（controller, service, model.dto, model.entity, model.constant, model.response, repository, config, exception, statemachine）
- Entity: 單數名詞 (`ChatSession`, `UserFeedback`)
- Table: 複數蛇底線 (`chat_sessions`, `user_feedbacks`)
- DTO: 動作 + 名詞 (`ProfileRequest`, `SessionResponse`)
- Domain Method: 動詞開頭 (`updateFrom()`, `updateTitle()`)

## 註解風格
- Class level: 簡短中文 javadoc，1-2 行
- Method level: `/** 一行中文說明 */`
- Inline: 中文為主，技術術語可用英文
- **不要** 寫 "對應 Python: xxx" 這種對照註解
