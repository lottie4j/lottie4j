package be.webtechie.jlottie.core.handler;

import be.webtechie.jlottie.core.model.Animation;
import be.webtechie.jlottie.core.model.Layer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileLoader {

    private static final ObjectMapper mapper = new ObjectMapper();

    private FileLoader() {
        // Hide constructor
    }

    public static String loadFileAsString(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    public static Layer parseLayer(String json) throws JsonProcessingException {
        return mapper.readValue(json, Layer.class);
    }

    public static Layer parseLayer(File f) throws IOException {
        return parseLayer(loadFileAsString(f));
    }

    public static Animation parseAnimation(String json) throws JsonProcessingException {
        return mapper.readValue(json, Animation.class);
    }

    public static Animation parseAnimation(File f) throws IOException {
        return parseAnimation(loadFileAsString(f));
    }
}
