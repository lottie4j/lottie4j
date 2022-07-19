package be.webtechie.jlottie.core.model;

import be.webtechie.jlottie.core.handler.FileLoader;
import be.webtechie.jlottie.core.model.Animation;
import be.webtechie.jlottie.core.model.Layer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;

public class ModelTest {

    /**
     * https://jsonassert.skyscreamer.org/
     */
    @Test
    void fromJsonToJsonSmallFileSingleLayerNoShapes() throws IOException, JSONException {
        File f = new File(this.getClass().getResource("/lottie/java_duke_single_layer_no_shapes.json").getFile());
        String fromJson = FileLoader.loadFileAsString(f);
        Layer animationFromJson = FileLoader.parseLayer(fromJson);

        ObjectMapper mapper = new ObjectMapper();
        String fromObject = mapper.writeValueAsString(animationFromJson);

        System.out.println("Original:\n" + fromJson.replace("\n", "").replace(" ", ""));
        System.out.println("Generated:\n" + fromObject);

        JSONAssert.assertEquals(fromJson, fromObject, false);
    }

    @Test
    void fromJsonToJsonSmallFileSingleLayer() throws IOException, JSONException {
        File f = new File(this.getClass().getResource("/lottie/java_duke_single_layer.json").getFile());
        String fromJson = FileLoader.loadFileAsString(f);
        Layer animationFromJson = FileLoader.parseLayer(fromJson);

        ObjectMapper mapper = new ObjectMapper();
        String fromObject = mapper.writeValueAsString(animationFromJson);

        System.out.println("Original:\n" + fromJson.replace("\n", "").replace(" ", ""));
        System.out.println("Generated:\n" + fromObject);

        JSONAssert.assertEquals(fromJson, fromObject, false);
    }

    @Test
    void fromJsonToJsonSmallFile() throws IOException, JSONException {
        File f = new File(this.getClass().getResource("/lottie/java_duke.json").getFile());
        String fromJson = FileLoader.loadFileAsString(f);
        Animation animationFromJson = FileLoader.parseAnimation(fromJson);

        ObjectMapper mapper = new ObjectMapper();
        String fromObject = mapper.writeValueAsString(animationFromJson);

        System.out.println("Original:\n" + fromJson.replace("\n", "").replace(" ", ""));
        System.out.println("Generated:\n" + fromObject);

        JSONAssert.assertEquals(fromJson, fromObject, false);
    }
}
