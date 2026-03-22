# AWS 架構概念 (GenBI AWS Architecture Guide)

GenBI 部署在 AWS 上，以下是相關的 AWS 服務概念和架構設計。

## GenBI 部署架構

```
                        AWS Region (us-west-2)
                    ┌─────────────────────────────────┐
                    │         VPC (Private Network)     │
                    │                                   │
  用戶 (Browser) ──→│  ALB (Application Load Balancer)  │
                    │    ↓                              │
                    │  ECS Fargate (Spring Boot)        │
                    │    ├── /qa/ws    WebSocket 查詢    │
                    │    ├── /qa/*     REST API          │
                    │    └── /api/v1/* Sample 管理       │
                    │    ↓          ↓           ↓       │
                    │  Bedrock    OpenSearch   RDS/Aurora│
                    │  (LLM+Emb)  (向量搜尋)   (業務DB)  │
                    └─────────────────────────────────┘
```

## AWS 服務對照表

### 核心服務

| AWS 服務 | GenBI 用途 | Java 程式碼 | 設定檔 |
|----------|-----------|------------|--------|
| **Bedrock (Claude)** | LLM 推理：SQL 生成、意圖識別、數據分析 | `BedrockLlmService` | `nlq.bedrock.*` |
| **Bedrock (Titan)** | 文字向量化 (1536 維 Embedding) | `BedrockEmbeddingService` | 常數 `amazon.titan-embed-text-v1` |
| **OpenSearch Service** | KNN 向量搜尋，存放 RAG 範例 (3 索引) | `OpenSearchRetrievalService`, `OpenSearchSampleManagementService` | `nlq.opensearch.*` |
| **RDS / Aurora** | 客戶業務資料庫（GenBI 查詢的目標） | `DatabaseService` | `spring.datasource.*` |
| **ECS Fargate** | 跑 Spring Boot Docker 容器（無伺服器） | `Dockerfile` | `docker-compose.yml` |

### 輔助服務

| AWS 服務 | 用途 | 說明 |
|----------|------|------|
| **IAM** | 權限管理 — ECS Task Role 授權 Bedrock/OpenSearch 存取 | 不需要在程式碼放 API Key |
| **VPC** | 虛擬私有網路 — 所有服務走內網通訊 | 資料安全不出 AWS |
| **CloudWatch** | 日誌 + 監控 + 告警 | Spring Boot log 自動送到 CloudWatch |
| **S3** | 前端靜態資源部署 | 前端團隊負責 |
| **CDK** | Infrastructure as Code (Python 版有) | 或用 docker-compose / Terraform |

## AWS 基礎概念

### Region & AZ
- **Region** = 實體機房區域 (如 us-west-2 奧勒岡, ap-northeast-1 東京)
- **AZ** = 同 Region 內的獨立機房，互相備援
- GenBI 的 Bedrock Claude 目前在 `us-west-2` 可用
- 選 Region 原則：Bedrock 模型可用性 > 離用戶近

### VPC (Virtual Private Cloud)
```
VPC (10.0.0.0/16)
├── Public Subnet — 放 ALB (Load Balancer)，對外開放
├── Private Subnet — 放 ECS, RDS, OpenSearch，不對外
└── NAT Gateway — Private Subnet 要對外時走 NAT
```
- GenBI 所有後端服務在 Private Subnet
- 只有 ALB 對外開放 (HTTPS 443)
- Bedrock / OpenSearch 走 VPC Endpoint (不經公網)

### IAM (Identity and Access Management)
```
IAM Role: genbi-ecs-task-role
├── Policy: bedrock:InvokeModel          → 呼叫 Claude / Titan
├── Policy: es:ESHttpGet, es:ESHttpPost  → 存取 OpenSearch
└── Policy: rds-db:connect               → 連接 RDS
```
- **不要在程式碼放 Access Key** — ECS Task 掛 IAM Role 自動取得權限
- AWS SDK 自動從 IAM Role 拿 credentials (credential chain)

