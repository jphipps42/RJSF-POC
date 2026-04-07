package com.egs.rjsf.transformer.hook;

import java.util.Map;

@FunctionalInterface
public interface ReadHook {
    Map<String, Object> apply(Map<String, Object> data);
}
