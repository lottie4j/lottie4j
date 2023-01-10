package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ListListSerializer extends JsonSerializer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String rt = "[" +
                ((List<List<Double>>) o).stream()
                        .map(v -> "[" + getValue(v.get(0)) + "," + getValue(v.get(1)) + "]")
                        .collect(Collectors.joining(",")) +
                "]";
        jsonGenerator.writeRawValue(rt);
    }

    private String getValue(Double value) {
        return value == 0 ? "0" : (value % 1) == 0 ? String.valueOf(Math.round(value)) : String.valueOf(value);
    }
}
