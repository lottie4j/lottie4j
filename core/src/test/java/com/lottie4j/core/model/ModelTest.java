package com.lottie4j.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.bezier.FixedBezier;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;

/**
 * https://jsonassert.skyscreamer.org/
 */
public class ModelTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void fromJsonBezier() throws IOException, JSONException {
        File f = new File(this.getClass().getResource("/lottie/model/bezier.json").getFile());
        String fromJson = FileLoader.loadFileAsString(f);
        var bezier = mapper.readValue(fromJson, FixedBezier.class);

        ObjectMapper mapper = new ObjectMapper();
        String fromObject = mapper.writeValueAsString(bezier);

        System.out.println("Original:\n" + fromJson.replace("\n", "").replace(" ", ""));
        System.out.println("Generated:\n" + fromObject);

        JSONAssert.assertEquals(fromJson, fromObject, false);
    }
}
