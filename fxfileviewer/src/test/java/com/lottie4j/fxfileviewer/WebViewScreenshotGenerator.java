package com.lottie4j.fxfileviewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxfileviewer.render.DotLottieFrameRenderer;
import com.lottie4j.fxfileviewer.util.ImageSaver;

/**
 * One-time utility for generating reference screenshots for all Lottie animation files using
 * headless Chrome via Selenium. Delegates the actual rendering to
 * {@link DotLottieFrameRenderer} \u2014 the same code path the live {@code LottieWebView}
 * component uses, so the live JavaFX viewer and the committed reference PNGs are produced
 * pixel-for-pixel by the same engine.
 *
 * <p>The renderer is {@code @lottiefiles/dotlottie-wc} (thorvg under the hood), matching the
 * LottieFiles online preview \u2014 in particular this correctly renders gradients-with-alpha
 * (e.g. soft halos in {@code interactive_mood_selector_ui}) that {@code lottie-web} canvas
 * mishandled.</p>
 *
 * <p>Run this class manually whenever a new animation file is added to
 * {@link CompareFxViewWithWebViewTest#lottieJsonFiles()}.
 * The generated PNG images are committed to {@code src/test/resources} and used by
 * {@link CompareFxViewWithWebViewTest} without requiring a live browser at test time.</p>
 *
 * <p>Requires Chrome to be installed. ChromeDriver is managed automatically by Selenium Manager.</p>
 *
 * <p>Output layout: {@code src/test/resources/json/angry_bird-webview/frame_0.png}, etc.</p>
 *
 * <p><b>Runbook</b>: every-frame sampling can produce hundreds of frames per animation. Before
 * committing regenerated references, sanity-check the on-disk size:
 * <pre>
 *   du -sh src/test/resources/json src/test/resources/dot   # expected &lt; 500 MB
 * </pre>
 * Reference PNGs are re-encoded through {@link ImageSaver} at
 * {@link java.util.zip.Deflater#BEST_COMPRESSION} to keep this bounded.</p>
 */
