package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.math.BigDecimal;

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
    public PropertyListingList getList() {
        var list = new PropertyListingList("Number Keyframe");
        list.add("Number value", this.doubleValue());
        return list;
    }
}

