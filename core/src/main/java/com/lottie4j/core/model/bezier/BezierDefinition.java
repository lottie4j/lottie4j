package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.ListListSerializer;

import java.util.List;

public record BezierDefinition(
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
