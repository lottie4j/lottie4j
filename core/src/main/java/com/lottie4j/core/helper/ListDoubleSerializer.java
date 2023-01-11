package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class ListDoubleSerializer extends JsonSerializer {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        String rt = "[" +
                ((List<Double>) o).stream()
                        .map(d -> String.valueOf(d))
                        .collect(Collectors.joining(",")) +
                "]";
        jsonGenerator.writeRawValue(rt);
    }
}
