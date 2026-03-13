package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.lottie4j.core.helper.ListListSerializer;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Represents a Bezier curve or path definition with vertices and control points.
 * This record defines a complete Bezier path structure including vertex positions,
 * incoming tangent control points, and outgoing tangent control points. The curve
 * can be either open or closed based on the closed flag.
 * <p>
 * The vertices define the anchor points of the Bezier path, while tangentsIn and
 * tangentsOut define the control points that determine the curvature at each vertex.
 * Each tangent is relative to its corresponding vertex.
 *
 * @param closed      indicates whether the Bezier path is closed (forms a loop)
 * @param vertices    list of coordinate pairs representing the anchor points of the path
 * @param tangentsIn  list of coordinate pairs representing the incoming tangent control points
 *                    relative to each vertex
 * @param tangentsOut list of coordinate pairs representing the outgoing tangent control points
 *                    relative to each vertex
 */
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
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Bezier Definition");
        list.add("Closed", closed);
        list.addDoubleDoubleList("Vertices", vertices);
        list.addDoubleDoubleList("Tangents in", tangentsIn);
        list.addDoubleDoubleList("Tangents out", tangentsOut);
        return list;
    }
}
