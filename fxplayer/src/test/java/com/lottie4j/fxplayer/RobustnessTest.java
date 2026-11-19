package com.lottie4j.fxplayer;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static com.lottie4j.fxplayer.TestLottie.document;
import static com.lottie4j.fxplayer.TestLottie.layer;
import static com.lottie4j.fxplayer.TestLottie.rectangle;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Files that are broken, or built to make a player run forever. Whatever is wrong with a file,
 * rendering it ends, and what is sound in it is drawn.
 */
class RobustnessTest {

    private static final String RED = "1,0,0";
    private static final String BLUE = "0,0,1";

    @TempDir
    Path directory;

    @BeforeAll
    static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    /** The pixel at (x, y) of frame 0 on a transparent background. */
    private Color pixel(String json, int x, int y) throws Exception {
        Animation animation = TestLottie.load(directory, json);
        return FxTestHelper.callAndWait(() -> {
            LottiePlayer player = new LottiePlayer(animation, 20, 20);
            player.setBackgroundColor(Color.TRANSPARENT);
            return player.snapshotFrame(0).getPixelReader().getColor(x, y);
        });
    }

    private static void assertRed(Color color) {
        assertEquals(1.0, color.getRed(), 0.02, color.toString());
        assertEquals(0.0, color.getBlue(), 0.02, color.toString());
        assertEquals(1.0, color.getOpacity(), 0.02, color.toString());
    }

    private static void assertNothing(Color color) {
        assertEquals(0.0, color.getOpacity(), 0.02, color.toString());
    }

    private static String precomp(String name, int index, String reference) {
        return layer(0, name, index, "\"refId\":\"" + reference + "\",\"w\":20,\"h\":20");
    }

    private static String composition(String id, String... layers) {
        return "{\"id\":\"" + id + "\",\"layers\":[" + String.join(",", layers) + "]}";
    }

    @Test
    void aPrecompositionThatContainsItselfIsDrawnOnce() throws Exception {
        String json = document(composition("loop", precomp("again", 1, "loop"),
                rectangle("red", 2, 0, 0, 10, 10, RED, "")), precomp("outer", 1, "loop"));

        assertRed(pixel(json, 5, 5));
    }

    @Test
    void precompositionsThatContainEachOtherEnd() throws Exception {
        String json = document(
                composition("a", precomp("to b", 1, "b"), rectangle("red", 2, 0, 0, 10, 10, RED, "")) + ","
                        + composition("b", precomp("to a", 1, "a")),
                precomp("outer", 1, "a"));

        assertRed(pixel(json, 5, 5));
    }

    @Test
    void precompositionsNestedDeeperThanTheLimitEnd() throws Exception {
        StringBuilder assets = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            assets.append(composition("c" + i, precomp("deeper", 1, "c" + (i + 1)),
                    i == 0 ? rectangle("red", 2, 0, 0, 10, 10, RED, "") : layer(3, "empty", 2, ""))).append(',');
        }
        assets.append(composition("c200", layer(3, "bottom", 1, "")));

