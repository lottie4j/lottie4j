package com.lottie4j.fxplayer.player;

import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import com.lottie4j.fxplayer.element.ShapeDrawer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

public class LottiePlayer extends Canvas {

    private final Animation animation;
    private final GraphicsContext graphicContext;

    public LottiePlayer(Animation animation) {
        this.animation = animation;
        this.setWidth(animation.width());
        this.setHeight(animation.height());
        graphicContext = this.getGraphicsContext2D();
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
        layer.shapes().forEach(s -> ShapeDrawer.draw(graphicContext, s));
    }
}
