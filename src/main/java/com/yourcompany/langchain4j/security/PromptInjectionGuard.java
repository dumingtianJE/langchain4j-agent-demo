package com.yourcompany.langchain4j.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Prompt 注入防护组件
 * 检测并拦截恶意的 Prompt 注入攻击，保护 AI 系统安全
 *
 * 防护类型：
 * 1. 指令覆盖攻击（"ignore previous instructions"）
 * 2. 角色劫持攻击（"you are now..."）
 * 3. System Prompt 泄露尝试
 * 4. 越狱攻击（jailbreak patterns）
 */
@Slf4j
@Component
public class PromptInjectionGuard {

    /**
     * 已知攻击模式列表（正则表达式）
     */
    private static final List<Pattern> ATTACK_PATTERNS = List.of(
        // 指令覆盖
        Pattern.compile("(?i)ignore\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|prompts|rules)"),
        Pattern.compile("(?i)disregard\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|prompts|rules)"),
        Pattern.compile("(?i)forget\\s+(all\\s+)?(previous|prior|above)\\s+(instructions|prompts|rules)"),
        Pattern.compile("(?i)\\u5ffd\\u7565\\u4e4b\\u524d\\u7684(\\u6240\\u6709)?\\u6307\\u4ee4"),  // 中文：忽略之前的指令
        Pattern.compile("(?i)\\u65e0\\u89c6\\u4e0a\\u8ff0(\\u6240\\u6709)?\\u89c4\\u5219"),  // 中文：无视上述规则

        // 角色劫持
        Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an|the)\\s+"),
        Pattern.compile("(?i)act\\s+as\\s+if\\s+you\\s+(are|were)\\s+"),
        Pattern.compile("(?i)pretend\\s+(to\\s+be|you\\s+are)\\s+"),
        Pattern.compile("(?i)new\\s+role\\s*:\\s*"),
        Pattern.compile("(?i)\\u4f60\\u73b0\\u5728\\u662f"),  // 中文：你现在是
        Pattern.compile("(?i)\\u5047\\u88c5\\u4f60\\u662f"),  // 中文：假装你是

        // System Prompt 泄露
        Pattern.compile("(?i)(show|display|reveal|print|output)\\s+(me\\s+)?(your|the)\\s+(system\\s+)?(prompt|instructions|rules)"),
        Pattern.compile("(?i)what\\s+(is|are)\\s+your\\s+(system\\s+)?(prompt|instructions|rules)"),
        Pattern.compile("(?i)\\u663e\\u793a\\u4f60\\u7684(\\u7cfb\\u7edf)?\\u63d0\\u793a\\u8bcd"),  // 中文：显示你的提示词
        Pattern.compile("(?i)\\u8f93\\u51fa\\u4f60\\u7684(\\u7cfb\\u7edf)?(prompt|\\u6307\\u4ee4|\\u89c4\\u5219)"),  // 中文：输出你的指令

        // 越狱攻击
        Pattern.compile("(?i)do\\s+anything\\s+now"),
        Pattern.compile("(?i)DAN\\s+mode"),
        Pattern.compile("(?i)developer\\s+mode\\s+(enabled|activated|on)"),
        Pattern.compile("(?i)jailbreak"),
        Pattern.compile("(?i)\\u8d8a\\u72f1\\u6a21\\u5f0f"),  // 中文：越狱模式
        Pattern.compile("(?i)\\u65e0\\u9650\\u5236\\u6a21\\u5f0f")  // 中文：无限制模式
    );

    /**
     * 检测输入是否包含 Prompt 注入攻击
     *
     * @param userInput 用户输入文本
     * @return true=检测到攻击，应拦截
     */
    public boolean isInjectionAttack(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return false;
        }

        for (Pattern pattern : ATTACK_PATTERNS) {
            if (pattern.matcher(userInput).find()) {
                log.warn("检测到 Prompt 注入攻击: pattern={}, input={}",
                    pattern.pattern(),
                    userInput.length() > 100 ? userInput.substring(0, 100) + "..." : userInput);
                return true;
            }
        }

        return false;
    }

    /**
     * 清洗用户输入，移除潜在的攻击指令
     *
     * @param userInput 用户输入
     * @return 清洗后的安全输入，或 null 表示检测到严重攻击应拒绝
     */
    public String sanitizeInput(String userInput) {
        if (userInput == null) {
            return null;
        }

        // 检测严重攻击，直接拒绝
        if (isInjectionAttack(userInput)) {
            return null;
        }

        // 移除不可见字符（可能被用于绕过检测）
        String cleaned = userInput.replaceAll("[\\p{Cc}\\p{Cf}\\p{Co}\\p{Cn}&&[^\\s]]", "");

        // 限制最大长度（防止超长输入消耗资源）
        if (cleaned.length() > 10000) {
            log.warn("用户输入超长，已截断: 原始长度={}", cleaned.length());
            cleaned = cleaned.substring(0, 10000);
        }

        return cleaned;
    }

    /**
     * 检测输出是否泄露了 System Prompt
     *
     * @param aiOutput AI 输出文本
     * @return true=检测到泄露，应过滤
     */
    public boolean isPromptLeakage(String aiOutput) {
        if (aiOutput == null) {
            return false;
        }

        // 检测 AI 输出中是否包含 System Prompt 的标志性内容
        List<String> leakageIndicators = List.of(
            "你是一个世界级的 AI 编程专家",
            "【核心能力】",
            "【代码生成规范】",
            "【质量保证】",
            "You are a world-class AI programming expert"
        );

        for (String indicator : leakageIndicators) {
            if (aiOutput.contains(indicator)) {
                log.warn("检测到 System Prompt 泄露: indicator={}", indicator);
                return true;
            }
        }

        return false;
    }
}
