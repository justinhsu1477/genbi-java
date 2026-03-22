# Code Review Checklist (GenBI 重構 Review)

對新增或修改的 Java 程式碼進行 review，檢查以下項目：

## 1. 架構一致性
- [ ] 是否符合分層架構：Controller → Service → Repository
- [ ] Controller 只做參數接收和回傳，不含業務邏輯
- [ ] Service 處理業務邏輯，不直接回傳 HTTP 相關物件
- [ ] DTO 和 Entity 是否分離（不直接回傳 Entity）

## 2. 程式碼規範（參照 /java-style）
- [ ] 依賴注入用 `@RequiredArgsConstructor`，不用 `@Autowired`
- [ ] 不可變資料用 `record`
- [ ] Logging 用 `@Slf4j`，格式: `[模組名] 動作: key={}`
- [ ] 異常處理用 `BusinessException`，不在 Controller 寫 try-catch
- [ ] API 回傳用 `ApiResponse<T>` 包裝

## 3. Python 對照
- [ ] 功能是否與 Python 原始碼行為一致？（參照 /python-to-java）
- [ ] 是否遺漏 Python 的邊界條件或錯誤處理？
- [ ] 狀態機的 transition 是否正確？

## 4. 安全性
- [ ] 密碼 / API Key 不出現在 Response DTO
- [ ] SQL 執行結果不暴露內部錯誤細節給前端
- [ ] Profile 的 dbPassword 不寫進 log
- [ ] RLS 設定是否被正確套用（或正確跳過）

## 5. 測試
- [ ] 新功能是否有對應的單元測試
- [ ] 是否測試了正常路徑和異常路徑（404, 400, 500）
- [ ] Mock 是否合理（不 mock 被測對象本身）
- [ ] 測試名稱是否清楚描述行為

## 6. 產品化注意
- [ ] Auto Correction 是否正確處理？（只重試一次）
- [ ] LLM 回傳的 JSON 是否有容錯處理？（markdown code block）
- [ ] Profile 刪除是否連帶清理相關資料？
- [ ] 是否有 TODO 標記未完成的功能？

Now read the changed files using `git diff` or `git diff --cached`, and perform the review based on the checklist above. Report findings with severity: 🔴 Must Fix / 🟡 Should Fix / 🟢 Nice to Have.
