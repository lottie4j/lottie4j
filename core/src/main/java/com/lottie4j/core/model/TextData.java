package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;
import java.util.Map;

/**
 * Text data for text layers in Lottie animations
 * https://lottiefiles.github.io/lottie-docs/text/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextData(
        // Text document data (keyframed text content and styling)
        @JsonProperty("d") TextDocument document,

        // Text animator data
        @JsonProperty("a") List<Map<String, Object>> animators,

        // Path data
        @JsonProperty("p") Map<String, Object> path
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Text Data");
        list.add("Document", document);
        return list;
    }
}

