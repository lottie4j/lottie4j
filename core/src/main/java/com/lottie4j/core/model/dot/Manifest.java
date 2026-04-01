package com.lottie4j.core.model.dot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;

import java.util.List;

/**
 * Represents the {@code manifest.json} metadata of a dotLottie archive.
 * <p>
 * Supports both dotLottie v1 and v2 manifest shapes:
 * <ul>
 *   <li><b>Common (v1 &amp; v2)</b>: {@code version}, {@code generator}, {@code animations}</li>
 *   <li><b>v1 only</b>: {@code author}</li>
 *   <li><b>v2 only</b>: {@code themes}, {@code stateMachines}, {@code initial}</li>
 * </ul>
 *
 * @param version       the dotLottie format version (e.g. {@code "1"} or {@code "2"})
 * @param generator     name and version of the tool that generated this file
 * @param author        (v1) author or creator of the animation
 * @param animations    metadata for each animation bundled in the archive
 * @param themes        (v2) metadata for each theme file in {@code t/}
 * @param stateMachines (v2) metadata for each state machine file in {@code s/}
 * @param initial       (v2) which animation or state machine to load first
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public record Manifest(
        @JsonProperty("version") String version,
        @JsonProperty("generator") String generator,
        // v1 field
        @JsonProperty("author") String author,
        @JsonProperty("animations") List<ManifestAnimation> animations,
        // v2 fields
        @JsonProperty("themes") List<ManifestTheme> themes,
        @JsonProperty("stateMachines") List<ManifestStateMachine> stateMachines,
        @JsonProperty("initial") ManifestInitial initial
) implements PropertyListing {
    @Override
    @JsonIgnore
    public PropertyListingList getList() {
        var list = new PropertyListingList("Manifest");
        list.add("Version", version);
        list.add("Generator", generator);
        list.add("Author (v1)", author);
        list.addList("Animations", animations);
        list.addList("Themes (v2)", themes);
        list.addList("State machines (v2)", stateMachines);
        if (initial != null) {
            list.add("Initial animation (v2)", initial.animation());
            list.add("Initial state machine (v2)", initial.stateMachine());
        }
        return list;
    }
}
