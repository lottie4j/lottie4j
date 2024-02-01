package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/assets/">Lottie Docs: Asset</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Asset(
        // Generic for Image, Sound, Precomposition, Datasource
        @JsonProperty("id") String id,
        @JsonProperty("nm") String name,

        // Generic for Image, Sound, Datasource
        @JsonProperty("u") ShapeType shapeType,
        @JsonProperty("p") Boolean fileName,
        @JsonProperty("e") Integer embedded,

        // Image
        @JsonProperty("w") Integer width,
        @JsonProperty("h") String height,

        // Precomposition
        @JsonProperty("layers") List<Layer> layers,
        @JsonProperty("fr") Integer frameRate,
        @JsonProperty("xt") Integer extraComposition,

        // Used for Image and Datasource
        // Image "seq" = Marks as part of an image sequence if present
        // Datasource "3" = Type
        @JsonProperty("t") Integer type
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Asset");
        list.add("ID", id());
        list.add("Path", shapeType);
        list.add("File name", fileName());
        list.add("Embedded", embedded());
        list.add("Width", width());
        list.add("Height", height());
        list.add("Frame rate", frameRate());
        list.add("Extra composition", extraComposition());
        list.add("Type", type());
        list.addList("Layers", layers);

        return list;
    }
}
