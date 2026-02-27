package com.lottie4j.core.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.Animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class LottieFileLoader {

    private LottieFileLoader() {
        // Hide constructor
    }

    public static Animation load(File file) throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.getInstance();
        return mapper.readValue(loadAsString(file), Animation.class);
    }

    public static Animation load(InputStream inputStream) throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.getInstance();
        return mapper.readValue(inputStream, Animation.class);
    }

    public static String loadAsString(File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
