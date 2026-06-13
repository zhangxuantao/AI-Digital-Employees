package com.ai.cs.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class LocalFileStorageService {

    private static String basePath;

    public LocalFileStorageService(@Value("${app.storage.base-path:./data}") String basePath) {
        LocalFileStorageService.basePath = basePath;
    }

    public static String saveKnowledgeFile(Long kbId, MultipartFile file) {
        try {
            Path dir = Path.of(basePath, "knowledge", kbId.toString());
            Files.createDirectories(dir);
            Path target = dir.resolve(file.getOriginalFilename());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("文件存储失败", e);
        }
    }

    /** 删除单个文件 */
    public static void deleteFile(String filePath) {
        try {
            if (filePath != null) {
                Files.deleteIfExists(Path.of(filePath));
                log.info("文件已删除: {}", filePath);
            }
        } catch (IOException e) {
            log.error("文件删除失败: {}", filePath, e);
        }
    }

    /** 删除知识库目录及其所有文件 */
    public static void deleteKnowledgeDir(Long kbId) {
        try {
            Path dir = Path.of(basePath, "knowledge", kbId.toString());
            if (Files.exists(dir)) {
                try (var stream = Files.walk(dir)) {
                    stream.sorted(java.util.Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.deleteIfExists(path);
                                } catch (IOException ignored) {
                                }
                            });
                }
                log.info("知识库目录已删除: kbId={}", kbId);
            }
        } catch (IOException e) {
            log.error("知识库目录删除失败: kbId={}", kbId, e);
        }
    }
}
