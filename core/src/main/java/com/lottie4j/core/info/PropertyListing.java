package com.lottie4j.core.info;

/**
 * Interface implemented by all core models to produce readable output of the structure of a Lottie file.
 */
public interface PropertyListing {

    /**
     * Returns a structured property listing for this object.
     *
     * @return property listing with labels and values
     */
    PropertyListingList getList();
}
