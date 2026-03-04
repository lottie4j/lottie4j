package com.lottie4j.core.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.Animation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * Utility class for loading Lottie animation files from various sources.
 * Provides methods to deserialize JSON into {@link Animation} objects.
 */
public class LottieFileLoader {

    /**
     * Prevents instantiation of this utility class.
     */
    private LottieFileLoader() {
        // Hide constructor
    }

    /**
     * Loads a Lottie animation from a file.
     *
     * @param file JSON file containing the Lottie animation data
     * @return deserialized Animation object
     * @throws IOException if file reading or parsing fails
     */
    public static Animation load(File file) throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.getInstance();
        return mapper.readValue(loadAsString(file), Animation.class);
    }

    /**
     * Loads a Lottie animation from an input stream.
     *
     * @param inputStream stream containing JSON Lottie animation data
     * @return deserialized Animation object
     * @throws IOException if stream reading or parsing fails
     */
    public static Animation load(InputStream inputStream) throws IOException {
        ObjectMapper mapper = ObjectMapperFactory.getInstance();
        return mapper.readValue(inputStream, Animation.class);
    }

    /**
     * Reads a Lottie file as a raw JSON string.
     *
     * @param file JSON file to read
     * @return file contents as string
     * @throws IOException if file reading fails
     */
    public static String loadAsString(File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
