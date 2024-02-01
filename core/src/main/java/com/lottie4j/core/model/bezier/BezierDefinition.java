package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.ListListSerializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

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
) implements PropertyListing {
    @Override
    public PropertyListingList getList() {
        var list = new PropertyListingList("Bezier Definition");
        list.add("Closed", closed);
        list.addDoubleDoubleList("Vertices", vertices);
        list.addDoubleDoubleList("Tangents in", tangentsIn);
        list.addDoubleDoubleList("Tangents out", tangentsOut);
        return list;
    }
}
