# GenBI 產品化檢查 (Product Readiness Check)

檢查目前的實作是否符合產品化需求。

## 核心功能完成度
- [ ] WebSocket `/qa/ws` 問答流程
- [ ] 狀態機 4 條路徑 (normal, knowledge, reject, agent)
- [ ] SQL 生成 + 執行 + 結果回傳
- [ ] Auto Correction（SQL 錯誤自動重試）
- [ ] 資料分析 (insights)
- [ ] 建議問題生成
- [ ] 圖表類型推薦

## Profile 管理
- [ ] Profile CRUD API
- [ ] DDL 自動抓取（連上 DB 讀 schema）→ 未實作
- [ ] Hints 管理（業務邏輯提示）
- [ ] Prompt 模板管理 → 未實作

## 安全性
- [ ] Row Level Security (RLS) → 介面已預留
- [ ] 多租戶隔離 (tenant_id) → 未實作
- [ ] Auth 整合 (Lnfusion) → 未實作
- [ ] 敏感資料不暴露（密碼、API Key）

## 效能與成本
- [ ] LLM Token 追蹤 → 未實作
- [ ] 常見問題快取 → 未實作
- [ ] Streaming 回應 (Bedrock streaming) → 未實作
- [ ] 並行處理優化 → 未實作

## 多資料庫支援
- [ ] MySQL ✅
- [ ] Oracle → 未測試
- [ ] PostgreSQL → 未實作
- [ ] SQL 方言自動適配

## 監控與維運
- [ ] Health check endpoint
- [ ] Spring Actuator
- [ ] Error tracking / alerting
- [ ] API 用量統計

Now read the codebase and check each item above. Report the current status and suggest the top 3 priorities for product readiness.
