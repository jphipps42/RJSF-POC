package com.egs.rjsf.transformer.registry;

import com.egs.rjsf.transformer.config.TransformerProperties;
import com.egs.rjsf.transformer.exception.TemplateNotFoundException;
import com.egs.rjsf.transformer.model.TransformerTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class FileSystemTemplateLoader implements TemplateLoader, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(FileSystemTemplateLoader.class);
    private static final String TEMPLATE_SUFFIX = ".transformer.json";

    private final TransformerProperties properties;
    private final ObjectMapper objectMapper;

    private Consumer<String> changeCallback;
    private volatile boolean running = false;
    private Thread watchThread;
    private WatchService watchService;

    public FileSystemTemplateLoader(TransformerProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public TransformerTemplate load(String formId) {
        String dir = properties.getTemplate().getDir();
        String filename = formId + TEMPLATE_SUFFIX;

        try {
            InputStream inputStream;
            if (isClasspath(dir)) {
                String classpathLocation = dir.substring("classpath:".length());
                String resourcePath = classpathLocation + "/" + filename;
                ClassPathResource resource = new ClassPathResource(resourcePath);
                if (!resource.exists()) {
                    throw new TemplateNotFoundException(formId);
                }
                inputStream = resource.getInputStream();
            } else {
                Path filePath = Path.of(dir, filename);
                if (!Files.exists(filePath)) {
                    throw new TemplateNotFoundException(formId);
                }
                inputStream = Files.newInputStream(filePath);
            }

            try (inputStream) {
                return objectMapper.readValue(inputStream, TransformerTemplate.class);
            }
        } catch (TemplateNotFoundException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to load transformer template for formId: {}", formId, e);
            throw new TemplateNotFoundException(formId);
        }
    }

    @Override
    public List<String> list() {
        String dir = properties.getTemplate().getDir();
        List<String> formIds = new ArrayList<>();

        try {
            if (isClasspath(dir)) {
                String classpathLocation = dir.substring("classpath:".length());
                String pattern = "classpath:" + classpathLocation + "/*" + TEMPLATE_SUFFIX;
                PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources(pattern);
                for (Resource resource : resources) {
                    String filename = resource.getFilename();
                    if (filename != null && filename.endsWith(TEMPLATE_SUFFIX)) {
                        formIds.add(filename.substring(0, filename.length() - TEMPLATE_SUFFIX.length()));
                    }
                }
            } else {
                Path dirPath = Path.of(dir);
                if (Files.isDirectory(dirPath)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath, "*" + TEMPLATE_SUFFIX)) {
                        for (Path entry : stream) {
                            String filename = entry.getFileName().toString();
                            formIds.add(filename.substring(0, filename.length() - TEMPLATE_SUFFIX.length()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to list transformer templates in dir: {}", dir, e);
        }

        return formIds;
    }

    @Override
    public void watch(Consumer<String> onChanged) {
        this.changeCallback = onChanged;
    }

    // --- SmartLifecycle ---

    @Override
    public void start() {
        String dir = properties.getTemplate().getDir();
        boolean hotReload = properties.getTemplate().isHotReload();

        if (!hotReload) {
            log.info("Template hot-reload is disabled");
            return;
        }

        if (isClasspath(dir)) {
            log.info("Template directory is classpath-based ({}); hot-reload is not supported for classpath resources", dir);
            return;
        }

        Path dirPath = Path.of(dir);
        if (!Files.isDirectory(dirPath)) {
            log.warn("Template directory does not exist, skipping hot-reload watch: {}", dir);
            return;
        }

        try {
            watchService = FileSystems.getDefault().newWatchService();
            dirPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

            watchThread = new Thread(() -> {
                log.info("Template file watcher started on directory: {}", dir);
                while (running) {
                    try {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            if (kind == StandardWatchEventKinds.OVERFLOW) {
                                continue;
                            }
                            Path changed = (Path) event.context();
                            String filename = changed.toString();
                            if (filename.endsWith(TEMPLATE_SUFFIX) && changeCallback != null) {
                                String formId = filename.substring(0, filename.length() - TEMPLATE_SUFFIX.length());
                                log.info("Detected change in template file: {}", filename);
                                changeCallback.accept(formId);
                            }
                        }
                        boolean valid = key.reset();
                        if (!valid) {
                            log.warn("Watch key is no longer valid, stopping template watcher");
                            break;
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (ClosedWatchServiceException e) {
                        break;
                    }
                }
                log.info("Template file watcher stopped");
            }, "template-file-watcher");
            watchThread.setDaemon(true);
            running = true;
            watchThread.start();

        } catch (IOException e) {
            log.error("Failed to start template file watcher on directory: {}", dir, e);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                log.warn("Error closing watch service", e);
            }
        }
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private boolean isClasspath(String dir) {
        return dir != null && dir.startsWith("classpath:");
    }
}
