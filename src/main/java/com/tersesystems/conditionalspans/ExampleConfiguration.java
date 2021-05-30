package com.tersesystems.conditionalspans;

import io.honeycomb.opentelemetry.DistroMetadata;
import io.honeycomb.opentelemetry.EnvironmentConfiguration;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerProvider;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.nio.file.Path;

import static io.honeycomb.opentelemetry.EnvironmentConfiguration.*;
import static java.util.Objects.requireNonNull;

class ExampleConfiguration {

    static final String serviceName = "conditional-spans";

    static OpenTelemetry initOpenTelemetry(Path samplerPath, Path spanBuilderPath) {
        return honeycombBuilder(samplerPath, spanBuilderPath);
    }

    static Sampler sampler(Path path) {
        try {
            final ConditionSource conditionSource = new FileConditionSource(path);
            return new ConditionalSampler(conditionSource);
        } catch (IOException ie) {
            ie.printStackTrace();
            return Sampler.alwaysOn();
        }
    }

    static SpanProcessor loggingSpanProcessor() {
         var exporter = new LoggingSpanExporter();
         return SimpleSpanProcessor.create(exporter);
     }

     static SpanProcessor otlpProcessor() {
         OtlpGrpcSpanExporter exporter = OtlpGrpcSpanExporter.builder().build();
         return SimpleSpanProcessor.create(exporter);
     }

    static OpenTelemetry honeycombBuilder(Path samplerPath, Path spanBuilderPath) {
        String apiKey = requireNonNull(getHoneycombApiKey());
        String dataset = "dynamic-spans"; // requireNonNull(getHoneycombDataset());

        SpanExporter exporter = OtlpGrpcSpanExporter.builder().setEndpoint(DEFAULT_HONEYCOMB_ENDPOINT)
                .addHeader(HONEYCOMB_TEAM_HEADER, apiKey)
                .addHeader(EnvironmentConfiguration.HONEYCOMB_DATASET_HEADER, dataset)
                .build();

        // .addSpanProcessor(loggingSpanProcessor())
        SdkTracerProviderBuilder tracerProviderBuilder = SdkTracerProvider.builder()
                .setSampler(sampler(samplerPath))
                .addSpanProcessor(BatchSpanProcessor.builder(exporter).build());

        AttributesBuilder resourceAttributes = Attributes.builder();
        DistroMetadata.getMetadata().forEach(resourceAttributes::put);
        resourceAttributes.put(EnvironmentConfiguration.SERVICE_NAME_FIELD, serviceName);
        tracerProviderBuilder.setResource(
                Resource.create(resourceAttributes.build()));

        ContextPropagators propagators = ContextPropagators.create(
                TextMapPropagator.composite(
                        W3CTraceContextPropagator.getInstance(),
                        W3CBaggagePropagator.getInstance()));

        OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProviderBuilder.build())
                .setPropagators(propagators).build();

        return new ConditionalOpenTelemetry(sdk, spanBuilderPath);
    }

    private static void shutdownHook(SdkTracerProvider sdkTracerProvider) {
        Thread t = new Thread(
                () -> {
                    System.err.println(
                            "*** forcing the Span Exporter to shutdown and process the remaining spans");
                    sdkTracerProvider.shutdown();
                    System.err.println("*** Trace Exporter shut down");
                });
        Runtime.getRuntime().addShutdownHook(t);
    }

    static final class ConditionalOpenTelemetry implements OpenTelemetry {
        private final OpenTelemetrySdk sdk;
        private final Path spanBuilderPath;

        public ConditionalOpenTelemetry(OpenTelemetrySdk sdk, Path spanBuilderPath) {
            this.sdk = sdk;
            this.spanBuilderPath = spanBuilderPath;
        }

        @Override
        public TracerProvider getTracerProvider() {
            try {
                var source = new FileConditionSource(spanBuilderPath);
                return new ConditionalTracerProvider(sdk.getTracerProvider(), source);
            } catch (IOException e) {
                throw new EvaluationException(e);
            }
        }

        @Override
        public ContextPropagators getPropagators() {
            return sdk.getPropagators();
        }
    }

    @ThreadSafe
    static class ConditionalTracerProvider implements TracerProvider {
        private final TracerProvider underlying;
        private final ConditionSource source;

        ConditionalTracerProvider(TracerProvider underlying, ConditionSource source) {
            this.underlying = underlying;
            this.source = source;
        }

        @Override
        public Tracer get(String instrumentationName) {
            return get(instrumentationName, null);
        }

        @Override
        public Tracer get(String instrumentationName, String instrumentationVersion) {
            return new Tracer() {
                @Override
                public SpanBuilder spanBuilder(String spanName) {
                    Tracer tracer = underlying.get(instrumentationName, instrumentationVersion);
                    return new ConditionalSpanBuilder(spanName, tracer, source);
                }
            };
        }
    }

}
