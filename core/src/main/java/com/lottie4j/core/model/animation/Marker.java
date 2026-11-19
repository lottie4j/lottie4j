package com.lottie4j.core.model.animation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

/**
 * Represents a marker in a Lottie animation that defines a named point or range in time.
 * <p>
 * Markers are used to identify specific frames or time ranges within an animation timeline,
 * allowing for synchronized playback control, navigation to specific animation segments,
 * or triggering events at designated points during animation playback.
 * <p>
 * This record encapsulates marker information including its temporal position, an optional
 * descriptive comment, and an optional duration for range-based markers. The JSON property
 * mappings use abbreviated names for compact serialization.
 *
 * @param time     the temporal position of the marker in the animation timeline, measured in frames
 * @param comment  an optional descriptive label or identifier for the marker
 * @param duration an optional duration in frames, defining a time range when the marker represents
 *                 a segment rather than a single point in time
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Marker(
        @JsonProperty("tm") Double time,
        @JsonProperty("cm") String comment,
        @JsonProperty("dr") Double duration
) implements PropertyListing {

    /**
     * The name of the marker. The comment is usually the name itself, but some exporters write a
     * JSON object there (<code>{"name": "intro"}</code>); then the name is taken out of it.
     *
     * @return the name, without surrounding white space; empty if the marker has none
     */
    @JsonIgnore
    public String name() {
        if (comment == null) {
            return "";
        }
        String text = comment.trim();
        if (text.startsWith("{")) {
            try {
                JsonNode name = ObjectMapperFactory.getInstance().readTree(text).get("name");
                if (name != null && name.isString()) {
                    return name.stringValue().trim();
                }
            } catch (JacksonException e) {
                // not JSON after all: the comment is the name
            }
        }
        return text;
    }
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Marker");
        list.add("Time", time);
        list.add("Comment", comment);
        list.add("Duration", duration);
        return list;
    }
}

