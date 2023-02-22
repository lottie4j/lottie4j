package com.lottie4j.fxplayer.player;

import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.Group;
import com.lottie4j.fxplayer.element.GroupDrawer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LottiePlayer extends Canvas {

    private static final Logger logger = Logger.getLogger(LottiePlayer.class.getName());

    private final Animation animation;
    private final GraphicsContext graphicContext;
    private final List<GroupDrawer> groupDrawers;

    public LottiePlayer(Animation animation) {
        this.animation = animation;
        this.groupDrawers = new ArrayList<>();
        this.setWidth(animation.width());
        this.setHeight(animation.height());
        graphicContext = this.getGraphicsContext2D();
        // Adding background color, just to see if something is drawn on top of it
        graphicContext.setFill(Color.LIGHTBLUE);
        graphicContext.fillRect(0, 0, animation.width(), animation.height());
        play();
    }

    private void play() {
        animation.layers().forEach(this::drawLayer);

        // DUMMY TEST
        /*
        graphicContext.setFill(Color.DARKKHAKI);
        graphicContext.setStroke(Color.DARKVIOLET);
        graphicContext.setLineWidth(6);
        graphicContext.fillOval(11, 61, 31, 31);
        graphicContext.strokeOval(61, 61, 31, 31);
        graphicContext.fillRoundRect(111, 61, 31, 31, 11, 11);
        graphicContext.strokeRoundRect(161, 61, 31, 31, 11, 11);
        graphicContext.fillArc(11, 111, 31, 31, 46, 241, ArcType.OPEN);
        graphicContext.strokeArc(11, 111, 31, 31, 46, 241, ArcType.OPEN);
        graphicContext.fillArc(61, 111, 31, 31, 46, 241, ArcType.CHORD);
        graphicContext.strokeArc(61, 111, 31, 31, 46, 241, ArcType.CHORD);
        graphicContext.fillArc(110, 111, 31, 31, 46, 241, ArcType.ROUND);
        graphicContext.strokeArc(111, 111, 31, 31, 46, 241, ArcType.ROUND);
        */
    }

    private void drawLayer(Layer layer) {
        logger.log(Level.INFO, "Drawing layer " + layer.name());
        if (layer.referenceId() != null && !layer.referenceId().isEmpty()) {
            logger.log(Level.INFO, "\tReference ID " + layer.referenceId());
            var asset = getAsset(layer.referenceId());
            if (asset != null && asset.layers() != null) {
                for (Layer nestedLayer : asset.layers()) {
                    logger.log(Level.INFO, "\t\tAsset Layer " + nestedLayer.name());
                    if (nestedLayer.shapes() != null) {
                        nestedLayer.shapes().forEach(s -> {
                            if (s instanceof Group group) {
                                var groupDrawer = new GroupDrawer(graphicContext, group);
                                groupDrawers.add(groupDrawer);
                            } else {
                                logger.log(Level.WARNING, "Unexpected shape");
                            }
                        });
                    }
                }
            }
        }
        /*
        if (layer.shapes() != null) {
            layer.shapes().forEach(s -> {
                GroupDrawer.draw(graphicContext, s)
                var groupDrawer = new GroupDrawer(graphicContext, group);
                groupDrawers.add(groupDrawer);
            });
        }
        */
    }

    private Asset getAsset(String referenceId) {
        if (animation.assets() != null) {
            return animation.assets().stream()
                    .filter(a -> a.id().equals(referenceId))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
