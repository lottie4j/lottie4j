package com.lottie4j.core.model.shape;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.info.PropertyListingList;

/**
 * A shape of a type this library has no model for: one from a newer exporter, or one without a
 * type at all. It stands in for the shape so that the rest of the file can be read; it draws
 * nothing, and only its name survives, so writing the animation again does not restore it.
 *
 * @param name      the name of the shape
 * @param matchName the match name of the shape
 * @param hidden    whether the shape is hidden
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UnknownShape(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("hd") Boolean hidden
) implements BaseShape {

    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Unknown shape");
        list.add("Match name", matchName);
        list.add("Hidden", hidden);
        return list;
    }

    @Override
    public ShapeType shapeType() {
        return ShapeType.UNKNOWN;
    }
}
