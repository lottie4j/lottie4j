package com.lottie4j.fxfileviewer.component;

import com.lottie4j.core.file.LottieFileLoader;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.scene.control.CheckBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tree cells are reused for other items while scrolling or expanding. A cell must only control
 * the visibility of the layer it currently shows.
 */
class LayerTreeViewTest {

    @BeforeAll
    static void initJavaFX() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyStarted) {
            // Platform already started by another test class — fine.
            return;
        }
        assertTrue(latch.await(5, TimeUnit.SECONDS), "JavaFX Platform failed to start");
    }

    @Test
    void reusedCellOnlyControlsItsCurrentLayer() throws Exception {
        runOnFxThread(() -> {
            LayerTreeView tree = createTree();
            var layerA = tree.getRoot().getChildren().get(0).getValue();
            var layerB = tree.getRoot().getChildren().get(1).getValue();

            LayerTreeView.LayerTreeCell cell = tree.new LayerTreeCell();
            cell.updateItem(layerA, false);
            cell.updateItem(layerB, false);
            checkBoxOf(cell).setSelected(false);

            assertTrue(layerA.getVisibleProperty().get(), "Layer A must stay visible after the cell moved to B");
            assertFalse(layerB.getVisibleProperty().get(), "Layer B must be hidden");
        });
    }

    @Test
    void reusedCellShowsStateOfNewLayer() throws Exception {
        runOnFxThread(() -> {
            LayerTreeView tree = createTree();
            var layerA = tree.getRoot().getChildren().get(0).getValue();
            var layerB = tree.getRoot().getChildren().get(1).getValue();
            layerB.getVisibleProperty().set(false);

            LayerTreeView.LayerTreeCell cell = tree.new LayerTreeCell();
            cell.updateItem(layerA, false);
            assertTrue(checkBoxOf(cell).isSelected());

            cell.updateItem(layerB, false);
            assertFalse(checkBoxOf(cell).isSelected(), "Checkbox must reflect layer B");
            assertTrue(layerA.getVisibleProperty().get(), "Showing layer B must not change layer A");
        });
    }

    @Test
    void emptiedCellNoLongerControlsLayer() throws Exception {
        runOnFxThread(() -> {
            LayerTreeView tree = createTree();
            var layerA = tree.getRoot().getChildren().get(0).getValue();

            LayerTreeView.LayerTreeCell cell = tree.new LayerTreeCell();
            cell.updateItem(layerA, false);
            cell.updateItem(null, true);
            checkBoxOf(cell).setSelected(false);

            assertTrue(layerA.getVisibleProperty().get(), "An empty cell must not change layer A");
        });
    }

    private static LayerTreeView createTree() {
        try {
            var resource = LayerTreeViewTest.class.getResource("/json/lottie4j.json");
            assertNotNull(resource, "Test animation not found");
            Animation animation = LottieFileLoader.load(new File(resource.getFile()));
            return new LayerTreeView(animation, new LottiePlayer(animation, 100, 100));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create LayerTreeView", e);
        }
    }

    private static CheckBox checkBoxOf(LayerTreeView.LayerTreeCell cell) {
        try {
            var field = LayerTreeView.LayerTreeCell.class.getDeclaredField("checkBox");
            field.setAccessible(true);
            return (CheckBox) field.get(cell);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void runOnFxThread(Runnable test) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                test.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Test timed out on the JavaFX thread");
        if (failure.get() instanceof AssertionError assertionError) {
            throw assertionError;
        } else if (failure.get() != null) {
            throw new Exception(failure.get());
        }
    }
}