### ECS Fargate (容器服務)
- **Fargate** = 不需要管 EC2 機器，直接跑 Docker 容器
- Spring Boot 打包成 Docker Image → 推到 ECR (Container Registry) → ECS 部署
- Auto Scaling: 可以根據 CPU/請求量自動擴縮容器數

## Bedrock 呼叫方式

### LLM 推理 (Claude)
```java
// AWS SDK v2 — 不需要 API Key
BedrockRuntimeClient client = BedrockRuntimeClient.builder()
    .region(Region.of("us-west-2"))
    .build();  // 自動用 IAM Role 認證

// Messages API 格式（跟 Anthropic API 幾乎一樣）
Map<String, Object> body = Map.of(
    "anthropic_version", "bedrock-2023-05-31",
    "max_tokens", 4096,
    "system", "You are a SQL expert.",
    "messages", List.of(Map.of("role", "user", "content", query)),
    "temperature", 0.01
);
```

### Embedding (Titan)
```java
// Titan Embedding — 文字轉向量
Map<String, Object> body = Map.of("inputText", "monthly revenue");
// 回傳: {"embedding": [0.12, -0.34, ...]} (1536 維)
```

### 跟直接呼叫 Anthropic API 的差異
| | Claude API (直接) | AWS Bedrock |
|---|---|---|
| 認證 | API Key (`sk-ant-xxx`) | IAM Role (自動) |
| Endpoint | `api.anthropic.com` | AWS 內網 Endpoint |
| 資料安全 | 走公網 | VPC 內網 |
| 帳單 | Anthropic 帳單 | AWS 統一帳單 |
| 可選模型 | 只有 Claude | Claude + Titan + Llama + Mistral |
| 適用場景 | Side project | 企業產品 |

## OpenSearch 架構

### 三個向量索引
```
OpenSearch Cluster (AWS Managed)
├── uba (sql_index)
│   ├── vector_field: knn_vector (1536 dim, cosinesimil)
│   ├── text: keyword (自然語言問題)
│   ├── sql: keyword (對應 SQL)
│   └── profile: keyword (所屬 profile)
│
├── uba_ner (ner_index)
│   ├── vector_field: knn_vector (1536 dim)
│   ├── entity: keyword (實體名稱)
│   ├── comment: keyword (說明)
│   ├── entity_type: keyword (metrics/dimension)
│   ├── entity_count: integer
│   └── entity_table_info: nested (table_name, column_name, value)
│
└── uba_agent (agent_index)
    ├── vector_field: knn_vector (1536 dim)
    ├── query: keyword (複雜問題)
    └── comment: keyword (COT 拆解步驟)
```

### KNN 搜尋流程
```
用戶問題 "上個月營收"
    ↓ Titan Embedding
向量 [0.12, -0.34, ...]
    ↓ OpenSearch KNN (cosine similarity, top-5)
找到最相似的歷史 Q&A 範例
    ↓ 塞進 LLM prompt 作為 few-shot
Claude 參考範例生成更準確的 SQL
```

## 費用概估 (參考)

| 服務 | 計費方式 | 月估算 |
|------|---------|--------|
| Bedrock Claude Sonnet | Input $3/M tokens, Output $15/M tokens | 依查詢量而定 |
| Bedrock Titan Embedding | $0.1/M tokens | 很便宜，幾乎可忽略 |
| OpenSearch Service | 機器規格 + 儲存 | $50-200 |
| ECS Fargate | vCPU + 記憶體 × 時間 | $30-100 |
| RDS (MySQL/PostgreSQL) | 機器規格 | $15-100 |

## 本地開發 vs AWS 部署

| 環境 | Profile | Bedrock | OpenSearch | DB |
|------|---------|---------|------------|-----|
| **本地開發** | `dev` | MockLlmService | MockRetrievalService | 本地 MySQL (docker-compose) |
| **QAS 測試** | `qas` | 真實 Bedrock | 真實 OpenSearch | 測試 RDS |
| **Production** | `prod` | 真實 Bedrock | 真實 OpenSearch | 正式 RDS |

開發時用 `SPRING_PROFILES_ACTIVE=dev`，所有外部依賴都是 Mock，不需要 AWS 帳號。
