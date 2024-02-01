package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.EasingHandle;

import java.util.List;

public record BezierKeyframe(
        @JsonProperty("t") Integer time, // in frames
        @JsonProperty("i") EasingHandle easingIn,
        @JsonProperty("o") EasingHandle easingOut,
        @JsonProperty("s") List<BezierDefinition> beziers
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Bezier Keyframe");
        list.add("Time", time);
        list.add("Easing in", easingIn);
        list.add("Easing out", easingOut);
        list.addList("Beziers", beziers);
        return list;
    }
}
