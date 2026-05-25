package com.yourcompany.langchain4j.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Git 集成工具 - 方案2：通过 Git 分支和 PR 管理代码
 * 适用于：团队协作、生产环境、代码审查
 */
@Slf4j
@Component
public class GitIntegrationTool {
    
    /**
     * 存储每个任务的分支信息
     */
    private final Map<String, BranchInfo> taskBranches = new ConcurrentHashMap<>();
    
    @Tool("为AI代码创建新的Git分支，避免直接在主分支上修改。参数：taskId-任务ID, branchName-分支名称, description-分支描述")
    public String createFeatureBranch(String taskId, String branchName, String description) {
        
        try {
            // 1. 检查是否在Git仓库中
            if (!isGitRepository()) {
                return "❌ 错误：当前目录不是Git仓库";
            }
            
            // 2. 创建新分支
            String fullBranchName = "ai/" + branchName.toLowerCase().replace(" ", "-");
            String command = String.format("git checkout -b %s", fullBranchName);
            
            ProcessResult result = executeGitCommand(command);
            
            if (result.exitCode != 0) {
                return "❌ 创建分支失败: " + result.error;
            }
            
            // 3. 保存分支信息
            BranchInfo branchInfo = new BranchInfo(
                taskId,
                fullBranchName,
                description,
                System.currentTimeMillis()
            );
            taskBranches.put(taskId, branchInfo);
            
            return String.format("✅ 成功创建分支: %s\n任务ID: %s\n描述: %s\n\n下一步：使用 commitCode 提交代码",
                fullBranchName, taskId, description);
            
        } catch (Exception e) {
            log.error("创建分支失败", e);
            return "❌ 创建分支失败: " + e.getMessage();
        }
    }
    
    @Tool("将AI生成的代码提交到当前分支。参数：taskId-任务ID, commitMessage-提交信息, filePaths-文件路径（逗号分隔）")
    public String commitCode(String taskId, String commitMessage, String filePaths) {
        
        try {
            // 1. 获取分支信息
            BranchInfo branchInfo = taskBranches.get(taskId);
            if (branchInfo == null) {
                return "❌ 错误：任务 " + taskId + " 没有关联的分支，请先创建分支";
            }
            
            // 2. 添加文件到暂存区
            String[] files = filePaths.split(",");
            for (String file : files) {
                String addCommand = String.format("git add %s", file.trim());
                ProcessResult addResult = executeGitCommand(addCommand);
                
                if (addResult.exitCode != 0) {
                    return "❌ 添加文件失败: " + file + "\n" + addResult.error;
                }
            }
            
            // 3. 提交代码
            String commitCommand = String.format("git commit -m \"%s\"", commitMessage);
            ProcessResult commitResult = executeGitCommand(commitCommand);
            
            if (commitResult.exitCode != 0) {
                return "❌ 提交失败: " + commitResult.error;
            }
            
            // 4. 更新分支信息
            branchInfo.addCommit(commitMessage, System.currentTimeMillis());
            
            return String.format("✅ 成功提交代码\n分支: %s\n提交信息: %s\n文件: %s",
                branchInfo.getBranchName(), commitMessage, filePaths);
            
        } catch (Exception e) {
            log.error("提交代码失败", e);
            return "❌ 提交代码失败: " + e.getMessage();
        }
    }
    
    @Tool("将当前分支推送到远程仓库。参数：taskId-任务ID")
    public String pushToRemote(String taskId) {
        try {
            BranchInfo branchInfo = taskBranches.get(taskId);
            if (branchInfo == null) {
                return "❌ 错误：任务 " + taskId + " 没有关联的分支";
            }
            
            String pushCommand = String.format("git push -u origin %s", branchInfo.getBranchName());
            ProcessResult result = executeGitCommand(pushCommand);
            
            if (result.exitCode != 0) {
                return "❌ 推送失败: " + result.error;
            }
            
            branchInfo.setPushed(true);
            
            return String.format("✅ 成功推送到远程仓库\n分支: %s\n\n下一步：创建 Pull Request",
                branchInfo.getBranchName());
            
        } catch (Exception e) {
            log.error("推送失败", e);
            return "❌ 推送失败: " + e.getMessage();
        }
    }
    
