package com.ai.cs.gateway.rest;

import com.ai.cs.application.knowledge.KnowledgeBaseService;
import com.ai.cs.domain.knowledge.KnowledgeBase;
import com.ai.cs.domain.knowledge.KnowledgeDocument;
import com.ai.cs.domain.knowledge.KnowledgeChunk;
import com.ai.cs.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('knowledge:view')")
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;

    @GetMapping
    public ApiResponse<List<KnowledgeBase>> list() { return ApiResponse.success(kbService.listAll()); }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeBase> get(@PathVariable Long id) { return ApiResponse.success(kbService.getById(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('knowledge:edit')")
    public ApiResponse<KnowledgeBase> create(@RequestBody KnowledgeBase kb) { return ApiResponse.success(kbService.create(kb)); }

    @PostMapping("/{kbId}/documents")
    @PreAuthorize("hasAuthority('knowledge:edit')")
    public ApiResponse<KnowledgeDocument> upload(@PathVariable Long kbId, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(kbService.uploadDocument(kbId, file));
    }

    @GetMapping("/{kbId}/documents")
    public ApiResponse<List<KnowledgeDocument>> getDocuments(@PathVariable Long kbId) {
        return ApiResponse.success(kbService.getDocuments(kbId));
    }

    @GetMapping("/{kbId}/documents/{docId}/chunks")
    public ApiResponse<List<KnowledgeChunk>> getChunks(@PathVariable Long kbId, @PathVariable Long docId) {
        return ApiResponse.success(kbService.getChunks(docId));
    }
}
