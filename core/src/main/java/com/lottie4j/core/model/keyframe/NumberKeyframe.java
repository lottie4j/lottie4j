package com.lottie4j.core.model.keyframe;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.math.BigDecimal;

/**
 * Represents a simple numeric keyframe that stores a single numeric value.
 * <p>
 * NumberKeyframe extends BigDecimal to provide precise numeric storage while implementing
 * the Keyframe interface for use in animation sequences. This implementation is used for
 * static numeric values or as the default keyframe type when specific timing information
 * is not required.
 * <p>
 * The class supports construction from both Integer and Double values, which are internally
 * stored with BigDecimal precision. This ensures accurate representation of numeric animation
 * values without floating-point precision issues.
 * <p>
 * As the default implementation for the Keyframe interface, this class is automatically
 * selected during JSON deserialization when the specific keyframe type cannot be deduced.
 * It provides a simple, lightweight representation for constant numeric values in animations.
 * <p>
 * The class ignores unknown JSON properties during deserialization and excludes null values
 * during serialization to maintain clean JSON output.
 *
 * @see Keyframe
 * @see TimedKeyframe
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NumberKeyframe extends BigDecimal implements Keyframe, PropertyListing {
    /**
     * Constructs a NumberKeyframe with an integer value.
     *
     * @param val the integer value
     */
    public NumberKeyframe(Integer val) {
        super(val);
    }

    /**
     * Constructs a NumberKeyframe with a double value.
     *
     * @param val the double value
     */
    public NumberKeyframe(Double val) {
        super(val);
    }

    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Number Keyframe");
        list.add("Number value", this.doubleValue());
        return list;
    }
}

