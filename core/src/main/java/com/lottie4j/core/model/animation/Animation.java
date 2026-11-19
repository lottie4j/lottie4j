package com.lottie4j.core.model.animation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.helper.DoubleAsIntegerIfWholeSerializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.asset.Asset;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.core.model.text.FontList;
import tools.jackson.databind.annotation.JsonSerialize;

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
 * @param framesPerSecond the frame rate at which the animation should be played; not always a whole
 *                        number (29.97 is common)
 * @param inPoint         the starting frame of the animation timeline
 * @param outPoint        the ending frame of the animation timeline
 * @param width           the width of the animation canvas in pixels
 * @param height          the height of the animation canvas in pixels
 * @param assets          collection of reusable assets referenced by layers (images, precompositions, etc.)
 * @param layers          ordered collection of layers that compose the visual hierarchy of the animation
 * @param markers         collection of named markers for identifying specific points in the timeline
 * @param props           additional properties object
 * @param fonts           the fonts used by the text layers, or {@code null} if the file has none
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Animation(
        @JsonProperty("v") String version,
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ddd") Integer has3dLayers,
        @JsonProperty("fr") @JsonSerialize(using = DoubleAsIntegerIfWholeSerializer.class) Double framesPerSecond,
        @JsonProperty("ip") @JsonSerialize(using = DoubleAsIntegerIfWholeSerializer.class) Double inPoint,
        @JsonProperty("op") @JsonSerialize(using = DoubleAsIntegerIfWholeSerializer.class) Double outPoint,
        @JsonProperty("w") Integer width,
        @JsonProperty("h") Integer height,
        @JsonProperty("assets") List<Asset> assets,
        @JsonProperty("layers") List<Layer> layers,
        @JsonProperty("markers") List<Marker> markers,
        @JsonProperty("props") Object props,
        @JsonProperty("fonts") FontList fonts
) implements PropertyListing {

    /**
     * A copy of this animation with other layers and assets.
     *
     * @param layers the layers of the copy
     * @param assets the assets of the copy
     * @return the copy
     */
    public Animation withLayersAndAssets(List<Layer> layers, List<Asset> assets) {
        return new Animation(version, name, matchName, has3dLayers, framesPerSecond, inPoint, outPoint, width,
                height, assets, layers, markers, props, fonts);
    }

    /**
     * Creates an animation without fonts.
     *
     * @param version         the version of the Lottie specification used for this animation
     * @param name            the human-readable name of the animation
     * @param matchName       the match name identifier used for referencing
     * @param has3dLayers     1 if the animation contains 3D layers
     * @param framesPerSecond the frame rate
     * @param inPoint         the starting frame
     * @param outPoint        the ending frame (exclusive)
     * @param width           the width in pixels
     * @param height          the height in pixels
     * @param assets          the assets
     * @param layers          the layers
     * @param markers         the markers
     * @param props           additional properties object
     */
    public Animation(String version, String name, String matchName, Integer has3dLayers, Double framesPerSecond,
                     Double inPoint, Double outPoint, Integer width, Integer height, List<Asset> assets,
                     List<Layer> layers, List<Marker> markers, Object props) {
        this(version, name, matchName, has3dLayers, framesPerSecond, inPoint, outPoint, width, height, assets,
                layers, markers, props, null);
    }

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
        list.add("Fonts", fonts != null && fonts.list() != null ? fonts.list().size() : null);
        return list;
    }
}
