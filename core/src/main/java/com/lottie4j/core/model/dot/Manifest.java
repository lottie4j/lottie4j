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
 * Represents the manifest metadata of a Lottie animation file.
 * <p>
 * The manifest contains top-level information about the animation file including
 * version details, the tool that generated it, authorship information, and a
 * collection of animation configurations.
 * <p>
 * This record is immutable and provides JSON serialization/deserialization
 * capabilities, ignoring unknown properties during deserialization and excluding
 * null values during serialization.
 * <p>
 * Implements PropertyListing to provide a human-readable representation of the
 * manifest structure.
 *
 * @param version    the version of the Lottie format specification
 * @param generator  the name and version of the tool that generated this animation
 * @param author     the author or creator of the animation
 * @param animations the list of animation configurations contained in this manifest
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Manifest(
        @JsonProperty("version") String version,
        @JsonProperty("generator") String generator,
        @JsonProperty("author") String author,
        @JsonProperty("animations") List<ManifestAnimation> animations
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Animation");
        list.add("Version", version);
        list.add("Generator", generator);
        list.add("Author", author);
        list.addList("Animations", animations);
        return list;
    }
}
