package com.ai.cs.infrastructure.storage;

import java.io.InputStream;

public interface DocumentParser {
    String[] supportedTypes();
    String parse(InputStream inputStream) throws Exception;
}
