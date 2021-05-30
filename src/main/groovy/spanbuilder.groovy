import com.launchdarkly.sdk.LDUser
import com.launchdarkly.sdk.server.LDClient
import com.tersesystems.conditionalspans.Main
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.context.Context

import java.time.LocalTime

import static io.opentelemetry.api.common.AttributeKey.booleanKey

boolean shouldStartSpan(String spanName, Context parent, HashMap<AttributeKey<?>, Object> attributes) {
    return always()
    //return byDebugAttribute(spanName, attributes);
    //return byOddNanos(spanName);
    //return byFeatureFlag(spanName);
    //return byDeadline(spanName, LocalTime.now().plusMinutes(10));
}

boolean always() {
    return true
}

boolean byDebugAttribute(String spanName, HashMap<AttributeKey<?>, Object> attributes) {
    // println("byDebugAttribute: instance = $spanName, attributes = $attributes")
    if (attributes.containsKey(booleanKey("DEBUG"))) {
        println("byAttribute: ignoring debug span $spanName")
        return false;
    } else {
        return true;
    }
}

boolean byOddNanos(String spanName) {
    //println("byOddNanos: spanName = $spanName")
    if (System.nanoTime() % 3 == 0) {
        println("byOddNanos: ignoring ${spanName}")
        return false;
    } else {
        return true;
    }
}

boolean byFeatureFlag(String spanName) {
    //println("byFeatureFlag: spanName = $spanName")
    Context context = Context.current();
    LDClient ldClient = context.get(Main.LD_CLIENT_KEY);
    LDUser user = context.get(Main.LD_USER_KEY);

    if (ldClient.boolVariation("testflag", user, false)) {
        return true;
    } else {
        println("byFeatureFlag: ignoring $spanName for $user")
        return false;
    }
}

boolean byDeadline(String spanName, LocalTime deadline) {
    LocalTime currentTime = LocalTime.now();
    if (currentTime.isAfter(deadline)) {
        println("byDeadline: ignoring ${spanName} because $currentTime is after $deadline")
        return false
    } else {
        return true
    }
}
