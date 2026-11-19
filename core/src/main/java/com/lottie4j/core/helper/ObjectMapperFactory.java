package com.lottie4j.core.helper;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.deser.ValueInstantiator;

import java.util.List;

/**
 * Factory class that provides a properly configured ObjectMapper instance
 * with all necessary modules registered.
 */
public class ObjectMapperFactory {

    private static final ObjectMapper INSTANCE = createObjectMapper();

    private ObjectMapperFactory() {
        // Private constructor to prevent instantiation
    }

    /**
     * Returns a shared, thread-safe ObjectMapper instance configured with:
     * <ul>
     *     <li>Built-in JDK8 support (Optional, OptionalInt, etc.) provided by Jackson 3</li>
     *     <li>Proper handling of Java records with @JsonProperty annotations</li>
     *     <li>Ignores non-annotated methods</li>
     * </ul>
     *
     * @return configured ObjectMapper instance
     */
    public static ObjectMapper getInstance() {
        return INSTANCE;
    }

    private static ObjectMapper createObjectMapper() {
        return JsonMapper.builder()
                .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS)
                // Note: SORT_PROPERTIES_ALPHABETICALLY is disabled to preserve the original property order
                // from the Lottie JSON, which is critical for features like track mattes that depend on
                // layer adjacency and property ordering
                .changeDefaultPropertyInclusion(i -> i.withValueInclusion(JsonInclude.Include.NON_NULL))
                // A value of the wrong kind (a text where a number belongs, a number where an object
                // belongs) is read as absent instead of failing the whole file, and what is absent
                // is left out of lists, so that a list holds only what could be read.
                .addHandler(new LenientValues())
                .withConfigOverride(List.class,
                        override -> override.setNullHandling(JsonSetter.Value.forContentNulls(Nulls.SKIP)))
                .build();
    }

    /** Reads a value that is not of the kind its place asks for as {@code null}. */
    private static final class LenientValues extends DeserializationProblemHandler {

        private static final Logger logger = LoggerFactory.getLogger(LenientValues.class);

        @Override
        public Object handleWeirdStringValue(DeserializationContext context, Class<?> targetType, String value,
                                             String failureMessage) {
            return absent(targetType, failureMessage);
        }

        @Override
        public Object handleWeirdNumberValue(DeserializationContext context, Class<?> targetType, Number value,
                                             String failureMessage) {
            return absent(targetType, failureMessage);
        }

        @Override
        public Object handleWeirdNativeValue(DeserializationContext context, JavaType targetType, Object value,
                                             JsonParser parser) {
            return absent(targetType.getRawClass(), "unexpected value");
        }

        @Override
        public Object handleUnexpectedToken(DeserializationContext context, JavaType targetType, JsonToken token,
                                            JsonParser parser, String failureMessage) {
            parser.skipChildren();
            return absent(targetType.getRawClass(), failureMessage);
        }

        @Override
        public Object handleMissingInstantiator(DeserializationContext context, Class<?> type,
                                                ValueInstantiator instantiator, JsonParser parser,
                                                String failureMessage) {
            parser.skipChildren();
            return absent(type, failureMessage);
        }

        private static Object absent(Class<?> type, String reason) {
            if (type.isPrimitive()) {
                return NOT_HANDLED;
            }
            logger.debug("A value that is no {} is read as absent: {}", type.getSimpleName(), reason);
            return null;
        }
    }
}
