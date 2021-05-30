package com.tersesystems.conditionalspans;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

class ConditionalSpanBuilder implements SpanBuilder {
    private static final ScriptEngineManager factory = new ScriptEngineManager();
    private static final String engineName = "groovy";
    private static final String evaluationName = "shouldStartSpan";

    private final SpanBuilder spanBuilder;
    private final ScriptEngine engine;
    private final ConditionSource source;

    private final String spanName;

    private Context parent;
    private HashMap<AttributeKey<?>, Object> attributes = new HashMap<>();

    ConditionalSpanBuilder(String spanName, Tracer tracer, ConditionSource source) throws EvaluationException {
        engine = factory.getEngineByName(engineName);
        this.spanBuilder = tracer.spanBuilder(spanName);
        this.source = source;
        this.spanName = spanName;
        eval();
    }

    @Override
    public SpanBuilder setParent(Context parent) {
        this.parent = parent;
        spanBuilder.setParent(parent);
        return this;
    }

    @Override
    public SpanBuilder setNoParent() {
        spanBuilder.setNoParent();
        return this;
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext) {
        spanBuilder.addLink(spanContext);
        return this;
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext, Attributes attributes) {
        spanBuilder.addLink(spanContext, attributes);
        return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, String value) {
        this.attributes.put(AttributeKey.stringKey(key), value);
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, long value) {
        this.attributes.put(AttributeKey.longKey(key), value);
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, double value) {
        this.attributes.put(AttributeKey.doubleKey(key), value);
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, boolean value) {
        this.attributes.put(AttributeKey.booleanKey(key), value);
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public <T> SpanBuilder setAttribute(AttributeKey<T> key, T value) {
        this.attributes.put(key, value);
        spanBuilder.setAttribute(key, value);
        return this;
    }

    @Override
    public SpanBuilder setSpanKind(SpanKind spanKind) {
        spanBuilder.setSpanKind(spanKind);
        return this;
    }

    @Override
    public SpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit) {
        spanBuilder.setStartTimestamp(startTimestamp, unit);
        return this;
    }

    @Override
    public Span startSpan() {
        final boolean result = evaluate(spanName, parent, attributes);
        if (result) {
            return spanBuilder.startSpan();
        } else {
            return Span.current();
        }
    }

    protected boolean evaluate(String spanName, Context parent, HashMap<AttributeKey<?>, Object> attributes) {
        try {
            if (source.isInvalid()) {
                eval();
            }
            Object result = ((Invocable) engine).invokeFunction(evaluationName, spanName, parent, attributes);
            return (Boolean) result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void eval() {
        try {
            engine.eval(source.getReader());
        } catch (ScriptException e) {
            throw new EvaluationException(e);
        }
    }

}
