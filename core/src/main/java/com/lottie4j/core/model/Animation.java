package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
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
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Version", version),
                new PropertyLabelValue("Match name", matchName),
                new PropertyLabelValue("Has 3D layers", has3dLayers),
                new PropertyLabelValue("Frames per second", framesPerSecond),
                new PropertyLabelValue("In point", inPoint),
                new PropertyLabelValue("Out point", outPoint),
                new PropertyLabelValue("Width", width),
                new PropertyLabelValue("Height", height),
                new PropertyLabelValue("Assets", assets == null ? "0" : String.valueOf(assets.size()),
                        assets == null ? new ArrayList<>() : assets.stream().map(a -> new PropertyLabelValue("Asset", a.name() == null ? "No name" : a.name(), a.getLabelValues())).toList()),
                new PropertyLabelValue("Layers", layers == null ? "0" : String.valueOf(layers.size()),
                        layers == null ? new ArrayList<>() : layers.stream().map(l -> new PropertyLabelValue("Layer", l.name() == null ? "No name" : l.name(), l.getLabelValues())).toList())
        );
    }
}
