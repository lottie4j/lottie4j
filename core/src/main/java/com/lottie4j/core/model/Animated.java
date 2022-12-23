package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.KeyframeDeserializer;
import com.lottie4j.core.helper.KeyframeSerializer;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#animated-property
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Animated(
        @JsonProperty("a") Integer animated,

        @JsonProperty("k")
        @JsonSerialize(using = KeyframeSerializer.class)
        @JsonDeserialize(using = KeyframeDeserializer.class)
        List<Keyframe> keyframes,

        @JsonProperty("ix") Integer ix,
        @JsonProperty("l") Integer l
) {
}
