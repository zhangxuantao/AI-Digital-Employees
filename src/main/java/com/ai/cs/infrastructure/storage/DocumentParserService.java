package com.ai.cs.infrastructure.storage;

import com.ai.cs.shared.exception.BusinessException;
import com.ai.cs.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParserService {
    private final List<DocumentParser> parsers;

    public String parse(String fileType, InputStream inputStream) {
        for (DocumentParser parser : parsers) {
            for (String type : parser.supportedTypes()) {
                if (type.equalsIgnoreCase(fileType)) {
                    try {
                        return parser.parse(inputStream);
                    } catch (Exception e) {
                        log.error("文档解析失败: type={}", fileType, e);
                        throw new BusinessException(ErrorCode.DOCUMENT_PARSE_FAILED);
                    }
                }
            }
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型: " + fileType);
    }
}
