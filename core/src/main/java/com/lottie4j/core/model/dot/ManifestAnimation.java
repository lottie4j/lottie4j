package com.lottie4j.core.model.dot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents the animation configuration within a Lottie manifest file.
 * This record encapsulates the core animation properties that define how a Lottie animation
 * should be played, including playback controls, visual settings, and identification.
 * <p>
 * The ManifestAnimation provides configuration for animation behavior such as whether it should
 * start automatically, loop continuously, and at what speed and direction it should play.
 * It also includes visual theming through color configuration and unique identification.
 * <p>
 * This class is designed to be deserialized from JSON manifest files and implements
 * PropertyListing to provide a human-readable representation of its configuration.
 * Unknown JSON properties are ignored during deserialization, and null values are
 * excluded during serialization.
 *
 * @param id         the unique identifier for this animation configuration
 * @param autoplay   indicates whether the animation should start playing automatically when loaded
 * @param loop       indicates whether the animation should repeat continuously after completion
 * @param speed      the playback speed multiplier for the animation
 * @param direction  the playback direction of the animation (typically 1 for forward, -1 for reverse)
 * @param themeColor the theme color to be applied to the animation, typically in hex format
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record ManifestAnimation(
        @JsonProperty("id") String id,
        @JsonProperty("autoplay") Boolean autoplay,
        @JsonProperty("loop") Boolean loop,
        @JsonProperty("speed") Integer speed,
        @JsonProperty("direction") Integer direction,
        @JsonProperty("themeColor") String themeColor
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("ManifestAnimation");
        list.add("ID", id);
        list.add("Autoplay", autoplay);
        list.add("Loop", loop);
        list.add("Speed", speed);
        list.add("Direction", direction);
        list.add("Theme color", themeColor);
        return list;
    }
}
