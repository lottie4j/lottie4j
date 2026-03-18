package com.lottie4j.core.model.asset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.layer.Layer;

import java.util.List;

/**
 * Represents an asset in a Lottie animation composition.
 * <p>
 * Assets are reusable resources that can be referenced throughout the animation, including
 * images, audio files, precompositions (nested animations), and data sources. Each asset
 * is uniquely identified and can be embedded or referenced externally.
 * <p>
 * This record supports multiple asset types with varying properties:
 * <p>
 * Images contain dimensional properties (width, height) and can be either embedded or
 * externally referenced. Image sequences are supported through the type field.
 * <p>
 * Audio assets (sounds) reference external audio files through the fileName and shapeType
 * properties.
 * <p>
 * Precompositions represent nested animation layers with their own layer hierarchy,
 * frame rate, and composition settings, allowing complex animations to be reused.
 * <p>
 * Data sources provide external data references with type classification.
 * <p>
 * The fileName property can represent either an embedded resource (as a Boolean) or
 * an external reference (as a String containing a base64 data URI or file path).
 * <p>
 * This class implements PropertyListing to provide structured inspection of asset
 * properties for debugging and analysis purposes.
 *
 * @param id the unique identifier for the asset
 * @param name the display name of the asset
 * @param shapeType the shape type or path reference
 * @param fileName the file name (Boolean or String URI)
 * @param embedded flag indicating if the asset is embedded
 * @param width the width in pixels (for images)
 * @param height the height in pixels (for images)
 * @param layers the layer hierarchy (for precompositions)
 * @param frameRate the frame rate (for precompositions)
 * @param extraComposition extra composition settings (for precompositions)
 * @param type the asset type identifier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Asset(
        // Generic for Image, Sound, Precomposition, Datasource
        @JsonProperty("id") String id,
        @JsonProperty("nm") String name,

        // Generic for Image, Sound, Datasource
        @JsonProperty("u") ShapeType shapeType,
        @JsonProperty("p") Object fileName,  // Can be Boolean (true/false) or String (base64 data URI)
        @JsonProperty("e") Integer embedded,

        // Image
        @JsonProperty("w") Integer width,
        @JsonProperty("h") Integer height,

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
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Asset");
        list.add("ID", id());
        list.add("Path", shapeType);
        list.add("File name", fileName != null ? fileName.toString() : null);
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
