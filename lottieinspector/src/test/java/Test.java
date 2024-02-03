import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lottie4j.lottieinspector.MainApplication;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.logging.Logger;

public class Test {
    static File file;
    static Logger logger = Logger.getLogger("info");


    static {
        try {
            file = new File(MainApplication.class.getResource("lotties/loading1.json").toURI());
            logger.info(file.getAbsolutePath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        FileReader fileReader = new FileReader(file);
        JsonObject jsonObject = JsonParser.parseReader(fileReader).getAsJsonObject();
        handleJsonObject(jsonObject);
    }

    public static void handleJsonArray(JsonArray jsonArray) {
        logger.info("Array size: " + jsonArray.size() + "\n");
        for (JsonElement jsonElement : jsonArray) {
            if (jsonElement.isJsonPrimitive()) {
                logger.info(jsonElement.getAsString() + ": is a primitive" + "\n");
            } else if (jsonElement.isJsonObject()) {
                logger.info(String.valueOf(jsonElement.getAsJsonObject()) + "\n");
                handleJsonObject(jsonElement.getAsJsonObject());
            } else if (jsonElement.isJsonArray()) {
                logger.info("Nested Array:\n");
                handleJsonArray(jsonElement.getAsJsonArray());
            }
        }
    }

    public static void handleJsonObject(JsonObject jsonObject) {
        logger.info(String.valueOf(jsonObject));
        for (String key : jsonObject.keySet()) {
            JsonElement jsonElement = jsonObject.get(key);
            if (jsonElement.isJsonArray()) {
                logger.info(key + ": is an array" + "\n");
                handleJsonArray(jsonElement.getAsJsonArray());
            } else if (jsonElement.isJsonObject()) {
                logger.info(key + ": is an object" + "\n");
                handleJsonObject(jsonElement.getAsJsonObject());
            } else if (jsonElement.isJsonPrimitive()) {
                logger.info(key + ": is a primitive" + "\n");
            }
        }
    }
}
