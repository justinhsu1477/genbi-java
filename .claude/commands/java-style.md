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
- **需要 setter** → 用 `class` + `@Getter @Setter`（如 Answer，狀態機會逐步修改）
- Controller 的 Request/Response 一律用 record
- **不要** 在 Entity 和 DTO 之間共用同一個 class

## Entity
- 使用 Lombok `@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder`
- `@PrePersist` / `@PreUpdate` 自動管理 createdAt / updatedAt
- 敏感欄位（密碼）不放進 Response DTO

## Logging
- 統一使用 Lombok `@Slf4j`
- 格式: `log.info("[模組名] 動作: key={}, key={}", val, val)`
- 範例: `log.info("[Session] deleteSession: sessionId={}", sessionId)`
- **不要** 用 `System.out.println`
- **不要** 手動 `LoggerFactory.getLogger()`

## 異常處理
- 業務錯誤 → 拋 `BusinessException`（自動被 GlobalExceptionHandler 捕獲）
- 工廠方法: `BusinessException.notFound("Session xxx")` → 404
- 工廠方法: `BusinessException.badRequest("Invalid input")` → 400
- Controller 不需要 try-catch，交給全域處理

## API 回傳格式
- 統一用 `ApiResponse<T>` 包裝
- 成功: `ApiResponse.ok(data)` → `{ code: 200, message: "ok", data: ... }`
- 失敗: 由 GlobalExceptionHandler 自動處理

## Validation
- Request DTO 上加 `@NotBlank`, `@Pattern` 等 Jakarta Validation
- Controller 方法參數加 `@Valid`
- 驗證失敗自動回傳 400 + 錯誤訊息

## 命名規範
- Package: `com.nlq.{layer}` (controller, service, dto, entity, repository, config, exception, enums, statemachine)
- Entity: 單數名詞 (`ChatSession`, `UserFeedback`)
- Table: 複數蛇底線 (`chat_sessions`, `user_feedbacks`)
- DTO: 動作 + 名詞 (`ProfileRequest`, `SessionResponse`)

## 註解風格
- Class level: 簡短中文 javadoc，1-2 行
- Method level: `/** 一行中文說明 */`
- Inline: 中文為主，技術術語可用英文
- **不要** 寫 "對應 Python: xxx" 這種對照註解
