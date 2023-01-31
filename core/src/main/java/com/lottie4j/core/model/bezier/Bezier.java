package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "a")
@JsonSubTypes({
        @JsonSubTypes.Type(names = {"0", "0.0"}, value = FixedBezier.class),
        @JsonSubTypes.Type(names = {"1", "1.0"}, value = AnimatedBezier.class)
})
public interface Bezier {

}