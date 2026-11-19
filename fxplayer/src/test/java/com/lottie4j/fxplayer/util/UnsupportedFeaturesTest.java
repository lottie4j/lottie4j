package com.lottie4j.fxplayer.util;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.util.UnsupportedFeatures.Feature;
import com.lottie4j.fxplayer.util.UnsupportedFeatures.Finding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnsupportedFeaturesTest {

    private static final String KS = "\"ks\":{\"o\":{\"a\":0,\"k\":100}}";

    @TempDir
    Path directory;

    private Map<Feature, Finding> find(String assets, String... layers) throws Exception {
        Path file = Files.createTempFile(directory, "lottie", ".json");
        Files.writeString(file, "{\"v\":\"5\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":20,\"h\":20,\"assets\":[" + assets
                + "],\"layers\":[" + String.join(",", layers) + "]}");
        Animation animation = LottieFileLoader.load(file.toFile());
        return UnsupportedFeatures.find(animation).stream().collect(Collectors.toMap(Finding::feature, f -> f));
    }

    @Test
    void aPlainFileHasNoFindings() throws Exception {
        assertTrue(find("", "{\"ty\":4,\"nm\":\"plain\",\"ind\":1," + KS + ",\"shapes\":["
                + "{\"ty\":\"el\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[4,4]}}]}").isEmpty());
    }

    @Test
    void whatALayerUsesIsCountedAndNamed() throws Exception {
        Map<Feature, Finding> findings = find("",
                "{\"ty\":4,\"nm\":\"wiggly\",\"ind\":1,\"ao\":1,\"sy\":[{\"ty\":1}],"
                        + "\"ks\":{\"o\":{\"a\":0,\"k\":100,\"x\":\"wiggle(1, 2)\"},\"r\":{\"a\":0,\"k\":0,\"x\":\"time\"},"
                        + "\"sk\":{\"a\":0,\"k\":20}},"
                        + "\"ef\":[{\"ty\":25,\"en\":1,\"ef\":[]},{\"ty\":29,\"en\":1,\"ef\":[]},{\"ty\":25,\"en\":0,\"ef\":[]}],"
                        + "\"shapes\":[{\"ty\":\"gr\",\"it\":[{\"ty\":\"rp\",\"c\":{\"a\":0,\"k\":3}},{\"ty\":\"xx\"},"
                        + "{\"ty\":\"rd\",\"r\":{\"a\":0,\"k\":2}}]}]}",
                "{\"ty\":6,\"nm\":\"sound\",\"ind\":2," + KS + "}",
                "{\"ty\":99,\"nm\":\"from the future\",\"ind\":3," + KS + "}",
                "{\"ty\":0,\"nm\":\"remapped\",\"ind\":4,\"refId\":\"gone\",\"tm\":{\"a\":0,\"k\":1}," + KS + "}");

        assertEquals(2, findings.get(Feature.EXPRESSION).count());
        assertEquals(List.of("wiggly"), findings.get(Feature.EXPRESSION).layers());
        assertEquals(1, findings.get(Feature.SKEW).count());
        assertEquals(1, findings.get(Feature.AUTO_ORIENT).count());
        assertEquals(1, findings.get(Feature.LAYER_STYLE).count());
        assertEquals(1, findings.get(Feature.EFFECT).count());
        assertEquals(2, findings.get(Feature.SHAPE_MODIFIER).count());
        assertEquals(1, findings.get(Feature.UNKNOWN_SHAPE).count());
        assertEquals(List.of("sound", "from the future"), findings.get(Feature.LAYER_TYPE).layers());
        assertEquals(List.of("remapped"), findings.get(Feature.TIME_REMAPPING).layers());
        assertEquals(List.of("remapped"), findings.get(Feature.MISSING_PRECOMPOSITION).layers());
    }

    @Test
    void brokenReferencesAreFoundInsidePrecompositionsToo() throws Exception {
        Map<Feature, Finding> findings = find(
                "{\"id\":\"a\",\"layers\":[{\"ty\":0,\"nm\":\"to b\",\"ind\":1,\"refId\":\"b\"," + KS + "}]},"
                        + "{\"id\":\"b\",\"layers\":[{\"ty\":0,\"nm\":\"to a\",\"ind\":1,\"refId\":\"a\"," + KS + "},"
                        + "{\"ty\":3,\"nm\":\"x\",\"ind\":2,\"parent\":3," + KS + "},"
                        + "{\"ty\":3,\"nm\":\"y\",\"ind\":3,\"parent\":2," + KS + "}]}",
                "{\"ty\":0,\"nm\":\"outer\",\"ind\":1,\"refId\":\"a\"," + KS + "}");

        assertEquals(2, findings.get(Feature.SELF_CONTAINING_PRECOMPOSITION).count());
        assertEquals(List.of("x", "y"), findings.get(Feature.CIRCULAR_PARENT).layers());
        assertTrue(findings.get(Feature.CIRCULAR_PARENT).describe().contains("x, y"));
    }
}
