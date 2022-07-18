package be.webtechie.jlottie.core.model;

import be.webtechie.jlottie.core.definition.BlendMode;
import be.webtechie.jlottie.core.definition.MatteMode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * https://lottiefiles.github.io/lottie-docs/layers/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Layer(
        @JsonProperty("nm") String name,
        @JsonProperty("mn") String matchName,
        @JsonProperty("ddd") Integer has3dLayers,
        @JsonProperty("hd") Boolean hidden,
        @JsonProperty("ty") Integer layerType,
        @JsonProperty("ind") Integer indexLayer,
        @JsonProperty("parent") Integer indexParent,
        @JsonProperty("sr") Integer timeStretch,
        @JsonProperty("ip") Integer inPoint,
        @JsonProperty("op") Integer outPoint,
        @JsonProperty("st") Integer startTime,
        @JsonProperty("bm") BlendMode blendMode,
        @JsonProperty("cl") String cssClass,
        @JsonProperty("ln") String idAttribute,
        @JsonProperty("tg") String tagName,
        @JsonProperty("tt") MatteMode matteMode,
        @JsonProperty("td") Integer matteTarget,
        @JsonProperty("masksProperties") List<Mask> masks,
        @JsonProperty("ef") List<Effect> effects
) {
}
