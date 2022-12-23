package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#bezier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Bezier(
        @JsonProperty("c") Boolean closed,
        @JsonProperty("v") List<List<Integer>> vertices,
        @JsonProperty("i") List<List<Integer>> tangentsIn,
        @JsonProperty("o") List<List<Integer>> tangentsOut
) {
}
