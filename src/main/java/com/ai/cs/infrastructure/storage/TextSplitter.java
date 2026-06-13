package com.ai.cs.infrastructure.storage;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文本分片器 — 基于 LangChain4j HierarchicalDocumentSplitter。
 * 分层策略：段落 → 句子 → 字符，超长段落自动细粒度切分。
 */
@Component
public class TextSplitter {

    /** 每段最大 token 数，远低于 Embedding API 的 8192 限制 */
    private static final int MAX_SEGMENT_TOKENS = 2000;
    /** 相邻段重叠 token 数 */
    private static final int OVERLAP_TOKENS = 100;

    private final DocumentByParagraphSplitter splitter = new DocumentByParagraphSplitter(
            MAX_SEGMENT_TOKENS, OVERLAP_TOKENS);

    public List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();

        return splitter.split(Document.from(text)).stream()
                .map(seg -> seg.text().trim())
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
