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
    public Double getValue(int value, long timestamp) {
        return (double) value;
    }

    public Double getValue(AnimatedValueType valueType, long timestamp) {
        if (keyframes == null || keyframes.isEmpty()) {
            return 0D;
        }
        if (animated == null || animated == 0) {
            // not animated, fixed value
            return getValue(valueType.getIndex());
        }
        for (Keyframe keyframe : keyframes) {
            if (keyframe instanceof TimedKeyframe timedKeyframe) {
                if (timedKeyframe.time() >= timestamp) {
                    return 0D;
                }
            }
        }
        return 0D;
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
