package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lottie4j.core.model.PropertyLabelValue;
import com.lottie4j.core.model.PropertyListing;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NumberKeyframe extends BigDecimal implements Keyframe, PropertyListing {


    public NumberKeyframe(Integer val) {
        super(val);
    }

    public NumberKeyframe(Double val) {
        super(val);
    }

    @Override
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Number value", this.doubleValue())
        );
    }
}

