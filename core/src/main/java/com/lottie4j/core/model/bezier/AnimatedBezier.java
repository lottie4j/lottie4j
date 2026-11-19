package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListingList;

import com.lottie4j.core.helper.OldFormatKeyframes;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.List;

/**
 * Represents a Bezier curve that changes over time through keyframe animation.
 * <p>
 * This class implements the Bezier interface and provides support for animated
 * Bezier curves by storing a collection of keyframes. Each keyframe defines
 * the state of the Bezier curve at a specific point in time, allowing for
 * smooth interpolation between different curve shapes.
 * <p>
 * The animation state is controlled through the animated flag, and the actual
 * curve data is stored as a list of BezierKeyframe objects that define the
 * curve's appearance at different time points.
 * <p>
 * This class is designed to work with JSON serialization/deserialization,
 * using custom property names for compact representation.
 *
 * @param animated flag indicating whether this Bezier curve is animated
 * @param beziers  list of keyframes defining the Bezier curve at different time points
 * @param ix       property index for identification purposes
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record AnimatedBezier(
        @JsonProperty("a")
        Integer animated,

        @JsonProperty("k")
        @JsonSerialize(converter = OldFormatKeyframes.BezierWriter.class)
        List<BezierKeyframe> beziers,

        @JsonProperty("ix")
        Integer ix
) implements Bezier {

    /**
     * Completes keyframes in the old form, in which the last keyframe has no shape of its own;
     * see {@link OldFormatKeyframes}.
     */
    public AnimatedBezier {
        beziers = OldFormatKeyframes.completeBeziers(beziers);
    }
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Animated Bezier");
        list.add("Animated", animated);
        list.addList("Beziers", beziers);
        list.add("ix", ix);
        return list;
    }
}
