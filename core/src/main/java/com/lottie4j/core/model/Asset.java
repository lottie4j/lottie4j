package com.lottie4j.core.model;

import com.lottie4j.core.definition.ShapeType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/assets/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Asset(
        // Generic for Image, Sound, Precomposition, Datasource
        @JsonProperty("id") String id,
        @JsonProperty("nm") String name,

        // Generic for Image, Sound, Datasource
        @JsonProperty("u") ShapeType path,
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
) {
}
