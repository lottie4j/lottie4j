package com.lottie4j.core.helper;

import tools.jackson.core.JacksonException;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.SerializationContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom serializer for nested lists of Double values (List of List of Double).
 * Formats numbers as integers when appropriate and handles coordinate pairs.
 */
public class ListListSerializer extends ValueSerializer {

    /**
     * Default constructor for ListListSerializer.
     */
    public ListListSerializer() {
        // Default constructor
    }

    /**
     * Serializes a nested list structure to JSON.
     * Each inner list is expected to contain coordinate pairs.
     *
     * @param o                  the object to serialize (expected to be a List of List of Doubles)
     * @param jsonGenerator      the JSON generator
     * @param serializerProvider the serializer provider
     * @throws JacksonException if serialization fails
     */
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializationContext serializerProvider) throws JacksonException {
        String rt = "[" +
                ((List<List<Double>>) o).stream()
                        .map(v -> "[" + getValue(v.getFirst()) + "," + getValue(v.get(1)) + "]")
                        .collect(Collectors.joining(",")) +
                "]";
        jsonGenerator.writeRawValue(rt);
    }

    /**
     * Formats a double value for JSON output.
     * Returns "0" for zero, integer format for whole numbers, otherwise decimal format.
     *
     * @param value the value to format
     * @return the formatted string
     */
    private String getValue(Double value) {
        return value == 0 ? "0" : (value % 1) == 0 ? String.valueOf(Math.round(value)) : String.valueOf(value);
    }
}
