# Opentelemetry with Dynamic Conditional Spans

This is an implementation of OpenTelemetry with "conditional spans" -- spans driven by application driven conditions rather than by the tracing infrastructure.

This is part of a talk on [conditional distributed tracing](https://o11ycon-hnycon.io/agenda/conditional-distributed-tracing/) given at [o11ycon 2021](https://o11ycon-hnycon.io/).  Slides are [here](slides.pdf).

There are two implementations here, a conditional sampler and a conditional span builder.  They serve slightly different purposes, and are both useful in their own right.

This is a proof of concept only, and is not intended for production use.

## Conditional Sampler

The `ConditionalSampler` contains a [scripting engine](https://en.wikipedia.org/wiki/Scripting_for_the_Java_Platform) that is backed by [Groovy](https://en.wikipedia.org/wiki/Apache_Groovy), and is re-evaluated on file change.

The conditional sampler makes use of the [sampling](https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/sdk.md#sampler) specification in OpenTelemetry.  This allows spans to be dropped, recorded, or recorded and sampled (visible to the exporter).  Spans that are not sampled will not be seen externally, i.e. the OpenTelemetry collector or Honeycomb will not see any parent at all.

Sampling is typically used to limit the number of spans generated in a trace.  In particular, if a span is dropped, any child spans that are recorded and exported will point to a "missing span" that the receiving system will typically not know how to display.

## ConditionalSpanBuilder

The `ConditionalSpanBuilder` is a span builder that contains a [scripting engine](https://en.wikipedia.org/wiki/Scripting_for_the_Java_Platform) that is backed by [Groovy](https://en.wikipedia.org/wiki/Apache_Groovy), and is re-evaluated on file change.

The conditional span builder intercepts calls to the span builder to collect attributes, the span name, and the parent context of the span builder.  On `startSpan`, the script is evaluated and if `true` then a new span is built -- if `false`, then `Span.current()` is returned instead.

The conditional span builder is useful in situations in which there can be many irrelevant spans generated.  Any attributes or child spans can be seamlessly added to the parent span, and there is no "missing span" at issue... they are "folded" into the parent span.  Because spans and traces are a "write-only" API, there's no internal logic that will query for a particular span name or attribute.

## Feature Flags

There is an integration with LaunchDarkly using the `Context` API to show how sampling of spans can be driven by feature flags or by individual users.

You can set up a LaunchDarkly account and set 

```bash
# https://app.launchdarkly.com/settings/projects
export LD_API_KEY=<SDK key>
```

## Honeycomb

The default backend collector is Honeycomb.  You can switch it out using the `ExampleConfiguration`.

To set the honeycomb credentials, add the following:

```bash
# https://ui.honeycomb.io/account
export HONEYCOMB_API_KEY=<SDK key>
export HONEYCOMB_DATASET=dynamic-spans
```

## Running

Running the test program with some sample spans is straight-forward:

```bash
./gradlew run
```