    @Tool("创建 Pull Request（GitHub/GitLab）。参数：taskId-任务ID, title-PR标题, description-PR描述, targetBranch-目标分支")
    public String createPullRequest(String taskId, String title, String description, String targetBranch) {
        
        try {
            BranchInfo branchInfo = taskBranches.get(taskId);
            if (branchInfo == null) {
                return "❌ 错误：任务 " + taskId + " 没有关联的分支";
            }
            
            if (!branchInfo.isPushed()) {
                return "❌ 错误：请先推送到远程仓库";
            }
            
            // 检测是 GitHub 还是 GitLab
            String remoteUrl = getRemoteUrl();
            boolean isGitHub = remoteUrl.contains("github.com");
            boolean isGitLab = remoteUrl.contains("gitlab.com");
            
            String prUrl;
            
            if (isGitHub) {
                // GitHub PR
                prUrl = createGitHubPR(branchInfo.getBranchName(), targetBranch, title, description);
            } else if (isGitLab) {
                // GitLab MR
                prUrl = createGitLabMR(branchInfo.getBranchName(), targetBranch, title, description);
            } else {
                return "❌ 不支持的远程仓库类型: " + remoteUrl;
            }
            
            branchInfo.setPrUrl(prUrl);
            branchInfo.setPrCreated(true);
            
            return String.format("✅ Pull Request 创建成功\n标题: %s\n链接: %s\n\n等待代码审查...",
                title, prUrl);
            
        } catch (Exception e) {
            log.error("创建 PR 失败", e);
            return "❌ 创建 PR 失败: " + e.getMessage();
        }
    }
    
    @Tool("查看当前任务的分支和提交历史。参数：taskId-任务ID")
    public String getTaskStatus(String taskId) {
        BranchInfo branchInfo = taskBranches.get(taskId);
        
        if (branchInfo == null) {
            return "❌ 任务不存在: " + taskId;
        }
        
        StringBuilder status = new StringBuilder();
        status.append("📋 任务状态\n");
        status.append("═══════════════════════════\n");
        status.append("任务ID: ").append(taskId).append("\n");
        status.append("分支: ").append(branchInfo.getBranchName()).append("\n");
        status.append("描述: ").append(branchInfo.getDescription()).append("\n");
        status.append("已推送: ").append(branchInfo.isPushed() ? "✅" : "❌").append("\n");
        status.append("PR 已创建: ").append(branchInfo.isPrCreated() ? "✅" : "❌").append("\n");
        
        if (branchInfo.getPrUrl() != null) {
            status.append("PR 链接: ").append(branchInfo.getPrUrl()).append("\n");
        }
        
        status.append("\n提交历史:\n");
        for (CommitInfo commit : branchInfo.getCommits()) {
            status.append("  - ").append(commit.getMessage()).append("\n");
        }
        
        return status.toString();
    }
    
    @Tool("合并分支到目标分支。参数：taskId-任务ID, targetBranch-目标分支名称")
    public String mergeBranch(String taskId, String targetBranch) {
        
        try {
            BranchInfo branchInfo = taskBranches.get(taskId);
            if (branchInfo == null) {
                return "❌ 错误：任务 " + taskId + " 没有关联的分支";
            }
            
            // 切换到目标分支
            executeGitCommand("git checkout " + targetBranch);
            
            // 合并
            String mergeCommand = String.format("git merge %s", branchInfo.getBranchName());
            ProcessResult result = executeGitCommand(mergeCommand);
            
            if (result.exitCode != 0) {
                return "❌ 合并失败（可能有冲突）: " + result.error;
            }
            
            return String.format("✅ 成功合并分支 %s -> %s",
                branchInfo.getBranchName(), targetBranch);
            
        } catch (Exception e) {
            log.error("合并分支失败", e);
            return "❌ 合并分支失败: " + e.getMessage();
        }
    }
    
