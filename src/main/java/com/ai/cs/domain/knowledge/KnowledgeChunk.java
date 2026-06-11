package com.ai.cs.domain.knowledge;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "knowledge_chunk")
public class KnowledgeChunk extends BaseEntity {

    @Column(name = "doc_id")
    private Long docId;

    @Column(name = "kb_id")
    private Long kbId;

    @Column(name = "content", columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "chunk_index")
    private Integer chunkIndex;

    @Column(name = "es_doc_id")
    private String esDocId;

    @Column(name = "embedding_status")
    private String embeddingStatus = "PENDING";
}
