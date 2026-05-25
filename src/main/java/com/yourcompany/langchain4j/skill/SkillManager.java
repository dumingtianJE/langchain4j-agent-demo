package com.yourcompany.langchain4j.skill;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Skill 技能管理器
 * 负责技能的注册、检索和管理
 */
@Component
public class SkillManager {
    
    private final Map<String, Skill> skillRegistry = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void initializeDefaultSkills() {
        // 初始化默认技能
        registerSkill(new Skill(
            "java-spring-boot",
            "Java Spring Boot 开发",
            "精通 Spring Boot 框架开发，包括 REST API、依赖注入、数据访问等",
            "框架",
            "使用 Spring Boot 3.x 开发企业级应用。关键技术点：\n" +
            "1. @RestController 和 @RequestMapping\n" +
            "2. @Service、@Repository、@Component 依赖注入\n" +
            "3. Spring Data JPA 数据访问\n" +
            "4. Spring Security 认证授权\n" +
            "5. 配置管理和 Profile",
            Arrays.asList("java", "spring", "spring-boot", "rest-api", "backend"),
            5,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        ));
        
        registerSkill(new Skill(
            "python-fastapi",
            "Python FastAPI 开发",
            "使用 FastAPI 构建高性能 Python Web 服务",
            "框架",
            "使用 FastAPI 开发异步 Web 应用。关键技术点：\n" +
            "1. 异步路由和依赖注入\n" +
            "2. Pydantic 数据验证\n" +
            "3. SQLAlchemy 数据库集成\n" +
            "4. OpenAPI 自动生成文档\n" +
            "5. WebSocket 支持",
            Arrays.asList("python", "fastapi", "async", "rest-api"),
            5,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        ));
        
        registerSkill(new Skill(
            "react-typescript",
            "React + TypeScript 前端开发",
            "使用 React 和 TypeScript 构建现代化前端应用",
            "框架",
            "React 18 + TypeScript 前端开发。关键技术点：\n" +
            "1. Hooks（useState, useEffect, useContext, useReducer）\n" +
            "2. TypeScript 类型系统\n" +
            "3. React Router 路由管理\n" +
            "4. 状态管理（Redux/Zustand）\n" +
            "5. 组件设计和性能优化",
            Arrays.asList("react", "typescript", "frontend", "hooks"),
            5,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        ));
        
        registerSkill(new Skill(
            "design-patterns",
            "设计模式应用",
            "熟练应用 23 种 GoF 设计模式解决实际问题",
            "设计模式",
            "常用设计模式：\n" +
            "1. 创建型：工厂、单例、建造者、原型\n" +
            "2. 结构型：适配器、装饰器、代理、观察者\n" +
            "3. 行为型：策略、命令、责任链、模板方法\n" +
            "4. 并发模式：线程池、Future、CompletableFuture",
            Arrays.asList("design-patterns", "oop", "architecture", "best-practices"),
            5,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        ));
        
        registerSkill(new Skill(
            "sql-optimization",
            "SQL 优化",
            "数据库查询优化和性能调优",
            "数据库",
            "SQL 性能优化技术：\n" +
            "1. 索引优化（B-Tree、Hash、覆盖索引）\n" +
            "2. 查询优化（EXPLAIN 分析、避免 N+1）\n" +
            "3. 分库分表策略\n" +
            "4. 缓存策略（Redis、本地缓存）\n" +
            "5. 事务隔离级别和锁优化",
            Arrays.asList("sql", "database", "optimization", "performance"),
            4,
            LocalDateTime.now(),
            LocalDateTime.now(),
            0
        ));
    }
    
    /**
     * 注册新技能
     */
    public void registerSkill(Skill skill) {
        skill.setId(skill.getId() != null ? skill.getId() : UUID.randomUUID().toString());
        skill.setCreatedAt(LocalDateTime.now());
        skill.setUpdatedAt(LocalDateTime.now());
        skill.setUsageCount(0);
        skillRegistry.put(skill.getId(), skill);
    }
    
    /**
     * 根据 ID 获取技能
     */
    public Optional<Skill> getSkillById(String id) {
        return Optional.ofNullable(skillRegistry.get(id));
    }
    
    /**
     * 根据关键词搜索技能
     */
    public List<Skill> searchSkills(String query) {
        String lowerQuery = query.toLowerCase();
        return skillRegistry.values().stream()
            .filter(skill -> 
                skill.getName().toLowerCase().contains(lowerQuery) ||
                skill.getDescription().toLowerCase().contains(lowerQuery) ||
                skill.getCategory().toLowerCase().contains(lowerQuery) ||
                skill.getKeywords().stream().anyMatch(kw -> kw.toLowerCase().contains(lowerQuery))
            )
            .sorted(Comparator.comparing(Skill::getProficiencyLevel).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * 根据分类获取技能
     */
    public List<Skill> getSkillsByCategory(String category) {
        return skillRegistry.values().stream()
            .filter(skill -> skill.getCategory().equals(category))
            .sorted(Comparator.comparing(Skill::getProficiencyLevel).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * 获取所有技能
     */
    public List<Skill> getAllSkills() {
        return new ArrayList<>(skillRegistry.values());
    }
    
    /**
     * 记录技能使用
     */
    public void recordSkillUsage(String skillId) {
        Skill skill = skillRegistry.get(skillId);
        if (skill != null) {
            skill.setUsageCount(skill.getUsageCount() + 1);
            skill.setUpdatedAt(LocalDateTime.now());
        }
    }
    
    /**
     * 更新技能内容
     */
    public void updateSkill(String skillId, Skill updatedSkill) {
        Skill existing = skillRegistry.get(skillId);
        if (existing != null) {
            existing.setName(updatedSkill.getName());
            existing.setDescription(updatedSkill.getDescription());
            existing.setContent(updatedSkill.getContent());
            existing.setKeywords(updatedSkill.getKeywords());
            existing.setProficiencyLevel(updatedSkill.getProficiencyLevel());
            existing.setUpdatedAt(LocalDateTime.now());
        }
    }
    
    /**
     * 删除技能
     */
    public boolean deleteSkill(String skillId) {
        return skillRegistry.remove(skillId) != null;
    }
    
    /**
     * 获取技能统计信息
     */
    public Map<String, Object> getSkillStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSkills", skillRegistry.size());
        stats.put("categories", skillRegistry.values().stream()
            .map(Skill::getCategory)
            .distinct()
            .count());
        stats.put("totalUsage", skillRegistry.values().stream()
            .mapToInt(Skill::getUsageCount)
            .sum());
        
        return stats;
    }
}
