package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.definition.MatteMode;
import com.lottie4j.core.info.PropertyListing;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.shape.BaseShape;

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
    public PropertyListingList getList() {
        var list = new PropertyListingList("Layer");
        list.add("Match name", matchName);
        list.add("Has 3D layers", has3dLayers);
        list.add("Hidden", hidden);
        list.add("Layer type", layerType);
        list.add("Index layer", indexLayer);
        list.add("Index Parent", indexParent);
        list.add("Time stretch", timeStretch);
        list.add("In point", inPoint);
        list.add("Out point", outPoint);
        list.add("Start type", startTime);
        list.add("Blend mode", blendMode);
        list.add("Clazz", clazz);
        list.add("ID attribute", idAttribute);
        list.add("Tag name", tagName);
        list.add("Matte mode", matteMode);
        list.add("Matte target", matteTarget);
        list.add("Auto rotate", autoRotate);
        list.add("Hix", hix);
        list.add("Reference ID", referenceId);
        list.add("Width", width);
        list.add("Height", height);
        list.add("Time remapping", timeRemapping);
        list.addList("Masks", masks);
        list.addList("Effects", effects);
        list.addShapeList("Shapes", shapes);
        return list;
    }
}
