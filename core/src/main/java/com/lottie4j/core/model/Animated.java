package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.KeyframeDeserializer;
import com.lottie4j.core.helper.KeyframeSerializer;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/concepts/#animated-property">Lottie Docs: Animated</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Animated(
        @JsonProperty("a")
        Double animated,

        @JsonProperty("k")
        @JsonSerialize(using = KeyframeSerializer.class)
        @JsonDeserialize(using = KeyframeDeserializer.class)
        List<Keyframe> keyframes,

        @JsonProperty("ix") Integer ix,
        @JsonProperty("l") Integer l,
        @JsonProperty("s") Boolean s,
        @JsonProperty("x") Animated x,
        @JsonProperty("y") Animated y
) {
    public Keyframe atTime(Long time) {
        if (keyframes == null || keyframes.isEmpty()) {
            return new NumberKeyframe(0);
        }
        if (animated == null || animated == 0) {
            // not animated, fixed value
            return keyframes.get(0);
        }
        for (Keyframe keyframe : keyframes) {
            if (keyframe instanceof TimedKeyframe timedKeyframe) {
                if (timedKeyframe.time() >= time) {
                    return timedKeyframe;
                }
            }
        }
        return keyframes.get(0);
    }
}
