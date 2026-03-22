# GenBI 產品化檢查 (Product Readiness Check)

檢查目前的實作是否符合產品化需求。

## 核心功能完成度
- [x] WebSocket `/qa/ws` 問答流程 ✅
- [x] 狀態機 4 條路徑 (normal, knowledge, reject, agent) ✅
- [x] SQL 生成 + 執行 + 結果回傳 ✅
- [x] Auto Correction（SQL 錯誤自動重試） ✅
- [x] 資料分析 (insights) ✅
- [x] 建議問題生成 ✅
- [x] 圖表類型推薦 ✅

## RAG 向量搜尋 (Phase 4)
- [x] Bedrock Titan Embedding (1536 維) ✅
- [x] OpenSearch KNN 向量搜尋 ✅
- [x] 三索引: sql_index, ner_index, agent_index ✅
- [x] 範例管理 REST API (CRUD) ✅
- [x] 索引自動初始化 ✅
- [x] 重複範例去重 (score=1.0) ✅

## Profile 管理
- [x] Profile CRUD API ✅
- [ ] DDL 自動抓取（連上 DB 讀 schema）→ 未實作
- [x] Hints 管理（業務邏輯提示） ✅
- [x] Prompt 模板管理 (PromptType + SqlDialect + PromptService) ✅

## 安全性
- [ ] Row Level Security (RLS) → 介面已預留
- [ ] 多租戶隔離 (tenant_id) → Phase 7 待做
- [ ] Auth 整合 (Lnfusion) → Phase 7 待做
- [x] 敏感資料不暴露（密碼、API Key） ✅

## 效能與成本
- [ ] LLM Token 追蹤 → 未實作
- [ ] 常見問題快取 → 未實作
- [ ] Streaming 回應 (Bedrock streaming) → 未實作
- [ ] 並行處理優化 → 未實作

## 多資料庫支援
- [x] MySQL ✅
- [ ] Oracle → 未測試
- [ ] PostgreSQL → Phase 6 待做
- [x] SQL 方言自動適配 (SqlDialect enum, 8 dialects) ✅

## 監控與維運
- [ ] Health check endpoint
- [ ] Spring Actuator
- [ ] Error tracking / alerting
- [ ] API 用量統計

Now read the codebase and check each item above. Report the current status and suggest the top 3 priorities for product readiness.
