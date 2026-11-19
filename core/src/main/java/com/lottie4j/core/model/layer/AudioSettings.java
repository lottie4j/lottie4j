package com.lottie4j.core.model.layer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.model.animation.Animated;

/**
 * The audio settings ({@code au}) of an audio layer.
 * <p>
 * The sound itself is the asset the layer refers to with its reference id. The player does not
 * play sound; the settings are read and written so that a file keeps them and other tools can
 * use them.
 *
 * @param level the audio levels ({@code lv}): the left and the right channel, each in percent
 *              with 100 being the sound as it is
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AudioSettings(
        @JsonProperty("lv") Animated level
) {
}
