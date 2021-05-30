import com.launchdarkly.sdk.LDUser
import com.launchdarkly.sdk.server.LDClient
import com.tersesystems.conditionalspans.Main
import com.tersesystems.conditionalspans.SamplingInstance
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.samplers.SamplingResult

import static io.opentelemetry.api.common.AttributeKey.stringKey
import static io.opentelemetry.sdk.trace.samplers.SamplingDecision.DROP
import static io.opentelemetry.sdk.trace.samplers.SamplingDecision.RECORD_AND_SAMPLE

// This is the entry point from ConditionalSampler
SamplingResult shouldSample(SamplingInstance instance) {
    always(instance)
    //byOddNanos(instance)
    //byFeatureFlag(instance)
}

SamplingResult always(SamplingInstance instance) {
    //println("always: instance = $instance")
    Attributes attrs = Attributes.of(stringKey("sampler"), "always")
    return SamplingResult.create(RECORD_AND_SAMPLE, attrs);
}

SamplingResult byOddNanos(SamplingInstance instance) {
    //println("byOddNanos: instance = $instance")
    if (System.nanoTime() % 3 == 0) {
        println("byOddNanos: dropping ${instance.getName()} in trace id ${instance.traceId}")
        return SamplingResult.create(DROP);
    } else {
        Attributes attrs = Attributes.of(stringKey("sampler"), "byOddSecond")
        return SamplingResult.create(RECORD_AND_SAMPLE, attrs);
    }
}

SamplingResult byFeatureFlag(SamplingInstance instance) {
    //println("byFeatureFlag: instance = $instance")
    Context context = instance.parentContext;
    LDClient ldClient = context.get(Main.LD_CLIENT_KEY);
    LDUser user = context.get(Main.LD_USER_KEY);

    if (ldClient.boolVariation("testflag", user, true)) {
        //println("feature flag returns true for $user")
        Attributes attrs = Attributes.of(stringKey("sampler"), "featureFlag")
        return SamplingResult.create(RECORD_AND_SAMPLE, attrs);
    } else {
        println("byFeatureFlag: feature flag dropping for $user in trace id ${instance.traceId}")
        return SamplingResult.create(DROP);
    }
}
