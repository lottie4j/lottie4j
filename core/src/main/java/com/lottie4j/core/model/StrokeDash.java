package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.StrokeDashType;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/shapes/#stroke">Lottie Docs: Stroke</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record StrokeDash(
        @JsonProperty("nm") String name,
        @JsonProperty("n") StrokeDashType type,
        @JsonProperty("v") Animated length
) implements PropertyListing {
    @Override
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Type", type == null ? "-" : type.label()),
                new PropertyLabelValue("Length", "", length == null ? new ArrayList<>() : length.getLabelValues())
        );
    }
}
