package com.egs.rjsf.transformer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "transformer")
@Getter
@Setter
public class TransformerProperties {

    private TemplateProperties template = new TemplateProperties();
    private CacheProperties cache = new CacheProperties();

    @Getter
    @Setter
    public static class TemplateProperties {
        private String dir = "classpath:templates";
        private boolean hotReload = true;
    }

    @Getter
    @Setter
    public static class CacheProperties {
        private int maxSize = 500;
    }
}
