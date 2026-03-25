# 智能翻译助手实现方案（文本翻译 / 文档翻译 / 实时对话 / 术语库 / 质量评估）

版本：1.0.0  
适用项目：translation-ai-agent  
目标读者：后端开发、前端开发、架构与运维

## 总体架构
- 分层设计：
  - 前端 Web（`templates/index.html`）+ WebSocket/SSE 实时交互
  - API 层（Spring Boot Controller）
  - 引擎适配层（阿里云 MT 及回退策略）
  - 异步任务层（文档翻译）
  - 数据层（关系型数据库 + MinIO 对象存储 + Redis 缓存）
  - AI智能层（LLM 增强、RAG 检索、质量评估 QE、个# Translation AI Agent

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.10-6DB33F)
![Spring AI](https://img.shields.io/badge/Spring_AI-1.0.0-blue)
![Status](https://img.shields.io/badge/Status-Active-success)

一个面向实际业务场景的 AI 翻译平台，覆盖文本翻译、文档翻译、实时对话翻译、术语库、质量评估、会员积分与支付能力，并引入 Agent 化翻译流程与流式交互体验。

这个项目不是单一的翻译接口封装，而是一个完整的多模块翻译系统原型，重点展示了 AI 能力接入、业务系统设计、文件处理链路、术语一致性控制，以及面向产品化的后端架构实现。

## 项目亮点

- 支持文本翻译、批量翻译、文档翻译、聊天翻译四类核心场景
- 接入 Spring AI、阿里云机器翻译、Qwen 能力，支持 AI 增强翻译流程
- 内置术语库、润色、质量评估、RAG/Agent 扩展能力
- 支持 SSE 流式输出，适合实时翻译和渐进式结果展示
- 覆盖会员、积分、订单、支付模拟等产品化模块
- 提供完整前端页面模板，便于直接演示和二次开发

## 功能概览

### 1. 文本翻译

- 普通文本翻译
- 批量文本翻译
- 语言检测
- 支持语言对校验
- 翻译引擎列表查询
- 译文润色与术语增强
- SSE 流式翻译输出

### 2. 文档翻译

- 文档上传、任务创建、启动翻译
- 进度查询、结果下载、原文下载
- 支持多种办公文档与 PDF 处理链路
- 集成对象存储，适合中大型文件翻译场景

### 3. 对话翻译

- 会话创建、更新、结束
- 消息发送与历史拉取
- SSE 流式消息翻译
- 自动回复与会话事件流

### 4. 翻译增强能力

- 术语库管理与统计
- 翻译质量评估
- 基于 Prompt 的润色能力
- Agent 任务、步骤、时间线与事件流
- Review 审核任务流转

### 5. 产品化能力

- 手机号注册登录
- 会员订阅
- 点数余额管理
- 下单、预支付、确认支付
- 支付宝/微信模拟支付页面

## 技术栈

- 后端：Java 21、Spring Boot 3.5.10
- Web：Spring MVC、Thymeleaf、WebSocket、SSE
- AI：Spring AI、阿里云机器翻译、Qwen
- 数据层：Spring Data JPA、MySQL
- 缓存与消息：Redis、RocketMQ
- 文件处理：Apache POI、PDFBox、MinIO
- 安全：JWT、拦截器鉴权

## 系统模块

- `TranslationController`：文本翻译、批量翻译、流式翻译、润色、语言检测
- `DocumentTranslationController`：文档上传、任务启动、进度查询、下载
- `ChatTranslationController`：聊天翻译、SSE 会话流、自动回复
- `TerminologyController`：术语库增删改查与统计
- `QualityController`：翻译质量评估
- `AgentTaskController` / `AgentController`：Agent 翻译任务、事件流、步骤追踪
- `AuthController` / `MembershipController` / `OrderPaymentController`：用户、会员、积分、支付链路

## 页面展示

项目已经包含较完整的演示页面，可直接用于 GitHub 项目说明或本地功能演示：

- 首页
- 文本翻译
- 批量翻译
- 文档翻译
- 术语库
- 实时聊天
- 历史记录
- 质量评估
- 统计分析
- 系统设置
- 登录、会员、订单、支付页

对应模板位于 `src/main/resources/templates`。

## 目录结构

```text
translation-ai-agent/
├─ src/main/java/cn/net/wanzni/ai/translation
│  ├─ controller
│  ├─ service
│  ├─ service/file
│  ├─ service/impl
│  ├─ service/impl/agent
│  ├─ service/llm
│  ├─ core/agent
│  ├─ repository
│  ├─ entity
│  ├─ dto
│  └─ config
├─ src/main/resources
│  ├─ templates
│  └─ static
├─ database
└─ docs
```

## 适合展示的能力点

- 从单次翻译到对话翻译、文档翻译的完整业务闭环
- 从 AI 能力接入到术语库、质量评估、审核流程的扩展设计
- 从后端接口到前端页面模板的全栈式工程组织
- 从翻译能力到会员、积分、支付的产品化落地思路

## 本地运行说明

由于仓库中未提交实际运行配置文件和敏感密钥，启动前需要自行补充本地配置。

### 建议准备

- JDK 21
- Maven 3.9+
- MySQL
- Redis
- MinIO
- 阿里云翻译相关密钥
- Qwen / Spring AI 相关配置

### 启动步骤

```bash
mvn -q -DskipTests compile
mvn -q -DskipTests spring-boot:run
```

启动后可从首页及各业务页面进入演示流程。

## 为什么这个项目值得展示

这个项目比较适合放在 GitHub 作为作品展示，因为它同时体现了：

- AI 能力集成经验
- Spring Boot 后端架构能力
- 多模块业务系统建模能力
- 文档处理与流式交互能力
- 从 Demo 到产品化方案的完整设计思路

## 后续可继续强化的方向

- 增加项目截图或功能录屏
- 补充部署文档与环境变量模板
- 增加 OpenAPI/Swagger 文档说明
- 增加 Docker Compose 一键启动方案
- 补充 GitHub Actions 持续集成配置

---

如果你希望，我下一步可以继续帮你把这份内容再改成更“简历作品集风格”或更“开源项目首页风格”的版本。性化/风格化）
- 引擎策略：优先阿里云翻译，失败或超限回退到本地 Mock 引擎；多引擎可扩展（百度/谷歌/微软/DeepL）。
- 通信协议：HTTP/REST（文本与术语库）；WebSocket/SSE（实时对话流式结果）；消息队列/任务调度（文档翻译）。
- 可观测性：统一日志、Trace ID、QPS/耗时/错误率、审计日志与风控（关键字、敏感内容）。

## AI智能能力与集成
- 大语言模型（LLM）增强：
  - 翻译后编辑（Post-Editing）：对 MT 结果进行语义润色、风格统一、歧义消解。
  - 指令化翻译：通过系统/用户 Prompt 注入领域风格（正式/口语/学术/法律）。
  - 术语一致性与偏好：结合术语库与用户偏好，对译文进行术语纠正与一致化建议。
- 检索增强生成（RAG）：
  - 检索源：翻译记忆（Translation Memory）、术语库、项目文档、历史对话。
  - 用途：为 LLM 提供可引用的上下文，提高领域一致性与事实性。
  - 技术选型（可选）：Elasticsearch/`pgvector`/Milvus 作为向量检索；通过嵌入模型构建索引。
- 质量评估（Quality Estimation, QE）：
  - 规则与模型结合：数字/单位/命名实体规则 + LLM 质量打分（流畅度/充分性/风格）。
  - 输出：`quality_score`、`confidence`、`errorTypes`、改进建议与警示。
- 自适应引擎路由：
  - 基于文本长度、语言对、历史质量与成本选择最优引擎（阿里云/备用）。
  - 支持 A/B 测试与联邦路由策略，按场景动态切换。
- 个性化与风格化：
  - 用户画像：偏好风格、术语优先、敏感词策略、黑白名单；
  - 细粒度 Prompt 模板：按项目/领域/语言对定制系统提示与约束。
- 安全与合规（AI 辅助）：
  - LLM 辅助敏感内容检测、语义水印、可疑模式识别；
  - 自动摘要与脱敏（PII）建议，遵从隐私法规。

### Spring AI 集成（可选模块）
- 说明：当前代码主路径以阿里云 MT 为核心，可选新增 Spring AI 模块以接入通用 LLM 能力（如 OpenAI、Qwen、Azure OpenAI 等）。
- 典型用法：
  - `ChatClient`/`StreamingChatClient` 进行后编辑、摘要、术语建议、质量评估；
  - 通过 `Prompt` 模板化系统指令，注入术语/风格/领域上下文；
  - 将术语库与历史记录作为 RAG 检索的知识源（向量存储配合 Spring Data/外部向量库）。
- 配置占位：
  - `application.yml -> spring.ai.*`（模型供应商、API Key、模型名称、超时、重试策略）。
  - 根据安全策略可采用代理网关和审计中间层。


## 技术栈与外部接口
- 后端：Spring Boot 3.5.x，Java 21，Spring Web，Spring Data JPA，Spring Cache，Spring WebSocket（内置 SimpleBroker）。
- 存储：
  - 开发：H2（`jdbc:h2:mem:testdb`，控制台 `/h2-console`）
  - 生产：MySQL / PostgreSQL（参考 `database/01_create_tables.sql`）
  - 对象存储：MinIO（`application.yml -> minio.endpoint`）
  - 缓存：Redis（用于短文本缓存与术语热点缓存）
- 文档处理：Apache Tika（格式探测）、POI/docx4j（Docx/PPTx/Xlsx）、PDFBox（PDF）
- 语言引擎：
  - 阿里云机器翻译 MT（`com.aliyun:alimt20181012` + `tea-openapi`）
  - 配置：`application.yml -> ai.aliyun.*`（`access-key-id`、`access-key-secret`、`region`、`endpoint`）
- 实时通信：WebSocket（STOMP）或 SSE；客户端增量显示。
- 可选 NLP：中文术语匹配与分词可接入 HanLP（提升术语识别与一致性）。

## 数据库表（核心）
以下基于 `database/01_create_tables.sql` 与 `database/table_relationships.md`：
- 文本与会话：
  - `translation_records`：翻译记录（文本/文档/聊天）；字段含语言对、引擎、耗时、质量分、字符数、状态等。
  - `chat_sessions` / `chat_messages`：实时对话会话与消息；含语言偏好、自动翻译标志、消息原文与译文、状态等。
- 文档：
  - `document_translations`：文档翻译任务与进度；含源/译文件路径、页段统计、引擎、质量分、错误信息等。
  - `file_storage`：文件存储元数据（与 MinIO 对应）。
- 术语库：
  - `terminology_entries` / `terminology_categories`：术语条目与分类；含语言对、领域、偏好译法、启用状态、使用频次等。
- 质量评估：
  - `quality_assessments`：翻译质量评估记录（与 `translation_records` 关联）。
- 系统与用户：
  - `users` / `user_settings`：用户与配置；
  - `supported_languages`、`translation_engines`、`system_configs`：系统支持语言、翻译引擎配置、系统参数。
  - `usage_statistics`、`operation_logs`、`system_notifications`、`user_notifications`：统计、操作日志与通知体系。

（具体列与索引详见 `database/01_create_tables.sql` 与 `database/03_create_indexes.sql`）

## 功能实现方案

### 1) 文本翻译
- 前端体验：支持源语言自动检测、语言对选择、引擎切换、质量评分展示、复制结果、历史记录与通知。
- 流程：
  1. `POST /api/translation/translate` 接收请求；若源语言为自动检测，先 `POST /api/translation/detect`。
  2. 校验语言对与引擎可用性；计算源文本指纹（缓存键）。
  3. 命中缓存则直接返回；否则调用阿里云 MT 引擎。
  4. 术语库前处理：基于语言对与领域匹配术语，进行预替换或引擎提示；
  5. 引擎返回后进行术语后处理与一致性校验；生成质量指标（`quality_score`、`confidence`、`terminologyCount`）。
  6. 写入 `translation_records` 与缓存；返回结果。
- 术语处理：
  - 精确匹配 / 忽略大小写 / 形态变化；可选分词（HanLP）提升中文术语命中率。
  - 使用频次回填至 `terminology_entries.usage_count`，驱动术语优化。
- 缓存策略：
  - Redis 键：`hash(sourceText) + sourceLanguage + targetLanguage + engine`；TTL 按长度与热度分级。
- 失败回退：
  - 引擎失败或超限时，回退到 Mock 引擎（可提示质量较低）；记录 `status=FAILED` 并返回友好错误。

- AI增强：
  - LLM 后编辑：依据术语库与风格 Prompt 对 MT 结果二次润色；
  - RAG 注入：从翻译记忆/术语库检索相似上下文，为 LLM 提供依据；
  - 动态引擎选择：结合历史质量分与成本，智能路由至最佳引擎；
  - 质量评估：LLM 输出质量点评与可执行改进建议。

### 2) 文档翻译
- 支持格式：`docx/pptx/xlsx/pdf/txt/md`；保留结构与样式。
- 流程：
  1. `POST /api/docs/translate` 上传或指定 MinIO `source_file_path`；创建 `document_translations` 任务（`status=PENDING`）。
  2. 异步执行器加载文档 → 分片（页/段）→ 并行调用翻译引擎 → 合并保真；
  3. 术语前后处理与质量评估，更新进度与状态；译件写入 MinIO（`translated_file_path`）。
  4. `GET /api/docs/job/{jobId}` 查询进度，支持取消/重试。
- 结构保真：
  - Docx：段落、标题、列表、表格；PPTx：文本框与布局；PDF：优先文本层，必要时 OCR（可选）。
- 大文件与并发：
  - 分片限流（引擎 QPS）、失败重试（指数退避）、去重（相同片段指纹）。

- AI增强：
  - 布局感知分片：结合版式分析与 LLM 提示避免错译表格/图注；
  - 长文风格统一：对跨段落译文进行全局风格一致性检查与修正；
  - 结构化摘要：生成文档摘要、重点术语与术语覆盖率报告；
  - OCR/图文理解（可选）：对 PDF 扫描件结合 OCR 与 LLM 校对文本。

### 3) 实时对话
- 通信：WebSocket（STOMP）或 SSE；服务端流式返回增量（句子/Token 级）。
- 流程：
  1. 建立会话：`WS /ws/chat` 或 `POST /api/chat/start`，创建 `chat_sessions`；
  2. 发送消息：`POST /api/chat/message` 或通过 WS；写入 `chat_messages`；
  3. 自动翻译：对文本消息进行实时翻译（短文本路径 + 术语处理），回传增量结果；
  4. 更新统计：消息序号、已读状态、会话消息计数与最后活跃时间。
- 上下文与偏好：
  - 会话级语言偏好、术语启用、风格参数；与引擎配置耦合。
- 断线与重连：心跳、重连、最近上下文保留与去重处理。

- AI增强：
  - 意图识别与自动语言切换；
  - 实时摘要与重点提取，便于跨语聊天记录梳理；
  - 内容安全：不良内容检测与提示；
  - 口语到书面风格转换与合规化建议。

### 4) 术语库
- 管理：
  - CRUD：`GET/POST/PUT/DELETE /api/terminology`；批量导入/导出（CSV/TBX）；分类与领域管理。
  - 审核与版本：条目启用、版本控制与生效范围（按项目/领域/语言对）。
- 翻译集成：
  - 前处理注入提示（Prefer 翻译）；后处理一致性校验与纠正；生成一致性报告。
- 数据：`terminology_entries`、`terminology_categories`；热点术语缓存（Redis）。

- AI增强：
  - 智能术语建议：基于历史文本挖掘候选术语，建议新增/归并；
  - 冲突检测：同词条多译名的场景下给出冲突提示与推荐；
  - 术语覆盖率度量：评估译文对术语的遵循度，输出改进清单。

### 5) 质量评估
- 指标：
  - `quality_score`（综合）、`confidence`（引擎置信度）、`terminologyConsistency`、`fluency/adequacy/style`（规则与统计）、`formatPreservation`、`errorTypes`。
- 流程：
  1. 引擎质量信号（若有）；
  2. 术语一致性检测与数字/单位/专有名词规则校验；
  3. 文档保真检查（样式与结构）；
  4. 回填 `quality_assessments` 与 `translation_records.quality_score`；生成报告。
- 输出：文本翻译返回简要评分与建议；文档翻译生成详细报告（问题清单、改进建议）。

- AI增强：
  - LLM 质量点评：结合上下文与术语库给出细粒度问题定位；
  - 自动重写建议：为问题句段生成替代译法；
  - 质量趋势与根因分析：按语言对/领域/项目维度输出趋势与可解释因子。

## API 设计（摘要）
- 文本：
  - `POST /api/translation/translate`（文本翻译）
  - `POST /api/translation/detect`（语言检测）
  - `GET /api/translation/history`（分页历史）
- 文档：
  - `POST /api/docs/translate`（提交文档任务）
  - `GET /api/docs/job/{jobId}`（查询进度）
  - `POST /api/docs/job/{jobId}/cancel`（取消任务）
- 对话：
  - `WS /ws/chat` 或 `SSE /stream/chat`（建立与消息流）
  - `POST /api/chat/start`、`POST /api/chat/message`、`GET /api/chat/sessions`
- 术语：
  - `GET/POST/PUT/DELETE /api/terminology`、`POST /api/terminology/import`、`GET /api/terminology/export`
- 质量：
  - `GET /api/translation/{id}/quality`、`POST /api/translation/{id}/quality/reassess`

- AI（可选扩展）：
  - `POST /api/ai/post-edit`（LLM 后编辑与风格化）
  - `POST /api/ai/summarize`（摘要与关键点提取）
  - `POST /api/ai/terminology/suggest`（术语候选建议与冲突检测）
  - `POST /api/ai/quality/assess`（LLM 质量评估与改写建议）

## 配置清单（关键）
- 阿里云翻译：
  - `application.yml -> ai.aliyun.access-key-id`
  - `application.yml -> ai.aliyun.access-key-secret`
  - `application.yml -> ai.aliyun.region`（如 `cn-hangzhou`）
  - `application.yml -> ai.aliyun.endpoint`（如 `mt.cn-hangzhou.aliyuncs.com`）
- MinIO：`application.yml -> minio.endpoint`、Access/Secret、Bucket；
- WebSocket：端点、心跳与消息队列缓冲参数；
- Redis：缓存 TTL、命名空间、最大对象限制；
- 数据库：驱动、连接池、DDL 对齐（参考 `database/*.sql`）。
 - Spring AI（可选）：`spring.ai.*`（模型、供应商、API Key、代理、超时、重试）

## 缓存与限流
- 文本翻译缓存：短文本命中缓存，键基于文本指纹 + 语言对 + 引擎；
- 文档分片去重：相同片段指纹复用结果；
- 限流：用户级与全局级 QPS 限制；异常退避重试。
 - AI调用配额：LLM 与向量检索服务的并发与速率限制；超限回退。

## 安全与合规
- 鉴权与授权：登录态或 API Key；角色与配额区分；
- 合规校验：敏感词、隐私遮蔽、导出审计与水印；
- 异常与补偿：幂等键、部分成功与补偿事务。

## 运维与监控
- 指标：QPS、平均耗时、错误率、缓存命中、分片进度；
- 日志：翻译调用链日志与审计日志；
- 告警：引擎失败率、队列堆积、文档任务卡滞。

## 测试与上线
- 单元测试：术语匹配、语言检测、分片合并、回退逻辑；
- 集成测试：引擎调用、缓存与数据库一致性、MinIO 读写；
- 压测：文本 QPS、文档并行分片、实时流延迟与抖动；
- 上线与回滚：灰度发布、配额限制、监控告警、熔断与回退策略。
 - AI专项：Prompt 回归测试、RAG 命中率评估、质量评估一致性对比、A/B 测试。

## 风险与回退
- 外部引擎不可用：自动回退至备用引擎或 Mock；
- 大文档资源耗尽：分片限流、暂停与重试；
- 术语误匹配：人工审核与版本回滚；

---
本文档为实现蓝图，落地时应结合当前代码结构（如 `AliyunTranslationService`、`TranslationServiceImpl`、`AliyunTranslationConfig`、`templates/index.html`）逐步实现与验证。数据库与索引请参考 `database` 目录下 SQL。
