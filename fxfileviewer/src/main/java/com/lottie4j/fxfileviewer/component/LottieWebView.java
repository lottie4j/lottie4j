package com.lottie4j.fxfileviewer.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.Animation;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class LottieWebView extends Pane {
    private static final Logger logger = LoggerFactory.getLogger(LottieWebView.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private final WebEngine webEngine;
    private final WebView webView;

    public LottieWebView() {
        webView = new WebView();
        webEngine = webView.getEngine();
        webView.setPrefSize(500, 500);
        webView.setMaxSize(500, 500);

        getChildren().add(webView);
        setPrefSize(500, 500);
        setMaxSize(500, 500);

        webView.getEngine().setOnAlert(event -> logger.warn("JS Alert: {}", event.getData()));
        webView.getEngine().setOnError(event -> logger.error("JS Error: {}", event.getMessage()));
    }

    public WebEngine getEngine() {
        return webEngine;
    }

    public int getCurrentFrame() {
        Object frameObj = executeScriptSync("window.getCurrentFrame && window.getCurrentFrame()", -1);
        if (frameObj instanceof Number number) {
            return number.intValue();
        }
        return -1;
    }

    public String getRenderDebug() {
        Object debugObj = executeScriptSync("window.getRenderDebug && window.getRenderDebug()", "");
        return debugObj != null ? String.valueOf(debugObj) : "";
    }

    public boolean waitUntilReady(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Object readyObj = executeScriptSync("window.isAnimationReady && window.isAnimationReady()", false);
            if (Boolean.TRUE.equals(readyObj)) {
                return true;
            }
            waitForFxPulse();
        }
        return false;
    }

    public boolean waitUntilFrame(int expectedFrame, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (getCurrentFrame() == expectedFrame) {
                return true;
            }
            waitForFxPulse();
        }
        return false;
    }

    private Object executeScriptSync(String script, Object fallback) {
        if (Platform.isFxApplicationThread()) {
            try {
                return webEngine.executeScript(script);
            } catch (Exception e) {
                logger.debug("Script execution failed: {}", e.getMessage());
                return fallback;
            }
        }

        CountDownLatch latch = new CountDownLatch(1);
        final Object[] result = new Object[]{fallback};
        Platform.runLater(() -> {
            try {
                result[0] = webEngine.executeScript(script);
            } catch (Exception e) {
                logger.debug("Script execution failed: {}", e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return fallback;
        }
        return result[0];
    }

    private void waitForFxPulse() throws InterruptedException {
        CountDownLatch pulse = new CountDownLatch(1);
        Platform.runLater(pulse::countDown);
        pulse.await();
    }

    public void setFrame(int frame) {
        try {
            webEngine.executeScript("window.seekToFrame(" + frame + ")");
            webView.requestLayout();
        } catch (Exception e) {
            logger.warn("Failed to seek JS animation: {}", e.getMessage());
        }
    }

    public void setSize(int width, int height) {
        setPrefSize(width, height);
        setMaxSize(width, height);
        setMinSize(width, height);
        webView.setPrefSize(width, height);
        webView.setMaxSize(width, height);
        webView.setMinSize(width, height);
    }

    public void play() {
        try {
            webEngine.executeScript("window.playAnimation()");
        } catch (Exception e) {
            logger.warn("Failed to start JS animation: {}", e.getMessage());
        }
    }

    public void playOnce() {
        try {
            webEngine.executeScript("window.playAnimationOnce()");
        } catch (Exception e) {
            logger.warn("Failed to start JS animation once: {}", e.getMessage());
        }
    }

    public void pause() {
        try {
            webEngine.executeScript("window.pauseAnimation()");
        } catch (Exception e) {
            logger.warn("Failed to pause JS animation: {}", e.getMessage());
        }
    }

    /**
     * Loads a Lottie animation into the WebView using the lottie-web JavaScript library.
     * Generates HTML with embedded animation data and control functions.
     *
     * @param animation {@link Animation}
     * @param width     the width of the player in pixels
     * @param height    the height of the player in pixels
     */
    public void loadLottie(Animation animation, int width, int height) {
        try {
            setSize(width, height);
            // Serialize the parsed model so JSON and dotLottie inputs use the same normalized data in WebView.
            var lottieJson = OBJECT_MAPPER.writeValueAsString(animation);

            // Escape JSON for embedding in JavaScript
            var escapedJson = lottieJson.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");

            // Create HTML with lottie-web player
            var html = """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <style>
                            body {
                                margin: 0;
                                padding: 0;
                                width: %spx;
                                height: %spx;
                                background-color: #ffffff;
                                overflow: hidden;
                            }
                            #lottie-container {
                                width: %spx;
                                height: %spx;
                                margin: 0;
                                padding: 0;
                            }
                        </style>
                        <script src="https://cdnjs.cloudflare.com/ajax/libs/bodymovin/5.12.2/lottie.min.js"></script>
                    </head>
                    <body>
                        <div id="lottie-container"></div>
                        <script>
                            var animationReady = false;
                            var animationData = JSON.parse("%s");
                            var animation = lottie.loadAnimation({
                                container: document.getElementById('lottie-container'),
                                renderer: 'svg',
                                loop: true,
                                autoplay: false,
                                animationData: animationData,
                                rendererSettings: {
                                    preserveAspectRatio: 'xMidYMid meet'
                                }
                            });
                    
                            animation.addEventListener('DOMLoaded', function() {
                                animationReady = true;
                            });
                    
                            window.playAnimation = function() {
                                animation.loop = true;
                                animation.play();
                            };
                    
                            window.playAnimationOnce = function() {
                                animation.loop = false;
                                animation.goToAndPlay(0, true);
                            };
                    
                            window.pauseAnimation = function() {
                                animation.pause();
                            };
                    
                            window.stopAnimation = function() {
                                animation.stop();
                            };
                    
                            window.seekToFrame = function(frame) {
                                animation.goToAndStop(frame, true);
                                var svg = document.querySelector('svg');
                                if (svg) {
                                    void svg.getBoundingClientRect();
                                }
                            };
                    
                            window.isAnimationReady = function() {
                                return animationReady;
                            };
                    
                            window.getCurrentFrame = function() {
                                return animation ? Math.round(animation.currentFrame) : -1;
                            };
                    
                            window.getRenderDebug = function() {
                                var svgCount = document.querySelectorAll('svg').length;
                                var svg = document.querySelector('svg');
                                var display = svg ? window.getComputedStyle(svg).display : 'no svg';
                                return 'SVG count: ' + svgCount + ', display: ' + display + ', currentFrame: ' + window.getCurrentFrame();
                            };
                    
                            window.setBackgroundColor = function(color) {
                                document.body.style.backgroundColor = color;
                            };
                        </script>
                    </body>
                    </html>
                    """.formatted(width, height, width, height, escapedJson);

            webEngine.loadContent(html);
        } catch (IOException e) {
            logger.error("Failed to load Lottie file in WebView: {}", e.getMessage());
        }
    }

    public void setBackgroundColor(Color backgroundColor) {
        try {
            var colorHex = String.format("#%02X%02X%02X",
                    (int) (backgroundColor.getRed() * 255),
                    (int) (backgroundColor.getGreen() * 255),
                    (int) (backgroundColor.getBlue() * 255));
            webEngine.executeScript("window.setBackgroundColor('" + colorHex + "')");
        } catch (Exception e) {
            logger.warn("Failed to update JS background color: {}", e.getMessage());
        }
    }
}
