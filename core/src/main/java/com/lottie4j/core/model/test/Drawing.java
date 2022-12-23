package com.lottie4j.core.model.test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.lottie4j.core.helper.TestDeserializer;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Drawing(
        @JsonProperty("name")
        String name,

        @JsonProperty("shapes")
        @JsonDeserialize(using = TestDeserializer.class)
        List<Object> shapes
) {
}