    // ==================== 私有方法 ====================
    
    private boolean isGitRepository() {
        try {
            ProcessResult result = executeGitCommand("git rev-parse --git-dir");
            return result.exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    private String getRemoteUrl() {
        try {
            ProcessResult result = executeGitCommand("git remote get-url origin");
            if (result.exitCode == 0) {
                return result.output;
            }
        } catch (Exception e) {
            log.error("获取远程URL失败", e);
        }
        return "";
    }
    
    private String createGitHubPR(String sourceBranch, String targetBranch, 
                                   String title, String description) {
        // 使用 GitHub CLI
        String command = String.format(
            "gh pr create --base %s --head %s --title \"%s\" --body \"%s\"",
            targetBranch, sourceBranch, title, description
        );
        
        ProcessResult result = executeCommand(command);
        
        if (result.exitCode == 0) {
            return result.output; // 返回 PR URL
        }
        
        throw new RuntimeException("GitHub PR 创建失败: " + result.error);
    }
    
    private String createGitLabMR(String sourceBranch, String targetBranch,
                                   String title, String description) {
        // 使用 GitLab CLI 或 API
        String command = String.format(
            "glab mr create --source-branch %s --target-branch %s --title \"%s\" --description \"%s\"",
            sourceBranch, targetBranch, title, description
        );
        
        ProcessResult result = executeCommand(command);
        
        if (result.exitCode == 0) {
            return result.output;
        }
        
        throw new RuntimeException("GitLab MR 创建失败: " + result.error);
    }
    
    private ProcessResult executeGitCommand(String command) {
        return executeCommand(command);
    }
    
    private ProcessResult executeCommand(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("bash", "-c", command);
            }
            
            pb.directory(new File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            StringBuilder error = new StringBuilder();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            
            return new ProcessResult(exitCode, output.toString(), error.toString());
            
        } catch (Exception e) {
            log.error("执行命令失败: {}", command, e);
            return new ProcessResult(1, "", e.getMessage());
        }
    }
    
    // ==================== 数据类 ====================
    
    private static class BranchInfo {
        private final String taskId;
        private final String branchName;
        private final String description;
        private final long createdAt;
        private boolean pushed = false;
        private boolean prCreated = false;
        private String prUrl;
        private final List<CommitInfo> commits = new java.util.ArrayList<>();
        
        public BranchInfo(String taskId, String branchName, String description, long createdAt) {
            this.taskId = taskId;
            this.branchName = branchName;
            this.description = description;
            this.createdAt = createdAt;
        }
        
        public void addCommit(String message, long timestamp) {
            commits.add(new CommitInfo(message, timestamp));
        }
        
        // Getters and Setters
        public String getTaskId() { return taskId; }
        public String getBranchName() { return branchName; }
        public String getDescription() { return description; }
        public long getCreatedAt() { return createdAt; }
        public boolean isPushed() { return pushed; }
        public void setPushed(boolean pushed) { this.pushed = pushed; }
        public boolean isPrCreated() { return prCreated; }
        public void setPrCreated(boolean prCreated) { this.prCreated = prCreated; }
        public String getPrUrl() { return prUrl; }
        public void setPrUrl(String prUrl) { this.prUrl = prUrl; }
        public List<CommitInfo> getCommits() { return commits; }
    }
    
    private static class CommitInfo {
        private final String message;
        private final long timestamp;
        
        public CommitInfo(String message, long timestamp) {
            this.message = message;
            this.timestamp = timestamp;
        }
        
        public String getMessage() { return message; }
        public long getTimestamp() { return timestamp; }
    }
    
    private static class ProcessResult {
        final int exitCode;
        final String output;
        final String error;
        
        public ProcessResult(int exitCode, String output, String error) {
            this.exitCode = exitCode;
            this.output = output;
            this.error = error;
        }
    }
}
