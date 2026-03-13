package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.core.model.shape.modifier.*;
import com.lottie4j.core.model.shape.shape.Ellipse;
import com.lottie4j.core.model.shape.shape.Path;
import com.lottie4j.core.model.shape.shape.Polystar;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.core.model.shape.style.*;

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
 * identifier used in Lottie files.
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
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "ty")
@JsonSubTypes({
        @JsonSubTypes.Type(name = "el", value = Ellipse.class),
        @JsonSubTypes.Type(name = "fl", value = Fill.class),
        @JsonSubTypes.Type(name = "gf", value = GradientFill.class),
        @JsonSubTypes.Type(name = "gs", value = GradientStroke.class),
        @JsonSubTypes.Type(name = "gr", value = Group.class),
        @JsonSubTypes.Type(name = "mm", value = Merge.class),
        @JsonSubTypes.Type(name = "no", value = NoStyle.class),
        @JsonSubTypes.Type(name = "op", value = OffsetPath.class),
        @JsonSubTypes.Type(name = "sh", value = Path.class),
        @JsonSubTypes.Type(name = "sr", value = Polystar.class),
        @JsonSubTypes.Type(name = "pb", value = Pucker.class),
        @JsonSubTypes.Type(name = "rc", value = Rectangle.class),
        @JsonSubTypes.Type(name = "rp", value = Repeater.class),
        @JsonSubTypes.Type(name = "rd", value = RoundedCorners.class),
        @JsonSubTypes.Type(name = "st", value = Stroke.class),
        @JsonSubTypes.Type(name = "tr", value = Transform.class),
        @JsonSubTypes.Type(name = "tm", value = TrimPath.class),
        @JsonSubTypes.Type(name = "tw", value = Twist.class),
        @JsonSubTypes.Type(name = "zz", value = ZigZag.class)
})
public interface BaseShape {
    String name = "";
    String matchName = "";
    ShapeType type = ShapeType.UNKNOWN;
    Boolean hidden = false;
    BlendMode blendMode = BlendMode.NORMAL;
    Integer index = 0;
    String clazz = "";
    String id = "";

    // Undefined
    Integer d = 0;
    Integer cix = 0;

    PropertyListingList getList();

    default String name() {
        return name;
    }

    ShapeType shapeType();
}
