package com.lottie4j.core.model.dot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents the {@code initial} section of a dotLottie v2 manifest.
 * <p>
 * Tells a player which animation or state machine to load when the dotLottie file is first
 * opened. Both fields are optional; if neither is present the player should fall back to the
 * first animation declared in the {@code animations} array.
 *
 * @param animation    ID of the animation to load initially (corresponds to a file in {@code a/})
 * @param stateMachine ID of the state machine to load initially (corresponds to a file in {@code s/})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record ManifestInitial(
        @JsonProperty("animation") String animation,
        @JsonProperty("stateMachine") String stateMachine
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("ManifestInitial");
        list.add("Animation", animation);
        list.add("State machine", stateMachine);
        return list;
    }
}

