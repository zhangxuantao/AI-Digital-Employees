package com.ai.cs.application.knowledge;

import com.ai.cs.domain.knowledge.KnowledgeChunk;
import com.ai.cs.domain.knowledge.KnowledgeDocument;
import com.ai.cs.domain.knowledge.repository.KnowledgeChunkRepository;
import com.ai.cs.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.ai.cs.infrastructure.llm.EmbeddingService;
import com.ai.cs.infrastructure.search.KnowledgeChunkIndexService;
import com.ai.cs.infrastructure.storage.DocumentParserService;
import com.ai.cs.infrastructure.storage.TextSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTrainingService {

    private final KnowledgeDocumentRepository docRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final DocumentParserService parserService;
    private final TextSplitter textSplitter;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkIndexService chunkIndexService;

    /**
     * Full document training pipeline:
     * Parse -> Split -> Vectorize -> ES Index -> Update Status
     * ES indexing failure does NOT fail the entire pipeline;
     * chunks and embedding status are preserved in MySQL.
     */
    @Transactional
    public void train(Long documentId) {
        KnowledgeDocument doc = docRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        boolean esIndexed = false;

        try {
            // 1. Set status to PROCESSING
            doc.setStatus("PROCESSING");
            docRepository.save(doc);
            log.info("文档训练开始: docId={}, fileType={}", documentId, doc.getFileType());

            // 2. Parse document
            File file = new File(doc.getFilePath());
            if (!file.exists()) {
                doc.setStatus("PARTIAL_FAILED");
                docRepository.save(doc);
                log.error("文档文件不存在: {}", doc.getFilePath());
                return;
            }
            String text = parserService.parse(doc.getFileType(), new FileInputStream(file));
            log.info("文档解析完成: docId={}, textLen={}", documentId, text.length());

            // 3. Split into chunks
            List<String> chunkTexts = textSplitter.split(text);
            log.info("文档分片完成: docId={}, chunks={}", documentId, chunkTexts.size());

            if (chunkTexts.isEmpty()) {
                doc.setChunkCount(0);
                doc.setStatus("DONE");
                docRepository.save(doc);
                log.info("文档无有效内容，训练完成: docId={}", documentId);
                return;
            }

            // 4. Create KnowledgeChunk records in MySQL
            List<KnowledgeChunk> chunks = new ArrayList<>();
            for (int i = 0; i < chunkTexts.size(); i++) {
                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setDocId(doc.getId());
                chunk.setKbId(doc.getKbId());
                chunk.setContent(chunkTexts.get(i));
                chunk.setChunkIndex(i);
                chunk.setEmbeddingStatus("PENDING");
                chunks.add(chunk);
            }
            chunks = chunkRepository.saveAll(chunks);
            log.info("分片已存储MySQL: docId={}, count={}", documentId, chunks.size());

            // 5. Vectorize
            List<String> contents = chunks.stream().map(KnowledgeChunk::getContent).toList();
            List<float[]> embeddings = embeddingService.embedBatch(contents);

            boolean hasRealVectors = false;
            for (int i = 0; i < embeddings.size(); i++) {
                boolean isZero = true;
                float[] vec = embeddings.get(i);
                for (float v : vec) {
                    if (v != 0.0f) { isZero = false; break; }
                }
                if (!isZero) { hasRealVectors = true; break; }
            }

            for (KnowledgeChunk chunk : chunks) {
                chunk.setEmbeddingStatus(hasRealVectors ? "EMBEDDED" : "PENDING");
            }
            chunkRepository.saveAll(chunks);
            log.info("向量化完成: docId={}, count={}, hasRealVectors={}", documentId, chunks.size(), hasRealVectors);

            // 6. Index to Elasticsearch (non-critical — failure does not block training completion)
            try {
                if (hasRealVectors) {
                    chunkIndexService.bulkIndex(chunks, embeddings);
                    esIndexed = true;
                    log.info("ES索引完成: docId={}, count={}", documentId, chunks.size());
                } else {
                    log.warn("向量为空(API Key未配置或API不可用)，跳过ES索引: docId={}", documentId);
                }
            } catch (Exception esEx) {
                log.warn("ES索引失败(不影响训练完成): docId={}, error={}", documentId, esEx.getMessage());
            }

            // 7. Mark document as DONE
            doc.setChunkCount(chunks.size());
            doc.setStatus("DONE");
            docRepository.save(doc);

            log.info("文档训练完成: docId={}, chunks={}, esIndexed={}, hasRealVectors={}",
                    documentId, chunks.size(), esIndexed, hasRealVectors);
        } catch (Exception e) {
            log.error("文档训练失败: docId={}", documentId, e);
            try {
                doc.setStatus("PARTIAL_FAILED");
                docRepository.save(doc);
            } catch (Exception innerEx) {
                log.error("更新文档状态失败", innerEx);
            }
        }
    }
}
