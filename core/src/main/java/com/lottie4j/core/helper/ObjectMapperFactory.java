package com.lottie4j.core.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Factory class that provides a properly configured ObjectMapper instance
 * with all necessary modules registered.
 */
public class ObjectMapperFactory {

    private static final ObjectMapper INSTANCE = createObjectMapper();

    private ObjectMapperFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns a shared, thread-safe ObjectMapper instance configured with:
     * <ul>
     *     <li>Built-in JDK8 support (Optional, OptionalInt, etc.) provided by Jackson 3</li>
     *     <li>Proper handling of Java records with @JsonProperty annotations</li>
     *     <li>Ignores non-annotated methods</li>
     * </ul>
     *
     * @return configured ObjectMapper instance
     */
    public static ObjectMapper getInstance() {
        return INSTANCE;
    }

    private static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                // Note: SORT_PROPERTIES_ALPHABETICALLY is disabled to preserve the original property order
                // from the Lottie JSON, which is critical for features like track mattes that depend on
                // layer adjacency and property ordering
                .changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }
}