        assertRed(pixel(document(assets.toString(), precomp("outer", 1, "c0")), 5, 5));
    }

    @Test
    void aPrecompositionThatIsNotInTheFileDrawsNothing() throws Exception {
        String json = document("", precomp("lost", 1, "nowhere"), rectangle("red", 2, 0, 0, 10, 10, RED, ""));

        assertRed(pixel(json, 5, 5));
        assertNothing(pixel(json, 15, 15));
    }

    @Test
    void layersWhoseParentsRunInACircleAreDrawnWithoutParent() throws Exception {
        String json = document("", rectangle("red", 1, 0, 0, 10, 10, RED, "\"parent\":2"),
                layer(3, "b", 2, "\"parent\":3"), layer(3, "c", 3, "\"parent\":1"));

        assertRed(pixel(json, 5, 5));
    }

    @Test
    void aLayerWhoseParentIsMissingIsDrawn() throws Exception {
        assertRed(pixel(document("", rectangle("red", 1, 0, 0, 10, 10, RED, "\"parent\":42")), 5, 5));
    }

    @Test
    void aLayerThatIsItsOwnMatteIsDrawnUnmatted() throws Exception {
        assertRed(pixel(document("", rectangle("red", 1, 0, 0, 10, 10, RED, "\"tt\":1,\"tp\":1")), 5, 5));
    }

    @Test
    void aMatteNamedByItsIndexIsUsedInsteadOfTheLayerAbove() throws Exception {
        // the content covers everything; its matte, named with "tp", is the small square listed last
        String json = document("",
                layer(3, "in between", 1, ""),
                rectangle("content", 2, 0, 0, 20, 20, RED, "\"tt\":1,\"tp\":3"),
                rectangle("matte", 3, 0, 0, 10, 10, BLUE, ""));

        assertRed(pixel(json, 5, 5));
        assertNothing(pixel(json, 15, 15));
    }

    @Test
    void valuesBeyondAnyDrawingDoNotStopTheRest() throws Exception {
        String huge = layer(4, "huge", 1, "\"shapes\":[]").replace("\"p\":{\"a\":0,\"k\":[0,0,0]}",
                "\"p\":{\"a\":0,\"k\":[1e300,-1e300,0]}").replace("\"s\":{\"a\":0,\"k\":[100,100,100]}",
                "\"s\":{\"a\":0,\"k\":[1e300,1e300,100]}");

        assertRed(pixel(document("", huge, rectangle("red", 2, 0, 0, 10, 10, RED, "")), 5, 5));
    }

    @Test
    void anAnimationWithoutLayersOrTimingRenders() throws Exception {
        Animation animation = TestLottie.load(directory, "{\"v\":\"5\",\"w\":20,\"h\":20,\"layers\":[]}");

        double width = FxTestHelper.callAndWait(() -> new LottiePlayer(animation, 20, 20).snapshotFrame(0).getWidth());

        assertEquals(20, width);
    }

    @Test
    void aHiddenLayerIsNotDrawnButStillCarriesItsChildren() throws Exception {
        // the hidden parent is moved 10 to the right; its red child follows, the parent's own blue does not show
        String parent = rectangle("hidden parent", 2, 0, 0, 5, 5, BLUE, "\"hd\":true")
                .replace("\"p\":{\"a\":0,\"k\":[0,0,0]}", "\"p\":{\"a\":0,\"k\":[10,0,0]}");
        String json = document("", rectangle("child", 1, 0, 10, 5, 5, RED, "\"parent\":2"), parent);

        assertRed(pixel(json, 12, 12));
        assertNothing(pixel(json, 12, 2));
        assertNothing(pixel(json, 2, 12));
    }

    @Test
    void aHiddenShapeAndAHiddenFillDrawNothing() throws Exception {
        String hiddenGroup = rectangle("hidden group", 1, 10, 10, 10, 10, BLUE, "")
                .replace("{\"ty\":\"gr\",\"nm\":\"g\",", "{\"ty\":\"gr\",\"nm\":\"g\",\"hd\":true,");
        String hiddenFill = rectangle("hidden fill", 2, 10, 0, 10, 10, BLUE, "")
                .replace("{\"ty\":\"fl\",", "{\"ty\":\"fl\",\"hd\":true,");
        String json = document("", hiddenGroup, hiddenFill, rectangle("red", 3, 0, 0, 10, 10, RED, ""));

        assertRed(pixel(json, 5, 5));
        assertNothing(pixel(json, 15, 15));
        assertNothing(pixel(json, 15, 5));
    }

    @Test
    void aHeldPathKeyframeKeepsItsShapeUntilTheNext() throws Exception {
        String small = "{\"c\":true,\"v\":[[0,0],[4,0],[4,4],[0,4]],\"i\":[[0,0],[0,0],[0,0],[0,0]],"
                + "\"o\":[[0,0],[0,0],[0,0],[0,0]]}";
        String large = small.replace("4", "20");
        String shapes = "\"shapes\":[{\"ty\":\"gr\",\"nm\":\"g\",\"it\":[{\"ty\":\"sh\",\"nm\":\"p\",\"ks\":{\"a\":1,\"k\":["
                + "{\"t\":0,\"h\":1,\"s\":[" + small + "]},{\"t\":20.5,\"s\":[" + large + "]}]}},"
                + "{\"ty\":\"fl\",\"nm\":\"f\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}},"
                + "{\"ty\":\"tr\",\"p\":{\"a\":0,\"k\":[0,0]},\"a\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[100,100]},"
                + "\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}}]}]";
        Animation animation = TestLottie.load(directory, document("", layer(4, "morph", 1, shapes)));

        Color[] pixels = FxTestHelper.callAndWait(() -> {
            LottiePlayer player = new LottiePlayer(animation, 20, 20);
            player.setBackgroundColor(Color.TRANSPARENT);
            return new Color[]{player.snapshotFrame(10).getPixelReader().getColor(10, 10),
                    player.snapshotFrame(20).getPixelReader().getColor(10, 10),
                    player.snapshotFrame(21).getPixelReader().getColor(10, 10)};
        });

        assertNothing(pixels[0]);
        assertNothing(pixels[1]);
        assertRed(pixels[2]);
    }
}
