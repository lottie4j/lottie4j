package com.lottie4j.core.model.dot;

import com.lottie4j.core.model.Animation;

import java.util.List;

public record DotLottie(Manifest manifest, List<Animation> animations) {
}
