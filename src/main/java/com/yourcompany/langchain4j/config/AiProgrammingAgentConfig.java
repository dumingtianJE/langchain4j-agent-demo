package com.yourcompany.langchain4j.config;

import com.yourcompany.langchain4j.agent.AiProgrammingAgent;
import com.yourcompany.langchain4j.supervisor.AiSupervisor;
import com.yourcompany.langchain4j.tool.CodeAnalysisTool;
import com.yourcompany.langchain4j.tool.CodeFileTool;
import com.yourcompany.langchain4j.tool.KnowledgeBaseTool;
import com.yourcompany.langchain4j.tool.CodeQualityTool;
import com.yourcompany.langchain4j.tool.ProjectContextTool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 编程 Agent 配置
 * 注意：embeddingModel 和 embeddingStore 已移至 EmbeddingStoreConfig
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AiProgrammingAgentConfig {
    
    private final ChatModel chatModel;
    private final CodeFileTool codeFileTool;
    private final CodeAnalysisTool codeAnalysisTool;
    private final KnowledgeBaseTool knowledgeBaseTool;
    private final CodeQualityTool codeQualityTool;
    private final ProjectContextTool projectContextTool;
    private final AiSupervisor aiSupervisor;
    
    /**
     * 配置 Chat Memory（对话记忆）
     * 使用消息窗口记忆，保留最近的消息
     */
    @Bean
    public ChatMemory chatMemory() {
        log.info("初始化 Chat Memory (最多 20 条消息)");
        return MessageWindowChatMemory.withMaxMessages(20);
    }
    
    /**
     * 配置 AI 编程 Agent
     * 集成所有工具和能力
     */
    @Bean
    public AiProgrammingAgent aiProgrammingAgent(ChatMemory chatMemory) {
        log.info("初始化 AI 编程 Agent");
        
        return dev.langchain4j.service.AiServices.builder(AiProgrammingAgent.class)
            .chatModel(chatModel)
            .chatMemory(chatMemory)
            .tools(codeFileTool, codeAnalysisTool, knowledgeBaseTool, 
                   codeQualityTool, projectContextTool)
            .build();
    }
}
