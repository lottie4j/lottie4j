package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

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
     *     <li>Jdk8Module for Optional, OptionalInt, etc. support</li>
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);

        // Configure to only serialize fields marked with @JsonProperty
        // This prevents serialization of helper methods like getList()
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return mapper;
    }
}
