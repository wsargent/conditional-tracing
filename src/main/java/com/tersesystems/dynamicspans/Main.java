package com.tersesystems.dynamicspans;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    private static final OpenTelemetry openTelemetry = ExampleConfiguration.initOpenTelemetry();


    public static void main(String[] args) throws Exception {
        DoStuff doStuff = new DoStuff();
        doStuff.doStuff();
    }

    static class DoStuff {

        // "io.opentelemetry.example.HelloWorldServer");
        private final Tracer tracer =
                openTelemetry.getTracer(getClass().getName());

        private final ConditionManager cm = new ConditionManager();

        final Path path = Paths.get("src/main/groovy/condition.groovy");
        final ConditionSource src;

        DoStuff() throws IOException {
            src = new FileConditionSource(path);
        }

        public void doStuff() throws Exception {
            for (int i = 0; i < 100; i++) {
                sayHello();
                Thread.sleep(1000L);
            }
        }

        public void sayHello() throws ScriptException {
            final SpanBuilder spanBuilder = tracer.spanBuilder( getClass().getName() + "/" + "SayHello");
            final ConditionalSpanBuilder csb = cm.conditionalSpan(src, spanBuilder);
            Span span = csb.startSpan();
            try {
                System.out.println("I like opentelemetry");
            } finally {
                span.end();
            }
        }
    }
}
