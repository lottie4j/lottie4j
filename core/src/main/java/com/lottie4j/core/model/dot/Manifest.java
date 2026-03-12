package com.lottie4j.core.model.dot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;


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
