三、具体可落地的优化方案
#
优化项
维度
预期效果
1
按意图动态挂载工具：简单问答只挂 KnowledgeBaseTool，代码生成才挂全量工具
Token
每次省 1500-2000 tokens
2
工具返回值截断：readFile 限制最大行数（如 200 行），listFilesRecursively 限制深度和数量
Token
防止单次工具调用吃掉 5000+ tokens
3
接入 ContextOptimizer：用 TokenWindowChatMemory 替代 MessageWindowChatMemory
两者
记忆管理从"数量驱动"变为"预算驱动"
4
项目缓存增加代码签名：缓存核心类/接口的方法签名和 1-2 个代码风格样本
准确性
AI 生成的代码与项目风格一致
5
精简 SystemPrompt：移除工具用法说明（工具定义已有），拆分为通用基础 + 按任务追加
Token
SystemPrompt 减少 40-50%
6
SSE 流式接口对齐能力：注入项目上下文 + PromptGuard
准确性
流式输出也能感知项目
7
编排器修正循环优化：只发送问题点 + 受影响代码段，而非全量代码
Token
修正轮次 Token 减少 60%+
8
Few-Shot 注入：从知识库中检索 1-2 个相似代码示例注入 Prompt
准确性
显著提高代码风格一致性
9
流式 + 工具调用：考虑用 LangChain4j 的 StreamingAiService 替代裸 StreamingChatModel
两者
流式接口具备完整 Agent 能力