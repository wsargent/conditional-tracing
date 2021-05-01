package com.tersesystems.dynamicspans;

import io.opentelemetry.api.trace.SpanBuilder;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

class ConditionManager {
    static final ScriptEngineManager factory = new ScriptEngineManager();

    public ConditionalSpanBuilder conditionalSpan(ConditionSource source, SpanBuilder spanBuilder) throws ScriptException {
        final ScriptEngine engine = factory.getEngineByName("groovy");
        return new ConditionalSpanBuilder(engine, spanBuilder, source);
    }
}
