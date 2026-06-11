package com.ai.cs.infrastructure.storage;

import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserServiceTest {

    private DocumentParserService parserService;

    @BeforeEach
    void setUp() {
        parserService = new DocumentParserService(List.of(
                new TxtParser(),
                new PdfParserStub()
        ));
    }

    @Test
    void parse_shouldReturnTextForTxtType() {
        String content = "Hello, this is a text file.";
        InputStream is = new ByteArrayInputStream(content.getBytes());
        String result = parserService.parse("TXT", is);
        assertEquals(content, result);
    }

    @Test
    void parse_shouldReturnTextForMdType() {
        String content = "# Markdown Content";
        InputStream is = new ByteArrayInputStream(content.getBytes());
        String result = parserService.parse("MD", is);
        assertEquals(content, result);
    }

    @Test
    void parse_shouldThrowExceptionForUnsupportedType() {
        InputStream is = new ByteArrayInputStream("test".getBytes());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> parserService.parse("UNSUPPORTED", is));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("不支持的文件类型"));
    }

    @Test
    void parse_shouldThrowExceptionForParseFailure() {
        DocumentParser failingParser = new DocumentParser() {
            @Override public String[] supportedTypes() { return new String[]{"FAIL"}; }
            @Override public String parse(InputStream inputStream) throws Exception {
                throw new RuntimeException("parse error");
            }
        };
        DocumentParserService service = new DocumentParserService(List.of(failingParser));
        InputStream is = new ByteArrayInputStream("test".getBytes());
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.parse("FAIL", is));
        assertEquals(ErrorCode.DOCUMENT_PARSE_FAILED.getCode(), ex.getCode());
    }

    /**
     * A stub PdfParser that works without PDFBox dependency for testing the orchestrator.
     */
    private static class PdfParserStub implements DocumentParser {
        @Override public String[] supportedTypes() { return new String[]{"PDF"}; }
        @Override
        public String parse(InputStream inputStream) throws Exception {
            return "Stub PDF content";
        }
    }
}
