package com.tersesystems.conditionalspans;

import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.LDClient;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

public class Main {

    private static final Path samplerPath = Paths.get("src/main/groovy/sampler.groovy");
    private static final Path spanBuilderPath = Paths.get("src/main/groovy/spanbuilder.groovy");
    private static final OpenTelemetry openTelemetry = ExampleConfiguration.initOpenTelemetry(samplerPath, spanBuilderPath);

    private static final Tracer tracer = openTelemetry.getTracer("hello-i-am-instrumentation");
    private static final LDClient ldClient = new LDClient(requireNonNull(System.getenv("LD_API_KEY")));

    public static final ContextKey<LDClient> LD_CLIENT_KEY = ContextKey.named("ldClient");
    public static final ContextKey<LDUser> LD_USER_KEY = ContextKey.named("ldUser");

    public static void main(String[] args) throws Exception {
        LDUser user = new LDUser("user@test.com");
        Context withLD = Context.current()
                .with(LD_CLIENT_KEY, ldClient)
                .with(LD_USER_KEY, user);

        try (var ignored = withLD.makeCurrent()) {
            DoStuff doStuff = new DoStuff();
            for (int i = 0; i < 1000; i++) {
                doStuff.level1();
                Thread.sleep(1000L);
            }
        }

        ldClient.close();
    }

    static class DoStuff {

        public void level1() {
            wrap("DoStuff/level1", span -> {
                System.out.println("level 1");
                level2();
            });
        }

        public void level2() {
            wrapDebug("DoStuff/level2", span -> {
                System.out.println("level 2");
                level3();
            });
        }

        public void level3() {
            wrap("DoStuff/level3", span -> {
                System.out.println("level 3");
                level4();
            });
        }

        public void level4() {
            wrapDebug("DoStuff/level4", span -> {
                System.out.println("level 4");
            });
        }

        public void wrap(String spanName, Consumer<Span> consumer) {
            Span span = tracer.spanBuilder(spanName).startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.addEvent("Enter " + spanName);
                consumer.accept(span);
                span.addEvent("Exit " + spanName);
            } finally {
                span.end();
            }
        }

        public void wrapDebug(String spanName, Consumer<Span> consumer) {
            Span span = tracer.spanBuilder(spanName).setAttribute("DEBUG", true).startSpan();
            try (Scope scope = span.makeCurrent()) {
                span.addEvent("Enter " + spanName);
                consumer.accept(span);
                span.addEvent("Exit " + spanName);
            } finally {
                span.end();
            }
        }
    }
}
