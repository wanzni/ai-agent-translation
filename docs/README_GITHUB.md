# Translation AI Agent

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

---

