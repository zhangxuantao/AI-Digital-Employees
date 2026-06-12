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
}
