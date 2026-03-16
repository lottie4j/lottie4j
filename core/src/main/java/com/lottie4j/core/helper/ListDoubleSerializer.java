package com.lottie4j.core.helper;

import tools.jackson.core.JacksonException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom serializer for lists of Double values.
 * Serializes the list as a JSON array.
 */
public class ListDoubleSerializer extends ValueSerializer {

    /**
     * Default constructor for ListDoubleSerializer.
     */
    public ListDoubleSerializer() {
        // Default constructor
    }

    /**
     * Serializes a list of Double values to a JSON array.
     *
     * @param o                  the object to serialize (expected to be a List of Doubles)
     * @param jsonGenerator      the JSON generator
     * @param serializerProvider the serializer provider
     * @throws IOException if serialization fails
     */
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializationContext serializerProvider) throws JacksonException {
        String rt = "[" +
                ((List<Double>) o).stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",")) +
                "]";
        jsonGenerator.writeRawValue(rt);
    }
}
