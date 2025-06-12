package com.lottie4j.core.model;

public enum AnimatedValueType {
    X(0),
    Y(1),
    WIDTH(0),
    HEIGHT(1),
    RED(0),
    GREEN(1),
    BLEU(2),
    OPACITY(3),
    COLOR(0) // TODO
    ;

    final int index;

    AnimatedValueType(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
