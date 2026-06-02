package com.yourcompany.langchain4j.controller;

import com.yourcompany.langchain4j.service.CodeFileService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 代码文件 API：加载项目目录树、读写文件
 */
@Slf4j
@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeFileController {

    private final CodeFileService codeFileService;

    /**
     * 获取工作区根目录
     */
    @GetMapping("/workspace")
    public ResponseEntity<Map<String, Object>> workspace() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "workspaceRoot", codeFileService.getWorkspaceRoot()
        ));
    }

    /**
     * 加载项目目录树（递归，排除 node_modules/target 等）
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> tree(@RequestParam(defaultValue = ".") String path) {
        try {
            Map<String, Object> data = codeFileService.buildDirectoryTree(path);
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.warn("加载目录树失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 读取文件内容
     */
    @GetMapping("/read")
    public ResponseEntity<Map<String, Object>> read(@RequestParam String path) {
        try {
            Map<String, Object> data = codeFileService.readFile(path);
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.warn("读取文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * 写入文件
     */
    @PostMapping("/write")
    public ResponseEntity<Map<String, Object>> write(@RequestBody WriteFileRequest request) {
        try {
            Map<String, Object> data = codeFileService.writeFile(request.getPath(), request.getContent());
            return ResponseEntity.ok(Map.of("success", true, "data", data));
        } catch (Exception e) {
            log.warn("写入文件失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @Data
    public static class WriteFileRequest {
        private String path;
        private String content;
        private String description;
    }
}
