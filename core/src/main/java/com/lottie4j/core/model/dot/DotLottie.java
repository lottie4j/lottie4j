package com.lottie4j.core.model.dot;

import com.lottie4j.core.model.animation.Animation;

import java.util.List;

/**
 * Represents a dotLottie file structure containing animation metadata and data.
 * <p>
 * A dotLottie file is a container format that bundles Lottie animations with their
 * associated metadata in a single package. This record provides access to both the
 * manifest, which contains metadata about the animations and their playback configuration,
 * and the actual animation data.
 * <p>
 * The manifest includes information such as the dotLottie version, generator tool,
 * author details, and playback settings for each animation (autoplay, loop, speed, etc.).
 * The animations list contains the full Lottie animation data including layers, assets,
 * timing information, and rendering properties.
 *
 * @param manifest   the manifest containing metadata and configuration for the dotLottie package
 * @param animations the list of Lottie animation objects included in this dotLottie file
 */
public record DotLottie(Manifest manifest, List<Animation> animations) {
}
