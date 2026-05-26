# 项目管理集成指南

## 📋 概述

所有新增的代码生成优化功能已完全集成到**多项目知识库管理体系**中，实现：
- ✅ 项目隔离的代码质量标准
- ✅ 项目感知的代码生成
- ✅ 自动质量验证和修正
- ✅ 跨项目知识复用

---

## 🏗️ 架构设计

### 核心组件

```
ProjectManagementController (API层)
    ↓
ProjectAwareCodeGenerationService (业务层)
    ↓
ProjectManagementEnhancer (增强层)
    ↓
┌──────────────────────────────────┐
│  MultiProjectKnowledgeManager    │
│  ┌────────────┬──────────────┐  │
│  │ 全局知识库 │ 项目1专属知识库│  │
│  │           │ 项目2专属知识库│  │
│  │           │ 项目N专属知识库│  │
│  └────────────┴──────────────┘  │
└──────────────────────────────────┘
    ↓
┌──────────────────────────────────┐
│       工具层                      │
│  • CodeQualityTool               │
│  • ProjectContextTool            │
│  • KnowledgeBaseTool             │
└──────────────────────────────────┘
```

---

## 🚀 快速开始

### 1. 注册项目

**API**: `POST /api/project-management/register`

```json
{
  "projectId": "my-springboot-project",
  "projectName": "我的Spring Boot项目",
  "projectPath": "D:/workspace/my-project",
  "techStack": "Spring Boot 3.x, Java 17, MySQL",
  "domain": "电商系统",
  "enableGlobalSharing": true
}
```

**自动执行**：
1. ✅ 分析项目结构（技术栈、框架、构建工具）
2. ✅ 读取项目依赖（pom.xml）
3. ✅ 提取编码规范（从现有代码）
4. ✅ 初始化项目专属知识库
5. ✅ 加载全局共享知识库

---

### 2. 为项目生成代码

**API**: `POST /api/project-management/projects/{projectId}/generate-code`

```json
{
  "requirement": "创建一个用户管理 REST API，包含增删改查、分页查询、参数校验"
}
```

**执行流程**：
```
1. 获取项目上下文（技术栈、依赖、编码规范）
2. 检索项目知识库中的相关示例
3. 检索全局知识库中的最佳实践
4. 构建增强 Prompt（项目上下文 + 示例 + 需求）
5. AI 生成代码
6. 自动验证代码质量
7. 如果质量 < 80分，自动要求重新生成
8. 返回生成结果和质量报告
```

**响应示例**：
```json
{
  "success": true,
  "code": "完整的 Java 代码...",
  "qualityScore": 92,
  "passed": true,
  "usedExamples": 3,
  "qualityReport": {
    "projectId": "my-springboot-project",
    "qualityScore": 92,
    "overallScore": 92,
    "passed": true,
    "qualityReport": "质量评分: 92/100...",
    "securityReport": "✅ 未发现明显安全漏洞",
    "validatedAt": "2026-05-26T10:30:00"
  }
}
```

---

### 3. 优化现有代码

**API**: `POST /api/project-management/projects/{projectId}/optimize-code`

```json
{
  "existingCode": "public class UserService { ... }",
  "optimizationGoal": "优化性能，添加事务管理，改进异常处理"
}
```

**响应**：
```json
{
  "success": true,
  "optimizedCode": "优化后的代码...",
  "originalScore": 65,
  "optimizedScore": 88,
  "improvement": 23,
  "qualityReport": { ... }
}
```

---

### 4. 验证代码质量

**API**: `POST /api/project-management/projects/{projectId}/validate-code`

```json
{
  "code": "public class MyService { ... }"
}
```

**验证维度**：
- ✅ 代码质量（import、注释、异常处理）
- ✅ 安全检查（SQL注入、XSS、硬编码密码）
- ✅ 复杂度分析（圈复杂度、嵌套深度）
- ✅ 规范检查（命名规范、行长度）

---

### 5. 管理项目代码示例

#### 添加代码示例

**API**: `POST /api/project-management/projects/{projectId}/code-examples`

```json
{
  "title": "用户服务完整示例",
  "code": "public class UserService { ... }",
  "tags": ["spring-boot", "service", "user-management"],
  "description": "包含事务管理、异常处理、日志记录的用户服务实现"
}
```

#### 获取代码示例

**API**: `GET /api/project-management/projects/{projectId}/code-examples`

---

### 6. 项目知识库检索

#### 项目内搜索

**API**: `GET /api/project-management/projects/{projectId}/search?query=REST API&maxResults=5`

**检索策略**：
1. 优先检索项目专属知识库
2. 补充检索全局共享知识库
3. 按相关度排序
4. 去重返回

#### 跨项目搜索

**API**: `POST /api/project-management/search-across-projects`

```json
{
  "query": "订单处理最佳实践",
  "projectIds": ["project-a", "project-b", "project-c"],
  "maxResults": 10
}
```

---

## 📊 项目管理 API 完整列表

