package com.lottie4j.core.model.layer;

import com.fasterxml.jackson.annotation.*;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.definition.MatteMode;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.effect.Effect;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.text.TextData;
import com.lottie4j.core.model.transform.Transform;

import java.util.List;

/**
 * Represents a layer in a Lottie animation composition.
 * <p>
 * A layer is a fundamental building block of Lottie animations that defines a visual or functional element
 * within the composition. Layers can contain various types of content such as shapes, images, text, or
 * precompositions, and support transformations, masking, effects, and blending operations.
 * <p>
 * Layers are organized hierarchically and can be parented to other layers for coordinated transformations.
 * They have temporal properties that control when they appear in the animation timeline and can be styled
 * with different blend modes and visibility states.
 * <p>
 * Different layer types support specific properties:
 * - Shape layers contain vector shapes defined in the shapes array
 * - Precomposition layers reference other compositions via referenceId and may include dimensions and time remapping
 * - Solid color layers render a rectangle with a specified color
 * - Text layers contain text data with styling and animation properties
 * <p>
 * This record uses JSON property annotations for deserialization from Lottie JSON files and implements
 * PropertyListing to provide structured output of the layer's configuration.
 *
 * @param name          the display name of the layer
 * @param matchName     the match name used for expressions and identification
 * @param has3dLayers   indicator for 3D layer capabilities (0 for 2D, 1 for 3D)
 * @param hidden        whether the layer is hidden from rendering
 * @param layerType     the type of layer defining its content and behavior
 * @param indexLayer    the unique index identifying this layer in the composition
 * @param indexParent   the index of the parent layer for hierarchical transformations, null if no parent
 * @param timeStretch   the time stretch factor applied to the layer's animation speed
 * @param inPoint       the frame number when the layer becomes active in the timeline
 * @param outPoint      the frame number when the layer becomes inactive in the timeline
 * @param startTime     the offset in frames for when the layer's content starts playing
 * @param blendMode     the blend mode determining how this layer composites with layers below
 * @param clazz         the CSS class attribute for HTML rendering contexts
 * @param idAttribute   the ID attribute for HTML rendering contexts
 * @param tagName       the HTML tag name for HTML rendering contexts
 * @param matteMode     the matte mode defining how this layer affects or is affected by adjacent layers
 * @param matteTarget   indicates if this layer is the target of a matte operation
 * @param hasMask       indicates whether this layer has masks
 * @param masks         the list of masks applied to this layer for clipping or alpha operations
 * @param effects       the list of effects applied to this layer
 * @param transform     the transformation properties controlling position, rotation, scale, and opacity
 * @param autoRotate    whether the layer automatically orients along a motion path
 * @param hix           unknown property, potentially for internal use
 * @param ct            unknown property, potentially for internal use
 * @param tp            unknown property, potentially for internal use
 * @param shapes        the list of shapes contained in a shape layer
 * @param referenceId   the ID referencing a precomposition asset
 * @param width         the width of a precomposition layer in pixels
 * @param height        the height of a precomposition layer in pixels
 * @param timeRemapping animated property for remapping time in precomposition layers
 * @param solidColor    the hex color string for solid color layers
 * @param textData      the text content and styling information for text layers
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
    "ddd", "ind", "ty", "nm", "mn", "tt", "td", "sr", "ks", "ao",
    "hasMask", "masksProperties", "ef", "shapes", "ip", "op", "st", "bm",
    "refId", "w", "h", "tm", "sc", "t", "parent", "cl", "ln", "tg", "hix", "ct", "tp", "hd"
})
public record Layer(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ddd") Integer has3dLayers,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("ty") LayerType layerType,
        @JsonProperty("ind") Integer indexLayer,
        @JsonProperty("parent") Integer indexParent,
        @JsonProperty("sr") Integer timeStretch,
        @JsonProperty("ip") Integer inPoint,
        @JsonProperty("op") Integer outPoint,
        @JsonProperty("st") Integer startTime,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String idAttribute,
        @JsonProperty("tg") String tagName,
        @JsonProperty("tt") MatteMode matteMode,
        @JsonProperty("td") Integer matteTarget,
        @JsonProperty("hasMask") Boolean hasMask,
        @JsonProperty("masksProperties") List<Mask> masks,
        @JsonProperty("ef") List<Effect> effects,
        @JsonProperty("ks") Transform transform,
        @JsonProperty("ao") Integer autoRotate,

        // Unknown
        @JsonProperty("hix") Integer hix,
        @JsonProperty("ct") Integer ct,
        @JsonProperty("tp") Integer tp,

        // Shape
        @JsonProperty("shapes") List<BaseShape> shapes,

        // Precomposition
        // https://lottiefiles.github.io/lottie-docs/assets/#precomposition
        @JsonProperty("refId") String referenceId,
        @JsonProperty("w") Integer width,
        @JsonProperty("h") Integer height,
        @JsonProperty("tm") Animated timeRemapping,

        // Solid Color Layer
        @JsonProperty("sc") String solidColor,

        // Text Layer
        @JsonProperty("t") TextData textData
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Layer");
        list.add("Match name", matchName);
        list.add("Has 3D layers", has3dLayers);
        list.add("Hidden", hidden);
        list.add("Layer type", layerType);
        list.add("Index layer", indexLayer);
        list.add("Index Parent", indexParent);
        list.add("Time stretch", timeStretch);
        list.add("In point", inPoint);
        list.add("Out point", outPoint);
        list.add("Start type", startTime);
        list.add("Blend mode", blendMode);
        list.add("Clazz", clazz);
        list.add("ID attribute", idAttribute);
        list.add("Tag name", tagName);
        list.add("Matte mode", matteMode);
        list.add("Matte target", matteTarget);
        list.add("Has mask", hasMask);
        list.add("Auto rotate", autoRotate);
        list.add("Hix", hix);
        list.add("Ct", ct);
        list.add("Tp", tp);
        list.add("Reference ID", referenceId);
        list.add("Width", width);
        list.add("Height", height);
        list.add("Time remapping", timeRemapping);
        list.add("Solid color", solidColor);
        list.add("Text data", textData);
        list.addList("Masks", masks);
        list.addList("Effects", effects);
        list.addShapeList("Shapes", shapes);
        return list;
    }
}
