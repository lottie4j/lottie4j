package com.lottie4j.core.helper;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Visits everything an animation, a layer or any other part of the model is made of.
 * <p>
 * The model is records and lists of records all the way down, so a walk is: look at a record,
 * then at each of its components. That answers questions that are about the whole file and not
 * about one kind of object, such as "which properties have an expression?", without code that
 * knows every record there is.
 */
public final class ModelWalker {

    /** Deeper than any model goes; a stop for a structure that should not exist. */
    private static final int MAX_DEPTH = 200;

    private ModelWalker() {
    }

    /**
     * Visits a part of the model and everything below it, parents before their components.
     *
     * @param root    an animation, a layer, a shape, a property...; {@code null} visits nothing
     * @param visitor called with every record met on the way, the root included
     */
    public static void visit(Object root, Consumer<Object> visitor) {
        visit(root, visitor, 0);
    }

    private static void visit(Object value, Consumer<Object> visitor, int depth) {
        if (value == null || depth > MAX_DEPTH) {
            return;
        }
        if (value instanceof List<?> list) {
            list.forEach(element -> visit(element, visitor, depth + 1));
            return;
        }
        if (!value.getClass().isRecord()) {
            return;
        }
        visitor.accept(value);
        for (Method accessor : ACCESSORS.get(value.getClass())) {
            visit(read(accessor, value), visitor, depth + 1);
        }
    }

    /**
     * The accessors of the components of a record class that can hold more of the model: those
     * of numbers, texts, flags and definitions lead nowhere and are left out. Looked up once per
     * class; asking a class for its components on every visit is what makes a walk slow.
     */
    private static final ClassValue<Method[]> ACCESSORS = new ClassValue<>() {
        @Override
        protected Method[] computeValue(Class<?> type) {
            return Arrays.stream(type.getRecordComponents())
                    .filter(component -> !isLeaf(component.getType()))
                    .map(RecordComponent::getAccessor)
                    .toArray(Method[]::new);
        }
    };

    private static boolean isLeaf(Class<?> type) {
        return type.isPrimitive() || type.isEnum() || type == String.class || Number.class.isAssignableFrom(type)
                || type == Boolean.class;
    }

    /** The value of a record component; {@code null} if it cannot be read, which leaves nothing to visit. */
    private static Object read(Method accessor, Object owner) {
        try {
            return accessor.invoke(owner);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
