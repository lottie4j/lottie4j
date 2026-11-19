package com.lottie4j.fxplayer;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LottiePlayerSnapshotTest {

    /** A 20 x 20 composition with a red rectangle of 10 x 10 in its top left corner. */
    private static final String RED_SQUARE = """
            {"v":"5.7.4","fr":30,"ip":0,"op":30,"w":20,"h":20,"ddd":0,"assets":[],"layers":[
              {"ddd":0,"ind":1,"ty":4,"nm":"red","sr":1,"ks":{
                "o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[0,0,0]},
                "a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},
               "ao":0,"ip":0,"op":30,"st":0,"bm":0,"shapes":[{"ty":"gr","nm":"g","it":[
                 {"ty":"rc","nm":"r","p":{"a":0,"k":[5,5]},"s":{"a":0,"k":[10,10]},"r":{"a":0,"k":0}},
                 {"ty":"fl","nm":"f","c":{"a":0,"k":[1,0,0,1]},"o":{"a":0,"k":100}},
                 {"ty":"tr","p":{"a":0,"k":[0,0]},"a":{"a":0,"k":[0,0]},"s":{"a":0,"k":[100,100]},
                  "r":{"a":0,"k":0},"o":{"a":0,"k":100}}]}]}]}""";

    @TempDir
    Path directory;

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    private Animation animation() throws Exception {
        Path file = Files.writeString(directory.resolve("red-square.json"), RED_SQUARE);
        return LottieFileLoader.load(file.toFile());
    }

    @Test
    void snapshotOfAFrameShowsTheAnimationWhereItPaints() throws Exception {
        Animation animation = animation();

        Color[] pixels = FxTestHelper.callAndWait(() -> {
            LottiePlayer player = new LottiePlayer(animation, 20, 20);
            player.setBackgroundColor(Color.TRANSPARENT);
            WritableImage image = player.snapshotFrame(0);
            return new Color[]{image.getPixelReader().getColor(5, 5), image.getPixelReader().getColor(15, 15)};
        });

        assertEquals(1.0, pixels[0].getRed(), 0.02);
        assertEquals(0.0, pixels[0].getGreen(), 0.02);
        assertEquals(1.0, pixels[0].getOpacity(), 0.02);
        assertEquals(0.0, pixels[1].getOpacity(), 0.02);
    }

    @Test
    void snapshotLeavesThePlayerAsItIsAndIgnoresItsScale() throws Exception {
        Animation animation = animation();

        double[] result = FxTestHelper.callAndWait(() -> {
            LottiePlayer player = new LottiePlayer(animation, 20, 20);
            player.setScaleX(2);
            player.setRotate(45);
            player.seekToFrame(3);
            WritableImage image = player.snapshotFrame(10);
            return new double[]{image.getWidth(), image.getHeight(), player.getCurrentFrame(),
                    player.isAdaptiveOffscreenScalingEnabled() ? 1 : 0};
        });

        assertEquals(20, result[0]);
        assertEquals(20, result[1]);
        assertEquals(3, result[2]);
        assertEquals(1, result[3]);
    }

    @Test
    void snapshotDrawsOnlyTheVisibleLayers() throws Exception {
        Animation animation = animation();

        Color pixel = FxTestHelper.callAndWait(() -> {
            LottiePlayer player = new LottiePlayer(animation, 20, 20);
            player.setBackgroundColor(Color.TRANSPARENT);
            player.setVisibleLayerIndices(java.util.Set.of());
            return player.snapshotFrame(0).getPixelReader().getColor(5, 5);
        });

        assertEquals(0.0, pixel.getOpacity(), 0.02);
    }

    @Test
    void aSolidLayerPaintsARectangleOfItsOwnSize() throws Exception {
        Path file = Files.writeString(directory.resolve("solid.json"), """
                {"v":"5.7.4","fr":30,"ip":0,"op":30,"w":20,"h":20,"ddd":0,"assets":[],"layers":[
                  {"ddd":0,"ind":1,"ty":1,"nm":"red","sr":1,"ks":{
                    "o":{"a":0,"k":100},"r":{"a":0,"k":0},"p":{"a":0,"k":[0,0,0]},
                    "a":{"a":0,"k":[0,0,0]},"s":{"a":0,"k":[100,100,100]}},
                   "ao":0,"sw":10,"sh":10,"sc":"#ff0000","ip":0,"op":30,"st":0,"bm":0}]}""");
        Animation animation = LottieFileLoader.load(file.toFile());

        Color[] pixels = FxTestHelper.callAndWait(() -> {
            LottiePlayer player = new LottiePlayer(animation, 20, 20);
            player.setBackgroundColor(Color.TRANSPARENT);
            WritableImage image = player.snapshotFrame(0);
            return new Color[]{image.getPixelReader().getColor(5, 5), image.getPixelReader().getColor(15, 15)};
        });

        assertEquals(1.0, pixels[0].getRed(), 0.02);
        assertEquals(1.0, pixels[0].getOpacity(), 0.02);
        assertEquals(0.0, pixels[1].getOpacity(), 0.02);
    }

    @Test
    void whatTheLibraryDoesNotKnowIsSkippedAndTheRestIsDrawn() throws Exception {
        String json = RED_SQUARE
                .replace("\"it\":[", "\"it\":[{\"ty\":\"xx\",\"nm\":\"from a newer exporter\"},")
                .replace("\"layers\":[", "\"layers\":[{\"ty\":99,\"nm\":\"unknown layer\",\"ip\":0,\"op\":30,\"st\":0,"
                        + "\"ks\":{\"o\":{\"a\":0,\"k\":100,\"x\":\"wiggle(1, 2)\"}}},");
        Animation animation = LottieFileLoader.load(Files.writeString(directory.resolve("odd.json"), json).toFile());

        Color pixel = FxTestHelper.callAndWait(() -> {
            LottiePlayer player = new LottiePlayer(animation, 20, 20);
            player.setBackgroundColor(Color.TRANSPARENT);
            return player.snapshotFrame(0).getPixelReader().getColor(5, 5);
        });

        assertEquals(1.0, pixel.getRed(), 0.02);
        assertEquals(1.0, pixel.getOpacity(), 0.02);
    }
}
