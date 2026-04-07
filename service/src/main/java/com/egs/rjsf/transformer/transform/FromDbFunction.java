package com.egs.rjsf.transformer.transform;

import com.egs.rjsf.transformer.model.FieldMapping;

@FunctionalInterface
public interface FromDbFunction {
    Object apply(Object dbValue, FieldMapping field);
}
