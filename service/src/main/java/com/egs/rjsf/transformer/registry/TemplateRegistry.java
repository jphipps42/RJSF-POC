package com.egs.rjsf.transformer.registry;

import com.egs.rjsf.transformer.config.TransformerProperties;
import com.egs.rjsf.transformer.ddl.DdlManager;
import com.egs.rjsf.transformer.model.TransformerTemplate;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TemplateRegistry {

    private static final Logger log = LoggerFactory.getLogger(TemplateRegistry.class);

    private final TemplateLoader loader;
    private final TemplateValidator validator;
    private final TransformerProperties properties;
    private final DdlManager ddlManager;
    private final ConcurrentHashMap<String, TransformerTemplate> cache = new ConcurrentHashMap<>();

    public TemplateRegistry(TemplateLoader loader, TemplateValidator validator,
                            TransformerProperties properties, DdlManager ddlManager) {
        this.loader = loader;
        this.validator = validator;
        this.properties = properties;
        this.ddlManager = ddlManager;
    }

    @PostConstruct
    void init() {
        log.info("Initializing template registry (maxCacheSize: {})", properties.getCache().getMaxSize());

        List<String> formIds = loader.list();
        log.info("Found {} transformer template(s) to load", formIds.size());

        for (String formId : formIds) {
            try {
                loadAndCache(formId);
            } catch (Exception e) {
                log.error("Failed to load template during initialization: {}", formId, e);
            }
        }

        loader.watch(this::reloadTemplate);
        log.info("Template registry initialized with {} template(s)", cache.size());
    }

    public TransformerTemplate getTemplate(String formId) {
        TransformerTemplate cached = cache.get(formId);
        if (cached != null) {
            return cached;
        }

        return loadAndCache(formId);
    }

    public List<TransformerTemplate> getAllTemplates() {
        List<String> formIds = loader.list();
        return formIds.stream()
                .map(this::getTemplate)
                .toList();
    }

    public void invalidate(String formId) {
        cache.remove(formId);
        log.info("Invalidated cached template: {}", formId);
    }

    public void reloadTemplate(String formId) {
        log.info("Reloading template: {}", formId);
        invalidate(formId);
        try {
            loadAndCache(formId);
        } catch (Exception e) {
            log.error("Failed to reload template: {}", formId, e);
        }
    }

    private TransformerTemplate loadAndCache(String formId) {
        TransformerTemplate template = loader.load(formId);
        validator.validate(template);
        cache.put(formId, template);
        log.info("Loaded and cached template: {} (version {})", formId, template.version());

        try {
            ddlManager.reconcile(template);
        } catch (Exception e) {
            log.error("DDL reconciliation failed for template '{}': {}", formId, e.getMessage(), e);
        }

        return template;
    }
}
