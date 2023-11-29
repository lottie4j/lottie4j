package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.definition.MatteMode;
import com.lottie4j.core.model.shape.BaseShape;

import java.util.ArrayList;
import java.util.List;

/**
 * <a href="https://lottiefiles.github.io/lottie-docs/layers/">Lottie Docs: Layer</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Layer(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ddd") Integer has3dLayers,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("ty") LayerType layerType,
        @JsonProperty("ind") Double indexLayer,
        @JsonProperty("parent") Integer indexParent,
        @JsonProperty("sr") Integer timeStretch,
        @JsonProperty("ip") Double inPoint,
        @JsonProperty("op") Integer outPoint,
        @JsonProperty("st") Integer startTime,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("cl") String clazz,
        @JsonProperty("ln") String idAttribute,
        @JsonProperty("tg") String tagName,
        @JsonProperty("tt") MatteMode matteMode,
        @JsonProperty("td") Integer matteTarget,
        @JsonProperty("masksProperties") List<Mask> masks,
        @JsonProperty("ef") List<Effect> effects,
        @JsonProperty("ks") Transform transform,
        @JsonProperty("ao") Integer autoRotate,

        // Unknown
        @JsonProperty("hix") Integer hix,

        // Shape
        @JsonProperty("shapes") List<BaseShape> shapes,

        // Precomposition
        // https://lottiefiles.github.io/lottie-docs/assets/#precomposition
        @JsonProperty("refId") String referenceId,
        @JsonProperty("w") Integer width,
        @JsonProperty("h") Integer height,
        @JsonProperty("tm") Animated timeRemapping

) implements PropertyListing {
    @Override
    public List<PropertyLabelValue> getLabelValues() {
        return List.of(
                new PropertyLabelValue("Match name", matchName()),
                new PropertyLabelValue("Has 3D layers", has3dLayers()),
                new PropertyLabelValue("Hidden", hidden()),
                new PropertyLabelValue("Layer type", (layerType() == null ? "-" : layerType().label())),
                new PropertyLabelValue("Index layer", indexLayer()),
                new PropertyLabelValue("Index Parent", indexParent()),
                new PropertyLabelValue("Time stretch", timeStretch()),
                new PropertyLabelValue("In point", inPoint()),
                new PropertyLabelValue("Out point", outPoint()),
                new PropertyLabelValue("Start type", startTime()),
                new PropertyLabelValue("Blend mode", (blendMode() == null ? "-" : blendMode().label())),
                new PropertyLabelValue("Clazz", clazz()),
                new PropertyLabelValue("ID attribute", idAttribute()),
                new PropertyLabelValue("Tag name", tagName()),
                new PropertyLabelValue("Matte mode", (matteMode() == null ? "-" : matteMode().label())),
                new PropertyLabelValue("Matte target", matteTarget()),
                new PropertyLabelValue("Auto rotate", autoRotate()),
                new PropertyLabelValue("Hix", hix()),
                new PropertyLabelValue("Reference ID", referenceId()),
                new PropertyLabelValue("Width", width()),
                new PropertyLabelValue("Height", height()),
                new PropertyLabelValue("Masks", masks == null ? "0" : String.valueOf(masks.size()),
                        masks == null ? new ArrayList<>() : masks.stream().map(m -> new PropertyLabelValue("Mask", "", m.getLabelValues())).toList()),
                new PropertyLabelValue("Effects", effects == null ? "0" : String.valueOf(effects.size()),
                        effects == null ? new ArrayList<>() : effects.stream().map(e -> new PropertyLabelValue("Effect", e.name() == null ? "No name" : e.name(), e.getLabelValues())).toList()),
                new PropertyLabelValue("Shapes", shapes == null ? "0" : String.valueOf(shapes.size()),
                        shapes == null ? new ArrayList<>() : shapes.stream().map(s -> new PropertyLabelValue("Shape", s.getName() == null ? "No name" : s.getName(), s.getLabelValues())).toList())
        );
    }
}
