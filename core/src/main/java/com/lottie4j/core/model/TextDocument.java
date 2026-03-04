package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextDocument(
        // Keyframed text content
        @JsonProperty("k") List<TextKeyframe> keyframes
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Text Document");
        if (keyframes != null) {
            list.add("Keyframes", String.valueOf(keyframes.size()));
        }
        return list;
    }
}