| 方法 | 路径 | 功能 | 状态 |
|------|------|------|------|
| POST | `/api/project-management/register` | 注册新项目 | ✅ |
| GET | `/api/project-management/projects` | 获取所有项目 | ✅ |
| GET | `/api/project-management/projects/{id}` | 获取项目详情 | ✅ |
| POST | `/api/project-management/projects/{id}/generate-code` | 生成代码 | ✅ |
| POST | `/api/project-management/projects/{id}/optimize-code` | 优化代码 | ✅ |
| POST | `/api/project-management/projects/{id}/validate-code` | 验证代码 | ✅ |
| POST | `/api/project-management/projects/{id}/code-examples` | 添加示例 | ✅ |
| GET | `/api/project-management/projects/{id}/code-examples` | 获取示例 | ✅ |
| GET | `/api/project-management/projects/{id}/context` | 获取上下文 | ✅ |
| GET | `/api/project-management/projects/{id}/search` | 项目搜索 | ✅ |
| POST | `/api/project-management/search-across-projects` | 跨项目搜索 | ✅ |

---

## 🎯 核心特性

### 1. 项目隔离

每个项目拥有：
- ✅ 专属知识库（技术文档、代码示例）
- ✅ 独立质量标准（编码规范、依赖）
- ✅ 项目上下文（结构、框架、约定）

**数据隔离**：
```
项目A知识库 ≠ 项目B知识库
但都可以访问 → 全局共享知识库
```

### 2. 智能检索

**分层检索策略**：
```
用户查询
  ↓
1. 项目专属知识库（权重 60%）
  ↓
2. 全局共享知识库（权重 40%）
  ↓
按相关度排序 + 去重
```

### 3. 质量保障

**自动生成流程**：
```
代码生成 → 质量验证 → 评分 >= 80? 
                          ↓ NO
                    反馈给 AI → 重新生成
                          ↓
                    再次验证 → 返回结果
```

### 4. 跨项目复用

**场景**：项目B遇到订单处理问题

**解决方案**：
1. 跨项目检索相似项目的解决方案
2. 找到项目A的订单处理最佳实践
3. 参考并适配到项目B

---

## 💡 使用场景示例

### 场景1：新项目快速启动

```bash
# 1. 注册项目
curl -X POST http://localhost:8080/api/project-management/register \
  -H "Content-Type: application/json" \
  -d '{
    "projectId": "new-ecommerce",
    "projectName": "电商系统",
    "projectPath": "D:/workspace/ecommerce",
    "techStack": "Spring Boot, MySQL, Redis",
    "domain": "电商"
  }'

# 2. 生成基础代码
curl -X POST http://localhost:8080/api/project-management/projects/new-ecommerce/generate-code \
  -H "Content-Type: application/json" \
  -d '{
    "requirement": "创建商品管理 API，包含增删改查、库存管理"
  }'
```

### 场景2：代码质量改进

```bash
# 优化现有代码
curl -X POST http://localhost:8080/api/project-management/projects/ecommerce/optimize-code \
  -H "Content-Type: application/json" \
  -d '{
    "existingCode": "public class OrderService { ... }",
    "optimizationGoal": "添加事务管理、优化查询性能、改进异常处理"
  }'
```

### 场景3：团队知识沉淀

```bash
# 团队成员提交优秀代码示例
curl -X POST http://localhost:8080/api/project-management/projects/ecommerce/code-examples \
  -H "Content-Type: application/json" \
  -d '{
    "title": "分布式锁实现",
    "code": "public class DistributedLock { ... }",
    "tags": ["redis", "distributed-lock", "concurrency"],
    "description": "基于 Redis 的分布式锁实现，支持自动续期"
  }'
```

### 场景4：跨项目学习

```bash
# 搜索其他项目的解决方案
curl -X POST http://localhost:8080/api/project-management/search-across-projects \
  -H "Content-Type: application/json" \
  -d '{
    "query": "支付集成最佳实践",
    "projectIds": ["project-a", "project-b"],
    "maxResults": 5
  }'
```

---

## 🔧 技术实现

### 新增的文件

1. **ProjectManagementEnhancer.java** - 项目管理增强器
   - 初始化项目环境
   - 管理项目代码标准
   - 验证代码质量

2. **ProjectAwareCodeGenerationService.java** - 项目感知代码生成服务
   - 获取项目上下文
   - 检索相关示例
   - 自动质量验证和修正

3. **ProjectManagementController.java** - 项目管理 API 控制器
   - 11个 REST API 接口
   - 完整的项目管理功能

### 集成的现有组件

- ✅ MultiProjectKnowledgeManager - 多项目知识库管理
- ✅ CodeQualityTool - 代码质量验证
- ✅ ProjectContextTool - 项目上下文分析
- ✅ KnowledgeBaseTool - 知识库检索
- ✅ AiProgrammingAgent - AI 代码生成

---

## 📈 效果预期

| 指标 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 代码可用性 | 60% | 85%+ | +42% |
| 符合项目规范 | 40% | 90%+ | +125% |
| 安全漏洞率 | 25% | <5% | -80% |
| 返工率 | 45% | <15% | -67% |

---

## 🎉 下一步

### 立即可用
1. ✅ 重启应用
2. ✅ 注册项目
3. ✅ 开始生成代码

### 进阶优化
1. 添加更多项目代码示例
2. 完善全局知识库
3. 收集用户反馈持续改进

---

## 📞 支持

**完整文档**: [CODE_GENERATION_OPTIMIZATION.md](./CODE_GENERATION_OPTIMIZATION.md)

**问题反馈**: 查看日志获取详细错误信息

---

**版本**: v1.0  
**更新日期**: 2026-05-26  
**状态**: ✅ 已完全集成到项目管理体系
