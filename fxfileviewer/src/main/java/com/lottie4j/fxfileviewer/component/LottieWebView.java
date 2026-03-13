package com.lottie4j.fxfileviewer.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animation;
import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.CountDownLatch;

/**
 * A JavaFX component that renders Lottie animations using a WebView with the dotlottie-wc library.
 * Provides controls for playing, pausing, seeking, and inspecting animation frames.
 * Includes optional debug overlay for troubleshooting rendering issues.
 */
public class LottieWebView extends Pane {
    private static final Logger logger = LoggerFactory.getLogger(LottieWebView.class);
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();
    private static final int DEFAULT_SIZE = 500;

    private final WebEngine webEngine;
    private final WebView webView;

    /**
     * Constructs a new LottieWebView with default size (500x500 pixels).
     * Initializes the WebView and sets up JavaScript error handlers.
     */
    public LottieWebView() {
        webView = new WebView();
        webView.setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        webView.setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);

        getChildren().add(webView);
        setPrefSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setMaxSize(DEFAULT_SIZE, DEFAULT_SIZE);

        webEngine = webView.getEngine();

    }

    /**
     * Gets the current frame number of the animation.
     *
     * @return the current frame number, or -1 if unavailable
     */
    public int getCurrentFrame() {
        Object frameObj = executeScriptSync("window.getCurrentFrame && window.getCurrentFrame()", -1);
        if (frameObj instanceof Number number) {
            return number.intValue();
        }
        return -1;
    }

    /**
     * Gets debug information from the WebView renderer.
     *
     * @return a string containing debug information, or empty string if unavailable
     */
    public String getRenderDebug() {
        Object debugObj = executeScriptSync("window.getRenderDebug && window.getRenderDebug()", "");
        return debugObj != null ? String.valueOf(debugObj) : "";
    }

    /**
     * Waits until the animation is ready to be played.
     *
     * @param timeoutMs the maximum time to wait in milliseconds
     * @return true if the animation became ready within the timeout, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public boolean waitUntilReady(long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            Object readyObj = executeScriptSync("window.isAnimationReady && window.isAnimationReady()", false);
            if (Boolean.TRUE.equals(readyObj)) {
                return true;
            }
            waitForFxPulse();
        }
        logger.warn("WebView animation did not become ready in {}ms. {}", timeoutMs, getRenderDebug());
        return false;
    }

    /**
     * Waits until the animation reaches a specific frame.
     *
     * @param expectedFrame the frame number to wait for
     * @param timeoutMs     the maximum time to wait in milliseconds
     * @return true if the expected frame was reached within the timeout, false otherwise
     * @throws InterruptedException if the thread is interrupted while waiting
     */
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

    /**
     * Executes a JavaScript command synchronously, handling both FX and non-FX threads.
     *
     * @param script   the JavaScript code to execute
     * @param fallback the value to return if execution fails
     * @return the result of script execution, or fallback on error
     */
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

    /**
     * Waits for the next JavaFX pulse cycle to complete.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    private void waitForFxPulse() throws InterruptedException {
        CountDownLatch pulse = new CountDownLatch(1);
        Platform.runLater(pulse::countDown);
        pulse.await();
    }

    /**
     * Seeks the animation to a specific frame.
     *
     * @param frame the frame number to seek to
     */
    public void setFrame(int frame) {
        try {
            webEngine.executeScript("window.seekToFrame(" + frame + ")");
            webView.requestLayout();
        } catch (Exception e) {
            logger.warn("Failed to seek JS animation: {}", e.getMessage());
        }
    }

    /**
     * Sets the size of the WebView component.
     *
     * @param width  the width in pixels
     * @param height the height in pixels
     */
    public void setSize(int width, int height) {
        setPrefSize(width, height);
        setMaxSize(width, height);
        setMinSize(width, height);
        webView.setPrefSize(width, height);
        webView.setMaxSize(width, height);
        webView.setMinSize(width, height);
    }

    /**
     * Starts playing the animation in a continuous loop.
     */
    public void play() {
        try {
            webEngine.executeScript("window.playAnimation()");
        } catch (Exception e) {
            logger.warn("Failed to start JS animation: {}", e.getMessage());
        }
    }

    /**
     * Plays the animation once from start to finish without looping.
     */
    public void playOnce() {
        try {
            webEngine.executeScript("window.playAnimationOnce()");
        } catch (Exception e) {
            logger.warn("Failed to start JS animation once: {}", e.getMessage());
        }
    }

    /**
     * Pauses the animation at the current frame.
     */
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
        loadLottie(animation, width, height, false);
    }

    /**
     * Loads a Lottie animation into the WebView and optionally shows an in-WebView debug overlay.
     *
     * @param animation     the Lottie animation to load
     * @param width         the width of the player in pixels
     * @param height        the height of the player in pixels
     * @param showDebugInfo if true, displays a debug overlay with rendering information
     */
    public void loadLottie(Animation animation, int width, int height, boolean showDebugInfo) {
        try {
            setSize(width, height);
            var lottieJson = OBJECT_MAPPER.writeValueAsString(animation);
            var encodedJson = Base64.getEncoder().encodeToString(lottieJson.getBytes(StandardCharsets.UTF_8));

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
                                position: relative;
                            }
                            #lottie-container {
                                width: %spx;
                                height: %spx;
                                margin: 0;
                                padding: 0;
                                display: block;
                            }
                            #debug-overlay {
                                position: absolute;
                                top: 6px;
                                left: 6px;
                                z-index: 9999;
                                max-width: calc(100%% - 12px);
                                max-height: calc(100%% - 12px);
                                overflow: hidden;
                                white-space: pre-wrap;
                                word-break: break-word;
                                font-family: Menlo, Monaco, monospace;
                                font-size: 10px;
                                line-height: 1.25;
                                color: #111111;
                                background: rgba(255, 255, 200, 0.92);
                                border: 1px solid rgba(0, 0, 0, 0.25);
                                border-radius: 4px;
                                padding: 6px;
                                pointer-events: auto;
                                cursor: copy;
                                user-select: text;
                            }
                        </style>
                        <script src="https://unpkg.com/@lottiefiles/dotlottie-wc@0.7.1/dist/dotlottie-wc.js" type="module"></script>
                    </head>
                    <body>
                        <dotlottie-wc id="lottie-container"></dotlottie-wc>
                        <div id="debug-overlay">Initializing WebView renderer...</div>
                        <script>
                            // Earliest possible marker: proves JS executes even if later init fails.
                            (function() {
                                var ov = document.getElementById('debug-overlay');
                                if (ov) ov.textContent = 'JS started';
                            })();
                        </script>
                        <script>
                            var animationReady = false;
                            var currentFrame = 0;
                            var showDebugInfo = %s;
                            var encodedAnimationData = "%s";
                            var player = document.getElementById('lottie-container');
                            var debugOverlay = document.getElementById('debug-overlay');
                            var diagLogs = [];
                    
                            if (debugOverlay && !showDebugInfo) {
                                debugOverlay.style.display = 'none';
                            }
                    
                            function decodeBase64Json(base64) {
                                var binary = atob(base64);
                                var bytes = new Uint8Array(binary.length);
                                for (var i = 0; i < binary.length; i++) {
                                    bytes[i] = binary.charCodeAt(i);
                                }
                                return new TextDecoder('utf-8').decode(bytes);
                            }
                    
                            function renderOverlay(extraLine) {
                                if (!debugOverlay || !showDebugInfo) {
                                    return;
                                }
                                var lines = [
                                    'renderer: dotlottie-wc@0.7.1',
                                    'ready: ' + animationReady,
                                    'frame: ' + window.getCurrentFrame(),
                                    'tag: ' + (player ? player.tagName : 'none'),
                                    '(click this panel to copy full debug text)'
                                ];
                                if (extraLine) {
                                    lines.push(extraLine);
                                }
                                var recent = diagLogs.slice(-5);
                                if (recent.length > 0) {
                                    lines.push('--- logs ---');
                                    lines = lines.concat(recent);
                                }
                                debugOverlay.textContent = lines.join('\\n');
                            }
                    
                            function buildDebugText() {
                                var inspection = window.getPlayerApiInspection ? window.getPlayerApiInspection() : { methods: [] };
                                var svgCount = document.querySelectorAll('svg').length;
                                var lines = [
                                    'dotlottie count: ' + document.querySelectorAll('dotlottie-wc').length,
                                    'fallbackActive: ' + !!window.__bodymovinFallbackInstalled,
                                    'svgCount: ' + svgCount,
                                    'ready: ' + animationReady,
                                    'currentFrame: ' + window.getCurrentFrame(),
                                    'methods: ' + (inspection.methods || []).join(','),
                                    'logs: ' + diagLogs.join(' | ')
                                ];
                                return lines.join('\\n');
                            }
                    
                            function copyDebugToClipboard(text) {
                                if (navigator.clipboard && typeof navigator.clipboard.writeText === 'function') {
                                    return navigator.clipboard.writeText(text);
                                }
                                return new Promise(function(resolve, reject) {
                                    try {
                                        var area = document.createElement('textarea');
                                        area.value = text;
                                        area.style.position = 'fixed';
                                        area.style.opacity = '0';
                                        document.body.appendChild(area);
                                        area.focus();
                                        area.select();
                                        var ok = document.execCommand('copy');
                                        document.body.removeChild(area);
                                        if (ok) {
                                            resolve();
                                        } else {
                                            reject(new Error('execCommand copy failed'));
                                        }
                                    } catch (e) {
                                        reject(e);
                                    }
                                });
                            }
                    
                            if (debugOverlay) {
                                debugOverlay.addEventListener('click', function() {
                                    var text = buildDebugText();
                                    copyDebugToClipboard(text)
                                        .then(function() {
                                            renderOverlay('copied debug text to clipboard');
                                        })
                                        .catch(function(err) {
                                            renderOverlay('copy failed: ' + String(err));
                                        });
                                });
                            }
                    
                            function diag(msg) {
                                var line = '[lottie-webview] ' + msg;
                                diagLogs.push(line);
                                if (diagLogs.length > 60) {
                                    diagLogs.shift();
                                }
                                renderOverlay(msg);
                            }
                    
                            window.onerror = function(message, source, lineNo, colNo) {
                                diag('window.onerror: ' + message + ' @ ' + source + ':' + lineNo + ':' + colNo);
                            };
                    
                            window.onunhandledrejection = function(event) {
                                diag('unhandledrejection: ' + String(event && event.reason));
                            };
                    
                            function callFirst(target, names, args) {
                                for (var i = 0; i < names.length; i++) {
                                    var fn = target[names[i]];
                                    if (typeof fn === 'function') {
                                        try {
                                            var value = fn.apply(target, args || []);
                                            diag('call ok: ' + names[i]);
                                            return { ok: true, value: value, name: names[i] };
                                        } catch (e) {
                                            diag('call ' + names[i] + ' failed: ' + String(e));
                                            return { ok: false, error: String(e), name: names[i] };
                                        }
                                    }
                                }
                                return { ok: false, error: 'no matching method', name: null };
                            }
                    
                            function setLoopEnabled(enabled) {
                                var methodResult = callFirst(player, ['setLoop', 'setLooping'], [!!enabled]);
                                if (!methodResult.ok) {
                                    player.loop = !!enabled;
                                    diag('set loop via property: ' + (!!enabled));
                                }
                            }
                    
                            function setFrameValue(frame) {
                                var target = Math.round(frame);
                                currentFrame = target;
                                var result = callFirst(player, ['setFrame', 'seek', 'goToAndStop', 'setCurrentFrame'], [target]);
                                if (!result.ok) {
                                    var ratio = 0;
                                    if (typeof player.totalFrames === 'number' && player.totalFrames > 1) {
                                        ratio = target / (player.totalFrames - 1);
                                        if (ratio < 0) ratio = 0;
                                        if (ratio > 1) ratio = 1;
                                    }
                                    callFirst(player, ['setSeeker'], [ratio]);
                                }
                                renderOverlay();
                            }
                    
                            function buildSrcFromJson(data) {
                                try {
                                    var json = JSON.stringify(data);
                                    var base64 = btoa(unescape(encodeURIComponent(json)));
                                    return 'data:application/json;base64,' + base64;
                                } catch (e) {
                                    var blob = new Blob([JSON.stringify(data)], { type: 'application/json' });
                                    return URL.createObjectURL(blob);
                                }
                            }
                    
                            function installBodymovinFallback(animationData) {
                                if (window.__bodymovinFallbackInstalled) {
                                    return;
                                }
                                window.__bodymovinFallbackInstalled = true;
                                diag('Installing bodymovin fallback');
                    
                                var fallbackScript = document.createElement('script');
                                fallbackScript.src = 'https://cdnjs.cloudflare.com/ajax/libs/bodymovin/5.12.2/lottie.min.js';
                                fallbackScript.onload = function() {
                                    var host = document.getElementById('lottie-container');
                                    host.style.display = 'none';
                    
                                    var fallbackHost = document.getElementById('bodymovin-fallback-host');
                                    if (!fallbackHost) {
                                        fallbackHost = document.createElement('div');
                                        fallbackHost.id = 'bodymovin-fallback-host';
                                        fallbackHost.style.position = 'absolute';
                                        fallbackHost.style.top = '0';
                                        fallbackHost.style.left = '0';
                                        fallbackHost.style.width = '100%%';
                                        fallbackHost.style.height = '100%%';
                                        fallbackHost.style.zIndex = '1';
                                        document.body.appendChild(fallbackHost);
                                    }
                                    fallbackHost.innerHTML = '';
                    
                                    var fallbackContainer = document.createElement('div');
                                    fallbackContainer.style.width = '100%%';
                                    fallbackContainer.style.height = '100%%';
                                    fallbackHost.appendChild(fallbackContainer);
                    
                                    var animation = window.lottie.loadAnimation({
                                        container: fallbackContainer,
                                        renderer: 'svg',
                                        loop: true,
                                        autoplay: false,
                                        animationData: animationData,
                                        rendererSettings: { preserveAspectRatio: 'xMidYMid meet' }
                                    });
                    
                                    animation.addEventListener('DOMLoaded', function() {
                                        animationReady = true;
                                        var svg = fallbackContainer.querySelector('svg');
                                        var rect = fallbackContainer.getBoundingClientRect();
                                        diag('bodymovin fallback DOMLoaded; svg=' + (svg ? 'yes' : 'no') + '; bounds=' + Math.round(rect.width) + 'x' + Math.round(rect.height));
                                        renderOverlay();
                                    });
                    
                                    window.playAnimation = function() {
                                        animation.loop = true;
                                        animation.play();
                                    };
                                    window.playAnimationOnce = function() {
                                        animation.loop = false;
                                        animation.goToAndPlay(0, true);
                                    };
                                    window.pauseAnimation = function() { animation.pause(); };
                                    window.stopAnimation = function() { animation.stop(); currentFrame = 0; renderOverlay(); };
                                    window.seekToFrame = function(frame) {
                                        currentFrame = Math.round(frame);
                                        animation.goToAndStop(currentFrame, true);
                                        renderOverlay();
                                    };
                                    window.getCurrentFrame = function() {
                                        return Math.round(animation.currentFrame || currentFrame || 0);
                                    };
                                    renderOverlay('fallback: bodymovin active');
                                };
                                fallbackScript.onerror = function() {
                                    diag('bodymovin fallback script failed to load');
                                };
                                document.head.appendChild(fallbackScript);
                            }
                    
                            try {
                                var animationData = JSON.parse(decodeBase64Json(encodedAnimationData));
                    
                                function attachReadyEvents() {
                                    ['ready', 'load', 'loaded', 'complete'].forEach(function(eventName) {
                                        player.addEventListener(eventName, function() {
                                            animationReady = true;
                                            diag('event:' + eventName);
                                            renderOverlay();
                                        });
                                    });
                                }
                    
                                function attachFrameEvents() {
                                    ['frame', 'render', 'enterFrame'].forEach(function(eventName) {
                                        player.addEventListener(eventName, function(event) {
                                            var value = null;
                                            if (event && typeof event.currentFrame === 'number') {
                                                value = event.currentFrame;
                                            } else if (event && event.detail && typeof event.detail.currentFrame === 'number') {
                                                value = event.detail.currentFrame;
                                            } else if (typeof player.currentFrame === 'number') {
                                                value = player.currentFrame;
                                            }
                                            if (typeof value === 'number') {
                                                currentFrame = Math.round(value);
                                                renderOverlay();
                                            }
                                        });
                                    });
                                }
                    
                                attachReadyEvents();
                                attachFrameEvents();
                    
                                customElements.whenDefined('dotlottie-wc').then(function() {
                                    var srcUrl = buildSrcFromJson(animationData);
                                    player.setAttribute('src', srcUrl);
                                    diag('dotlottie-wc defined, src assigned');
                                    renderOverlay();
                                }).catch(function(e) {
                                    diag('customElements.whenDefined failed: ' + String(e));
                                    installBodymovinFallback(animationData);
                                });
                    
                                setTimeout(function() {
                                    if (!animationReady) {
                                        diag('dotlottie-wc not ready after watchdog timeout');
                                        installBodymovinFallback(animationData);
                                    }
                                }, 2500);
                            } catch (e) {
                                diag('bootstrap failed: ' + String(e));
                            }
                    
                            window.playAnimation = function() {
                                setLoopEnabled(true);
                                callFirst(player, ['play'], []);
                                renderOverlay();
                            };
                    
                            window.playAnimationOnce = function() {
                                setLoopEnabled(false);
                                setFrameValue(0);
                                callFirst(player, ['play'], []);
                                renderOverlay();
                            };
                    
                            window.pauseAnimation = function() {
                                callFirst(player, ['pause'], []);
                                renderOverlay();
                            };
                    
                            window.stopAnimation = function() {
                                callFirst(player, ['stop'], []);
                                currentFrame = 0;
                                renderOverlay();
                            };
                    
                            window.seekToFrame = function(frame) {
                                setFrameValue(frame);
                                callFirst(player, ['pause'], []);
                                renderOverlay();
                            };
                    
                            window.isAnimationReady = function() {
                                return animationReady;
                            };
                    
                            window.getCurrentFrame = function() {
                                if (typeof player.currentFrame === 'number') {
                                    return Math.round(player.currentFrame);
                                }
                                return currentFrame;
                            };
                    
                            window.getPlayerApiInspection = function() {
                                var proto = Object.getPrototypeOf(player) || {};
                                var methods = Object.getOwnPropertyNames(proto).filter(function(name) {
                                    return typeof player[name] === 'function';
                                }).sort();
                                return {
                                    tagName: player.tagName,
                                    methods: methods,
                                    hasCurrentFrame: typeof player.currentFrame === 'number',
                                    hasTotalFrames: typeof player.totalFrames === 'number',
                                    ready: animationReady
                                };
                            };
                    
                            window.getRenderDebug = function() {
                                return buildDebugText().split('\\n').join(', ');
                            };
                    
                            window.setBackgroundColor = function(color) {
                                document.body.style.backgroundColor = color;
                            };
                    
                            window.setDebugOverlayVisible = function(visible) {
                                showDebugInfo = !!visible;
                                if (debugOverlay) {
                                    debugOverlay.style.display = showDebugInfo ? 'block' : 'none';
                                }
                                renderOverlay('debug overlay ' + (showDebugInfo ? 'enabled' : 'disabled'));
                            };
                    
                            renderOverlay('bootstrapping...');
                        </script>
                    </body>
                    </html>
                    """.formatted(width, height, width, height, showDebugInfo, encodedJson);

            webEngine.loadContent(html);
        } catch (IOException e) {
            logger.error("Failed to load Lottie file in WebView: {}", e.getMessage());
        }
    }

    /**
     * Sets the background color of the WebView animation container.
     *
     * @param backgroundColor the color to set as the background
     */
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

    /**
     * Shows or hides the debug overlay in the WebView.
     *
     * @param visible true to show the debug overlay, false to hide it
     */
    public void setDebugInfoVisible(boolean visible) {
        try {
            webEngine.executeScript("window.setDebugOverlayVisible && window.setDebugOverlayVisible(" + visible + ")");
        } catch (Exception e) {
            logger.warn("Failed to toggle JS debug overlay: {}", e.getMessage());
        }
    }
}
