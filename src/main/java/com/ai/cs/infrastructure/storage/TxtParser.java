package com.ai.cs.infrastructure.storage;

import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class TxtParser implements DocumentParser {
    @Override public String[] supportedTypes() { return new String[]{"TXT", "MD"}; }
    @Override
    public String parse(InputStream inputStream) throws Exception {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
}
