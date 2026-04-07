package com.egs.rjsf.transformer.transform;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class TransformRegistry {

    private static final Logger log = LoggerFactory.getLogger(TransformRegistry.class);

    private final ApplicationContext applicationContext;
    private final Map<String, ToDbFunction> toDbFunctions = new LinkedHashMap<>();
    private final Map<String, FromDbFunction> fromDbFunctions = new LinkedHashMap<>();

    public TransformRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void init() {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(TransformFunction.class);

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            Object bean = entry.getValue();
            TransformFunction annotation = bean.getClass().getAnnotation(TransformFunction.class);
            if (annotation == null) {
                continue;
            }
            String name = annotation.value();

            if (bean instanceof ToDbFunction toDb) {
                toDbFunctions.put(name, toDb);
                log.info("Registered toDb transform: {}", name);
            }
            if (bean instanceof FromDbFunction fromDb) {
                fromDbFunctions.put(name, fromDb);
                log.info("Registered fromDb transform: {}", name);
            }
        }

        log.info("Transform registry initialized — toDb: {}, fromDb: {}",
                toDbFunctions.keySet(), fromDbFunctions.keySet());
    }

    public Optional<ToDbFunction> getToDb(String name) {
        return Optional.ofNullable(toDbFunctions.get(name));
    }

    public Optional<FromDbFunction> getFromDb(String name) {
        return Optional.ofNullable(fromDbFunctions.get(name));
    }
}
