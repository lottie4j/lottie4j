import com.sun.javafx.application.ParametersImpl;
import com.sun.javafx.tk.Toolkit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.management.ObjectName;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

/**
 * Drives the fxfileviewer applications in-process to detect memory leaks and FX-thread problems.
 * Run on the CLASSPATH (not the module path) so private members and com.sun.* internals are reachable.
 * See tools/profiling/README.md for the full procedure.
 * <p>
 * Usage: java -cp ... ViewerStressHarness.java <simple|debug|marker|cellcheck> <jsonDir> [cycles] [dwellMs]
 */
public class ViewerStressHarness {

    private static final Pattern TRACKED = Pattern.compile(
            "com\\.lottie4j\\.fxplayer\\.LottiePlayer(\\$\\d+)?$"
                    + "|com\\.lottie4j\\.fxfileviewer\\.component\\.(LayerTileView|LayerTreeView|LottieWebView|LottieTreeView)$"
                    + "|javafx\\.scene\\.web\\.WebEngine$|javafx\\.stage\\.Stage$"
                    + "|javafx\\.scene\\.canvas\\.Canvas$|javafx\\.scene\\.image\\.WritableImage$"
                    + "|com\\.lottie4j\\.core\\.model\\.animation\\.Animation$");

    private static final AtomicLong maxStallMs = new AtomicLong();
    private static final AtomicLong stallsOver100 = new AtomicLong();
    private static final AtomicLong stallsOver500 = new AtomicLong();

    public static void main(String[] args) throws Exception {
        String mode = args[0];
        File jsonDir = new File(args[1]);
        int cycles = args.length > 2 ? Integer.parseInt(args[2]) : 2;
        long dwellMs = args.length > 3 ? Long.parseLong(args[3]) : 1500;

        File[] files = Optional.ofNullable(jsonDir.listFiles((d, n) -> n.endsWith(".json"))).orElse(new File[0]);
        Arrays.sort(files);

        Platform.startup(() -> { });
        Platform.setImplicitExit(false);
        startStallWatchdog();

        switch (mode) {
            case "simple" -> runSimple(files, cycles, dwellMs);
            case "debug" -> runDebug(files, cycles, dwellMs);
            case "marker" -> runMarker(dwellMs * cycles);
            case "cellcheck" -> runCellCheck(files);
            default -> throw new IllegalArgumentException("Unknown mode " + mode);
        }

        report("FINAL");
        printThreads();
        Platform.exit();
        System.exit(0);
    }

    // ---------- scenarios ----------

    private static void runSimple(File[] files, int cycles, long dwellMs) throws Exception {
        Application app = launch("com.lottie4j.fxfileviewer.LottieFileSimpleViewer");
        Method load = privateMethod(app, "loadAnimation", File.class);
        for (int c = 1; c <= cycles; c++) {
            for (File f : files) {
                timed("load " + f.getName(), () -> invoke(load, app, f));
                Thread.sleep(dwellMs);
            }
            report("simple cycle " + c);
        }
    }

