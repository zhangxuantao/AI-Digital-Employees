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
     */
    @Transactional
    public void train(Long documentId) {
        KnowledgeDocument doc = docRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("文档不存在: " + documentId));

        try {
            // 1. Set status to PROCESSING
            doc.setStatus("PROCESSING");
            docRepository.save(doc);

            // 2. Parse document
            File file = new File(doc.getFilePath());
            String text = parserService.parse(doc.getFileType(), new FileInputStream(file));

            // 3. Split into chunks
            List<String> chunkTexts = textSplitter.split(text);
            log.info("文档分片完成: docId={}, chunks={}", documentId, chunkTexts.size());

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

            // 5. Vectorize
            List<String> contents = chunks.stream().map(KnowledgeChunk::getContent).toList();
            List<float[]> embeddings = embeddingService.embedBatch(contents);

            for (KnowledgeChunk chunk : chunks) {
                chunk.setEmbeddingStatus("EMBEDDED");
            }

            // 6. Index to Elasticsearch
            chunkIndexService.bulkIndex(chunks, embeddings);

            // 7. Mark document as DONE
            doc.setChunkCount(chunks.size());
            doc.setStatus("DONE");
            docRepository.save(doc);

            log.info("文档训练完成: docId={}, chunks={}", documentId, chunks.size());
        } catch (Exception e) {
            log.error("文档训练失败: docId={}", documentId, e);
            doc.setStatus("PARTIAL_FAILED");
            docRepository.save(doc);
        }
    }
}
