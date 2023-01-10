package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.ListListSerializer;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/concepts/#bezier
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Bezier(
        @JsonProperty("c") Boolean closed,
        @JsonProperty("v")
        @JsonSerialize(using = ListListSerializer.class)
        List<List<Double>> vertices,
        @JsonProperty("i")
        @JsonSerialize(using = ListListSerializer.class)
        List<List<Double>> tangentsIn,
        @JsonProperty("o")
        @JsonSerialize(using = ListListSerializer.class)
        List<List<Double>> tangentsOut
) {
}
