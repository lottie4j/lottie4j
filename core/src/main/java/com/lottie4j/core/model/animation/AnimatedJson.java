package com.lottie4j.core.model.animation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.helper.KeyframeSerializer;
import com.lottie4j.core.helper.OldFormatKeyframes;
import com.lottie4j.core.model.keyframe.Keyframe;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.util.StdConverter;

import java.util.List;

/**
 * The JSON form of an {@link Animated}, for writing. It exists because of the member {@code x},
 * which is the x dimension of a split property (an object) or the expression of a property (a
 * string): one JSON member for two components of the record.
 *
 * @param animated  see {@link Animated#animated()}
 * @param keyframes see {@link Animated#keyframes()}
 * @param ix        see {@link Animated#ix()}
 * @param l         see {@link Animated#l()}
 * @param s         see {@link Animated#s()}
 * @param x         the x dimension if there is one, or else the expression
 * @param y         see {@link Animated#y()}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record AnimatedJson(
        @JsonProperty("a") Integer animated,
        @JsonProperty("k") @JsonSerialize(using = KeyframeSerializer.class) List<Keyframe> keyframes,
        @JsonProperty("ix") Integer ix,
        @JsonProperty("l") Integer l,
        @JsonProperty("s") Boolean s,
        @JsonProperty("x") Object x,
        @JsonProperty("y") Animated y
) {

    /** Turns a property into its JSON form. */
    static final class Writer extends StdConverter<Animated, AnimatedJson> {

        @Override
        public AnimatedJson convert(Animated value) {
            return new AnimatedJson(value.animated(), OldFormatKeyframes.restore(value.keyframes()), value.ix(), value.l(), value.s(),
                    value.x() != null ? value.x() : value.expression(), value.y());
        }
    }
}
