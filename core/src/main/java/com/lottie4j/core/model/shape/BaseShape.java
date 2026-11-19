package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import tools.jackson.databind.annotation.JsonTypeIdResolver;

/**
 * Base interface for all shape types in Lottie animations.
 * <p>
 * This interface serves as the foundation for the shape hierarchy in Lottie animation files,
 * providing common properties and methods shared across all shape implementations. Shapes are
 * the fundamental building blocks of vector graphics in Lottie, representing geometric primitives,
 * modifiers, styles, and transforms.
 * <p>
 * The interface uses Jackson annotations for polymorphic JSON deserialization, allowing the
 * animation parser to automatically instantiate the correct concrete shape type based on the
 * "ty" property in the JSON structure. Each shape type is mapped to a specific two-character
 * identifier used in Lottie files, see {@link ShapeTypeIdResolver}; a shape of any other type is
 * read as an {@link UnknownShape}.
 * <p>
 * Supported shape types include:
 * - Geometric primitives: Ellipse (el), Rectangle (rc), Polystar (sr), Path (sh)
 * - Styles: Fill (fl), Stroke (st), GradientFill (gf), GradientStroke (gs), NoStyle (no)
 * - Modifiers: Merge (mm), Repeater (rp), TrimPath (tm), RoundedCorners (rd), OffsetPath (op)
 * - Effects: Pucker (pb), Twist (tw), ZigZag (zz)
 * - Organization: Group (gr), Transform (tr)
 * <p>
 * All shapes share common metadata properties including name, visibility, blend mode, and
 * ordering information. The interface provides default implementations for common accessors
 * while requiring concrete implementations to specify their shape type and property listings.
 * <p>
 * Implementing classes are responsible for defining their specific properties and behavior
 * while adhering to the common shape contract defined by this interface.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, property = "ty", defaultImpl = UnknownShape.class, visible = true)
@JsonTypeIdResolver(ShapeTypeIdResolver.class)
public interface BaseShape {
    /** Default name for the shape. */
    String name = "";

    /** Default match name used for identification and referencing. */
    String matchName = "";

    /** Default shape type. */
    ShapeType type = ShapeType.UNKNOWN;

    /** Default blend mode for compositing. */
    BlendMode blendMode = BlendMode.NORMAL;

    /** Default rendering order index. */
    Integer index = 0;

    /** Default CSS class name. */
    String clazz = "";

    /** Default unique identifier. */
    String id = "";

    /** Undefined property for potential future use. */
    Integer d = 0;

    /** Undefined property for potential future use. */
    Integer cix = 0;

    /**
     * Returns a property listing describing the shape structure.
     *
     * @return the property listing for this shape
     */
    PropertyListingList getList();

    /**
     * Returns the name of the shape.
     *
     * @return the shape name
     */
    default String name() {
        return name;
    }

    /**
     * Returns the specific type of this shape.
     *
     * @return the shape type
     */
    ShapeType shapeType();

    /**
     * Whether the shape is switched off in the file ({@code hd}). A hidden shape draws nothing
     * and, if it is a style or a modifier, applies to nothing.
     *
     * @return {@code true} if hidden; {@code null} or {@code false} otherwise
     */
    default Boolean hidden() {
        return Boolean.FALSE;
    }

    /**
     * Whether the shape is switched off in the file.
     *
     * @return true if {@link #hidden()} says so
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    default boolean isHidden() {
        return Boolean.TRUE.equals(hidden());
    }
}
