package com.ai.cs.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Slf4j
@Component
public class WordParser implements DocumentParser {

    @Override
    public String[] supportedTypes() {
        return new String[]{"DOCX", "DOC"};
    }

    @Override
    public String parse(InputStream inputStream) throws Exception {
        byte[] bytes = inputStream.readAllBytes();
        StringBuilder text = new StringBuilder();

        // Try DOCX first, fall back to DOC
        try {
            parseDocx(bytes, text);
        } catch (Exception docxEx) {
            log.debug("不是DOCX格式，尝试DOC: {}", docxEx.getMessage());
            text.setLength(0);
            parseDoc(bytes, text);
        }

        log.info("Word解析完成: length={}", text.length());
        return text.toString();
    }

    private void parseDocx(byte[] bytes, StringBuilder text) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            for (var para : doc.getParagraphs()) {
                String paraText = para.getText().trim();
                if (!paraText.isEmpty()) {
                    text.append(paraText).append("\n");
                }
            }
        }
    }

    private void parseDoc(byte[] bytes, StringBuilder text) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(bytes))) {
            var range = doc.getRange();
            for (int i = 0; i < range.numParagraphs(); i++) {
                var para = range.getParagraph(i);
                String paraText = para.text().trim();
                if (!paraText.isEmpty()) {
                    text.append(paraText).append("\n");
                }
            }
        }
    }
}
