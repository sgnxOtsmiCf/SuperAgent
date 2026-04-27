<h1 align="center">SuperAgent - 多智能体 AI 平台</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21">
  <img src="https://img.shields.io/badge/Vue-3.4-blue" alt="Vue 3">
  <img src="https://img.shields.io/badge/Spring%20AI-1.1.3-green" alt="Spring AI">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

<p align="center">
  <b>🎓 个人学习项目 | 基于 Spring AI 和 Spring Boot 构建的多智能体 AI 平台</b>
</p>

<p align="center">
  支持 ReAct 推理、工具调用、RAG 检索增强生成和 MCP 协议
</p>

<p align="center">
  <a href="#项目简介">项目简介</a> •
  <a href="#核心特性">核心特性</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#项目截图">项目截图</a> •
  <a href="#版本说明">版本说明</a> •
  <a href="#已知问题">已知问题</a> •
  <a href="#更新计划">更新计划</a>
</p>

---

## 目录

- [项目简介](#项目简介)
- [核心特性](#核心特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [安装步骤](#安装步骤)
  - [数据库初始化](#数据库初始化)
- [项目截图](#项目截图)
- [项目结构](#项目结构)
- [技能系统](#技能系统-skills)
- [API 文档](#api-文档)
- [配置说明](#配置说明)
- [版本说明](#版本说明)
  - [当前版本](#当前版本)
  - [核心优势](#核心优势)
- [已知问题](#已知问题)
- [更新计划](#更新计划)
- [许可证](#许可证)
- [联系方式](#联系方式)

---

## 项目简介

> 🎓 **本项目为个人学习项目**，旨在深入学习和实践 Spring AI、多智能体架构、RAG 等前沿技术。代码可能存在不足之处，欢迎交流探讨！

SuperAgent 是一个多智能体 AI 平台，采用前后端分离架构。后端基于 **Spring Boot 3.5 + Spring AI + Spring AI Alibaba** 构建，前端使用 **Vue 3 + Vite**（AI 辅助开发）。

该平台支持多种 AI 代理模式（ReAct、ToolCall），分别实现了两种框架即 **Spring AI + Spring AI Alibaba 组件** 和 **纯 Spring AI Alibaba** 的 Agent 完整逻辑实现，内置丰富的工具生态，支持多模型接入（通义千问、OpenAI、DeepSeek、智谱 AI、Ollama 本地模型等），并具备完整的用户认证、会话管理、RAG 知识库等功能。

**学习重点**：
- Spring AI 框架深入实践
- 多智能体架构设计与实现
- ReAct 推理模式应用
- RAG 检索增强生成
- MCP 协议集成
- 流式响应与 SSE 实时通信

---

## 核心特性

### 多智能体架构
- **ReAct Agent**: 支持推理-行动循环，让 AI 能够自主规划并调用工具解决问题
- **ToolCall Agent**: 直接工具调用模式，适用于明确的工具执行场景
- **SuperAgent**: 核心超级代理，支持动态模型路由、拦截器链、Hook 机制

### 丰富的工具生态
平台内置 20+ 种工具，分为三类（许多实现可能不够优雅和完善，欢迎 PR 改进）：

| 类别 | 工具 | 功能描述 |
|------|------|----------|
| **通用工具** | PlanningTool | 任务规划与分解 |
| | SensitiveFilterTool | 敏感词过滤 |
| | AskUserQuestionTool | 主动向用户提问 |
| **在线工具** | WebSearchTool | 网络搜索 |
| | TavilySearchTool | Tavily 智能搜索 |
| | Crawl4aiTool | 网页内容爬取 |
| | EmailTool | 邮件发送 |
| | TouTiaoNewsTool | 头条新闻获取 |
| | SmartWebFetchTool | 智能网页获取 |
| | DateTimeTool | 日期时间处理 |
| | MarkdownToPdfTool | Markdown 转 PDF |
| **本地工具** | BashTool | Bash 命令执行 |
| | BrowserUseTool | 浏览器自动化（Selenium） |
| | FileOperationTool | 文件操作 |
| | PDFGenerationTool | PDF 生成 |
| | SandboxTool | 沙箱环境 |
| | TerminalOperationTool | 终端操作 |
| **MCP 工具** | MCP Client | 支持 Model Context Protocol 协议 |

### 多模型支持
- **通义千问** (DashScope) - 默认推荐
- **OpenAI** (GPT-4/GPT-3.5)
- **DeepSeek**
- **智谱 AI** (GLM)
- **Ollama** (本地模型部署)

### RAG 检索增强生成
- 基于 PGVector 的向量数据库支持
- 文档加载与向量化存储
- 查询重写与上下文增强
- 支持 Markdown 文档知识库

### 企业级功能
- **用户认证**: 基于 Sa-Token 的权限认证体系
- **会话管理**: 支持多会话、会话归档、历史消息管理
- **聊天记忆**: 支持 Redis、数据库、文件多种存储方式
- **消息队列**: RabbitMQ 异步处理会话归档
- **定时任务**: 自动归档过期会话
- **文件存储**: MinIO 对象存储支持

### 智能拦截器与 Hook 机制
- **动态模型路由**: 根据任务类型自动选择最优模型
- **提示词注入**: 动态修改系统提示词
- **工具缓存**: 智能缓存工具调用结果
- **消息修剪**: 自动管理上下文窗口
- **响应验证**: 确保输出质量

---

## 技术栈

### 后端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.11 | 核心框架 |
| Spring AI | 1.1.3 | AI 开发框架 |
| Spring AI Alibaba | 1.1.2.0 | 阿里云 AI 生态 |
| Java | 21 | 编程语言 |
| MyBatis Plus | 3.5.15 | ORM 框架 |
| MySQL | 8.0+ | 关系型数据库 |
| PostgreSQL | 14+ | 向量数据库 (PGVector) |
| Redis | 7.0+ | 缓存与会话存储 |
| RabbitMQ | 3.12+ | 消息队列 |
| Redisson | 3.52.0 | Redis 客户端 |
| Sa-Token | 1.45.0 | 权限认证 |
| MinIO | 8.6.0 | 对象存储 |
| Selenium | 4.15.0 | 浏览器自动化 |

### 前端技术栈
| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.21 | 前端框架 |
| Vite | 5.1.6 | 构建工具 |
| Element Plus | 2.6.1 | UI 组件库 |
| Pinia | 2.1.7 | 状态管理 |
| Marked | 12.0.1 | Markdown 渲染 |
| KaTeX | 0.16.45 | 数学公式渲染 |

> 前端采用 AI 辅助开发模式快速构建

---

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.9+
- MySQL 8.0+
- PostgreSQL 14+ (带 pgvector 扩展)
- Redis 7.0+
- RabbitMQ 3.12+
- Node.js 18+ (前端)

### 安装步骤

#### 1. 克隆项目
```bash
git clone https://github.com/yourusername/SuperAgent.git
cd SuperAgent
```

#### 2. 数据库初始化

**创建数据库：**
```sql
-- 创建 MySQL 数据库
create database superagent default character set utf8mb4 collate utf8mb4_unicode_ci;

-- 创建 PostgreSQL 向量数据库 (用于 RAG)
CREATE EXTENSION IF NOT EXISTS vector;
```

**执行 SQL 脚本：**
```bash
# 使用 MySQL 命令行或客户端工具执行
mysql -u root -p superagent < sql/db.sql
```

> 📄 SQL 脚本位置：[./sql/db.sql](./sql/db.sql)
> 
> 该脚本包含：
> - 用户表、角色表、权限表
> - 会话表、消息表
> - 基础角色数据（管理员、访客、普通用户、会员）
> - 权限配置数据

#### 3. 配置文件
复制 `application-dev.yml` 模板并配置：
```bash
cp src/main/resources/application.yml src/main/resources/application-dev.yml
```

编辑 `application-dev.yml`，配置以下关键信息：
- MySQL 数据库连接
- PostgreSQL 向量数据库连接
- Redis 连接
- RabbitMQ 连接
- AI 模型 API Key（DashScope/OpenAI/DeepSeek 等）
- MinIO 对象存储

#### 4. 启动后端
```bash
./mvnw spring-boot:run
```

或打包后运行：
```bash
./mvnw clean package -DskipTests
java -jar target/SuperAgent-0.0.1-SNAPSHOT.jar
```

#### 5. 启动前端
```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 即可使用。

---

## 项目截图

<p align="center">
  <img src="./assests/login.png" alt="登录页面" width="80%">
  <br>
  <em>登录页面</em>
</p>

<p align="center">
  <img src="./assests/main.png" alt="主界面" width="80%">
  <br>
  <em>主界面 - AI 对话</em>
</p>

<p align="center">
  <img src="./assests/menus.png" alt="功能菜单" width="80%">
  <br>
  <em>功能菜单</em>
</p>

<p align="center">
  <img src="./assests/tools.png" alt="工具展示" width="80%">
  <br>
  <em>工具调用展示</em>
</p>

<p align="center">
  <img src="./assests/skills.png" alt="技能系统" width="80%">
  <br>
  <em>技能系统</em>
</p>

<p align="center">
  <img src="./assests/user_profiles.png" alt="用户画像" width="80%">
  <br>
  <em>用户画像管理</em>
</p>

<p align="center">
  <img src="./assests/archive.png" alt="会话归档" width="80%">
  <br>
  <em>会话归档功能</em>
</p>

---

## 项目结构

```
SuperAgent/
├── frontend/                  # 前端项目 (Vue 3 + Vite)
│   ├── src/
│   │   ├── api/              # API 接口
│   │   ├── components/       # 组件
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── views/            # 页面视图
│   │   └── App.vue
│   └── package.json
│
├── src/main/java/cn/sgnxotsmicf/
│   ├── SuperAgentApplication.java    # 启动类
│   ├── app/                         # 应用层 - 智能体实现
│   │   ├── manus/                   # Manus 代理实现 (Spring AI + Spring AI Alibaba 组件)
│   │   │   ├── BaseAgent.java
│   │   │   ├── ReActAgent.java
│   │   │   ├── ToolCallAgent.java
│   │   │   ├── Manus.java
│   │   │   ├── AgentMessage.java
│   │   │   └── model/               # 模型类
│   │   │       ├── AgentContext.java
│   │   │       └── AgentState.java
│   │   ├── superagent/              # SuperAgent 核心 (纯 Spring AI Alibaba 构建)
│   │   │   ├── SuperAgent.java
│   │   │   ├── SuperAgentFactory.java
│   │   │   ├── StreamingHandler.java
│   │   │   ├── hook/                # Hook 机制
│   │   │   │   ├── HookRegistry.java
│   │   │   │   ├── log/             # 日志 Hook
│   │   │   │   ├── message/         # 消息 Hook
│   │   │   │   └── model/           # 模型 Hook
│   │   │   └── interceptor/         # 拦截器
│   │   │       ├── InterceptorRegistry.java
│   │   │       ├── DynamicPromptInterceptor.java
│   │   │       ├── ModelLoggingInterceptor.java
│   │   │       ├── ToolCacheInterceptor.java
│   │   │       ├── ToolMonitoringInterceptor.java
│   │   │       └── router/          # 动态路由
│   │   │           ├── DynamicModelRouter.java
│   │   │           ├── TaskProfileAnalyzer.java
│   │   │           └── ...
│   │   └── family/                  # 家庭和谐应用示例
│   │       └── FamilyHarmony.java
│   ├── agentTool/                   # 工具注册与管理
│   │   ├── ToolRegistry.java
│   │   ├── config/                  # 工具配置
│   │   ├── commonTool/              # 通用工具
│   │   │   ├── PlanningTool.java
│   │   │   ├── SensitiveFilterTool.java
│   │   │   └── AskUserQuestionTool.java
│   │   ├── localtool/               # 本地工具
│   │   │   ├── BashTool.java
│   │   │   ├── BrowserUseTool.java
│   │   │   ├── FileOperationTool.java
│   │   │   ├── PDFGenerationTool.java
│   │   │   ├── SandboxTool.java
│   │   │   ├── TerminalOperationTool.java
│   │   │   └── ResourceDownloadTool.java
│   │   ├── onlinetool/              # 在线工具
│   │   │   ├── WebSearchTool.java
│   │   │   ├── TavilySearchTool.java
│   │   │   ├── Crawl4aiTool.java
│   │   │   ├── EmailTool.java
│   │   │   ├── TouTiaoNewsTool.java
│   │   │   ├── SmartWebFetchTool.java
│   │   │   ├── DateTimeTool.java
│   │   │   ├── MarkdownToPdfTool.java
│   │   │   ├── OnlineDocumentTool.java
│   │   │   └── ImageSearchTool.java
│   │   └── specialTool/             # 特殊工具
│   │       └── TerminateTool.java
│   ├── advisor/                     # Spring AI Advisor
│   │   ├── AdvisorRegister.java
│   │   ├── AgentLogAdvisor.java
│   │   ├── ReActProtocolAdvisor.java
│   │   ├── SearchToolAdvisor.java
│   │   ├── ReReadingAdvisor.java
│   │   └── ...
│   ├── chatMemory/                  # 聊天记忆存储
│   │   ├── CustomRedissonRedisChatMemoryRepository.java
│   │   ├── MySqlChatMemoryRepository.java
│   │   ├── FileBasedMemory.java
│   │   ├── RedissonStore.java
│   │   ├── JdbcChatMemoryFactory.java
│   │   └── NoSqlChatMemoryFactory.java
│   ├── controller/                  # REST API 控制器层
│   │   ├── SuperAgentController.java
│   │   ├── ManusController.java
│   │   ├── FamilyHarmonyController.java
│   │   ├── UserController.java
│   │   ├── UserProfileController.java
│   │   ├── SessionController.java
│   │   ├── MessageController.java
│   │   ├── FunctionController.java
│   │   ├── CaptchaController.java
│   │   └── VersionController.java
│   ├── service/                     # 业务逻辑层
│   │   ├── SuperAgentService.java
│   │   ├── OpenManusService.java
│   │   ├── UserService.java
│   │   ├── ChatSessionService.java
│   │   ├── ChatMessageService.java
│   │   ├── FunctionService.java
│   │   ├── CaptchaService.java
│   │   ├── SessionArchiveService.java
│   │   ├── impl/                    # 实现类
│   │   │   ├── SuperAgentServiceImpl.java
│   │   │   ├── OpenManusServiceImpl.java
│   │   │   ├── UserServiceImpl.java
│   │   │   ├── ChatSessionServiceImpl.java
│   │   │   ├── ChatMessageServiceImpl.java
│   │   │   ├── FunctionServiceImpl.java
│   │   │   ├── CaptchaServiceImpl.java
│   │   │   ├── SessionArchiveServiceImpl.java
│   │   │   └── StpInterfaceImpl.java
│   │   └── strategy/                # 策略模式
│   │       ├── ChatSessionStrategy.java
│   │       ├── ChatSessionStrategyFactory.java
│   │       ├── ChatSessionContext.java
│   │       └── strategyImpl/
│   ├── dao/                         # 数据访问层 (DAO/Mapper)
│   │   ├── ChatSessionMapper.java
│   │   ├── ChatMessageMapper.java
│   │   ├── UserMapper.java
│   │   ├── RoleMapper.java
│   │   ├── PermissionMapper.java
│   │   ├── RolePermissionMapper.java
│   │   └── UserRoleMapper.java
│   ├── common/                      # 公共模块
│   │   ├── po/                      # 持久化对象 (Entity)
│   │   │   ├── User.java
│   │   │   ├── ChatSession.java
│   │   │   ├── ChatMessage.java
│   │   │   ├── Role.java
│   │   │   ├── Permission.java
│   │   │   ├── UserRole.java
│   │   │   ├── RolePermission.java
│   │   │   └── BaseEntity.java
│   │   ├── vo/                      # 视图对象 (View Object)
│   │   │   ├── UserVo.java
│   │   │   ├── ChatSessionVo.java
│   │   │   ├── ChatMessageVo.java
│   │   │   ├── UserProfileVo.java
│   │   │   ├── ProfileItemVo.java
│   │   │   ├── ChatRequest.java
│   │   │   └── ToolVo.java
│   │   ├── dto/                     # 数据传输对象 (DTO)
│   │   │   ├── ChatSessionDTO.java
│   │   │   ├── CaptchaDTO.java
│   │   │   └── ArchiveMessage.java
│   │   ├── result/                  # 统一响应结果
│   │   │   ├── Result.java
│   │   │   └── ResultCodeEnum.java
│   │   ├── tools/                   # 工具类
│   │   │   ├── AgentCommon.java
│   │   │   ├── ChatUserIdBinder.java
│   │   │   ├── MarkdownParser.java
│   │   │   ├── MinioUtil.java
│   │   │   ├── NickNameGenerator.java
│   │   │   ├── ServiceUtil.java
│   │   │   ├── SessionIdUtil.java
│   │   │   ├── TimeConverter.java
│   │   │   └── email/               # 邮件工具
│   │   ├── auth/                    # 权限常量
│   │   │   ├── RoleConstant.java
│   │   │   └── PermissionConstant.java
│   │   ├── version/                 # 版本信息
│   │   │   ├── SuperAgentVersion.java
│   │   │   ├── SuperAgentAdvantage.java
│   │   │   ├── SuperAgentDeficiency.java
│   │   │   └── SuperAgentDetail.java
│   │   └── rabbitmq/                # RabbitMQ 消息队列
│   │       ├── config/
│   │       ├── constant/
│   │       ├── consumer/
│   │       ├── entity/
│   │       └── service/
│   ├── config/                      # 配置类
│   │   ├── AiModelConfig.java
│   │   ├── MyBatisPlusConfig.java
│   │   ├── RedissonConfig.java
│   │   ├── MinioConfig.java
│   │   ├── RagConfig.java
│   │   ├── EmbeddingConfig.java
│   │   ├── DataSourceConfig.java
│   │   ├── JacksonConfig.java
│   │   ├── CorsConfig.java
│   │   ├── SaTokenConfigure.java
│   │   └── SmsConfig.java
│   ├── rag/                         # RAG 相关配置
│   │   ├── DocumentLoader.java
│   │   ├── PGVectorConfig.java
│   │   ├── SimpleVectorStoreConfig.java
│   │   ├── TokenTextSplitterConfig.java
│   │   ├── CompressionQueryTransformerFactory.java
│   │   ├── RewriteQueryTransformerFactory.java
│   │   ├── TranslationQueryTransformerFactory.java
│   │   ├── ContextualQueryAugmenterFactory.java
│   │   └── ...
│   ├── exception/                   # 异常处理
│   │   ├── AgentException.java
│   │   └── GlobalExceptionHandler.java
│   └── job/                         # 定时任务
│       └── SessionExpireArchiveJob.java
│
├── src/main/resources/
│   ├── mapper/                      # MyBatis XML 映射文件
│   ├── fonts/                       # 字体资源
│   ├── application.yml              # 主配置文件
│   ├── application-prod.yml         # 生产环境配置
│   └── mcp-servers.json             # MCP 服务器配置
│
├── skills/                          # 技能定义目录
│   ├── SuperAgent/                  # SuperAgent 技能
│   ├── FamilyHarmony/               # 家庭和谐技能
│   └── skills/                      # 其他技能
│
├── rag/                             # RAG 知识库文档
│   └── *.md
│
├── sql/                             # 数据库初始化脚本
│   └── db.sql
│
├── assests/                         # 项目截图
│   └── *.png
│
└── pom.xml                          # Maven 配置
```

---

## 技能系统 (Skills)

SuperAgent 支持通过 `SKILL.md` 文件定义技能，实现可插拔的 AI 能力：

```markdown
---
name: java-coding-standards
description: Java 编码规范检查与建议
---

# Java 编码规范

## 命名规范
- 类名使用大驼峰命名法
- 方法名使用小驼峰命名法
...
```

内置技能：
- `java-coding-standards` - Java 编码规范
- `java-design-standards` - Java 设计规范
- `java-security-standards` - Java 安全规范
- `time-aware-search` - 时间感知搜索
- `caveman` - 极简通信模式

---

## API 文档

启动后访问 Swagger UI：
```
http://localhost:8123/swagger-ui.html
```

主要接口：
- `POST /api/super-agent/chat` - 流式对话
- `POST /api/manus/chat` - Manus 代理对话
- `GET /api/session/list` - 获取会话列表
- `POST /api/user/login` - 用户登录

---

## 配置说明

### AI 模型配置 (application-dev.yml)
```yaml
spring:
  ai:
    dashscope:
      api-key: your-dashscope-api-key
    openai:
      api-key: your-openai-api-key
    deepseek:
      api-key: your-deepseek-api-key
```

### MCP 服务器配置 (mcp-servers.json)
```json
{
  "mcpServers": {
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path/to/files"]
    }
  }
}
```

---

## 版本说明

### 当前版本

| 属性 | 内容 |
|------|------|
| **版本号** | v0.10 |
| **作者** | sgnxotsmicf |
| **性质** | 🎓 个人学习项目 |
| **描述** | 多智能体平台服务项目 |
| **状态** | 🚧 持续学习开发中 |

### 核心优势

> 以下优势定义来自代码中的 [SuperAgentAdvantage.java](./src/main/java/cn/sgnxotsmicf/common/version/SuperAgentAdvantage.java)

| 特性 | 说明 |
|------|------|
| **多 Agent 架构支持** | 支持 SuperAgent、Manus、Family 等多个智能体，可根据不同场景灵活切换 |
| **流式响应体验** | 采用 SSE 流式传输，实时展示 AI 思考过程、工具调用和响应内容，用户体验流畅 |
| **完整的记忆系统** | 基于 Redis 的分布式会话存储，支持消息持久化、用户画像、历史会话回溯和归档管理 |
| **工具链生态丰富** | 内置多种工具，支持工具自动调用和结果展示 |
| **动态提示词拦截** | 支持通过拦截器动态修改系统提示词，实现个性化 Agent 行为定制 |
| **Hook 机制扩展** | 提供完整的 Hook 注册机制，支持在 Agent 生命周期各阶段插入自定义逻辑 |
| **前端交互友好** | 现代化的 Vue3 界面，支持 Markdown 渲染、代码高亮、消息操作等丰富交互 |
| **功能完备** | 支持用户认证、会话置顶、消息分享、文件上传等企业级功能 |

### 版本信息源码

- 版本定义: [SuperAgentVersion.java](./src/main/java/cn/sgnxotsmicf/common/version/SuperAgentVersion.java)
- 优势定义: [SuperAgentAdvantage.java](./src/main/java/cn/sgnxotsmicf/common/version/SuperAgentAdvantage.java)
- 缺陷定义: [SuperAgentDeficiency.java](./src/main/java/cn/sgnxotsmicf/common/version/SuperAgentDeficiency.java)
- 详细信息: [SuperAgentDetail.java](./src/main/java/cn/sgnxotsmicf/common/version/SuperAgentDetail.java)

---

## 已知问题 ⚠️

> **坦诚说明**：当前版本处于早期开发阶段，存在以下已知问题和不足。这些问题来自代码中的 [SuperAgentDeficiency.java](./src/main/java/cn/sgnxotsmicf/common/version/SuperAgentDeficiency.java) 定义，会逐步修复完善。

### 核心缺陷

| 问题 | 说明 | 优先级 |
|------|------|--------|
| **流式输出推理缺陷** | 无法将推理内容保存到 memory 中，可能是框架层面的限制 | 🔴 高 |
| **流式输出工具缺陷** | 有时候明明调用并执行了工具，但是却没有展示出来，memory 中也没有存储 | 🔴 高 |
| **推理阻塞** | 推理时可能阻塞工具执行显示（实际已执行），导致工具存储失败 | 🔴 高 |

### 体验问题

| 问题 | 说明 | 优先级 |
|------|------|--------|
| **前端 Markdown 渲染不完全** | 前端知识储备有限，部分复杂 Markdown 渲染效果有待优化 | 🟡 中 |
| **Sa-Token 流式输入异常** | 在流式输出中使用 sa-token 会触发上下文未初始化异常，暂未解决，仅做拦截处理 | 🟡 中 |

### 功能缺失

| 问题 | 说明 | 优先级 |
|------|------|--------|
| **更多细节不足** | 包括提示词优化、异常处理等各种细节都有待提高，目前只是搭建了基础框架 | 🟢 低 |
| **功能缺失** | 包括自定义温度、模型选择等功能都还没有设计实现 | 🟢 低 |

> 💡 **欢迎贡献**：如果你有兴趣解决上述问题，欢迎提交 PR！详细缺陷定义见 [SuperAgentDeficiency.java](./src/main/java/cn/sgnxotsmicf/common/version/SuperAgentDeficiency.java)

---

## 更新计划

> 🎓 **持续学习，持续进步**：本项目作为学习项目，将跟随技术发展和个人成长不断更新完善。

计划中的学习方向和功能包括：

- [ ] 修复流式输出和工具调用的稳定性问题
- [ ] 完善前端 Markdown 渲染和交互体验
- [ ] 完善Spring AI Alibaba不兼容的Message级别管理
- [ ] 优化提示词及模型调用完整链路
- [x] 解决Sa-Token的流式输入异常问题
- [ ] 支持用户自定义模型参数（温度、top_p、top_k 等）
- [ ] 增加更多内置工具和技能
- [ ] 优化 RAG 检索效果
- [ ] 支持更多 AI 模型接入
- [ ] 完善单元测试和文档
- [ ] 提供 Docker 一键部署方案

**长期目标**: 打造一个功能完善、稳定可靠、易于扩展的开源多智能体 AI 平台。

---

## 许可证

本项目采用 [MIT](LICENSE) 许可证开源。

---

## 联系方式

如有问题或建议，欢迎通过以下方式联系：
- 提交 GitHub Issue
- 发送邮件至: lixiangzhenshuaiqi@163.com

---

<p align="center">
  <b>如果这个项目对你有帮助，请给个 Star ⭐</b>
</p>

<p align="center">
  <b>持续学习，持续更新 🚀</b>
</p>
