package com.lottie4j.core.model.dot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

/**
 * Represents a theme entry in a dotLottie v2 manifest.
 * <p>
 * Each theme corresponds to a JSON file in the {@code t/} directory of the dotLottie archive
 * and carries the theme's unique identifier and an optional human-readable name.
 *
 * @param id   unique identifier for the theme, matching the filename (without extension) in {@code t/}
 * @param name optional descriptive name for the theme
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record ManifestTheme(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("ManifestTheme");
        list.add("ID", id);
        list.add("Name", name);
        return list;
    }
}

