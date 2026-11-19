package com.lottie4j.fxplayer.util;

import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

import java.util.Arrays;
import java.util.Objects;

/**
 * Color-aware, alpha-aware, windowed SSIM image similarity.
 *
 * <p>Improvements over a naive global, grayscale SSIM:
 * <ul>
 *   <li><b>Color</b>: SSIM is computed independently on each of the R, G, B channels
 *       and the three scores are averaged. A pure-color regression (e.g. red painted
 *       as blue) is no longer hidden by a luminance collapse.</li>
 *   <li><b>Windowed</b>: SSIM is computed over small sliding windows (default 8×8,
 *       stride 8) and averaged. Global SSIM over a whole image is too generous on
 *       large flat backgrounds and too punishing on small local offsets. Windowing is
 *       standard practice for SSIM and matches the original Wang et al. definition.</li>
 *   <li><b>Alpha-aware</b>: both images are composited over an opaque white background
 *       before comparison. This eliminates spurious mismatches when one renderer keeps
 *       a transparent background and the other paints over a default scene color.</li>
 * </ul>
 *
 * <p>The SSIM stability constants {@code c1} and {@code c2} are kept consistent with
 * the earlier global-grayscale implementation so absolute numbers remain comparable
 * in spirit:
 * <pre>
 *   c1 = (0.01 * 255)^2 = 6.5025
 *   c2 = (0.03 * 255)^2 = 58.5225
 * </pre>
 */
public final class ImageSimilarity {

    private static final int WINDOW = 8;
    private static final int STRIDE = 8;
    private static final double C1 = 6.5025;
    private static final double C2 = 58.5225;

    /** Opaque white background used for alpha compositing. */
    private static final int BACKGROUND_ARGB = 0xFFFFFFFF;

    private ImageSimilarity() {
        // utility
    }

    /**
     * Result of an image similarity comparison.
     *
     * @param overall       overall similarity in [0, 100], averaged over R/G/B
     * @param red           similarity in [0, 100] on the red channel
     * @param green         similarity in [0, 100] on the green channel
     * @param blue          similarity in [0, 100] on the blue channel
     * @param windowScores  per-window overall similarity in [0, 100], indexed as
     *                      {@code windowScores[windowY][windowX]}; useful for
     *                      heatmap diff rendering. May be empty if no full windows fit.
     */
    public record SimilarityResult(
            double overall,
            double red,
            double green,
            double blue,
            double[][] windowScores) {

        /** Two results are equal when their scores are, window by window. */
        @Override
        public boolean equals(Object other) {
            return other instanceof SimilarityResult(double o, double r, double g, double b, double[][] windows)
                    && Double.compare(overall, o) == 0
                    && Double.compare(red, r) == 0
                    && Double.compare(green, g) == 0
                    && Double.compare(blue, b) == 0
                    && Arrays.deepEquals(windowScores, windows);
        }

        @Override
        public int hashCode() {
            return 31 * Objects.hash(overall, red, green, blue) + Arrays.deepHashCode(windowScores);
        }

        @Override
        public String toString() {
            return "SimilarityResult[overall=" + overall + ", red=" + red + ", green=" + green + ", blue=" + blue
                    + ", windowScores=" + Arrays.deepToString(windowScores) + "]";
        }
    }

