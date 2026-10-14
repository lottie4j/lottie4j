# Task: memory-leak and threading check of the fxfileviewer applications

Repeatable procedure to run the `fxfileviewer` viewers under **JFR** and **unified GC logging**,
detect leaked players/timers, FX-thread stalls, and lingering threads, and review the architecture
for the causes. Last run: 2026-10-13 (baseline numbers at the bottom).

## Files

| File | Purpose |
|------|---------|
| `run.sh` | Builds the project, resolves the classpath, runs the harness with JFR + GC log |
| `ViewerStressHarness.java` | Single-file program that drives the viewers in-process and reports |

Output goes to `target/profiling/<mode>-<timestamp>/`:
`harness.log`, `recording.jfr`, `gc.log`, `screenshot/` (debug mode only).

## How the harness works

* Runs the viewers on the **classpath** (not the module path) so it can call private methods
  (`loadAnimation`, `startAnimationLoop`, ...) via reflection without changing production code.
  When the JDK bundles JavaFX (Zulu FX, Liberica Full), `run.sh` adds the needed `--add-exports/--add-opens`.
* Starts each `Application` with `Platform.startup` + `ParametersImpl.registerParameters` + `start(new Stage())`.
* **FX stall watchdog**: posts a `Platform.runLater` every 50 ms and records the latency
  (`fxMaxStall`, `stalls>100ms`, `stalls>500ms`).
* **"FX thread busy"** lines: wall time of each action executed on the FX thread (only printed when > 100 ms).
* **Leak report** after every cycle: 3x `System.gc()`, then the `gcClassHistogram` diagnostic command filtered
  on `LottiePlayer`, `LottiePlayer$N` (AnimationTimer subclasses), viewer components, `WebEngine`, `Stage`,
  `Canvas`, `WritableImage`, `Animation`; plus `activeAnimationTimers` read from the JavaFX primary timer.
  Counts that grow with every cycle are leaks. `heapUsedAfterGC` is the heap retained right after the last
  GC (memory-pool collection usage), so a playing animation doesn't inflate it. The six largest histogram
  entries are printed as `top` lines.
* **Thread report** at the end: every live thread with daemon flag and state.

## Procedure

