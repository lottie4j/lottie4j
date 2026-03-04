package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;
import java.util.Map;

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