package com.ai.cs.domain.knowledge.repository;

import com.ai.cs.domain.knowledge.KnowledgeChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {

    List<KnowledgeChunk> findByDocId(Long docId);

    Page<KnowledgeChunk> findByDocId(Long docId, Pageable pageable);

    void deleteByDocId(Long docId);

    void deleteByKbId(Long kbId);
}
