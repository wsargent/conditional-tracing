package com.tersesystems.dynamicspans;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.context.Context;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.concurrent.TimeUnit;

class ConditionalSpanBuilder implements SpanBuilder {
    private final ScriptEngine engine;
    private final ConditionSource source;
    private final SpanBuilder spanBuilder;
    private final String evaluationName = "sayHello";

    ConditionalSpanBuilder(ScriptEngine engine, SpanBuilder spanBuilder, ConditionSource source) throws ScriptException {
        this.engine = engine;
        this.spanBuilder = spanBuilder;
        this.source = source;
        eval();
    }

    @Override
    public SpanBuilder setParent(Context context) {
        return spanBuilder.setParent(context);
    }

    @Override
    public SpanBuilder setNoParent() {
        return spanBuilder.setNoParent();
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext) {
        return spanBuilder.addLink(spanContext);
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext, Attributes attributes) {
        return spanBuilder.addLink(spanContext, attributes);
    }

    @Override
    public SpanBuilder setAttribute(String key, String value) {
        return spanBuilder.setAttribute(key, value);
    }

    @Override
    public SpanBuilder setAttribute(String key, long value) {
        return spanBuilder.setAttribute(key, value);
    }

    @Override
    public SpanBuilder setAttribute(String key, double value) {
        return spanBuilder.setAttribute(key, value);
    }

    @Override
    public SpanBuilder setAttribute(String key, boolean value) {
        return spanBuilder.setAttribute(key, value);
    }

    @Override
    public <T> SpanBuilder setAttribute(AttributeKey<T> key, T value) {
        return spanBuilder.setAttribute(key, value);
    }

    @Override
    public SpanBuilder setSpanKind(SpanKind spanKind) {
        return spanBuilder.setSpanKind(spanKind);
    }

    @Override
    public SpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit) {
        return spanBuilder.setStartTimestamp(startTimestamp, unit);
    }

    @Override
    public Span startSpan() {
        final boolean result = evaluate();
        if (result) {
            return spanBuilder.startSpan();
        } else {
            final Tracer tracer = TracerProvider.noop().get("");
            return tracer.spanBuilder("").startSpan();
        }
    }

    protected boolean evaluate() {
        try {
            if (source.isInvalid()) {
                eval();
            }
            Object result = ((Invocable) engine).invokeFunction(evaluationName);
            return (Boolean) result;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void eval() throws ScriptException {
        engine.eval(source.getReader());
    }

}
