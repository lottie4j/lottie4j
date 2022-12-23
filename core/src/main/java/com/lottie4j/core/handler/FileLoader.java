package com.lottie4j.core.handler;

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
}
