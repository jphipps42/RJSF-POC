package com.egs.rjsf.transformer.registry;

import com.egs.rjsf.transformer.model.TransformerTemplate;

import java.util.List;
import java.util.function.Consumer;

public interface TemplateLoader {

    TransformerTemplate load(String formId);

    List<String> list();

    void watch(Consumer<String> onChanged);
}
