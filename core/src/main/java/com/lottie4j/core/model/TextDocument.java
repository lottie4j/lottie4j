package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Represents a text document in a Lottie animation containing keyframed text content and styling.
 * <p>
 * This record encapsulates animated text properties through a collection of keyframes that define
 * how text content and its visual attributes change over time. Each keyframe can specify text content,
 * font properties, colors, and other styling information at specific points in the animation timeline.
 * <p>
 * The text document uses JSON serialization with the "k" property mapping to the keyframes list,
 * following the Lottie JSON specification format. Unknown JSON properties are ignored during
 * deserialization, and null values are excluded from serialization.
 * <p>
 * This class implements PropertyListing to provide a human-readable representation of the text
 * document structure, showing the number of keyframes contained within.
 *
 * @param keyframes the list of text keyframes defining the animated text properties over time,
 *                  may be null if no keyframes are defined
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextDocument(
        // Keyframed text content
        @JsonProperty("k") List<TextKeyframe> keyframes
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Text Document");
        if (keyframes != null) {
            list.add("Keyframes", String.valueOf(keyframes.size()));
        }
        return list;
    }
}
