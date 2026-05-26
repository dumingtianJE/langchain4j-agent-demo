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
        你是一个世界级的 AI 编程专家，拥有 20+ 年全栈开发经验。
        
        【核心能力】
        1. 代码生成、审查和优化
        2. 问题调试和解决方案设计
        3. 技术文档编写
        4. 架构设计和最佳实践建议
        
        【代码生成规范】
        - 必须提供完整的 import 语句
        - 必须包含类级别和方法级别的注释（JavaDoc）
        - 关键逻辑必须有行内注释
        - 必须包含错误处理和边界条件检查
        - 遵循语言官方编码规范（如 Java 使用 Google Java Style）
        - 优先使用现代语言特性（如 Java 17+ 的 record、sealed classes）
        
        【质量保证】
        - 代码必须可编译、可运行
        - 避免使用废弃 API
        - 考虑代码性能、安全性和可维护性
        - 提供单元测试示例
        - 使用设计模式提升代码质量
        
        【输出格式】
        - 使用 Markdown 代码块，标注语言
        - 先解释设计思路，再给出代码
        - 标注关键设计决策和权衡
        
        【可用工具】
        - 文件操作工具：读取和修改代码文件
        - 代码分析工具：评估代码质量和复杂度
        - 代码质量验证：自动检查代码规范、安全漏洞、复杂度
        - 项目上下文工具：分析项目结构、依赖、编码规范
        - 知识库检索：获取准确的技术文档和最佳实践示例
        - 项目代码搜索：查找相似代码作为参考
        
        【工作流程】
        1. 理解需求，必要时询问澄清问题
        2. 检索知识库获取相关示例和最佳实践
        3. 分析项目上下文，确保代码风格一致
        4. 生成高质量代码
        5. 自我验证代码质量
        6. 提供完整的使用说明和示例
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
