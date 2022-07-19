package be.webtechie.jlottie.core.definition;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * https://lottiefiles.github.io/lottie-docs/effects/
 */
public enum EffectType {
    NORMAL(5, "Old-style Effect"),
    PAINT_OVER_TRANSPARENT(7, "Paint Over Transparent"),
    TINT(20, "Tint"),
    FILL(21, "Fill"),
    STROKE(22, "Stroke"),
    TRITONE(23, "Tritone"),
    PRO_LEVELS(24, "Pro Levels"),
    DROP_SHADOW(25, "Drop Shadow"),
    RADIAL_WIPE(26, "Radial Wipe"),
    DISPLACEMENT_MAP(27, "Displacement Map"),
    MATTE3(28, "Matte3"),
    GAUSSIAN_BLUR(29, "Gaussian Blur"),
    MESH_WARP(31, "Mesh Warp"),
    WAVY(32, "Wavy"),
    SPHERIZE(33, "Spherize"),
    PUPPET(34, "Puppet");

    @JsonValue
    private final int value;
    private final String label;

    EffectType(int value, String label) {
        this.value = value;
        this.label = label;
    }

    public int value() {
        return value;
    }

    public String label() {
        return label;
    }
}
