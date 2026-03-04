package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.TextData;
import com.lottie4j.core.model.TextKeyframe;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Renders text layers from Lottie animations.
 * Supports text rendering with font size, color, and family from animation data.
 */
public class TextRenderer {

    private static final Logger logger = Logger.getLogger(TextRenderer.class.getName());
    private static final double DEFAULT_FONT_SIZE = 24;
    private static final String DEFAULT_FONT_FAMILY = "Arial";

    /**
     * Render a text layer
     */
    public void render(GraphicsContext gc, Layer layer, double frame) {
        if (layer.textData() == null || layer.textData().document() == null || layer.textData().document().keyframes() == null || layer.textData().document().keyframes().isEmpty()) {
            return;
        }

        TextData textData = layer.textData();
        TextKeyframe keyframe = getKeyframeAtFrame(textData.document().keyframes(), frame);
        if (keyframe == null) {
            return;
        }

        String text = keyframe.getText();
        if (text == null || text.isBlank()) {
            return;
        }

        gc.save();

        Double baseSize = keyframe.getFontSize();
        if (baseSize == null) {
            baseSize = DEFAULT_FONT_SIZE;
        }
        // Keep size source from text keyframe; animator fs/fh/fb are fill HSB channels in this file.
        double fontSize = baseSize;

        String fontFamily = keyframe.getFontFamily();
        if (fontFamily == null || fontFamily.isEmpty()) {
            fontFamily = DEFAULT_FONT_FAMILY;
        }
        gc.setFont(new Font(fontFamily, fontSize));

        gc.setTextAlign(mapJustification(keyframe));
        gc.setTextBaseline(VPos.BASELINE);

        applyTextAnimatorTransform(gc, textData, frame);

        Color fill = getAnimatorFillColor(textData, frame);
        if (fill == null) {
            double[] rgb = keyframe.getFontColor();
            fill = (rgb != null && rgb.length >= 3) ? Color.color(rgb[0], rgb[1], rgb[2]) : Color.BLACK;
        }
        gc.setFill(fill);

        double[] strokeRgb = keyframe.getStrokeColor();
        Double strokeWidth = keyframe.getStrokeWidth();
        if (strokeRgb != null && strokeRgb.length >= 3 && strokeWidth != null && strokeWidth > 0) {
            gc.setStroke(Color.color(strokeRgb[0], strokeRgb[1], strokeRgb[2]));
            gc.setLineWidth(strokeWidth);
            gc.strokeText(text, 0, 0);
        }

        gc.fillText(text, 0, 0);
        gc.restore();
    }

    /**
     * Get text keyframe at a specific frame
     */
    private TextKeyframe getKeyframeAtFrame(List<TextKeyframe> keyframes, double frame) {
        TextKeyframe current = keyframes.get(0);
        for (TextKeyframe kf : keyframes) {
            if (kf.t() != null && kf.t() <= frame) {
                current = kf;
            } else {
                break;
            }
        }
        return current;
    }

    /**
     * Map Lottie text alignment to JavaFX TextAlignment
     */
    private TextAlignment mapJustification(TextKeyframe keyframe) {
        Map<String, Object> style = keyframe.getStyleMap();
        if (style == null) {
            return TextAlignment.LEFT;
        }
        Object j = style.get("j");
        if (!(j instanceof Number n)) {
            return TextAlignment.LEFT;
        }
        int v = n.intValue();
        // Lottie: 0 left, 1 right, 2 center
        return switch (v) {
            case 1 -> TextAlignment.RIGHT;
            case 2 -> TextAlignment.CENTER;
            default -> TextAlignment.LEFT;
        };
    }

