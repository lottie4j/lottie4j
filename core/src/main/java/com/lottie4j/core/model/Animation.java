package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Represents the root structure of a Lottie animation file.
 * <p>
 * This record encapsulates all the essential information required to define and render a Lottie animation,
 * including metadata, timing information, dimensions, and the hierarchical structure of assets, layers, and markers.
 * The animation serves as the top-level container that orchestrates all visual elements and their behaviors
 * throughout the animation timeline.
 * <p>
 * The animation timeline is defined by frame-based timing using inPoint and outPoint values, with the
 * frame rate specified by framesPerSecond. The coordinate system uses the width and height properties
 * to define the animation canvas dimensions.
 *
 * @param version         the version of the Lottie specification used for this animation
 * @param name            the human-readable name of the animation
 * @param matchName       the match name identifier used for referencing
 * @param has3dLayers     flag indicating whether the animation contains 3D layers (1 for true, 0 for false)
 * @param framesPerSecond the frame rate at which the animation should be played
 * @param inPoint         the starting frame of the animation timeline
 * @param outPoint        the ending frame of the animation timeline
 * @param width           the width of the animation canvas in pixels
 * @param height          the height of the animation canvas in pixels
 * @param assets          collection of reusable assets referenced by layers (images, precompositions, etc.)
 * @param layers          ordered collection of layers that compose the visual hierarchy of the animation
 * @param markers         collection of named markers for identifying specific points in the timeline
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
        @JsonProperty("layers") List<Layer> layers,
        @JsonProperty("markers") List<Marker> markers
) implements PropertyListing {
    @Override
    @JsonIgnore
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
        list.addList("Markers", markers);
        return list;
    }
}
