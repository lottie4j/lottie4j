package com.lottie4j.core.model.transform;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.StrokeDashType;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;

/**
 * Represents a single component of a stroke dash pattern in Lottie animations.
 * A stroke dash pattern is typically composed of multiple StrokeDash instances that work together
 * to define how a continuous stroke should be broken into dashed or dotted segments.
 * <p>
 * Each StrokeDash component defines one aspect of the dash pattern through its type (dash, gap, or offset)
 * and an animated length value that determines the magnitude of that component. The type specifies whether
 * this component represents a visible dash segment, an invisible gap between dashes, or an offset that
 * shifts the pattern's starting position.
 * <p>
 * The length property is animated, allowing the dash pattern to dynamically change over time. This enables
 * effects such as animated stroke reveals, pulsing dashed lines, or morphing dash patterns commonly seen
 * in motion graphics.
 * <p>
 * Multiple StrokeDash instances are combined in sequence to create complete dash patterns. For example,
 * a simple dashed line might consist of two components: a dash component defining the length of visible
 * segments, and a gap component defining the spacing between them. More complex patterns can be created
 * by combining multiple dash and gap components with varying lengths, and can be animated by including
 * an offset component.
 * <p>
 * This record is part of the Lottie animation model and is typically deserialized from JSON animation
 * data, where it appears as part of stroke style definitions in shape layers.
 *
 * @param name   optional identifier for this dash component
 * @param type   the type of dash component (dash, gap, or offset)
 * @param length the animated value defining the magnitude of this component in the dash pattern
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record StrokeDash(
        @JsonProperty("nm") String name,
        @JsonProperty("n") StrokeDashType type,
        @JsonProperty("v") Animated length
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Stroke Dash");
        list.add("Type", type);
        list.add("Length", length);
        return list;
    }
}
