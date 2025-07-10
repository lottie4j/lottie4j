package com.lottie4j.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.Animation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FileLoader {

    private FileLoader() {
        // Hide constructor
    }

    public static String loadFileAsString(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    public static Animation loadAnimation(File file) throws IOException {
        return (new ObjectMapper()).readValue(loadFileAsString(file), Animation.class);
    }
}
