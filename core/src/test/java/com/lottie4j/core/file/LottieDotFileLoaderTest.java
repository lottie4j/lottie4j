package com.lottie4j.core.file;

import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.dot.DotLottie;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LottieDotFileLoaderTest {

    private static Stream<Arguments> provideLottieFiles() {
        return Stream.of(
                Arguments.of("/dot/demo-1.lottie", 1),
                Arguments.of("/dot/demo-2.lottie", 1),
                Arguments.of("/dot/demo-3.lottie", 1),
                Arguments.of("/dot/lottie4j.lottie", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("provideLottieFiles")
    void testLoadDotLottieShouldContainAnimation(String file, int numberOfAnimations) throws LottieFileException {
        var testFile = this.getClass().getResource(file);

        if (testFile == null) {
            fail("File not found: " + file);
        }

        File f = new File(testFile.getFile());
        DotLottie dotLottie = LottieFileLoader.loadDotLottie(f);

        assertAll(
                () -> assertNotNull(dotLottie),
                () -> assertEquals(numberOfAnimations, dotLottie.animations().size())
        );
    }

    @ParameterizedTest
    @MethodSource("provideLottieFiles")
    void testLoadShouldReturnAnimation(String file) throws LottieFileException {
        var testFile = this.getClass().getResource(file);

        if (testFile == null) {
            fail("File not found: " + file);
        }

        File f = new File(testFile.getFile());
        Animation animation = LottieFileLoader.load(f);
        assertNotNull(animation);
    }
}
