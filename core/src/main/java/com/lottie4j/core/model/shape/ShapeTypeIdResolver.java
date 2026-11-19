package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.grouping.Transform;
import com.lottie4j.core.model.shape.modifier.Merge;
import com.lottie4j.core.model.shape.modifier.OffsetPath;
import com.lottie4j.core.model.shape.modifier.Pucker;
import com.lottie4j.core.model.shape.modifier.Repeater;
import com.lottie4j.core.model.shape.modifier.RoundedCorners;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.core.model.shape.modifier.Twist;
import com.lottie4j.core.model.shape.modifier.ZigZag;
import com.lottie4j.core.model.shape.shape.Ellipse;
import com.lottie4j.core.model.shape.shape.Path;
import com.lottie4j.core.model.shape.shape.Polystar;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.GradientStroke;
import com.lottie4j.core.model.shape.style.NoStyle;
import com.lottie4j.core.model.shape.style.Stroke;
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase;

import java.util.Map;

/**
 * Maps the {@code ty} of a shape to the record that models it, and back.
 * <p>
 * A type that is not in the list is not an error: the shape is read as an {@link UnknownShape},
 * which remembers its type and is written back with it.
 */
public final class ShapeTypeIdResolver extends TypeIdResolverBase {

    private static final Map<String, Class<? extends BaseShape>> TYPES = Map.ofEntries(
            Map.entry("el", Ellipse.class),
            Map.entry("fl", Fill.class),
            Map.entry("gf", GradientFill.class),
            Map.entry("gs", GradientStroke.class),
            Map.entry("gr", Group.class),
            Map.entry("mm", Merge.class),
            Map.entry("no", NoStyle.class),
            Map.entry("op", OffsetPath.class),
            Map.entry("sh", Path.class),
            Map.entry("sr", Polystar.class),
            Map.entry("pb", Pucker.class),
            Map.entry("rc", Rectangle.class),
            Map.entry("rp", Repeater.class),
            Map.entry("rd", RoundedCorners.class),
            Map.entry("st", Stroke.class),
            Map.entry("tr", Transform.class),
            Map.entry("tm", TrimPath.class),
            Map.entry("tw", Twist.class),
            Map.entry("zz", ZigZag.class));

    private JavaType baseType;

    /** Creates the resolver; Jackson does this. */
    public ShapeTypeIdResolver() {
        // nothing to set up
    }

    @Override
    public void init(JavaType type) {
        this.baseType = type;
    }

    @Override
    public String idFromValue(DatabindContext context, Object value) {
        if (value instanceof UnknownShape unknown) {
            return unknown.type() == null ? "" : unknown.type();
        }
        return ((BaseShape) value).shapeType().value();
    }

    @Override
    public String idFromValueAndType(DatabindContext context, Object value, Class<?> suggestedType) {
        return idFromValue(context, value);
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) {
        Class<? extends BaseShape> type = id == null ? null : TYPES.get(id);
        // null hands the shape to the default implementation, UnknownShape
        return type == null ? null : context.constructSpecializedType(baseType, type);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }
}
