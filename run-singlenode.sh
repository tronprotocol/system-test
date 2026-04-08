#!/bin/bash
# Single-node tests only (259 classes, no solidity/pbft/fullnode2 dependency)
# - Counts unique FAILING CLASSES, not individual test methods
# - Stops after 10 different classes fail
LOG=/tmp/singlenode-build.log
rm -f "$LOG"
echo "=== Single-node build started at $(date) ===" | tee "$LOG"
echo "=== 259 test classes (excludes 196 multi-node tests) ===" | tee -a "$LOG"

cd /Users/happy/AI/java-tron/system-test

FAIL_CLASSES_FILE=$(mktemp)

./gradlew :testcase:singleNodeBuild --info 2>&1 | while IFS= read -r line; do
  echo "$line" >> "$LOG"

  # Detect FAILED test
  if echo "$line" | grep -q " FAILED$"; then
    # Extract class name: "suite > test > package.Class > method FAILED"
    # Use sed to extract the class part (compatible with macOS)
    cls=$(echo "$line" | sed -n 's/.* > \([a-zA-Z0-9_.]*\.[A-Z][a-zA-Z0-9_]*\) > .* FAILED$/\1/p')
    if [ -z "$cls" ]; then
      cls="$line"
    fi
    # Extract method name
    method=$(echo "$line" | sed -n 's/.* > \([a-zA-Z0-9_]*\) FAILED$/\1/p')

    # Check if this class already failed
    if ! grep -qF "$cls" "$FAIL_CLASSES_FILE" 2>/dev/null; then
      echo "$cls" >> "$FAIL_CLASSES_FILE"
      fail_count=$(wc -l < "$FAIL_CLASSES_FILE" | tr -d ' ')
      echo "[FAIL CLASS #${fail_count}] $cls > $method"

      if [ "$fail_count" -ge 30 ]; then
        echo ""
        echo "=== 10 different classes failed at $(date), stopping ==="
        echo "=== Failed classes: ==="
        cat "$FAIL_CLASSES_FILE"
        echo ""
        echo "=== All failed tests: ==="
        grep " FAILED$" "$LOG"
        echo ""
        pkill -f "singleNodeBuild" 2>/dev/null
        pkill -f "Gradle Test Executor" 2>/dev/null
        rm -f "$FAIL_CLASSES_FILE"
        break
      fi
    fi
  fi
done

echo ""
echo "=== Summary ==="
PASSED=$(grep -c ' PASSED$' "$LOG" 2>/dev/null || echo 0)
FAILED=$(grep -c ' FAILED$' "$LOG" 2>/dev/null || echo 0)
SKIPPED=$(grep -c ' SKIPPED$' "$LOG" 2>/dev/null || echo 0)
FAIL_CLS=$(wc -l < "$FAIL_CLASSES_FILE" 2>/dev/null | tr -d ' ' || echo 0)
echo "PASSED:  $PASSED"
echo "FAILED:  $FAILED (in $FAIL_CLS classes)"
echo "SKIPPED: $SKIPPED"
if [ -f "$FAIL_CLASSES_FILE" ] && [ -s "$FAIL_CLASSES_FILE" ]; then
  echo "--- Failed classes ---"
  cat "$FAIL_CLASSES_FILE"
fi
rm -f "$FAIL_CLASSES_FILE"
echo "=== Finished at $(date) ==="
