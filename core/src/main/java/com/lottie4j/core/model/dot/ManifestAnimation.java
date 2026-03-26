package com.lottie4j.core.model.dot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Represents the animation configuration within a dotLottie manifest file.
 * <p>
 * Supports both dotLottie v1 and v2 manifest shapes:
 * <ul>
 *   <li><b>Common (v1 &amp; v2)</b>: {@code id}</li>
 *   <li><b>v1 only</b>: {@code autoplay}, {@code loop}, {@code speed}, {@code direction},
 *       {@code themeColor}</li>
 *   <li><b>v2 only</b>: {@code initialTheme}, {@code background}, {@code themes}</li>
 * </ul>
 * Unknown JSON properties are ignored during deserialization, and null values are
 * excluded during serialization.
 *
 * @param id           unique identifier for this animation (required by both versions)
 * @param autoplay     (v1) whether the animation should start playing automatically
 * @param loop         (v1) whether the animation should loop continuously
 * @param speed        (v1) playback speed multiplier
 * @param direction    (v1) playback direction (1 = forward, -1 = reverse)
 * @param themeColor   (v1) theme color in hex format
 * @param initialTheme (v2) ID of the theme to apply initially, corresponding to a file in {@code t/}
 * @param background   (v2) background color in hex format (e.g. {@code #FFFFFF})
 * @param themes       (v2) IDs of themes that are scoped to this animation
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record ManifestAnimation(
        @JsonProperty("id") String id,
        // v1 fields
        @JsonProperty("autoplay") Boolean autoplay,
        @JsonProperty("loop") Boolean loop,
        @JsonProperty("speed") Double speed,
        @JsonProperty("direction") Integer direction,
        @JsonProperty("themeColor") String themeColor,
        // v2 fields
        @JsonProperty("initialTheme") String initialTheme,
        @JsonProperty("background") String background,
        @JsonProperty("themes") List<String> themes
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("ManifestAnimation");
        list.add("ID", id);
        list.add("Autoplay", autoplay);
        list.add("Loop", loop);
        list.add("Speed", speed != null ? speed.toString() : null);
        list.add("Direction", direction);
        list.add("Theme color (v1)", themeColor);
        list.add("Initial theme (v2)", initialTheme);
        list.add("Background (v2)", background);
        if (themes != null && !themes.isEmpty()) {
            list.add("Themes (v2)", String.join(", ", themes));
        }
        return list;
    }
}
