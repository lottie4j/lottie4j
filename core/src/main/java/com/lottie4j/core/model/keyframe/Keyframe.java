package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(TimedKeyframe.class),
        @JsonSubTypes.Type(NumberKeyframe.class)
})
public interface Keyframe {

}