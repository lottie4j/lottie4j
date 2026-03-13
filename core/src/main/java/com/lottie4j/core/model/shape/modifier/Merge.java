package com.lottie4j.core.model.shape.modifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.MergeMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents a merge shape that combines multiple paths using boolean operations.
 * <p>
 * The Merge shape is used in Lottie animations to perform path operations such as union, intersect,
 * subtract, and exclude on multiple shape layers. It combines the geometry of child shapes based on
 * the specified merge mode to create complex vector shapes from simpler components.
 * <p>
 * This record implements BaseShape and provides JSON serialization/deserialization support for
 * Lottie animation files. The shape is identified by the type "mm" in the JSON structure.
 * <p>
 * The merge operation is non-destructive and allows for dynamic boolean operations during animation
 * playback. The behavior is controlled by the mergeMode property, which determines how the paths
 * of child shapes are combined.
 *
 * @param name      the human-readable name of the merge shape
 * @param matchName the name used for matching and identification in expressions or scripts
 * @param hidden    indicates whether the merge shape is hidden and should not be rendered
 * @param blendMode the blend mode used for compositing this shape with others
 * @param index     the ordering index of this shape within its parent group
 * @param clazz     the CSS class identifier for styling or categorization purposes
 * @param id        the unique identifier for this merge shape
 * @param d         an undefined property whose purpose is not documented in the Lottie specification
 * @param cix       an undefined property whose purpose is not documented in the Lottie specification
 * @param mergeMode the merge mode that defines how child paths are combined (union, intersect, subtract, exclude)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Merge(
        // Generic for all Shapes
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("ix") Integer index,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String id,

        // Undefined
        @JsonProperty("d") Integer d,
        @JsonProperty("cix") Integer cix,

        // Merge
        @JsonProperty("mm") MergeMode mergeMode
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Merge");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Merge mode", mergeMode);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.MERGE;
    }
}
