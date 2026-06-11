package com.ai.cs.shared.dto;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void ofShouldCreateCorrectPageResult() {
        List<String> records = List.of("a", "b");
        PageResult<String> result = PageResult.of(records, 10, 1, 2);
        assertEquals(records, result.getRecords());
        assertEquals(10, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getPageSize());
    }
}
