package com.lottie4j.core.model.text;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;
import java.util.Map;

/**
 * Represents a keyframe for text animation in a Lottie composition.
 * <p>
 * This record encapsulates text content, styling information, and timing data for a specific
 * point in a text animation. The keyframe can contain either simple text content as a string
 * or a complex map structure that includes text and various styling properties such as font
 * family, font size, colors, and stroke information.
 * <p>
 * The styling data is stored in a flexible Object format that can be either a String for
 * simple text content or a Map containing detailed text and style attributes. When the style
 * data is a Map, it may include:
 * - "t": text content
 * - "s": font size
 * - "fc": font color as RGB array
 * - "f": font family name
 * - "sc": stroke color as RGB array
 * - "sw": stroke width
 * <p>
 * The keyframe's start time is specified in frames, indicating when this text state
 * should be active during the animation timeline.
 *
 * @param s the text content and styling data, which can be a String or a Map containing
 *          detailed text and style information
 * @param t the start time of this keyframe in frames
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextKeyframe(
        // Text content and styling data
        @JsonProperty("s") Object s,

        // Start time (frame)
        @JsonProperty("t") Double t
) implements PropertyListing {
    /**
     * Get the text string from this keyframe
     *
     * @return the text content, or empty string if unavailable
     */
    public String getText() {
        if (s == null) {
            return "";
        }
        if (s instanceof String str) {
            return str;
        }
        if (s instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) s;
            if (map.containsKey("t")) {
                Object text = map.get("t");
                return text != null ? text.toString() : "";
            }
        }
        return s.toString();
    }

    /**
     * Get font size from this keyframe
     *
     * @return the font size, or null if unavailable
     */
    public Double getFontSize() {
        if (s instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) s;
            Object size = map.get("s");
            if (size instanceof Number num) {
                return num.doubleValue();
            }
        }
        return null;
    }

    /**
     * Get font color (RGB) from this keyframe
     *
     * @return the font color as RGB array, or null
     */
    @SuppressWarnings("unchecked")
    public double[] getFontColor() {
        if (s instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) s;
            Object color = map.get("fc");
            if (color instanceof List) {
                List<Object> colorList = (List<Object>) color;
                if (colorList.size() >= 3) {
                    return new double[]{
                            ((Number) colorList.get(0)).doubleValue(),
                            ((Number) colorList.get(1)).doubleValue(),
                            ((Number) colorList.get(2)).doubleValue()
                    };
                }
            }
        }
        return null;
    }

    /**
     * Get font family from this keyframe
     *
     * @return the font family name, or null if unavailable
     */
    public String getFontFamily() {
        if (s instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) s;
            Object font = map.get("f");
            if (font != null) {
                return font.toString();
            }
        }
        return null;
    }

    /**
     * Get stroke color (RGB) from this keyframe
     *
     * @return the stroke color as RGB array, or null
     */
    @SuppressWarnings("unchecked")
    public double[] getStrokeColor() {
        if (s instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) s;
            Object color = map.get("sc");
            if (color instanceof List) {
                List<Object> colorList = (List<Object>) color;
                if (colorList.size() >= 3) {
                    return new double[]{
                            ((Number) colorList.get(0)).doubleValue(),
                            ((Number) colorList.get(1)).doubleValue(),
                            ((Number) colorList.get(2)).doubleValue()
                    };
                }
            }
        }
        return null;
    }

    /**
     * Get stroke width from this keyframe
     *
     * @return the stroke width, or null if unavailable
     */
    public Double getStrokeWidth() {
        if (s instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) s;
            Object width = map.get("sw");
            if (width instanceof Number num) {
                return num.doubleValue();
            }
        }
        return null;
    }

    /**
     * Get the full style object for advanced processing
     *
     * @return the style map, or null if unavailable
     */
    public Map<String, Object> getStyleMap() {
        if (s instanceof Map) {
            return (Map<String, Object>) s;
        }
        return null;
    }

    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Text Keyframe");
        list.add("Text", getText());
        list.add("Font Size", getFontSize());
        double[] color = getFontColor();
        if (color != null) {
            list.add("Font Color", String.format("RGB(%.2f, %.2f, %.2f)", color[0], color[1], color[2]));
        }
        list.add("Font Family", getFontFamily());
        list.add("Time", t);
        return list;
    }
}