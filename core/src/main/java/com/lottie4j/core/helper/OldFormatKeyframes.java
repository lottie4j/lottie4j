package com.lottie4j.core.helper;

import com.lottie4j.core.model.bezier.BezierDefinition;
import com.lottie4j.core.model.bezier.BezierKeyframe;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

    private OldFormatKeyframes() {
    }

    /**
     * Completes the keyframes of a number, vector or color property.
     *
     * @param keyframes the keyframes as read; may be {@code null}
     * @return the same list if nothing was missing, otherwise a copy with the missing values filled in
     */
    public static List<Keyframe> complete(List<Keyframe> keyframes) {
        if (keyframes == null) {
            return null;
        }
        List<Keyframe> result = null;
        List<BigDecimal> previousEnd = null;
        List<BigDecimal> previousValue = null;
        for (int i = 0; i < keyframes.size(); i++) {
            if (!(keyframes.get(i) instanceof TimedKeyframe keyframe)) {
                continue;
            }
            List<BigDecimal> value = keyframe.values();
            if (value == null || value.isEmpty()) {
                value = previousEnd != null && !previousEnd.isEmpty() ? previousEnd : previousValue;
                if (value != null) {
                    if (result == null) {
                        result = new ArrayList<>(keyframes);
                    }
                    result.set(i, new TimedKeyframe(keyframe.time(), value, keyframe.unknown_e(),
                            keyframe.easingIn(), keyframe.easingOut(), keyframe.tangentOut(), keyframe.tangentIn(),
                            keyframe.holdFrame()));
                }
            }
            previousEnd = keyframe.unknown_e();
            previousValue = value;
        }
        return result == null ? keyframes : result;
    }

    /**
     * Completes the keyframes of a path property.
     *
     * @param keyframes the keyframes as read; may be {@code null}
     * @return the same list if nothing was missing, otherwise a copy with the missing shapes filled in
     */
    public static List<BezierKeyframe> completeBeziers(List<BezierKeyframe> keyframes) {
        if (keyframes == null) {
            return null;
        }
        List<BezierKeyframe> result = null;
        List<BezierDefinition> previousEnd = null;
        List<BezierDefinition> previousValue = null;
        for (int i = 0; i < keyframes.size(); i++) {
            BezierKeyframe keyframe = keyframes.get(i);
            if (keyframe == null) {
                continue;
            }
            List<BezierDefinition> value = keyframe.beziers();
            if (value == null || value.isEmpty()) {
                value = previousEnd != null && !previousEnd.isEmpty() ? previousEnd : previousValue;
                if (value != null) {
                    if (result == null) {
                        result = new ArrayList<>(keyframes);
                    }
                    result.set(i, new BezierKeyframe(keyframe.time(), keyframe.easingIn(), keyframe.easingOut(),
                            value, keyframe.endBeziers()));
                }
            }
            previousEnd = keyframe.endBeziers();
            previousValue = value;
        }
        return result == null ? keyframes : result;
    }

    /**
     * Undoes {@link #complete(List)} for writing, so that a file in the old form is written as it
     * was read. A value that was filled in is the very list object of the keyframe before it,
     * which is how it is told apart from a value the file had.
     *
     * @param keyframes the keyframes of a property; may be {@code null}
     * @return the same list if nothing had been filled in, otherwise a copy without the filled-in values
     */
    public static List<Keyframe> restore(List<Keyframe> keyframes) {
        if (keyframes == null) {
            return null;
        }
        List<Keyframe> result = null;
        TimedKeyframe previous = null;
        for (int i = 0; i < keyframes.size(); i++) {
            if (!(keyframes.get(i) instanceof TimedKeyframe keyframe)) {
                continue;
            }
            if (previous != null && keyframe.values() != null
                    && (keyframe.values() == previous.unknown_e() || keyframe.values() == previous.values())) {
                if (result == null) {
                    result = new ArrayList<>(keyframes);
                }
                result.set(i, new TimedKeyframe(keyframe.time(), null, keyframe.unknown_e(), keyframe.easingIn(),
                        keyframe.easingOut(), keyframe.tangentOut(), keyframe.tangentIn(), keyframe.holdFrame()));
            }
            previous = keyframe;
        }
        return result == null ? keyframes : result;
    }

    /**
     * Undoes {@link #completeBeziers(List)} for writing; see {@link #restore(List)}.
     *
     * @param keyframes the keyframes of a path property; may be {@code null}
     * @return the same list if nothing had been filled in, otherwise a copy without the filled-in shapes
     */
    public static List<BezierKeyframe> restoreBeziers(List<BezierKeyframe> keyframes) {
        if (keyframes == null) {
            return null;
        }
        List<BezierKeyframe> result = null;
        BezierKeyframe previous = null;
        for (int i = 0; i < keyframes.size(); i++) {
            BezierKeyframe keyframe = keyframes.get(i);
            if (keyframe == null) {
                continue;
            }
            if (previous != null && keyframe.beziers() != null
                    && (keyframe.beziers() == previous.endBeziers() || keyframe.beziers() == previous.beziers())) {
                if (result == null) {
                    result = new ArrayList<>(keyframes);
                }
                result.set(i, new BezierKeyframe(keyframe.time(), keyframe.easingIn(), keyframe.easingOut(), null,
                        keyframe.endBeziers()));
            }
            previous = keyframe;
        }
        return result == null ? keyframes : result;
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
