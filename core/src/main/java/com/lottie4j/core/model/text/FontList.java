package com.lottie4j.core.model.text;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The fonts of an animation ({@code fonts} at the top level of a Lottie file), which the text
 * layers refer to by name.
 *
 * @param list the fonts
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FontList(
        @JsonProperty("list") List<Font> list
) {

    /**
     * Finds the font a text document refers to with its {@code f}.
     *
     * @param name the name of the font ({@code fName})
     * @return the font, or {@code null} if there is no font of that name
     */
    public Font byName(String name) {
        if (list == null || name == null) {
            return null;
        }
        return list.stream().filter(font -> font != null && name.equals(font.name())).findFirst().orElse(null);
    }
}
