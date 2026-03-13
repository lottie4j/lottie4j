package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents a mask definition in a Lottie animation.
 * <p>
 * A mask is used to control the visibility of layers or shapes by defining areas that should be
 * shown or hidden. Masks can be inverted to reverse their effect, making the masked area visible
 * and the unmasked area hidden.
 * <p>
 * This is an immutable record that provides a simplified representation of mask properties,
 * with support for JSON deserialization through Jackson annotations. Unknown JSON properties
 * are ignored during deserialization to maintain forward compatibility.
 *
 * @param name      the display name of the mask
 * @param matchName the match name used for identification and referencing
 * @param inverted  indicates whether the mask effect is inverted (true) or normal (false)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Mask(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("inv") Boolean inverted
        // TODO EXTEND FURTHER
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Mask");
        list.add("Match name", matchName);
        list.add("Inverted", inverted);
        return list;
    }
}
