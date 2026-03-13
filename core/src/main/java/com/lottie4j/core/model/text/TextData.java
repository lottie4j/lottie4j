package com.lottie4j.core.model.text;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;
import java.util.Map;

/**
 * Represents text data within a Lottie animation, containing the text document,
 * animation properties, and path information.
 * <p>
 * This record encapsulates all text-related data including the keyframed text
 * content and styling through the document field, optional animator configurations
 * for text animations, and optional path data for text along a path effects.
 * <p>
 * The class implements PropertyListing to provide a structured representation
 * of the text data for debugging and inspection purposes.
 * <p>
 * JSON properties are mapped using short property names for compact serialization:
 * "d" for document, "a" for animators, and "p" for path.
 *
 * @param document the text document with content and styling
 * @param animators the list of text animator configurations
 * @param path the path data for text-on-path effects
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
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Text Data");
        list.add("Document", document);
        return list;
    }
}

