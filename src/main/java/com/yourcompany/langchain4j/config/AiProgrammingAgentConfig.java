package com.yourcompany.langchain4j.config;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.agent.LightweightQaAgent;
import com.yourcompany.langchain4j.agent.StreamingAiAgent;
import com.yourcompany.langchain4j.supervisor.AiSupervisor;
import com.yourcompany.langchain4j.tool.CodeAnalysisTool;
import com.yourcompany.langchain4j.tool.CodeFileTool;
import com.yourcompany.langchain4j.tool.KnowledgeBaseTool;
import com.yourcompany.langchain4j.tool.CodeQualityTool;
import com.yourcompany.langchain4j.tool.CodeWriteTool;
import com.yourcompany.langchain4j.tool.GitIntegrationTool;
import com.yourcompany.langchain4j.tool.ProjectContextTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 编程 Agent 配置
 * 
 * 三种 Agent 变体：
 * - 完整 Agent：挂载全部 7 个工具类，用于代码生成/审查（chat 同步接口 + 编排器代码生成步骤）
 * - 轻量 Agent：仅挂载 CodeFileTool + KnowledgeBaseTool，用于分析/问答/文档（编排器其他步骤）
 * - 流式 Agent（#9）：挂载文件+知识库工具，返回 TokenStream，流式输出同时具备工具调用能力
 * 
 * 优化（#3）：所有 Agent 使用 TokenWindowChatMemory 保持预算驱动
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiProgrammingAgentConfig {
    
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;
    private final CodeFileTool codeFileTool;
    private final CodeAnalysisTool codeAnalysisTool;
    private final KnowledgeBaseTool knowledgeBaseTool;
    private final CodeQualityTool codeQualityTool;
    private final CodeWriteTool codeWriteTool;
    private final GitIntegrationTool gitIntegrationTool;
    private final ProjectContextTool projectContextTool;
    private final AiSupervisor aiSupervisor;
    
    /**
     * 完整 Agent — 挂载全部工具（供 chat 同步接口使用）
     */
    @Bean
    public AiProgrammingAgent aiProgrammingAgent(ChatMemory chatMemory) {
        log.info("初始化完整 AI 编程 Agent（全部工具）");
        
        return dev.langchain4j.service.AiServices.builder(AiProgrammingAgent.class)
            .chatModel(chatModel)
            .chatMemory(chatMemory)
            .tools(codeFileTool, codeAnalysisTool, knowledgeBaseTool, 
                   codeQualityTool, codeWriteTool, gitIntegrationTool, projectContextTool)
            .build();
    }

    /**
     * 轻量级 Q&A Agent — 仅挂载文件操作 + 知识库工具
     * 适用于编排器中的技术分析、智能问答、文档生成步骤
     * 每次请求省去 ~20 个工具方法的定义开销
     * 
     * 优化（#3）：使用 TokenWindowChatMemory(4000) 替代 MessageWindowChatMemory(10)
     * 轻量场景 Token 预算 4000 即可
     */
    @Bean
    public LightweightQaAgent lightweightQaAgent(TokenCountEstimator tokenCountEstimator) {
        log.info("初始化轻量级 Q&A Agent（仅文件 + 知识库工具，Token 预算驱动记忆）");

        return dev.langchain4j.service.AiServices.builder(LightweightQaAgent.class)
            .chatModel(chatModel)
            .chatMemory(TokenWindowChatMemory.builder()
                    .maxTokens(4000, tokenCountEstimator)
                    .build())
            .tools(codeFileTool, knowledgeBaseTool)
            .build();
    }

    /**
     * 流式 AI Agent（log.md #9）
     * 替代裸 StreamingChatModel 调用，流式输出同时具备：
     * - 工具调用能力（文件读取、知识库检索）
     * - TokenWindowChatMemory 上下文记忆
     * - 专家级 SystemPrompt
     * 
     * 挂载文件+知识库工具（流式场景主要面向问答，不需要代码写入/编译等重型工具）
     */
    @Bean
    public StreamingAiAgent streamingAiAgent(TokenCountEstimator tokenCountEstimator) {
        log.info("初始化流式 AI Agent（文件 + 知识库工具，支持工具调用 + 流式输出）");

        return dev.langchain4j.service.AiServices.builder(StreamingAiAgent.class)
            .chatModel(chatModel)
            .streamingChatModel(streamingChatModel)
            .chatMemory(TokenWindowChatMemory.builder()
                    .maxTokens(6000, tokenCountEstimator)
                    .build())
            .tools(codeFileTool, knowledgeBaseTool)
            .build();
    }
}
