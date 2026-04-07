package com.egs.rjsf.transformer.transform;

import com.egs.rjsf.transformer.model.FieldMapping;

@FunctionalInterface
public interface ToDbFunction {
    Object apply(Object jsValue, FieldMapping field);
}
