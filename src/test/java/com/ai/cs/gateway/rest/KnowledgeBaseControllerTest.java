package com.ai.cs.gateway.rest;

import com.ai.cs.application.knowledge.KnowledgeBaseService;
import com.ai.cs.domain.knowledge.KnowledgeBase;
import com.ai.cs.domain.knowledge.KnowledgeDocument;
import com.ai.cs.domain.knowledge.KnowledgeChunk;
import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import com.ai.cs.shared.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = KnowledgeBaseController.class)
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(authorities = "knowledge:view")
@ActiveProfiles("test")
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeBaseService kbService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private KnowledgeBase createTestKb(Long id, String name) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setName(name);
        kb.setDescription("Description of " + name);
        return kb;
    }

    @Test
    void list_shouldReturnAllKnowledgeBases() throws Exception {
        List<KnowledgeBase> kbs = List.of(
                createTestKb(1L, "知识库1"),
                createTestKb(2L, "知识库2")
        );
        when(kbService.listAll()).thenReturn(kbs);

        mockMvc.perform(get("/api/v1/knowledge-bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("知识库1"))
                .andExpect(jsonPath("$.data[1].id").value(2))
                .andExpect(jsonPath("$.data[1].name").value("知识库2"));
    }

    @Test
    void getById_shouldReturnKnowledgeBase() throws Exception {
        KnowledgeBase kb = createTestKb(1L, "知识库1");
        when(kbService.getById(1L)).thenReturn(kb);

        mockMvc.perform(get("/api/v1/knowledge-bases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("知识库1"));
    }

    @Test
    void getById_shouldReturnErrorForNonExistent() throws Exception {
        when(kbService.getById(999L))
                .thenThrow(new BusinessException(ErrorCode.KNOWLEDGE_BASE_NOT_FOUND));

        mockMvc.perform(get("/api/v1/knowledge-bases/999"))
                .andExpect(jsonPath("$.code").value(1002))
                .andExpect(jsonPath("$.message").value("知识库不存在"));
    }

    @Test
    @WithMockUser(authorities = {"knowledge:view", "knowledge:edit"})
    void create_shouldReturnCreatedKnowledgeBase() throws Exception {
        KnowledgeBase saved = createTestKb(1L, "新知识库");
        when(kbService.create(any())).thenReturn(saved);

        mockMvc.perform(post("/api/v1/knowledge-bases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新知识库\",\"description\":\"描述信息\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("新知识库"));
    }

    @Test
    @WithMockUser(authorities = {"knowledge:view", "knowledge:edit"})
    void upload_shouldReturnDocument() throws Exception {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(1L);
        doc.setKbId(1L);
        doc.setFileName("test.txt");
        doc.setFileType("TXT");
        doc.setStatus("DONE");
        when(kbService.uploadDocument(eq(1L), any())).thenReturn(doc);

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "test content".getBytes());

        mockMvc.perform(multipart("/api/v1/knowledge-bases/1/documents")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fileName").value("test.txt"))
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    @Test
    void getDocuments_shouldReturnDocumentList() throws Exception {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(1L);
        doc.setKbId(1L);
        doc.setFileName("test.pdf");
        doc.setFileType("PDF");
        doc.setStatus("DONE");
        when(kbService.getDocuments(1L)).thenReturn(List.of(doc));

        mockMvc.perform(get("/api/v1/knowledge-bases/1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].fileName").value("test.pdf"))
                .andExpect(jsonPath("$.data[0].status").value("DONE"));
    }

    @Test
    void getChunks_shouldReturnChunkList() throws Exception {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(1L);
        chunk.setDocId(1L);
        chunk.setContent("chunk content");
        chunk.setChunkIndex(0);
        when(kbService.getChunks(1L)).thenReturn(List.of(chunk));

        mockMvc.perform(get("/api/v1/knowledge-bases/1/documents/1/chunks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].content").value("chunk content"))
                .andExpect(jsonPath("$.data[0].chunkIndex").value(0));
    }
}
