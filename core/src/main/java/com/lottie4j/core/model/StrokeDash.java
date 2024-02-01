package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.StrokeDashType;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#stroke">Lottie Docs: Stroke</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record StrokeDash(
        @JsonProperty("nm") String name,
        @JsonProperty("n") StrokeDashType type,
        @JsonProperty("v") Animated length
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Stroke Dash");
        list.add("Type", type);
        list.add("Length", length);
        return list;
    }
}
