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

import java.util.ArrayList;
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
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Animated", animated),
                new PropertyLabelValue("Keyframes", keyframes == null ? "0" : String.valueOf(keyframes.size()),
                        keyframes == null ? new ArrayList<>() : keyframes.stream().map(kf -> new PropertyLabelValue("Keyframe", kf.getClass().getSimpleName(), kf.getLabelValues())).toList()),
                new PropertyLabelValue("ix", ix),
                new PropertyLabelValue("l", l),
                new PropertyLabelValue("s", s),
                new PropertyLabelValue("x", "", x == null ? new ArrayList<>() : x.getLabelValues()),
                new PropertyLabelValue("y", "", y == null ? new ArrayList<>() : y.getLabelValues())
        );
    }

    public Double getValue(ValueType valueType, long timestamp) {
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

    private Double getValue(int idx) {
        if (keyframes == null || keyframes.isEmpty() || keyframes.size() < idx) {
            return 0D;
        }
        var keyframe = keyframes.get(idx);
        if (keyframe instanceof NumberKeyframe numberKeyframe) {
            return numberKeyframe.doubleValue();
        }
        return 0D;
    }

    public enum ValueType {
        X(0),
        Y(1),
        WIDTH(0),
        HEIGHT(1),
        RED(0),
        GREEN(1),
        BLEU(2),
        OPACITY(3);

        final int index;

        ValueType(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }
    }
}
