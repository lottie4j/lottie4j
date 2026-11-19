package com.lottie4j.core.helper;

import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.BezierKeyframe;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Completes keyframes written in the form Bodymovin used before version 5.
 * <p>
 * Today a keyframe has its own value in {@code s}, and the value a segment ends on is the
 * {@code s} of the next keyframe. The old form gives every keyframe its start value {@code s}
 * <em>and</em> its end value {@code e}, and closes the list with a keyframe that has a time only.
 * Read as the new form, that last keyframe has no value at all, and the property drops to 0 when
 * it is reached.
 * <p>
 * A keyframe without a value gets the end value of the keyframe before it, or, if that has none
 * either, its value. Keyframes that have a value are left as they are, so files in the new form
 * come out unchanged.
 */
public final class OldFormatKeyframes {

    /**
     * How to look at one kind of keyframe.
     *
     * @param hasValues whether an element of the list is a keyframe of this kind at all
     * @param value     the value of a keyframe
     * @param end       the end value of a keyframe
     * @param withValue a copy of a keyframe with another value
     * @param <K>       the keyframe
     * @param <V>       its value
     */
    private record Kind<K, V>(Predicate<K> hasValues, Function<K, List<V>> value, Function<K, List<V>> end,
                              BiFunction<K, List<V>, K> withValue) {
    }

    private static final Kind<Keyframe, BigDecimal> TIMED = new Kind<>(
            TimedKeyframe.class::isInstance,
            keyframe -> ((TimedKeyframe) keyframe).values(),
            keyframe -> ((TimedKeyframe) keyframe).unknown_e(),
            (keyframe, value) -> {
                TimedKeyframe timed = (TimedKeyframe) keyframe;
                return new TimedKeyframe(timed.time(), value, timed.unknown_e(), timed.easingIn(),
                        timed.easingOut(), timed.tangentOut(), timed.tangentIn(), timed.holdFrame());
            });

    private static final Kind<BezierKeyframe, BezierDefinition> BEZIER = new Kind<>(
            java.util.Objects::nonNull,
            BezierKeyframe::beziers,
            BezierKeyframe::endBeziers,
            (keyframe, value) -> new BezierKeyframe(keyframe.time(), keyframe.easingIn(), keyframe.easingOut(),
                    value, keyframe.endBeziers(), keyframe.holdFrame()));

    private OldFormatKeyframes() {
    }

    /**
     * Completes the keyframes of a number, vector or color property.
     *
     * @param keyframes the keyframes as read
     * @return the same list if nothing was missing, otherwise a copy with the missing values filled in
     */
    public static List<Keyframe> complete(List<Keyframe> keyframes) {
        return complete(keyframes, TIMED);
    }

    /**
     * Completes the keyframes of a path property.
     *
     * @param keyframes the keyframes as read
     * @return the same list if nothing was missing, otherwise a copy with the missing shapes filled in
     */
    public static List<BezierKeyframe> completeBeziers(List<BezierKeyframe> keyframes) {
        return complete(keyframes, BEZIER);
    }

    /**
     * Undoes {@link #complete(List)} for writing, so that a file in the old form is written as it
     * was read. A value that was filled in is the very list object of the keyframe before it,
     * which is how it is told apart from a value the file had.
     *
     * @param keyframes the keyframes of a property
     * @return the same list if nothing had been filled in, otherwise a copy without the filled-in values
     */
    public static List<Keyframe> restore(List<Keyframe> keyframes) {
        return restore(keyframes, TIMED);
    }

    /**
     * Undoes {@link #completeBeziers(List)} for writing; see {@link #restore(List)}.
     *
     * @param keyframes the keyframes of a path property
     * @return the same list if nothing had been filled in, otherwise a copy without the filled-in shapes
     */
    public static List<BezierKeyframe> restoreBeziers(List<BezierKeyframe> keyframes) {
        return restore(keyframes, BEZIER);
    }

    private static <K, V> List<K> complete(List<K> keyframes, Kind<K, V> kind) {
        List<K> result = keyframes;
        List<V> previousEnd = null;
        List<V> previousValue = null;
        for (int i = 0; i < keyframes.size(); i++) {
            K keyframe = keyframes.get(i);
            if (!kind.hasValues().test(keyframe)) {
                continue;
            }
            List<V> value = kind.value().apply(keyframe);
            if (isEmpty(value)) {
                value = isEmpty(previousEnd) ? previousValue : previousEnd;
                result = replaced(result, keyframes, i,
                        value == null ? keyframe : kind.withValue().apply(keyframe, value));
            }
            previousEnd = kind.end().apply(keyframe);
            previousValue = value;
        }
        return result;
    }

    private static <K, V> List<K> restore(List<K> keyframes, Kind<K, V> kind) {
        List<K> result = keyframes;
        K previous = null;
        for (int i = 0; i < keyframes.size(); i++) {
            K keyframe = keyframes.get(i);
            if (!kind.hasValues().test(keyframe)) {
                continue;
            }
            if (previous != null && wasFilledIn(kind.value().apply(keyframe), previous, kind)) {
                result = replaced(result, keyframes, i, kind.withValue().apply(keyframe, null));
            }
            previous = keyframe;
        }
        return result;
    }

    /** Whether a value is the very list of the keyframe before it: its end value, or else its value. */
    private static <K, V> boolean wasFilledIn(List<V> value, K previous, Kind<K, V> kind) {
        return value != null && (value == kind.end().apply(previous) || value == kind.value().apply(previous));
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /** Puts an element into a copy of the original list, which is made when the first one is replaced. */
    private static <K> List<K> replaced(List<K> result, List<K> original, int index, K element) {
        if (element == original.get(index)) {
            return result;
        }
        List<K> copy = result == original ? new ArrayList<>(original) : result;
        copy.set(index, element);
        return copy;
    }

    /** Writes the keyframes of a path property as they were read; see {@link #restoreBeziers(List)}. */
    public static final class BezierWriter
            extends tools.jackson.databind.util.StdConverter<List<BezierKeyframe>, List<BezierKeyframe>> {

        /** Creates the converter; Jackson does this. */
        public BezierWriter() {
            // nothing to set up
        }

        @Override
        public List<BezierKeyframe> convert(List<BezierKeyframe> value) {
            return restoreBeziers(value);
        }
    }
}
