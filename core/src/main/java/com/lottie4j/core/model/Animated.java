package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.KeyframeDeserializer;
import com.lottie4j.core.helper.KeyframeSerializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
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
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Animated");
        list.add("Animated", animated);
        list.addKeyframeList("Keyframes", keyframes);
        list.add("ix", ix);
        list.add("l", l);
        list.add("s", s);
        list.add("x", x);
        list.add("y", y);
        return list;
    }

    // TODO should not be needed
    public Double getValue(int value, double frame) {
        return (double) value;
    }

    public Double getValue(AnimatedValueType valueType, double frame) {
        if (keyframes == null || keyframes.isEmpty()) {
            return 0D;
        }
        if (animated == null || animated == 0) {
            // not animated, fixed value
            return getValue(valueType.getIndex());
        }

        // Find the appropriate keyframe(s) for the current frame
        TimedKeyframe prevKeyframe = null;
        TimedKeyframe nextKeyframe = null;

        for (int i = 0; i < keyframes.size(); i++) {
            if (keyframes.get(i) instanceof TimedKeyframe timedKeyframe) {
                if (timedKeyframe.time() <= frame) {
                    prevKeyframe = timedKeyframe;
                    // Look for the next keyframe
                    if (i + 1 < keyframes.size() && keyframes.get(i + 1) instanceof TimedKeyframe next) {
                        nextKeyframe = next;
                    }
                } else {
                    // We've gone past the current frame
                    if (prevKeyframe == null) {
                        // Frame is before the first keyframe
                        prevKeyframe = timedKeyframe;
                    }
                    break;
                }
            }
        }

        // If we only have one keyframe or we're at/after the last keyframe
        if (prevKeyframe == null) {
            return 0D;
        }

        if (nextKeyframe == null) {
            // Use the last keyframe value
            return getValueFromKeyframe(prevKeyframe, valueType.getIndex());
        }

        // Check for hold keyframe (step interpolation)
        if (prevKeyframe.holdFrame() != null && prevKeyframe.holdFrame() == 1) {
            return getValueFromKeyframe(prevKeyframe, valueType.getIndex());
        }

        // Interpolate between keyframes
        double startFrame = prevKeyframe.time();
        double endFrame = nextKeyframe.time();
        double progress = (frame - startFrame) / (endFrame - startFrame);

        // Clamp progress to [0, 1]
        progress = Math.max(0, Math.min(1, progress));

        double startValue = getValueFromKeyframe(prevKeyframe, valueType.getIndex());
        double endValue = getValueFromKeyframe(nextKeyframe, valueType.getIndex());

        // Linear interpolation for now (TODO: implement easing functions)
        return startValue + (endValue - startValue) * progress;
    }

    private Double getValueFromKeyframe(TimedKeyframe keyframe, int index) {
        if (keyframe.values() == null || keyframe.values().isEmpty() || index >= keyframe.values().size()) {
            return 0D;
        }
        return keyframe.values().get(index).doubleValue();
    }

    public Double getValue(int idx) {
        if (keyframes == null || keyframes.isEmpty() || keyframes.size() <= idx) {
            return 0D;
        }
        var keyframe = keyframes.get(idx);
        if (keyframe instanceof NumberKeyframe numberKeyframe) {
            return numberKeyframe.doubleValue();
        }
        return 0D;
    }
}