    private static void runDebug(File[] files, int cycles, long dwellMs) throws Exception {
        Application app = launch("com.lottie4j.fxfileviewer.LottieFileDebugViewer");
        Method load = privateMethod(app, "loadAnimation", File.class);
        Method play = privateMethod(app, "startAnimationLoop");
        Method pause = privateMethod(app, "pauseAnimation");
        Method shotAll = privateMethod(app, "takeAllScreenshots");
        for (int c = 1; c <= cycles; c++) {
            for (File f : files) {
                timed("load " + f.getName(), () -> invoke(load, app, f));
                fx(() -> invoke(play, app));
                Thread.sleep(dwellMs / 2);
                // Enable live tiles: renders one temporary LottiePlayer per layer per frame
                timed("liveTiles " + f.getName(), () -> {
                    Object tiles = field(app, "layerTileView");
                    invoke(privateMethod(tiles, "toggleLiveUpdates"), tiles);
                });
                Thread.sleep(dwellMs / 2);
                fx(() -> invoke(pause, app));
            }
            report("debug cycle " + c);
        }
        // Screenshot-all on a small file, then close the window mid-run to check thread lifecycle
        File small = Arrays.stream(files).filter(f -> f.getName().startsWith("box-moving")).findFirst().orElse(files[0]);
        fx(() -> invoke(load, app, small));
        fx(() -> invoke(shotAll, app));
        Thread.sleep(1500);
        fx(() -> {
            try {
                app.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            new ArrayList<>(Window.getWindows()).forEach(Window::hide);
        });
        Thread.sleep(1000);
        System.out.println("[harness] threads after window close while 'Screenshot All' runs:");
    }

    private static void runMarker(long durationMs) throws Exception {
        launch("com.lottie4j.fxfileviewer.LottieFileMarkerViewer");
        Thread.sleep(durationMs);
    }

    /**
     * Reproduces TreeCell reuse in LayerTreeView: the same cell shows layer A, then layer B,
     * then the user unticks the checkbox. Only B should become hidden.
     */
    private static void runCellCheck(File[] files) throws Exception {
        File f = Arrays.stream(files).filter(x -> x.getName().equals("lottie4j.json")).findFirst().orElse(files[0]);
        fx(() -> {
            try {
                Object animation = Class.forName("com.lottie4j.core.file.LottieFileLoader")
                        .getMethod("load", File.class).invoke(null, f);
                Class<?> playerCls = Class.forName("com.lottie4j.fxplayer.LottiePlayer");
                Object player = playerCls.getConstructor(animation.getClass(), int.class, int.class)
                        .newInstance(animation, 100, 100);
                Class<?> treeCls = Class.forName("com.lottie4j.fxfileviewer.component.LayerTreeView");
                Object tree = treeCls.getConstructor(animation.getClass(), playerCls).newInstance(animation, player);
                var root = (javafx.scene.control.TreeItem<?>) treeCls.getMethod("getRoot").invoke(tree);
                var a = root.getChildren().get(0).getValue();
                var b = root.getChildren().get(1).getValue();
                Class<?> cellCls = Class.forName("com.lottie4j.fxfileviewer.component.LayerTreeView$LayerTreeCell");
                var ctor = cellCls.getDeclaredConstructor(treeCls);
                ctor.setAccessible(true);
                Object cell = ctor.newInstance(tree);
                Method update = cellCls.getDeclaredMethod("updateItem", a.getClass(), boolean.class);
                update.setAccessible(true);
                update.invoke(cell, a, false);
                update.invoke(cell, b, false);
                var checkBox = (javafx.scene.control.CheckBox) field(cell, "checkBox");
                checkBox.setSelected(false);
                Method vis = a.getClass().getMethod("getVisibleProperty");
                boolean aVisible = ((javafx.beans.property.BooleanProperty) vis.invoke(a)).get();
                boolean bVisible = ((javafx.beans.property.BooleanProperty) vis.invoke(b)).get();
                System.out.printf("[cellcheck] after reuse A->B and unticking: A visible=%s, B visible=%s -> %s%n",
                        aVisible, bVisible, aVisible ? "OK" : "BUG: stale bidirectional binding also hid layer A");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    // ---------- instrumentation ----------

    /** Posts a runLater every 50 ms and measures how long it takes to execute. */
    private static void startStallWatchdog() {
        Thread t = new Thread(() -> {
            while (true) {
                long posted = System.nanoTime();
                CountDownLatch latch = new CountDownLatch(1);
                Platform.runLater(latch::countDown);
                try {
                    latch.await();
                    long ms = (System.nanoTime() - posted) / 1_000_000;
                    maxStallMs.accumulateAndGet(ms, Math::max);
                    if (ms > 100) stallsOver100.incrementAndGet();
                    if (ms > 500) stallsOver500.incrementAndGet();
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "harness-fx-stall-watchdog");
        t.setDaemon(true);
        t.start();
    }

    private static void report(String label) throws Exception {
        for (int i = 0; i < 3; i++) {
            System.gc();
            Thread.sleep(200);
        }
        // Heap retained right after the last GC (not current usage: a playing animation refills the heap within ms)
        long usedMb = ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(p -> p.getType() == java.lang.management.MemoryType.HEAP && p.getCollectionUsage() != null)
                .mapToLong(p -> p.getCollectionUsage().getUsed())
                .sum() / (1024 * 1024);
        int[] timers = new int[1];
        fx(() -> timers[0] = activeAnimationTimers());
        System.out.printf("%n===== %s: heapUsedAfterGC=%dMB activeAnimationTimers=%d windows=%d fxMaxStall=%dms stalls>100ms=%d stalls>500ms=%d%n",
                label, usedMb, timers[0], Window.getWindows().size(), maxStallMs.get(), stallsOver100.get(), stallsOver500.get());
        String histo = (String) ManagementFactory.getPlatformMBeanServer().invoke(
                new ObjectName("com.sun.management:type=DiagnosticCommand"),
                "gcClassHistogram", new Object[]{new String[]{"-all=false"}}, new String[]{String[].class.getName()});
        // Top 6 classes by retained bytes, plus the tracked lottie4j/JavaFX classes
        histo.lines().skip(3).limit(6).forEach(l -> System.out.println("  top   " + l.trim()));
        histo.lines().filter(l -> {
            String[] p = l.trim().split("\\s+");
            return p.length >= 4 && TRACKED.matcher(p[3]).find();
        }).forEach(l -> System.out.println("  histo " + l.trim()));
    }

    /** Number of AnimationTimers registered with the JavaFX primary timer (running timers hold their owner). */
    private static int activeAnimationTimers() {
        try {
            Object timer = Toolkit.getToolkit().getPrimaryTimer();
            Class<?> c = timer.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("animationTimersLength");
                    f.setAccessible(true);
                    return f.getInt(timer);
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
        } catch (Exception e) {
            System.out.println("[harness] cannot read primary timer: " + e);
        }
        return -1;
    }

    private static void printThreads() {
        System.out.println("\n===== live threads");
        Thread.getAllStackTraces().keySet().stream()
                .sorted(Comparator.comparing(Thread::getName))
                .forEach(t -> System.out.printf("  %-45s daemon=%-5s state=%s%n", t.getName(), t.isDaemon(), t.getState()));
    }

    private static void timed(String label, Runnable r) throws Exception {
        long[] ms = new long[1];
        fx(() -> {
            long s = System.nanoTime();
            r.run();
            ms[0] = (System.nanoTime() - s) / 1_000_000;
        });
        if (ms[0] > 100) {
            System.out.printf("[harness] FX thread busy %5d ms in %s%n", ms[0], label);
        }
    }

    // ---------- reflection / FX helpers ----------

    private static Application launch(String className) throws Exception {
        Application[] holder = new Application[1];
        fx(() -> {
            try {
                Application app = (Application) Class.forName(className).getConstructor().newInstance();
                ParametersImpl.registerParameters(app, new ParametersImpl(new String[0]));
                app.init();
                app.start(new Stage());
                holder[0] = app;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return holder[0];
    }

    private static void fx(Runnable r) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        Throwable[] err = new Throwable[1];
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err[0] = t;
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (err[0] != null) {
            System.out.println("[harness] error on FX thread: " + err[0]);
            err[0].printStackTrace(System.out);
        }
    }

    private static Method privateMethod(Object target, String name, Class<?>... types) {
        try {
            Method m = target.getClass().getDeclaredMethod(name, types);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object field(Object target, String name) {
        try {
            Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void invoke(Method m, Object target, Object... args) {
        try {
            m.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e.getCause() != null ? e.getCause() : e);
        }
    }
}
