package com.lottie4j.core.model.dot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record ManifestAnimation(
        @JsonProperty("id") String id,
        @JsonProperty("autoplay") Boolean autoplay,
        @JsonProperty("loop") Boolean loop,
        @JsonProperty("speed") Integer speed,
        @JsonProperty("direction") Integer direction,
        @JsonProperty("themeColor") String themeColor
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("ManifestAnimation");
        list.add("ID", id);
        list.add("Autoplay", autoplay);
        list.add("Loop", loop);
        list.add("Speed", speed);
        list.add("Direction", direction);
        list.add("Theme color", themeColor);
        return list;
    }
}
