package com.tersesystems.conditionalspans;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;

import java.util.List;

public final class SamplingInstance {
    private final Context parentContext;
    private final String traceId;
    private final String name;
    private final SpanKind spanKind;
    private final Attributes attributes;
    private final List<LinkData> parentLinks;

    SamplingInstance(Context parentContext, String traceId, String name, SpanKind spanKind, Attributes attributes, List<LinkData> parentLinks) {
        this.parentContext = parentContext;
        this.traceId = traceId;
        this.name = name;
        this.spanKind = spanKind;
        this.attributes = attributes;
        this.parentLinks = parentLinks;
    }

    public Context getParentContext() {
        return parentContext;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getName() {
        return name;
    }

    public SpanKind getSpanKind() {
        return spanKind;
    }

    public Attributes getAttributes() {
        return attributes;
    }

    public List<LinkData> getParentLinks() {
        return parentLinks;
    }

    @Override
    public String toString() {
        return "SamplingInstance{" +
                "parentContext=" + parentContext +
                ", traceId='" + traceId + '\'' +
                ", name='" + name + '\'' +
                ", spanKind=" + spanKind +
                ", attributes=" + attributes +
                ", parentLinks=" + parentLinks +
                '}';
    }
}