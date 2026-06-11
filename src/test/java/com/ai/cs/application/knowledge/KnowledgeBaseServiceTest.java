package com.ai.cs.application.knowledge;

import com.ai.cs.domain.knowledge.KnowledgeBase;
import com.ai.cs.domain.knowledge.KnowledgeChunk;
import com.ai.cs.domain.knowledge.KnowledgeDocument;
import com.ai.cs.domain.knowledge.repository.KnowledgeBaseRepository;
import com.ai.cs.domain.knowledge.repository.KnowledgeChunkRepository;
import com.ai.cs.domain.knowledge.repository.KnowledgeDocumentRepository;
import com.ai.cs.infrastructure.storage.DocumentParserService;
import com.ai.cs.infrastructure.storage.TextSplitter;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

    @Mock
    private KnowledgeBaseRepository kbRepository;

    @Mock
    private KnowledgeDocumentRepository docRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private DocumentParserService parserService;

    @Mock
    private TextSplitter textSplitter;

    private KnowledgeBaseService kbService;

    @BeforeEach
    void setUp() {
        kbService = new KnowledgeBaseService(kbRepository, docRepository, chunkRepository,
                parserService, textSplitter);
    }

    @Test
    void listAll_shouldReturnAllKnowledgeBases() {
        KnowledgeBase kb1 = new KnowledgeBase();
        kb1.setId(1L);
        kb1.setName("知识库1");
        KnowledgeBase kb2 = new KnowledgeBase();
        kb2.setId(2L);
        kb2.setName("知识库2");
        when(kbRepository.findAll()).thenReturn(List.of(kb1, kb2));

        List<KnowledgeBase> result = kbService.listAll();
        assertEquals(2, result.size());
        assertEquals("知识库1", result.get(0).getName());
        assertEquals("知识库2", result.get(1).getName());
    }

    @Test
    void getById_shouldReturnKnowledgeBase() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setName("知识库1");
        when(kbRepository.findById(1L)).thenReturn(Optional.of(kb));

        KnowledgeBase result = kbService.getById(1L);
        assertEquals("知识库1", result.getName());
    }

    @Test
    void getById_shouldThrowExceptionForNonExistent() {
        when(kbRepository.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> kbService.getById(999L));
        assertEquals(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void create_shouldSaveAndReturnKnowledgeBase() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setName("新知识库");
        kb.setDescription("描述");
        when(kbRepository.save(any())).thenAnswer(invocation -> {
            KnowledgeBase saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        KnowledgeBase result = kbService.create(kb);
        assertNotNull(result.getId());
        assertEquals("新知识库", result.getName());
    }

    @Test
    void getDocuments_shouldReturnDocuments() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(1L);
        doc.setKbId(1L);
        doc.setFileName("test.pdf");
        when(docRepository.findByKbId(1L)).thenReturn(List.of(doc));

        List<KnowledgeDocument> result = kbService.getDocuments(1L);
        assertEquals(1, result.size());
        assertEquals("test.pdf", result.get(0).getFileName());
    }

    @Test
    void getChunks_shouldReturnChunks() {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(1L);
        chunk.setDocId(1L);
        chunk.setContent("chunk content");
        when(chunkRepository.findByDocId(1L)).thenReturn(List.of(chunk));

        List<KnowledgeChunk> result = kbService.getChunks(1L);
        assertEquals(1, result.size());
        assertEquals("chunk content", result.get(0).getContent());
    }
}
