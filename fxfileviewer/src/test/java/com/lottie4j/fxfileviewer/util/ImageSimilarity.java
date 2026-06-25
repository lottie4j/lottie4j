package com.lottie4j.fxfileviewer.util;

import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

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

        int[][] r1 = new int[height][width];
        int[][] g1 = new int[height][width];
        int[][] b1 = new int[height][width];
        int[][] r2 = new int[height][width];
        int[][] g2 = new int[height][width];
        int[][] b2 = new int[height][width];

        PixelReader pr1 = a.getPixelReader();
        PixelReader pr2 = b.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int p1 = compositeOverWhite(pr1.getArgb(x, y));
                int p2 = compositeOverWhite(pr2.getArgb(x, y));
                r1[y][x] = (p1 >> 16) & 0xFF;
                g1[y][x] = (p1 >> 8) & 0xFF;
                b1[y][x] = p1 & 0xFF;
                r2[y][x] = (p2 >> 16) & 0xFF;
                g2[y][x] = (p2 >> 8) & 0xFF;
                b2[y][x] = p2 & 0xFF;
            }
        }

        int windowsY = Math.max(1, (height - WINDOW) / STRIDE + 1);
        int windowsX = Math.max(1, (width - WINDOW) / STRIDE + 1);
        // Only count windows that fully fit; if image is smaller than WINDOW, fall back to a single
        // truncated window so we still produce a result.
        boolean fits = width >= WINDOW && height >= WINDOW;
        if (!fits) {
            windowsY = 1;
            windowsX = 1;
        }

        double[][] perWindow = new double[windowsY][windowsX];
        double sumR = 0, sumG = 0, sumB = 0;
        int windowCount = 0;

        for (int wy = 0; wy < windowsY; wy++) {
            for (int wx = 0; wx < windowsX; wx++) {
                int x0 = wx * STRIDE;
                int y0 = wy * STRIDE;
                int wW = fits ? WINDOW : width;
                int wH = fits ? WINDOW : height;
                if (x0 + wW > width) wW = width - x0;
                if (y0 + wH > height) wH = height - y0;
                if (wW <= 0 || wH <= 0) continue;

                double sR = ssim(r1, r2, x0, y0, wW, wH);
                double sG = ssim(g1, g2, x0, y0, wW, wH);
                double sB = ssim(b1, b2, x0, y0, wW, wH);

                sumR += sR;
                sumG += sG;
                sumB += sB;
                perWindow[wy][wx] = (sR + sG + sB) / 3.0;
                windowCount++;
            }
        }

        if (windowCount == 0) {
            return new SimilarityResult(0, 0, 0, 0, perWindow);
        }

        double red = clampPct(sumR / windowCount);
        double green = clampPct(sumG / windowCount);
        double blue = clampPct(sumB / windowCount);
        double overall = (red + green + blue) / 3.0;
        return new SimilarityResult(overall, red, green, blue, perWindow);
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

        double sumX = 0, sumY = 0;
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

        double varX = 0, varY = 0, cov = 0;
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
