module com.lottie4j.core {
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;

    exports com.lottie4j.core.model.bezier;
    exports com.lottie4j.core.definition;
    exports com.lottie4j.core.exception;
    exports com.lottie4j.core.handler;
    exports com.lottie4j.core.helper;
    exports com.lottie4j.core.info;
    exports com.lottie4j.core.model;
    exports com.lottie4j.core.model.keyframe;
    exports com.lottie4j.core.model.shape;

    opens com.lottie4j.core.definition to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model.keyframe to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model.shape to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.model.bezier to com.fasterxml.jackson.databind;
    opens com.lottie4j.core.info to com.fasterxml.jackson.databind;
}