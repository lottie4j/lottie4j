package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.model.EasingHandle;

import java.util.List;

public record BezierKeyframe(
        @JsonProperty("t") Integer time, // in frames
        @JsonProperty("i") EasingHandle easingIn,
        @JsonProperty("o") EasingHandle easingOut,
        @JsonProperty("s") List<BezierDefinition> beziers
) {
}
