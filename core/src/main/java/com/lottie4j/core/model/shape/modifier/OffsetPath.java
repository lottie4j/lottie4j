package com.lottie4j.core.model.shape.modifier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LineJoin;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animated;
import com.lottie4j.core.model.shape.BaseShape;

/**
 * Represents an offset path shape modifier in a Lottie animation.
 * <p>
 * An offset path creates an outline that is offset from the original path by a specified amount.
 * This modifier can expand or contract paths, similar to a stroke expansion effect but applied
 * to the actual path geometry. The offset operation maintains the path's topology while moving
 * all points perpendicular to the path direction.
 * <p>
 * This shape modifier is commonly used to create outline effects, expand or shrink shapes,
 * and create buffer zones around paths. The offset amount can be animated to create dynamic
 * growing or shrinking effects.
 * <p>
 * As a BaseShape implementation, OffsetPath supports standard shape properties including
 * name, visibility, blend modes, and indexing for layer organization. It uses JSON annotations
 * for serialization and deserialization of Lottie animation data.
 * <p>
 * The offset calculation respects the line join style, which determines how corners are handled
 * when the path is offset. Sharp corners can be mitered, rounded, or beveled depending on the
 * lineJoin setting. The miter limit controls when mitered joins are converted to beveled joins
 * to prevent excessively long spikes at sharp angles.
 *
 * @param name       the display name of this shape
 * @param matchName  the match name used for expressions and references
 * @param hidden     whether this shape is hidden from rendering
 * @param blendMode  the blend mode applied when compositing this shape
 * @param index      the render order index of this shape
 * @param clazz      the CSS class identifier for this shape
 * @param id         the unique identifier for this shape
 * @param d          undefined property with integer value
 * @param cix        undefined property with integer value
 * @param amount     the animated offset distance from the original path (positive values expand, negative values contract)
 * @param lineJoin   the join style for corners in the offset path
 * @param miterLimit the animated maximum ratio of miter length to stroke width before converting to bevel join
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OffsetPath(
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

        // OffsetPath
        @JsonProperty("a") Animated amount,
        @JsonProperty("lj") LineJoin lineJoin,
        @JsonProperty("ml") Animated miterLimit
) implements BaseShape {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Offset Path");
        list.add("Match name", matchName);
        list.add("Type", type);
        list.add("Hidden", hidden);
        list.add("Blend mode", blendMode);
        list.add("Index", index);
        list.add("Clazz", clazz);
        list.add("ID", id);
        list.add("d", d);
        list.add("cix", cix);
        list.add("Line join", lineJoin);
        list.add("Amount", amount);
        list.add("Miter limit", miterLimit);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.OFFSET_PATH;
    }
}
