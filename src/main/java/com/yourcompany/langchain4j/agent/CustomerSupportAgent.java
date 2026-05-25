package com.yourcompany.langchain4j.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * 智能客服 Agent
 * 处理客户咨询、问题解答等
 */
@AiService
public interface CustomerSupportAgent {

    @SystemMessage("""
        你是一个专业的智能客服助手。
        
        你的职责:
        1. 回答用户关于产品和服务的问题
        2. 提供技术支持和指导
        3. 处理客户投诉和建议
        4. 引导用户完成相关操作
        
        要求:
        - 回答要专业、友好、简洁
        - 如果不知道答案,诚实告知并建议联系人工客服
        - 不要编造信息
        """)
    String chat(@UserMessage String message);
}
