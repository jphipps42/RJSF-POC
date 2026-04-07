package com.egs.rjsf.transformer.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);
    private final ApplicationContext applicationContext;

    public HookRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<WriteHook> resolveWriteHooks(List<String> beanNames) {
        List<WriteHook> hooks = new ArrayList<>();
        for (String name : beanNames) {
            try {
                hooks.add(applicationContext.getBean(name, WriteHook.class));
            } catch (Exception e) {
                log.warn("Write hook bean '{}' not found, skipping", name);
            }
        }
        return hooks;
    }

    public List<ReadHook> resolveReadHooks(List<String> beanNames) {
        List<ReadHook> hooks = new ArrayList<>();
        for (String name : beanNames) {
            try {
                hooks.add(applicationContext.getBean(name, ReadHook.class));
            } catch (Exception e) {
                log.warn("Read hook bean '{}' not found, skipping", name);
            }
        }
        return hooks;
    }
}
