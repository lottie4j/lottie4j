package com.lottie4j.core.model.bezier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lottie4j.core.info.PropertyListingList;

public interface Bezier {
    @JsonIgnore
    PropertyListingList getList();
}