1. Build once and run the four scenarios (about 7 minutes in total; needs network for the WebView's lottie-web CDN):

   ```bash
   tools/profiling/run.sh cellcheck                 # LayerTreeView cell-reuse binding check (seconds)
   SKIP_BUILD=1 tools/profiling/run.sh simple 2 1500   # SimpleViewer: load every test JSON, 2 cycles
   SKIP_BUILD=1 tools/profiling/run.sh debug 2 2000    # DebugViewer: load, play, live tiles, Screenshot All, close
   SKIP_BUILD=1 tools/profiling/run.sh marker 1 20000  # MarkerViewer: 20 s steady-state playback
   ```

   Arguments: `<mode> [cycles] [dwellMs] [outDir]`. Do not run two scenarios at the same time (they share the GPU).
   To find out **who retains** objects, add GC-root paths to the old-object samples (slower):

   ```bash
   JFR_EXTRA=",path-to-gc-roots=true" SKIP_BUILD=1 tools/profiling/run.sh marker 1 10000
   jfr print --events jdk.OldObjectSample $D/recording.jfr   # each sample shows the reference chain
   ```

   To compare with the code before your change, stash it, rebuild, run, and restore. Run `mvn install` again
   afterwards, or the local repository keeps the old jar:
   `git stash && mvn -q -pl core,fxplayer install -DskipTests && SKIP_BUILD=1 tools/profiling/run.sh marker 1 10000; git stash pop`

2. Read the harness summary:

   ```bash
   D=$(ls -td target/profiling/simple-* | head -1)
   grep -n "=====\|histo\|FX thread busy\|daemon=\|cellcheck" $D/harness.log
   grep -c Exception $D/harness.log
   ```

   * `LottiePlayer` / `LottiePlayer$1` / `activeAnimationTimers` growing per cycle -> leak.
   * `heapUsedAfterGC` growing per cycle -> leak.
   * Prism `NullPointerException`s in `ES2SwapChain` / `RTTexture` -> the GPU texture pool is exhausted (usually a leak symptom).
   * A non-daemon `Thread-N` after the windows are closed -> a thread that keeps the JVM alive.

3. Analyse JFR (JDK 21+ `jfr view`):

   ```bash
   jfr view --width 200 gc-pauses           $D/recording.jfr
   jfr view --width 200 allocation-by-class $D/recording.jfr
   jfr view --width 200 allocation-by-site  $D/recording.jfr
   jfr view --width 200 hot-methods         $D/recording.jfr
   jfr view --width 200 contention-by-site  $D/recording.jfr
   jfr summary $D/recording.jfr | grep -E "ThreadPark|JavaMonitor|ThreadSleep"
   ```

   Find which lottie4j code calls a JDK hotspot (replace the pattern):

   ```bash
   jfr print --events jdk.ExecutionSample --stack-depth 14 $D/recording.jfr > exec.txt
   awk -v RS='jdk.ExecutionSample' -v k="PNGImageLoader2" '$0 ~ k {n=split($0,a,"\n"); s="";
     for(i=1;i<=n;i++) if (a[i] ~ /lottie4j/) {gsub(/^[ \t]+/,"",a[i]); sub(/line:.*/,"",a[i]); s=s" <- "a[i]}
     print s}' exec.txt | sort | uniq -c | sort -rn | head
   ```

   **On macOS the FX application thread is named `Thread-1` in JFR** (JavaFX runs on the AppKit main thread),
   not "JavaFX Application Thread". To find what blocked it (blocked time produces no CPU samples):

   ```bash
   jfr print --events jdk.ThreadPark,jdk.JavaMonitorWait,jdk.JavaMonitorEnter,jdk.SocketRead,jdk.ThreadSleep \
     --stack-depth 20 $D/recording.jfr > block.txt
   awk 'BEGIN{RS="\n}\n"} /eventThread = "Thread-1"/ {if (match($0,/duration = [0-9.,]+ (s|min)/)) print}' block.txt \
     | grep -E "^jdk|duration =|lottie4j" | head -40
   ```

4. Check the GC log for pause counts, Full GCs, and humongous allocations:

   ```bash
   grep -c "Pause Young" $D/gc.log; grep -E "Pause Full|To-space" $D/gc.log | head
   ```

5. Review the architecture against the checklist below, then compare the numbers with the baseline.

## Code review checklist

* Every `AnimationTimer`/`Timeline` is stopped when its owner is replaced or removed from the scene.
* Listeners added to longer-lived properties (`currentFrameProperty`, visibility maps) are removed or bound weakly.
* `bindBidirectional` in `TreeCell/ListCell.updateItem` is paired with `unbindBidirectional` of the previous item.
* No per-frame creation of `LottiePlayer`, `Canvas`, `WritableImage`, `Image` (decode) or `snapshot()` without reuse.
* Hot render paths avoid `logger.debug` with more than two args or primitive args (varargs + boxing) unless guarded.
* Background threads are daemon/cancellable, and are interrupted in `Application.stop()`.
* No blocking `latch.await()` that can run on the FX thread (deadlock).
* Long work (file parsing, building large trees) is not done on the FX thread.

## Baseline results (2026-10-13, macOS, Zulu 21.0.6 FX, `-Xmx768m`)

The first runs used a heap metric that measured after a 200 ms sleep, which a playing animation inflates;
their heap numbers are left out. Use the instance counts and timers to judge leaks.

| Scenario | Result |
|----------|--------|
| cellcheck | **BUG**: after cell reuse A->B, unticking also hides layer A |
| simple (28 files x 2) | **LEAK**: 28 -> 56 live `LottiePlayer` + running timers; FX max stall 3.3 s; 7,346 GC pauses (30.3 s total); 822 Prism texture NPEs (GPU texture pool exhausted) |
| debug (x 2) | No leak (1 player); FX busy up to 2.9 s on load and 2.8 s on enabling live tiles; non-daemon `Thread-3` still running after the windows close |
| marker (20 s) | Stable, but 531 GC pauses (1.08 s) in 22 s; hot spots: PNG decode every frame, `MatteRenderer.applyMatte` pixel readback |
| marker after image cache + matte buffer reuse | 177 GC pauses (0.37 s) in 22 s; PNG decode gone; `applyMatte` 30% -> 0.1% of allocations; retained heap 77 MB (84 MB before; includes 8 MB decoded images). Remaining churn: `IntBuffer`/`ByteBuffer` allocated inside JavaFX `snapshot()` |
| all | No monitor contention (`JavaMonitorEnter` = 0) |

### After fixing the leak, the tree-cell binding and the Screenshot All thread (2026-10-14)

| Scenario | Result |
|----------|--------|
| cellcheck | OK: only layer B hidden |
| simple (28 files x 2) | 1 live `LottiePlayer` + 1 timer after both cycles; retained heap flat at 50 MB |
| debug (x 1) | Screenshot All thread gone after the window closes. New: **41 s FX-thread block in `stop()`**: `LottieWebView.shutdown()` -> `ChromeDriver.quit()` waits on Selenium timeouts when chromedriver isn't available |
| debug, after moving the ChromeDriver quit off the FX thread | No stall in `stop()` (max stall 5.0 s, already reached during loads). The quit continues on a non-daemon thread, which Selenium renames to "Forwarding quit on session ..." while it runs |

