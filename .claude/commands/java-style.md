# Java 開發規範 (Java Coding Conventions)

適用於 GenBI Java 重構專案，所有開發者請遵守以下規範。

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
- **不可變資料** → 用 `record`（Question, ProcessingContext, ApiResponse...）
- **需要逐步修改** → 用 `class` + `@Getter`（如 Answer，狀態機會逐步修改）
- Controller 的 Request/Response 一律用 record
- **不要** 在 Entity 和 DTO 之間共用同一個 class

## Entity — 不用 @Setter

Entity **禁止使用 `@Setter`**，遵循以下規則：

### 基本結構
```java
@Entity
@Getter                                          // ✅ 只讀
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA 需要，外部不可用
@AllArgsConstructor(access = AccessLevel.PRIVATE)    // 只給 Builder 用
@Builder
public class ChatSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                              // ← 沒有 setter，不可改

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;              // ← 沒有 setter，靠 @PrePersist

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
```

### 建立 Entity → 用 Builder
```java
// ✅ 正確：Builder 一次給齊
ChatMessage.builder()
    .sessionId("s1")
    .query("show orders")
    .build();

// ❌ 錯誤：new + setter
ChatMessage m = new ChatMessage();
m.setSessionId("s1");  // 編譯不過，沒有 setter
```

### 修改 Entity → 用 Domain Method
```java
// ✅ 正確：有限的、語義化的修改方法
session.touch();                    // 觸發 @PreUpdate
session.updateTitle("新標題");       // 限定只能改 title
profile.updateFrom(request);        // 從 DTO 批量更新

// ❌ 錯誤：裸 setter
session.setId(999);                 // 編譯不過
session.setCreatedAt(null);         // 編譯不過
```

### 各 Entity 的可修改策略
| Entity | 建立後可改？ | 暴露的 Domain Method |
|--------|------------|---------------------|
| `ChatMessage` | ❌ 不可改 | 無（完全 immutable） |
| `UserFeedback` | ❌ 不可改 | 無（完全 immutable） |
| `ChatSession` | 部分可改 | `touch()`, `updateTitle()` |
| `DbProfile` | 部分可改 | `updateFrom(ProfileRequest)` |

### 時間戳管理
- `createdAt` → `@PrePersist` 自動設定，**不給 setter**
- `updatedAt` → `@PreUpdate` 自動設定，**不給 setter**
- 要觸發更新 → 呼叫 domain method 後 `repository.save(entity)`

### 測試中建構 Entity
```java
// 測試用 Builder，不用 setter
private ChatSession buildSession(String sessionId) {
    return ChatSession.builder()
            .id(1L)
            .sessionId(sessionId)
            .userId("user1")
            .createdAt(LocalDateTime.now())
            .build();
}
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

## API 回傳格式
- 統一用 `ApiResponse<T>` 包裝
- 成功: `ApiResponse.ok(data)` → `{ code: 200, message: "ok", data: ... }`
- 失敗: 由 GlobalExceptionHandler 自動處理

## Validation
- Request DTO 上加 `@NotBlank`, `@Pattern` 等 Jakarta Validation
- Controller 方法參數加 `@Valid`
- 驗證失敗自動回傳 400 + 錯誤訊息

## 命名規範
- Package: `com.lndata.genbi.{layer}` (controller, service, dto, entity, repository, config, exception, enums, statemachine)
- Entity: 單數名詞 (`ChatSession`, `UserFeedback`)
- Table: 複數蛇底線 (`chat_sessions`, `user_feedbacks`)
- DTO: 動作 + 名詞 (`ProfileRequest`, `SessionResponse`)
- Domain Method: 動詞開頭 (`updateFrom()`, `touch()`, `updateTitle()`)

## 註解風格
- Class level: 簡短中文 javadoc，1-2 行
- Method level: `/** 一行中文說明 */`
- Inline: 中文為主，技術術語可用英文
- **不要** 寫 "對應 Python: xxx" 這種對照註解
- **不要** 放 AI 產生的痕跡（Co-Authored-By 等）
