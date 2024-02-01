package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.lottie4j.core.info.PropertyListingList;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = NumberKeyframe.class)
@JsonSubTypes({
        @JsonSubTypes.Type(NumberKeyframe.class),
        @JsonSubTypes.Type(TimedKeyframe.class),
})
public interface Keyframe {

    PropertyListingList getList();
}