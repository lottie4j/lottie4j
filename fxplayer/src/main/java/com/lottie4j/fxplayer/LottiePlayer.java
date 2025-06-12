package com.lottie4j.fxplayer;

import com.lottie4j.core.model.Animation;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import java.util.logging.Logger;

/**
 * JavaFX Canvas component for playing Lottie animations
 */
public class LottiePlayer extends Canvas {

    private static final Logger logger = Logger.getLogger(LottiePlayer.class.getName());

    private final Animation animation;
    private final LottieRenderEngine renderEngine;
    private final GraphicsContext gc;

    private AnimationTimer animationTimer;
    private long startTime;
    private boolean isPlaying = false;
    private double currentFrame = 0;

    public LottiePlayer(Animation animation) {
        this(animation, false);
    }

    public LottiePlayer(Animation animation, boolean debug) {
        this.animation = animation;
        this.renderEngine = new LottieRenderEngine(animation, debug);

        // Set canvas size to animation size
        setWidth(animation.width());
        setHeight(animation.height());

        this.gc = getGraphicsContext2D();

        // Initial render
        renderFrame(animation.inPoint());
    }

    public void play() {
        if (isPlaying) return;

        isPlaying = true;
        startTime = System.nanoTime();
        currentFrame = animation.inPoint();

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                double elapsedSeconds = (now - startTime) / 1_000_000_000.0;
                double totalFrames = animation.outPoint() - animation.inPoint();
                double animationDuration = totalFrames / animation.framesPerSecond();

                if (elapsedSeconds >= animationDuration) {
                    // Loop animation
                    startTime = now;
                    elapsedSeconds = 0;
                }

                currentFrame = animation.inPoint() + (elapsedSeconds * animation.framesPerSecond());
                renderFrame(currentFrame);
            }
        };

        animationTimer.start();
    }

    public void stop() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
        isPlaying = false;
    }

    public void seekToFrame(double frame) {
        currentFrame = Math.max(animation.inPoint(),
                Math.min(animation.outPoint(), frame));
        renderFrame(currentFrame);
    }

    private void renderFrame(double frame) {
        renderEngine.render(gc, frame);
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public double getCurrentFrame() {
        return currentFrame;
    }

    public Animation getAnimation() {
        return animation;
    }
}