import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestJsonSerialization {
    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = ObjectMapperFactory.getInstance();

        // Load the animation
        Animation animation = LottieFileLoader.fromJsonFile("fxfileviewer/src/test/resources/dot/demo-3-animation.json");

        // Serialize it back to JSON
        String serialized = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(animation);

        // Write to a file for comparison
        Files.writeString(Paths.get("/tmp/demo-3-reserialized.json"), serialized);

        System.out.println("Serialized JSON written to /tmp/demo-3-reserialized.json");

        // Read original
        String original = Files.readString(Paths.get("fxfileviewer/src/test/resources/dot/demo-3-animation.json"));

        // Compare layer order in the EYE asset
        System.out.println("\n=== Checking EYE asset layers ===");

        // Extract the layers array from assets[0] (the EYE composition)
        int assetsStart = serialized.indexOf("\"assets\"");
        int layersInAsset = serialized.indexOf("\"layers\"", assetsStart);
        String snippet = serialized.substring(layersInAsset, Math.min(layersInAsset + 500, serialized.length()));
        System.out.println("Serialized snippet:\n" + snippet);
    }
}
