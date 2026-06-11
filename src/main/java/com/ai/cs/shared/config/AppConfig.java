package com.ai.cs.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private Storage storage = new Storage();
    private Security security = new Security();
    private Long defaultTenantId = 1L;

    @Data
    public static class Storage {
        private String basePath = "./data";
    }

    @Data
    public static class Security {
        private String jwtSecret = "change-me-in-production";
        private long jwtExpiration = 86400000;
    }
}
