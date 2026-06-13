package com.ai.cs.application.knowledge;

import com.ai.cs.domain.knowledge.KnowledgeBase;
import com.ai.cs.domain.knowledge.KnowledgeDocument;
import com.ai.cs.domain.knowledge.KnowledgeChunk;
import com.ai.cs.domain.knowledge.repository.KnowledgeBaseRepository;
import com.ai.cs.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.ai.cs.domain.knowledge.repository.KnowledgeChunkRepository;
import com.ai.cs.infrastructure.search.KnowledgeChunkIndexService;
import com.ai.cs.infrastructure.storage.DocumentParserService;
import com.ai.cs.infrastructure.storage.TextSplitter;
import com.ai.cs.infrastructure.mq.DocumentTrainingProducer;
import com.ai.cs.infrastructure.storage.LocalFileStorageService;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository kbRepository;
    private final KnowledgeDocumentRepository docRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final DocumentParserService parserService;
    private final TextSplitter textSplitter;
    private final DocumentTrainingProducer trainingProducer;

    @Autowired(required = false)
    private KnowledgeChunkIndexService chunkIndexService;

    @Autowired
    private DocumentTrainingService trainingService;

    public List<KnowledgeBase> listAll() {
        List<KnowledgeBase> list = kbRepository.findAll();
        list.forEach(kb -> kb.setDocumentCount(docRepository.countByKbId(kb.getId())));
        return list;
    }

    public KnowledgeBase getById(Long id) {
        return kbRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND));
    }

    @Transactional
    public KnowledgeBase create(KnowledgeBase kb) { return kbRepository.save(kb); }

    @Transactional
    public KnowledgeDocument uploadDocument(Long kbId, MultipartFile file) {
        getById(kbId);
        String originalName = file.getOriginalFilename();
        String fileType = getFileType(originalName);

        // Save file
        String filePath = LocalFileStorageService.saveKnowledgeFile(kbId, file);

        // Create document record (status: UPLOADED — training is separate)
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKbId(kbId);
        doc.setFileName(originalName);
        doc.setFileType(fileType);
        doc.setFileSize(file.getSize());
        doc.setFilePath(filePath);
        doc.setStatus("UPLOADED");
        doc.setChunkCount(0);
        doc = docRepository.save(doc);

        // Trigger training asynchronously — both MQ and direct fallback
        final Long savedDocId = doc.getId();
        CompletableFuture.runAsync(() -> {
            try {
                trainingService.train(savedDocId);
            } catch (Exception e) {
                log.error("异步训练失败: docId={}", savedDocId, e);
            }
        });
        // Also send MQ message (best-effort)
//        trainingProducer.send(savedDocId);

        log.info("文档已上传: kbId={}, docId={}, fileName={}", kbId, doc.getId(), originalName);
        return doc;
    }

    @Transactional
    public void processDocument(KnowledgeDocument doc) {
        try {
            doc.setStatus("PROCESSING");
            docRepository.save(doc);

            // Parse
            java.io.File file = new java.io.File(doc.getFilePath());
            String text = parserService.parse(doc.getFileType(), new java.io.FileInputStream(file));

            // Split
            List<String> chunks = textSplitter.split(text);
            int chunkIndex = 0;
            for (String chunkText : chunks) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocId(doc.getId());
                chunk.setKbId(doc.getKbId());
                chunk.setContent(chunkText);
                chunk.setChunkIndex(chunkIndex++);
                chunk.setEmbeddingStatus("PENDING");
                chunkRepository.save(chunk);
            }

            doc.setChunkCount(chunks.size());
            doc.setStatus("DONE");
            docRepository.save(doc);
        } catch (Exception e) {
            log.error("文档处理失败: docId={}", doc.getId(), e);
            doc.setStatus("PARTIAL_FAILED");
            docRepository.save(doc);
        }
    }

    public List<KnowledgeDocument> getDocuments(Long kbId) {
        return docRepository.findByKbId(kbId);
    }

    public List<KnowledgeChunk> getChunks(Long docId) {
        return chunkRepository.findByDocId(docId);
    }

    public Page<KnowledgeChunk> getChunks(Long docId, int page, int size) {
        return chunkRepository.findByDocId(docId, PageRequest.of(page, size));
    }

    /**
     * 删除文档及其关联的分片、ES索引、物理文件
     */
    @Transactional
    public void deleteDocument(Long docId) {
        KnowledgeDocument doc = docRepository.findById(docId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "文档不存在: " + docId));

        log.info("删除文档: docId={}, fileName={}", docId, doc.getFileName());

        // 1. 删除分片 (MySQL)
        chunkRepository.deleteByDocId(docId);

        // 2. 删除 ES 索引 (非关键，失败不影响事务)
        if (chunkIndexService != null) {
            try {
                chunkIndexService.deleteByDocId(docId);
            } catch (Exception e) {
                log.warn("删除ES索引失败(不影响文档删除): docId={}", docId, e);
            }
        }

        // 3. 删除物理文件
        LocalFileStorageService.deleteFile(doc.getFilePath());

        // 4. 删除文档记录
        docRepository.delete(doc);

        log.info("文档已删除: docId={}", docId);
    }

    /**
     * 删除知识库及其下所有文档、分片、ES索引、物理文件
     */
    @Transactional
    public void deleteKnowledgeBase(Long kbId) {
        KnowledgeBase kb = getById(kbId);

        log.info("删除知识库: kbId={}, name={}", kbId, kb.getName());

        // 1. 获取所有文档（用于删除物理文件）
        List<KnowledgeDocument> docs = docRepository.findByKbId(kbId);

        // 2. 删除 ES 索引 (非关键)
        if (chunkIndexService != null) {
            try {
                chunkIndexService.deleteByKbId(kbId);
            } catch (Exception e) {
                log.warn("删除ES索引失败(不影响知识库删除): kbId={}", kbId, e);
            }
        }

        // 3. 删除物理文件目录
        LocalFileStorageService.deleteKnowledgeDir(kbId);

        // 4. 删除分片 (MySQL)
        chunkRepository.deleteByKbId(kbId);

        // 5. 删除文档 (MySQL)
        docRepository.deleteByKbId(kbId);

        // 6. 删除知识库
        kbRepository.delete(kb);

        log.info("知识库已删除: kbId={}, 文档数={}", kbId, docs.size());
    }

    private String getFileType(String fileName) {
        if (fileName == null) return "TXT";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
        return ext;
    }
}
