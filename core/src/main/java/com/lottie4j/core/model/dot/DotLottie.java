package com.lottie4j.core.model.dot;

import com.lottie4j.core.model.animation.Animation;

import java.util.List;
import java.util.Map;

/**
 * Represents the full content of a dotLottie archive after it has been loaded.
 * <ul>
 *     <li><a href="https://dotlottie.io/spec/1.0/">dotLottie v1.0 Specification</a></li>
 *     <li><a href="https://dotlottie.io/spec/2.0/">dotLottie v2.0 Specification</a></li>
 * </ul>
 * <p>
 * A dotLottie file is a ZIP archive that bundles one or more Lottie animations with
 * optional supporting assets. This record mirrors the archive's directory layout:
 * <pre>
 * animation.lottie
 * ├─ manifest.json    → {@link #manifest()}
 * ├─ a/               → {@link #animations()}   (Lottie JSON, parsed)
 * ├─ t/               → {@link #themes()}        (raw JSON bytes, keyed by theme id)
 * ├─ s/               → {@link #stateMachines()} (raw JSON bytes, keyed by state-machine id)
 * ├─ i/               → {@link #imageAssets()}   (binary bytes, keyed by filename)
 * └─ f/               → {@link #fontAssets()}    (binary bytes, keyed by filename)
 * </pre>
 * <p>
 * Theme and state-machine entries are stored as raw {@code byte[]} so that callers can
 * parse them with their own schema without imposing a fixed model.
 * Image and font entries are stored as raw {@code byte[]} because they are binary assets.
 * All four maps are never {@code null}; they are empty when the corresponding directory
 * is absent from the archive.
 *
 * @param manifest      manifest metadata parsed from {@code manifest.json}
 * @param animations    parsed Lottie animation objects from {@code a/}
 * @param themes        raw theme JSON bytes from {@code t/}, keyed by theme id (filename without {@code .json})
 * @param stateMachines raw state-machine JSON bytes from {@code s/}, keyed by state machine id
 * @param imageAssets   image asset bytes from {@code i/}, keyed by filename (e.g. {@code img_1.webp})
 * @param fontAssets    font asset bytes from {@code f/}, keyed by filename (e.g. {@code BrandFont-Bold.ttf})
 */
public record DotLottie(
        Manifest manifest,
        List<Animation> animations,
        Map<String, byte[]> themes,
        Map<String, byte[]> stateMachines,
        Map<String, byte[]> imageAssets,
        Map<String, byte[]> fontAssets
) {
}
