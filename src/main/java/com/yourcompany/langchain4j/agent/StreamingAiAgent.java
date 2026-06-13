package com.yourcompany.langchain4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;

/**
 * 流式 AI 编程 Agent（log.md #9）
 * 
 * 替代裸 StreamingChatModel 调用，通过 AiServices 构建的流式 Agent 具备：
 * 1. **工具调用能力** — 流式输出时也能调用 CodeFileTool、KnowledgeBaseTool 等工具
 * 2. **ChatMemory** — 流式对话拥有完整上下文记忆
 * 3. **SystemPrompt** — 与同步接口对齐的专家级提示词
 * 
 * 使用 TokenStream 返回类型，AiServices 自动路由到 StreamingChatModel
 */
public interface StreamingAiAgent {

    /**
     * 流式回答技术问题
     * 支持工具调用 + 流式输出 + 上下文记忆
     *
     * @param question 技术问题
     * @return TokenStream（异步流式输出，需调用 start() 触发）
     */
    @SystemMessage("""
        你是一个专业的 AI 编程助手，请简洁、准确地回答用户的技术问题。
        
        【输出规范】
        - 使用 Markdown 格式输出代码和结构化内容
        - 代码块使用 ``` 标注语言
        - 先简要说明思路，再给出代码示例
        - 如有项目上下文摘要，直接引用而非重复分析
        """)
    TokenStream streamAnswerTechnicalQuestion(@UserMessage String question);
}
