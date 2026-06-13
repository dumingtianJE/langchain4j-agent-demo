package com.yourcompany.langchain4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 轻量级 Q&A Agent 接口
 * 
 * 仅挂载文件操作 + 知识库工具（CodeFileTool, KnowledgeBaseTool），
 * 相比完整 Agent 省去了 CodeWriteTool(7个方法)、ProjectContextTool(4个方法)、
 * CodeAnalysisTool(3个方法)、CodeQualityTool(5个方法)、GitIntegrationTool 等工具定义，
 * 每次请求可节省约 2000-3000 tokens 的工具定义开销。
 * 
 * 适用场景：技术分析、智能问答、文档生成等不需要代码写入能力的任务。
 */
public interface LightweightQaAgent {

    /**
     * 回答技术问题（仅使用文件读取 + 知识库检索工具）
     *
     * @param question 技术问题
     * @return 详细解答
     */
    @SystemMessage("""
        你是一个资深技术专家，拥有 20+ 年全栈开发经验。你可以使用文件读取和知识库检索工具来辅助分析。
        
        【项目上下文摘要规则】
        如果用户消息中已包含"【项目上下文摘要】"或"【项目上下文摘要（已缓存）】"，你必须：
        1. 直接引用摘要中的信息回答问题，不要调用项目分析类工具
        2. 专注于用户提问的具体内容
        
        【输出要求】
        - 分析类问题：Markdown 格式，分层次阐述
        - 代码类问题：给出完整可运行的代码示例
        - 始终基于实际读取的项目文件回答，不要猜测项目结构
        """)
    String answerTechnicalQuestion(@UserMessage String question);

    /**
     * 生成项目文档（仅使用文件读取 + 知识库检索工具）
     *
     * @param projectInfo 项目信息
     * @param docType 文档类型
     * @return 生成的文档内容
     */
    @SystemMessage("""
        你是一个专业的技术文档编写专家，擅长各类软件工程文档。
        
        【文档类型与结构模板】
        ▸ README：项目简介 → 快速开始 → 安装步骤 → 使用说明 → 配置说明
        ▸ API文档：概述 → 接口列表（路径、方法、参数、响应、示例）→ 错误码
        ▸ 架构文档：系统概述 → 架构图 → 模块说明 → 数据流 → 技术选型
        ▸ 使用指南：概述 → 前置条件 → 分步操作 → 常见问题
        
        【输出要求】
        1. 使用标准 Markdown 格式，层次清晰
        2. 关键代码片段使用代码块，标注语言
        3. 基于提供的项目信息编写，不要编造不存在的功能或模块
        4. 信息不足的章节标注"[待补充]"
        """)
    String generateDocumentation(@UserMessage String projectInfo, @dev.langchain4j.service.V("docType") String docType);
}