    /**
     * Compare two images using color-aware, alpha-aware, windowed SSIM.
     *
     * @param a first image
     * @param b second image
     * @return structured similarity result
     */
    public static SimilarityResult compare(WritableImage a, WritableImage b) {
        int width = (int) Math.min(a.getWidth(), b.getWidth());
        int height = (int) Math.min(a.getHeight(), b.getHeight());

        if (width <= 0 || height <= 0) {
            return new SimilarityResult(0, 0, 0, 0, new double[0][0]);
        }

        int[][][] first = channels(a, width, height);
        int[][][] second = channels(b, width, height);

        // Only count windows that fully fit; if image is smaller than WINDOW, fall back to a single
        // truncated window so we still produce a result.
        boolean fits = width >= WINDOW && height >= WINDOW;
        int windowsY = fits ? (height - WINDOW) / STRIDE + 1 : 1;
        int windowsX = fits ? (width - WINDOW) / STRIDE + 1 : 1;
        int windowWidth = fits ? WINDOW : width;
        int windowHeight = fits ? WINDOW : height;

        double[][] perWindow = new double[windowsY][windowsX];
        double[] sums = new double[3];

        for (int wy = 0; wy < windowsY; wy++) {
            for (int wx = 0; wx < windowsX; wx++) {
                double total = 0;
                for (int channel = 0; channel < 3; channel++) {
                    double score = ssim(first[channel], second[channel], wx * STRIDE, wy * STRIDE,
                            windowWidth, windowHeight);
                    sums[channel] += score;
                    total += score;
                }
                perWindow[wy][wx] = total / 3.0;
            }
        }

        int windowCount = windowsY * windowsX;
        double red = clampPct(sums[0] / windowCount);
        double green = clampPct(sums[1] / windowCount);
        double blue = clampPct(sums[2] / windowCount);
        double overall = (red + green + blue) / 3.0;
        return new SimilarityResult(overall, red, green, blue, perWindow);
    }

    /**
     * The red, green and blue values of an image, composited over white, as
     * {@code [channel][y][x]}.
     */
    private static int[][][] channels(WritableImage image, int width, int height) {
        int[][][] result = new int[3][height][width];
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = compositeOverWhite(reader.getArgb(x, y));
                result[0][y][x] = (pixel >> 16) & 0xFF;
                result[1][y][x] = (pixel >> 8) & 0xFF;
                result[2][y][x] = pixel & 0xFF;
            }
        }
        return result;
    }

    /**
     * Composite an ARGB pixel over an opaque white background.
     * <p>For an opaque source ({@code alpha == 255}) this returns the input unchanged.
     */
    static int compositeOverWhite(int argb) {
        int a = (argb >>> 24) & 0xFF;
        if (a == 0xFF) {
            return argb | 0xFF000000;
        }
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int bgR = (BACKGROUND_ARGB >> 16) & 0xFF;
        int bgG = (BACKGROUND_ARGB >> 8) & 0xFF;
        int bgB = BACKGROUND_ARGB & 0xFF;
        // src-over with opaque background
        int outR = (r * a + bgR * (255 - a) + 127) / 255;
        int outG = (g * a + bgG * (255 - a) + 127) / 255;
        int outB = (b * a + bgB * (255 - a) + 127) / 255;
        return 0xFF000000 | (outR << 16) | (outG << 8) | outB;
    }

    /**
     * SSIM in [0, 100] over a rectangular region of two single-channel byte arrays.
     */
    private static double ssim(int[][] x, int[][] y, int x0, int y0, int w, int h) {
        long n = (long) w * h;
        if (n == 0) return 100.0;

        double sumX = 0;
        double sumY = 0;
        for (int yy = 0; yy < h; yy++) {
            int[] rowX = x[y0 + yy];
            int[] rowY = y[y0 + yy];
            for (int xx = 0; xx < w; xx++) {
                sumX += rowX[x0 + xx];
                sumY += rowY[x0 + xx];
            }
        }
        double meanX = sumX / n;
        double meanY = sumY / n;

        double varX = 0;
        double varY = 0;
        double cov = 0;
        for (int yy = 0; yy < h; yy++) {
            int[] rowX = x[y0 + yy];
            int[] rowY = y[y0 + yy];
            for (int xx = 0; xx < w; xx++) {
                double dx = rowX[x0 + xx] - meanX;
                double dy = rowY[x0 + xx] - meanY;
                varX += dx * dx;
                varY += dy * dy;
                cov += dx * dy;
            }
        }
        varX /= n;
        varY /= n;
        cov /= n;

        double numerator = (2 * meanX * meanY + C1) * (2 * cov + C2);
        double denominator = (meanX * meanX + meanY * meanY + C1) * (varX + varY + C2);
        if (denominator == 0) return 100.0;
        double ssim = numerator / denominator; // in [-1, 1]
        return clampPct((ssim + 1) * 50.0);
    }

    private static double clampPct(double v) {
        if (v < 0) return 0;
        if (v > 100) return 100;
        return v;
    }
}
