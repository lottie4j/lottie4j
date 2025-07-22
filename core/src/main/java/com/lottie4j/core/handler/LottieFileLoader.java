package com.lottie4j.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.Animation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LottieFileLoader {

    private LottieFileLoader() {
        // Hide constructor
    }

    public static Animation load(File file) throws IOException {
        return (new ObjectMapper()).readValue(loadAsString(file), Animation.class);
    }

    public static String loadAsString(File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