public class WebViewScreenshotGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WebViewScreenshotGenerator.class);

    private static final int DEFAULT_WIDTH = 800;
    private static final int DEFAULT_HEIGHT = 600;

    public static void main(String[] args) throws Exception {
        URL knownResource = WebViewScreenshotGenerator.class.getResource("/json/angry_bird.json");
        if (knownResource == null) {
            throw new IllegalStateException("Could not locate anchor resource json/angry_bird.json");
        }
        Path testResourcesDir = Path.of(knownResource.toURI())
                .getParent()  // json/
                .getParent()  // target/test-classes/
                .getParent()  // target/
                .getParent()  // <module-root>
                .resolve("src/test/resources");

        logger.info("Renderer: dotlottie-wc/thorvg @ {} ({})",
                DotLottieFrameRenderer.DOTLOTTIE_WC_VERSION,
                DotLottieFrameRenderer.DOTLOTTIE_WC_URL);

        List<String> failures = new ArrayList<>();

        try (DotLottieFrameRenderer renderer = new DotLottieFrameRenderer()) {
            for (String fileName : CompareFxViewWithWebViewTest.lottieJsonFiles().toList()) {
                logger.info("Generating screenshots for: {}", fileName);
                try {
                    generateScreenshotsForFile(fileName, testResourcesDir, renderer);
                } catch (Exception e) {
                    logger.error("FAILED {}: {}", fileName, e.getMessage(), e);
                    failures.add(fileName);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to initialize renderer: {}", e.getMessage(), e);
            System.exit(1);
        }

        if (failures.isEmpty()) {
            logger.info("All screenshots generated successfully.");
        } else {
            logger.error("Failed to generate screenshots for: {}", failures);
            System.exit(1);
        }
    }

    private static void generateScreenshotsForFile(String fileName, Path testResourcesDir,
                                                   DotLottieFrameRenderer renderer) throws Exception {
        URL resource = WebViewScreenshotGenerator.class.getResource("/" + fileName);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + fileName);
        }

        Animation animation = LottieFileLoader.load(new File(resource.getFile()));

        int animWidth = animation.width() != null ? animation.width() : DEFAULT_WIDTH;
        int animHeight = animation.height() != null ? animation.height() : DEFAULT_HEIGHT;

        Path outputDir = testResourcesDir.resolve(webViewDirPath(fileName));
        if (Files.isDirectory(outputDir) && hasScreenshots(outputDir)) {
            logger.info("Skipping {} \u2014 screenshots already exist in {}", fileName, outputDir);
            return;
        }
        recreateDirectory(outputDir);

        int inPoint = animation.inPoint() != null ? animation.inPoint() : 0;
        int outPoint = animation.outPoint() != null ? animation.outPoint() : 60;
        // Both lottie-web and dotlottie-web/thorvg treat outPoint as exclusive: totalFrames is
        // op - ip and the last renderable frame index is op - 1. Sampling frame == op would
        // produce a reference that disagrees with the JavaFX player (which clamps seekToFrame
        // to op - 1 via getLastRenderableFrame), so we stop at op - 1.
        int lastFrame = Math.max(inPoint, outPoint - 1);
        // Every frame (step=1, inclusive of lastFrame). See CompareFxViewWithWebViewTest for the rationale.
        List<Integer> frames = buildSampledFrames(inPoint, lastFrame, 1);

        String rawJson = readRawJson(new File(resource.getFile()));
        renderer.load(rawJson, animWidth, animHeight);

        for (int frame : frames) {
            byte[] png = renderer.renderFrame(frame);
            // Re-encode through ImageSaver at BEST_COMPRESSION. Most reference frames are
            // simple cartoons and shrink 30\u201360% \u2014 meaningful because every-frame sampling
            // multiplies committed PNG count by ~5 versus the previous step=5.
            writeRecompressedPng(png, outputDir.resolve("frame_" + frame + ".png"));
        }

        logger.info("Generated {} frames for {} \u2192 {}", frames.size(), fileName, outputDir);
    }

    /**
     * Decode a PNG byte buffer and re-encode it through {@link ImageSaver} at
     * {@link Deflater#BEST_COMPRESSION}. Output dimensions and pixel values are identical;
     * only the on-disk byte count changes.
     */
    private static void writeRecompressedPng(byte[] pngBytes, Path target) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(pngBytes));
        if (img == null) {
            // Fallback: just keep the raw bytes if decoding fails for some reason.
            Files.write(target, pngBytes);
            return;
        }
        int width = img.getWidth();
        int height = img.getHeight();
        int[] argb = img.getRGB(0, 0, width, height, null, 0, width);
        try (FileOutputStream fos = new FileOutputStream(target.toFile())) {
            ImageSaver.writePNG(fos, argb, width, height, Deflater.BEST_COMPRESSION);
        }
    }

    // \u2500\u2500 Shared helpers used by CompareFxViewWithWebViewTest \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500

    /**
     * Derives the classpath-relative webview directory path for a given animation file.
     * Example: {@code "json/angry_bird.json"} \u2192 {@code "json/angry_bird-webview"}
     */
    static String webViewDirPath(String fileName) {
        int slash = fileName.lastIndexOf('/');
        String dir = fileName.substring(0, slash);
        String base = fileName.substring(slash + 1).replaceAll("\\.(json|lottie)$", "");
        return dir + "/" + base + "-webview";
    }

    /**
     * Builds a sampled list of frame indices from {@code firstFrame} to {@code lastFrame}
     * (inclusive) stepping by {@code step}, always including the final frame.
     */
    static List<Integer> buildSampledFrames(int firstFrame, int lastFrame, int step) {
        if (step <= 0) step = 1;
        List<Integer> frames = new ArrayList<>();
        for (int f = firstFrame; f <= lastFrame; f += step) {
            frames.add(f);
        }
        if (frames.isEmpty() || frames.get(frames.size() - 1) != lastFrame) {
            frames.add(lastFrame);
        }
        return frames;
    }

    // \u2500\u2500 Private utilities \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500

    private static String readRawJson(File file) throws IOException {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".json")) {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        }
        if (name.endsWith(".lottie")) {
            try (ZipFile zip = new ZipFile(file)) {
                ZipEntry manifestEntry = zip.getEntry("manifest.json");
                if (manifestEntry == null) throw new IOException("missing manifest.json in " + file.getName());
                // Find the first animation entry under a/ or animations/
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String n = entry.getName();
                    if (!entry.isDirectory() && n.endsWith(".json")
                            && (n.startsWith("a/") || n.startsWith("animations/"))) {
                        return new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
                    }
                }
                throw new IOException("no animation JSON found in " + file.getName());
            }
        }
        throw new IOException("unsupported file type: " + file.getName());
    }

    private static boolean hasScreenshots(Path dir) throws IOException {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.anyMatch(p -> p.toString().endsWith(".png"));
        }
    }

    private static void recreateDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
                    try {
                        Files.delete(p);
                    } catch (IOException e) {
                        // ignore stale file cleanup failures
                    }
                });
            }
        }
        Files.createDirectories(dir);
    }
}
