package be.webtechie.jlottie.core.definition;

/**
 * https://lottiefiles.github.io/lottie-docs/constants/#blendmode
 */
public enum MatteMode {
    NORMAL(0, "Normal"),
 	ALPHA(1, "Alpha"),
 	INVERTED_ALPHA(2, "Inverted Alpha"),
 	LUMA(3, "Luma"),
 	INVERTED_LUMA(4, "Inverted Luma");

    private final int value;
    private final String label;

    MatteMode(int value, String label) {
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
