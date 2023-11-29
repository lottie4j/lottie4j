package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/concepts/#transform">Lottie Docs: Transform</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Transform(
        @JsonProperty("a") Animated anchor,
        @JsonProperty("p") Animated position,
        @JsonProperty("s") Animated scale,
        @JsonProperty("r") Animated rotation,
        @JsonProperty("rx") Animated rx,
        @JsonProperty("ry") Animated ry,
        @JsonProperty("rz") Animated rz,
        @JsonProperty("sk") Animated skew,
        @JsonProperty("sa") Animated skewAxis,
        @JsonProperty("o") Animated opacity,
        @JsonProperty("or") Animated unknown
) implements PropertyListing {
    @Override
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Anchor", "", anchor == null ? new ArrayList<>() : anchor.getLabelValues()),
                new PropertyLabelValue("Position", "", position == null ? new ArrayList<>() : position.getLabelValues()),
                new PropertyLabelValue("Scale", "", scale == null ? new ArrayList<>() : scale.getLabelValues()),
                new PropertyLabelValue("Rotation", "", rotation == null ? new ArrayList<>() : rotation.getLabelValues()),
                new PropertyLabelValue("RX", "", rx == null ? new ArrayList<>() : rx.getLabelValues()),
                new PropertyLabelValue("RY", "", ry == null ? new ArrayList<>() : ry.getLabelValues()),
                new PropertyLabelValue("RZ", "", rz == null ? new ArrayList<>() : rz.getLabelValues()),
                new PropertyLabelValue("Skew", "", skew == null ? new ArrayList<>() : skew.getLabelValues()),
                new PropertyLabelValue("Skew axis", "", skewAxis == null ? new ArrayList<>() : skewAxis.getLabelValues()),
                new PropertyLabelValue("Opacity", "", opacity == null ? new ArrayList<>() : opacity.getLabelValues()),
                new PropertyLabelValue("Unknown", "", unknown == null ? new ArrayList<>() : unknown.getLabelValues())
        );
    }
}
