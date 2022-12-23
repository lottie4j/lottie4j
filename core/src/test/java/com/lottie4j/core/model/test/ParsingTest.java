package com.lottie4j.core.model.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ParsingTest {
    @Test
    void fromJsonToJson() throws IOException, JSONException {
        File f = new File(this.getClass().getResource("/test.json").getFile());
        String jsonFromFile = Files.readString(f.toPath());

        ObjectMapper mapper = new ObjectMapper();
        Drawing drawing = mapper.readValue(jsonFromFile, Drawing.class);
        String jsonFromObject = mapper.writeValueAsString(drawing);

        System.out.println("Original:\n" + jsonFromFile.replace("\n", "").replace(" ", ""));
        System.out.println("Generated:\n" + jsonFromObject);

        assertAll(
                //() -> assertEquals(jsonFromFile, jsonFromObject),
                () -> assertEquals("testDrawing", drawing.name()),
                () -> assertTrue(drawing.shapes().get(0) instanceof ShapeA),
                () -> assertTrue(drawing.shapes().get(1) instanceof ShapeB),
                () -> assertTrue(drawing.shapes().get(2) instanceof ShapeC)
        );
    }
}
