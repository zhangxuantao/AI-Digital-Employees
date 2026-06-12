package com.ai.cs.application.knowledge;

import com.ai.cs.domain.knowledge.KnowledgeBase;
import com.ai.cs.domain.knowledge.KnowledgeDocument;
import com.ai.cs.domain.knowledge.KnowledgeChunk;
import com.ai.cs.domain.knowledge.repository.KnowledgeBaseRepository;
import com.ai.cs.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.ai.cs.domain.knowledge.repository.KnowledgeChunkRepository;
import com.ai.cs.infrastructure.storage.DocumentParserService;
import com.ai.cs.infrastructure.storage.TextSplitter;
import com.ai.cs.infrastructure.mq.DocumentTrainingProducer;
import com.ai.cs.infrastructure.storage.LocalFileStorageService;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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

    public List<KnowledgeBase> listAll() { return kbRepository.findAll(); }

    public KnowledgeBase getById(Long id) {
        return kbRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND));
    }

    @Transactional
    public KnowledgeBase create(KnowledgeBase kb) { return kbRepository.save(kb); }

    @Transactional
    public KnowledgeDocument uploadDocument(Long kbId, MultipartFile file) {
        KnowledgeBase kb = getById(kbId);
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

        // Send async training message via RocketMQ
        trainingProducer.send(doc.getId());

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

    private String getFileType(String fileName) {
        if (fileName == null) return "TXT";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
        return ext;
    }
}
