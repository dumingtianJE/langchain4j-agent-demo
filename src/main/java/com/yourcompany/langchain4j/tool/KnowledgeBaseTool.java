package com.yourcompany.langchain4j.tool;

import com.yourcompany.langchain4j.knowledge.KnowledgeBaseManager;
import com.yourcompany.langchain4j.knowledge.KnowledgeDocument;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库检索工具
 * 供 Agent 调用以检索相关知识
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseTool {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    @Tool("从知识库中检索与查询相关的技术文档和知识")
    public String searchKnowledge(String query) {
        log.info("Agent 正在检索知识库: {}", query);
        
        List<KnowledgeDocument> relevantDocs = 
            knowledgeBaseManager.searchRelevantDocuments(query, 5, 0.6);
        
        if (relevantDocs.isEmpty()) {
            return "未找到相关的知识文档。";
        }
        
        StringBuilder result = new StringBuilder("检索到以下相关知识：\n\n");
        
        for (int i = 0; i < relevantDocs.size(); i++) {
            KnowledgeDocument doc = relevantDocs.get(i);
            result.append(String.format("【%d】%s (相关度: %.2f%%)\n", 
                i + 1, doc.getTitle(), doc.getRelevanceScore() * 100));
            result.append(String.format("分类: %s\n", doc.getCategory()));
            result.append(String.format("标签: %s\n", 
                doc.getTags() != null ? String.join(", ", doc.getTags()) : "无"));
            result.append(String.format("内容:\n%s\n\n", doc.getContent()));
        }
        
        return result.toString();
    }
    
    @Tool("根据分类获取知识库文档列表")
    public String getDocumentsByCategory(String category) {
        List<KnowledgeDocument> docs = 
            knowledgeBaseManager.getDocumentsByCategory(category);
        
        if (docs.isEmpty()) {
            return String.format("分类 '%s' 下没有文档。", category);
        }
        
        return docs.stream()
            .map(doc -> String.format("- %s [%s]", doc.getTitle(), doc.getCategory()))
            .collect(Collectors.joining("\n"));
    }
    
    @Tool("根据标签搜索知识库文档")
    public String searchDocumentsByTags(String tags) {
        String[] tagArray = tags.split(",");
        List<KnowledgeDocument> docs = 
            knowledgeBaseManager.searchByTags(tagArray);
        
        if (docs.isEmpty()) {
            return String.format("未找到包含标签 '%s' 的文档。", tags);
        }
        
        return docs.stream()
            .map(doc -> String.format("- %s (标签: %s)", 
                doc.getTitle(), 
                doc.getTags() != null ? String.join(", ", doc.getTags()) : "无"))
            .collect(Collectors.joining("\n"));
    }
    
    @Tool("获取知识库统计信息")
    public String getKnowledgeBaseStats() {
        var stats = knowledgeBaseManager.getKnowledgeBaseStats();
        return String.format(
            "知识库统计：\n" +
            "- 文档总数: %d\n" +
            "- 分类列表: %s\n" +
            "- 总访问次数: %d",
            stats.get("totalDocuments"),
            stats.get("categories"),
            stats.get("totalAccessCount")
        );
    }
}
