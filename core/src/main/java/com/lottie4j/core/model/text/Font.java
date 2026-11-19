package com.lottie4j.core.model.text;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Locale;

/**
 * One font of an animation.
 *
 * @param name     the name text documents refer to ({@code fName})
 * @param family   the font family ({@code fFamily}), such as "Arial"
 * @param style    the style ({@code fStyle}), such as "Regular", "Bold" or "Bold Italic"
 * @param weight   the weight ({@code fWeight}), such as "700"; often absent
 * @param ascent   the ascent as a percentage of the font size
 * @param path     where the font comes from ({@code fPath}): a URL or a file name, depending on the origin
 * @param origin   the kind of source ({@code origin}): 0 local, 1 CSS URL, 2 script URL, 3 font URL
 * @param cssClass the CSS class of the font ({@code fClass})
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Font(
        @JsonProperty("fName") String name,
        @JsonProperty("fFamily") String family,
        @JsonProperty("fStyle") String style,
        @JsonProperty("fWeight") String weight,
        @JsonProperty("ascent") Double ascent,
        @JsonProperty("fPath") String path,
        @JsonProperty("origin") Integer origin,
        @JsonProperty("fClass") String cssClass
) {

    /**
     * Whether the font is a bold one, as far as the file says: by a style that names a heavy
     * weight ("Bold", "Black", "Heavy", "ExtraBold"...) or a numeric weight of 600 or more.
     *
     * @return true for a bold font
     */
    @JsonIgnore
    public boolean isBold() {
        String text = style == null ? "" : style.toLowerCase(Locale.ROOT);
        if (text.contains("bold") || text.contains("black") || text.contains("heavy")) {
            return true;
        }
        try {
            return weight != null && Double.parseDouble(weight.trim()) >= 600;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Whether the font is slanted, as far as the file says: a style that contains "Italic" or
     * "Oblique".
     *
     * @return true for an italic or oblique font
     */
    @JsonIgnore
    public boolean isItalic() {
        String text = style == null ? "" : style.toLowerCase(Locale.ROOT);
        return text.contains("italic") || text.contains("oblique");
    }
}
