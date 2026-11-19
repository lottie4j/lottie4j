package com.lottie4j.fxplayer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;

import java.nio.file.Files;
import java.nio.file.Path;

/** Small Lottie documents for tests, written out as JSON text. */
final class TestLottie {

    static final String KS = "\"ks\":{\"o\":{\"a\":0,\"k\":100},\"r\":{\"a\":0,\"k\":0},\"p\":{\"a\":0,\"k\":[0,0,0]},"
            + "\"a\":{\"a\":0,\"k\":[0,0,0]},\"s\":{\"a\":0,\"k\":[100,100,100]}}";

    private TestLottie() {
    }

    /** A 20 x 20 composition, 30 frames at 30 fps. */
    static String document(String assets, String... layers) {
        return "{\"v\":\"5.7.4\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":20,\"h\":20,\"assets\":[" + assets + "],\"layers\":["
                + String.join(",", layers) + "]}";
    }

    /** A layer with the standard transform, alive for the whole animation. */
    static String layer(int type, String name, int index, String more) {
        return "{\"ty\":" + type + ",\"nm\":\"" + name + "\",\"ind\":" + index + ",\"ip\":0,\"op\":30,\"st\":0," + KS
                + (more.isEmpty() ? "" : "," + more) + "}";
    }

    /** A shape layer with one filled rectangle. */
    static String rectangle(String name, int index, double x, double y, double width, double height, String rgb,
                            String more) {
        String shapes = "\"shapes\":[{\"ty\":\"gr\",\"nm\":\"g\",\"it\":["
                + "{\"ty\":\"rc\",\"nm\":\"r\",\"p\":{\"a\":0,\"k\":[" + (x + width / 2) + "," + (y + height / 2) + "]},"
                + "\"s\":{\"a\":0,\"k\":[" + width + "," + height + "]},\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"fl\",\"nm\":\"f\",\"c\":{\"a\":0,\"k\":[" + rgb + ",1]},\"o\":{\"a\":0,\"k\":100}},"
                + "{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0]},\"a\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]},"
                + "\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}}]}]";
        return layer(4, name, index, shapes + (more.isEmpty() ? "" : "," + more));
    }

    static Animation load(Path directory, String json) throws Exception {
        Path file = Files.createTempFile(directory, "lottie", ".json");
        Files.writeString(file, json);
        return LottieFileLoader.load(file.toFile());
    }
}
