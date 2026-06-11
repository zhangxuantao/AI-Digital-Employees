package com.ai.cs.domain.knowledge;

import com.ai.cs.infrastructure.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    @Column(name = "kb_id")
    private Long kbId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "status")
    private String status = "PENDING";

    @Column(name = "chunk_count")
    private Integer chunkCount = 0;
}
