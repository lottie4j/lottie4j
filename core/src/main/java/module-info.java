module com.lottie4j.core {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires org.slf4j;

    exports com.lottie4j.core.model.bezier;
    exports com.lottie4j.core.model.dot;
    exports com.lottie4j.core.definition;
    exports com.lottie4j.core.exception;
    exports com.lottie4j.core.file;
    exports com.lottie4j.core.helper;
    exports com.lottie4j.core.info;
    exports com.lottie4j.core.model.keyframe;
    exports com.lottie4j.core.model.shape;

    opens com.lottie4j.core.definition to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model.keyframe to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model.shape to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model.bezier to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.info to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.shape.shape;
    opens com.lottie4j.core.model.shape.shape to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.shape.style;
    opens com.lottie4j.core.model.shape.style to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.shape.grouping;
    opens com.lottie4j.core.model.shape.grouping to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.shape.modifier;
    opens com.lottie4j.core.model.shape.modifier to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model.animation to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.animation;
    exports com.lottie4j.core.model.asset;
    opens com.lottie4j.core.model.asset to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.effect;
    opens com.lottie4j.core.model.effect to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.layer;
    opens com.lottie4j.core.model.layer to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.text;
    opens com.lottie4j.core.model.text to com.fasterxml.jackson.databind;
    exports com.lottie4j.core.model.transform;
    opens com.lottie4j.core.model.transform to com.fasterxml.jackson.databind;
}