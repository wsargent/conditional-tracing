package com.tersesystems.conditionalspans;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import javax.annotation.concurrent.ThreadSafe;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.List;

/**
 * This class calls out to a groovy script to do sampling, so we can change sampling on the fly.
 * <p>
 * Note that sampling is hierarchical, so if a parent is dropped then a child span that is sampled
 * will not have a valid parent.
 */
@ThreadSafe
public class ConditionalSampler implements Sampler {
    private static final ScriptEngineManager factory = new ScriptEngineManager();
    private static final String engineName = "groovy";

    private static final boolean rethrow = true;

    private final ScriptEngine engine = factory.getEngineByName(engineName);
    private final ConditionSource source;
    private static final String methodName = "shouldSample";

    public ConditionalSampler(ConditionSource source) {
        this.source = source;
        eval();
    }

    @Override
    public SamplingResult shouldSample(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        if (source.isInvalid()) {
            eval();
        }
        try {
            var instance = new SamplingInstance(parentContext, traceId, name, spanKind, attributes, parentLinks);
            Invocable inv = ((Invocable) engine);
            var result = (SamplingResult) inv.invokeFunction(methodName, instance);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            if (rethrow) {
                throw new EvaluationException(e);
            }
            return SamplingResult.create(SamplingDecision.RECORD_AND_SAMPLE);
        }
    }

    @Override
    public String getDescription() {
        return "ConditionalSampler(source=" + source.toString() + ")";
    }

    private void eval() throws EvaluationException {
        try {
            engine.eval(source.getReader());
        } catch (ScriptException e) {
            throw new EvaluationException(e);
        }
    }

}
