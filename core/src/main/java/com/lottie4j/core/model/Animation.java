package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Top level object, describing the
 * <a href="https://lottiefiles.github.io/lottie-docs/animation/">Lottie Docs: Animation</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Animation(
        @JsonProperty("v") String version,
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ddd") Integer has3dLayers,
        @JsonProperty("fr") Integer framesPerSecond,
        @JsonProperty("ip") Integer inPoint,
        @JsonProperty("op") Integer outPoint,
        @JsonProperty("w") Integer width,
        @JsonProperty("h") Integer height,
        @JsonProperty("assets") List<Asset> assets,
        @JsonProperty("layers") List<Layer> layers
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Animation");
        list.add("Version", version);
        list.add("Match name", matchName);
        list.add("Has 3D layers", has3dLayers);
        list.add("Frames per second", framesPerSecond);
        list.add("In point", inPoint);
        list.add("Out point", outPoint);
        list.add("Width", width);
        list.add("Height", height);
        list.addList("Assets", assets);
        list.addList("Layers", layers);
        return list;
    }
}
