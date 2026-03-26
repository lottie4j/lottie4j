/**
 * Core module for Lottie4J animation parsing and manipulation.
 * Provides model classes, definitions, and utilities for Lottie animations.
 */
module com.lottie4j.core {
    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.databind;
    requires org.slf4j;

    exports com.lottie4j.core.model.bezier;
    exports com.lottie4j.core.model.dot;
    opens com.lottie4j.core.model.dot to tools.jackson.databind;
    exports com.lottie4j.core.definition;
    exports com.lottie4j.core.exception;
    exports com.lottie4j.core.file;
    exports com.lottie4j.core.helper;
    exports com.lottie4j.core.info;
    exports com.lottie4j.core.model.keyframe;
    exports com.lottie4j.core.model.shape;

    opens com.lottie4j.core.definition to tools.jackson.databind;
    opens com.lottie4j.core.model.keyframe to tools.jackson.databind;
    opens com.lottie4j.core.model.shape to tools.jackson.databind;
    opens com.lottie4j.core.model.bezier to tools.jackson.databind;
    opens com.lottie4j.core.info to tools.jackson.databind;
    exports com.lottie4j.core.model.shape.shape;
    opens com.lottie4j.core.model.shape.shape to tools.jackson.databind;
    exports com.lottie4j.core.model.shape.style;
    opens com.lottie4j.core.model.shape.style to tools.jackson.databind;
    exports com.lottie4j.core.model.shape.grouping;
    opens com.lottie4j.core.model.shape.grouping to tools.jackson.databind;
    exports com.lottie4j.core.model.shape.modifier;
    opens com.lottie4j.core.model.shape.modifier to tools.jackson.databind;
    opens com.lottie4j.core.model.animation to tools.jackson.databind;
    exports com.lottie4j.core.model.animation;
    exports com.lottie4j.core.model.asset;
    opens com.lottie4j.core.model.asset to tools.jackson.databind;
    exports com.lottie4j.core.model.effect;
    opens com.lottie4j.core.model.effect to tools.jackson.databind;
    exports com.lottie4j.core.model.layer;
    opens com.lottie4j.core.model.layer to tools.jackson.databind;
    exports com.lottie4j.core.model.text;
    opens com.lottie4j.core.model.text to tools.jackson.databind;
    exports com.lottie4j.core.model.transform;
    opens com.lottie4j.core.model.transform to tools.jackson.databind;
}