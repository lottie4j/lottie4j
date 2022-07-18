package be.webtechie.jlottie.core.handler;

import be.webtechie.jlottie.core.model.Animation;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class FileLoader {

    private FileLoader() {
        // Hide constructor
    }

    public static Animation loadFile(File file) throws IOException {
        // create object mapper instance
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, Animation.class);
    }
}
