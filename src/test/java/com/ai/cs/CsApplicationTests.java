package com.ai.cs;

import com.ai.cs.infrastructure.cache.CacheService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class CsApplicationTests {

    @MockBean
    private CacheService cacheService;

    @Test
    void contextLoads() {
    }
}
