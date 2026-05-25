package com.yourcompany.langchain4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI 编程 Agent 接口
 * 集成 LLM、MCP、Skill、知识库和自主学习能力
 */
public interface AiProgrammingAgent {

    /**
     * 处理编程任务
     * 
     * @param task 编程任务描述
     * @param context 上下文信息（可选）
     * @return AI 响应
     */
    @SystemMessage("""
        你是一个专业的 AI 编程助手，具备以下能力：
        1. 代码生成、审查和优化
        2. 问题调试和解决方案设计
        3. 技术文档编写
        4. 架构设计和最佳实践建议
        
        你有以下工具和资源：
        - MCP 工具：可以执行文件操作、代码分析等
        - Skill 技能：掌握多种编程语言和框架
        - 知识库：可以检索项目文档和技术资料
        - 学习能力：可以从交互中学习和改进
        
        请遵循以下原则：
        - 提供清晰、可执行的代码示例
        - 解释关键设计决策
        - 考虑代码性能和可维护性
        - 遵循最佳实践和设计模式
        """)
    String executeTask(@UserMessage String task, @V("context") String context);

    /**
     * 代码审查
     * 
     * @param code 待审查的代码
     * @param language 编程语言
     * @return 审查意见
     */
    @SystemMessage("你是一个资深代码审查专家，专注于代码质量、安全性和最佳实践。")
    String reviewCode(@UserMessage String code, @V("language") String language);

    /**
     * 技术问题解答（带知识库检索）
     * 
     * @param question 技术问题
     * @return 详细解答
     */
    @SystemMessage("你是一个技术专家，可以访问知识库获取准确的技术信息。")
    String answerTechnicalQuestion(@UserMessage String question);

    /**
     * 生成项目文档
     * 
     * @param projectInfo 项目信息
     * @param docType 文档类型（README、API文档、架构文档等）
     * @return 生成的文档内容
     */
    @SystemMessage("你是一个专业的技术文档编写专家。")
    String generateDocumentation(@UserMessage String projectInfo, @V("docType") String docType);

    /**
     * 学习和记忆新的编程知识
     * 
     * @param knowledge 要学习的知识内容
     * @param category 知识分类
     * @return 学习确认
     */
    String learnNewKnowledge(String knowledge, String category);
}
