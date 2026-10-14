#!/usr/bin/env bash
# Runs ViewerStressHarness with JFR + unified GC logging. See README.md in this folder.
# Usage: tools/profiling/run.sh <simple|debug|marker|cellcheck> [cycles] [dwellMs] [outDir]
set -euo pipefail

MODE=${1:?mode: simple|debug|marker|cellcheck}
CYCLES=${2:-2}
DWELL=${3:-1500}
REPO=$(cd "$(dirname "$0")/../.." && pwd)
OUT=${4:-$REPO/target/profiling/$MODE-$(date +%Y%m%d-%H%M%S)}
mkdir -p "$OUT"

# Build once (skip with SKIP_BUILD=1)
if [[ "${SKIP_BUILD:-0}" != "1" ]]; then
  (cd "$REPO" && mvn -q install -DskipTests -Dmaven.javadoc.skip=true)
fi
(cd "$REPO" && mvn -q -pl fxfileviewer dependency:build-classpath -Dmdep.outputFile="$OUT/cp.txt")
CP="$REPO/fxfileviewer/target/classes:$REPO/fxfileviewer/src/test/resources:$(cat "$OUT/cp.txt")"

cd "$OUT"   # 'Screenshot All' writes to ./screenshot
# The add-exports/opens are needed when the JDK bundles JavaFX as modules (e.g. Zulu FX, Liberica Full)
java -Xmx768m \
  --add-exports javafx.graphics/com.sun.javafx.tk=ALL-UNNAMED \
  --add-exports javafx.graphics/com.sun.javafx.application=ALL-UNNAMED \
  --add-opens javafx.graphics/com.sun.scenario.animation=ALL-UNNAMED \
  --add-opens javafx.graphics/com.sun.javafx.tk.quantum=ALL-UNNAMED \
  --add-opens javafx.controls/javafx.scene.control=ALL-UNNAMED \
  -XX:StartFlightRecording=filename="$OUT/recording.jfr",settings=profile,dumponexit=true${JFR_EXTRA:-} \
  -Xlog:gc*,safepoint:file="$OUT/gc.log":time,uptime,level,tags \
  -cp "$CP" \
  "$REPO/tools/profiling/ViewerStressHarness.java" "$MODE" "$REPO/fxfileviewer/src/test/resources/json" "$CYCLES" "$DWELL" \
  2>&1 | grep -v "^[0-9:.]* \(INFO\|DEBUG\)" | tee "$OUT/harness.log"

echo "Output in $OUT"