    /**
     * Get fill color from text animators, with support for RGB and HSB
     */
    private Color getAnimatorFillColor(TextData textData, double frame) {
        if (textData.animators() == null) {
            return null;
        }
        for (Map<String, Object> animator : textData.animators()) {
            Object aObj = animator.get("a");
            if (!(aObj instanceof Map<?, ?> raw)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> aMap = (Map<String, Object>) raw;

            // Prefer HSB override when present (matches this animation's behavior)
            Double h = getAnimatedNumberFromAnimatorMap(aMap, "fh", frame);
            Double s = getAnimatedNumberFromAnimatorMap(aMap, "fs", frame);
            Double br = getAnimatedNumberFromAnimatorMap(aMap, "fb", frame);
            if (h != null && s != null && br != null) {
                return Color.hsb(h, clamp01(s / 100.0), clamp01(br / 100.0));
            }

            // Fallback: direct RGB fill color (supports static and keyframed forms)
            Object fc = aMap.get("fc");
            double[] rgb = evalAnimatedColor(fc, frame);
            if (rgb != null) {
                return Color.color(clamp01(rgb[0]), clamp01(rgb[1]), clamp01(rgb[2]));
            }
        }
        return null;
    }

    /**
     * Apply text animator transform (position, scale, etc.)
     */
    private void applyTextAnimatorTransform(GraphicsContext gc, TextData textData, double frame) {
        if (textData.animators() == null) {
            return;
        }
        for (Map<String, Object> animator : textData.animators()) {
            Object aObj = animator.get("a");
            if (!(aObj instanceof Map<?, ?> raw)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> aMap = (Map<String, Object>) raw;

            List<Double> p = getAnimatedVec2(aMap.get("p"), frame);
            if (p != null) {
                gc.translate(p.get(0), p.get(1));
            }

            List<Double> s = getAnimatedVec2(aMap.get("s"), frame);
            if (s != null) {
                gc.scale(s.get(0) / 100.0, s.get(1) / 100.0);
            }

            List<Double> a = getAnimatedVec2(aMap.get("a"), frame);
            if (a != null) {
                gc.translate(-a.get(0), -a.get(1));
            }
        }
    }

    /**
     * Get animated Vec2 (2D vector) from animator property
     */
    private List<Double> getAnimatedVec2(Object prop, double frame) {
        if (!(prop instanceof Map<?, ?> raw)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> propMap = (Map<String, Object>) raw;
        Object k = propMap.get("k");

        if (k instanceof List<?> list && list.size() >= 2 && list.get(0) instanceof Number x && list.get(1) instanceof Number y) {
            return List.of(x.doubleValue(), y.doubleValue());
        }

        if (k instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            return evalKeyframedVec2(list, frame);
        }

        return null;
    }

    private List<Double> evalKeyframedVec2(List<?> keyframes, double frame) {
        Map<String, Object> prev = null;
        Map<String, Object> next = null;

        for (Object o : keyframes) {
            if (!(o instanceof Map<?, ?> raw)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> kf = (Map<String, Object>) raw;
            double t = readTime(kf);
            if (t <= frame) {
                prev = kf;
            } else {
                next = kf;
                break;
            }
        }

        if (prev == null && !keyframes.isEmpty() && keyframes.get(0) instanceof Map<?, ?> raw) {
            @SuppressWarnings("unchecked")
            Map<String, Object> kf = (Map<String, Object>) raw;
            return readVec2FromS(kf.get("s"));
        }
        if (prev == null) {
            return null;
        }

        List<Double> p0 = readVec2FromS(prev.get("s"));
        if (p0 == null) {
            return null;
        }
        if (next == null) {
            return p0;
        }

        List<Double> p1 = readVec2FromS(next.get("s"));
        if (p1 == null) {
            return p0;
        }

        double t0 = readTime(prev);
        double t1 = readTime(next);
        if (t1 <= t0) {
            return p0;
        }

        double u = clamp01((frame - t0) / (t1 - t0));
        return List.of(
                p0.get(0) + (p1.get(0) - p0.get(0)) * u,
                p0.get(1) + (p1.get(1) - p0.get(1)) * u
        );
    }

    private List<Double> readVec2FromS(Object sObj) {
        if (sObj instanceof List<?> sList && sList.size() >= 2 && sList.get(0) instanceof Number x && sList.get(1) instanceof Number y) {
            return List.of(x.doubleValue(), y.doubleValue());
        }
        return null;
    }

    private Double evalAnimatedNumber(Object k, double frame) {
        if (k instanceof Number n) {
            return n.doubleValue();
        }
        if (k instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            Map<String, Object> prev = null;
            Map<String, Object> next = null;

            for (Object o : list) {
                if (!(o instanceof Map<?, ?> rawKf)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> kf = (Map<String, Object>) rawKf;
                double t = readTime(kf);
                if (t <= frame) {
                    prev = kf;
                } else {
                    next = kf;
                    break;
                }
            }

            if (prev == null && !list.isEmpty() && list.get(0) instanceof Map<?, ?> rawKf) {
                @SuppressWarnings("unchecked")
                Map<String, Object> first = (Map<String, Object>) rawKf;
                return readScalarFromS(first.get("s"));
            }
            if (prev == null) {
                return null;
            }

            Double v0 = readScalarFromS(prev.get("s"));
            if (v0 == null) {
                return null;
            }
            if (next == null) {
                return v0;
            }

            Double v1 = readScalarFromS(next.get("s"));
            if (v1 == null) {
                return v0;
            }

            double t0 = readTime(prev);
            double t1 = readTime(next);
            if (t1 <= t0) {
                return v0;
            }

            double u = clamp01((frame - t0) / (t1 - t0));
            return v0 + (v1 - v0) * u;
        }
        return null;
    }

    private Double readScalarFromS(Object sObj) {
        if (sObj instanceof Number sn) {
            return sn.doubleValue();
        }
        if (sObj instanceof List<?> sv && !sv.isEmpty() && sv.get(0) instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private double readTime(Map<String, Object> kf) {
        Object tObj = kf.get("t");
        return (tObj instanceof Number tn) ? tn.doubleValue() : Double.NEGATIVE_INFINITY;
    }

    private double[] evalAnimatedColor(Object prop, double frame) {
        if (!(prop instanceof Map<?, ?> raw)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> propMap = (Map<String, Object>) raw;
        Object k = propMap.get("k");

        // static rgb list
        if (k instanceof List<?> list && list.size() >= 3 && list.get(0) instanceof Number r && list.get(1) instanceof Number g && list.get(2) instanceof Number b) {
            return new double[]{r.doubleValue(), g.doubleValue(), b.doubleValue()};
        }

        // keyframed color
        if (k instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?>) {
            Map<String, Object> prev = null;
            Map<String, Object> next = null;
            for (Object o : list) {
                if (!(o instanceof Map<?, ?> rawKf)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> kf = (Map<String, Object>) rawKf;
                double t = readTime(kf);
                if (t <= frame) {
                    prev = kf;
                } else {
                    next = kf;
                    break;
                }
            }
            if (prev == null && !list.isEmpty() && list.get(0) instanceof Map<?, ?> rawKf) {
                @SuppressWarnings("unchecked")
                Map<String, Object> first = (Map<String, Object>) rawKf;
                return readColorFromS(first.get("s"));
            }
            if (prev == null) {
                return null;
            }
            double[] c0 = readColorFromS(prev.get("s"));
            if (c0 == null) {
                return null;
            }
            if (next == null) {
                return c0;
            }
            double[] c1 = readColorFromS(next.get("s"));
            if (c1 == null) {
                return c0;
            }
            double t0 = readTime(prev);
            double t1 = readTime(next);
            if (t1 <= t0) {
                return c0;
            }
            double u = clamp01((frame - t0) / (t1 - t0));
            return new double[]{
                    c0[0] + (c1[0] - c0[0]) * u,
                    c0[1] + (c1[1] - c0[1]) * u,
                    c0[2] + (c1[2] - c0[2]) * u
            };
        }

        return null;
    }

    private double[] readColorFromS(Object sObj) {
        if (sObj instanceof List<?> list && list.size() >= 3 && list.get(0) instanceof Number r && list.get(1) instanceof Number g && list.get(2) instanceof Number b) {
            return new double[]{r.doubleValue(), g.doubleValue(), b.doubleValue()};
        }
        return null;
    }

    /**
     * Clamp value between 0 and 1
     */
    private double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    /**
     * Read a numeric animator property by key (supports static and keyframed values).
     */
    private Double getAnimatedNumberFromAnimatorMap(Map<String, Object> aMap, String key, double frame) {
        Object prop = aMap.get(key);
        if (!(prop instanceof Map<?, ?> raw)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> propMap = (Map<String, Object>) raw;
        Object k = propMap.get("k");
        return evalAnimatedNumber(k, frame);
    }
}
