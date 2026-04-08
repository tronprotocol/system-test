#!/bin/bash
# run-test.sh - Run specific test class(es) with @BeforeSuite support
#
# Usage:
#   ./run-test.sh ClassName                    # single class
#   ./run-test.sh ClassName1 ClassName2        # multiple classes
#   ./run-test.sh --setup                      # only run @BeforeSuite (establish chain state)
#   ./run-test.sh --no-setup ClassName         # skip @BeforeSuite trigger classes
#
# Examples:
#   ./run-test.sh WalletTestAccount001
#   ./run-test.sh ContractTrcToken003 ContractTrcToken060
#   ./run-test.sh --setup                      # first run: apply proposals (~12 min)
#   ./run-test.sh --no-setup WalletTestAccount001  # fast run after setup

set -euo pipefail
cd "$(dirname "$0")"

DEBUG_XML="testcase/src/test/resources/debug-test.xml"
PKG_BASE="stest.tron.wallet.dailybuild"

# @BeforeSuite trigger classes (methods excluded so only setup runs)
SETUP_CLASSES=(
  "stest.tron.wallet.dailybuild.jsonrpc.EthSmartContract001"
  "stest.tron.wallet.dailybuild.zentoken.WalletTestZenToken001"
)

SETUP_ONLY=false
NO_SETUP=false
CLASSES=()

# Parse args
for arg in "$@"; do
  case "$arg" in
    --setup)    SETUP_ONLY=true ;;
    --no-setup) NO_SETUP=true ;;
    *)          CLASSES+=("$arg") ;;
  esac
done

if [ "$SETUP_ONLY" = false ] && [ ${#CLASSES[@]} -eq 0 ]; then
  echo "Usage: $0 [--setup | --no-setup] ClassName [ClassName2 ...]"
  echo ""
  echo "Options:"
  echo "  --setup       Only run @BeforeSuite (establish chain state, ~12 min)"
  echo "  --no-setup    Skip @BeforeSuite trigger (fast, use after --setup)"
  echo ""
  echo "Examples:"
  echo "  $0 --setup                           # first time: apply proposals"
  echo "  $0 WalletTestAccount001              # with @BeforeSuite (skips if already applied)"
  echo "  $0 --no-setup WalletTestAccount001   # without @BeforeSuite"
  exit 1
fi

# Resolve short class names to fully qualified names
resolve_class() {
  local name="$1"
  if [[ "$name" == *.* ]]; then
    echo "$name"  # already fully qualified
    return
  fi
  # Search for the class file
  local found
  found=$(find testcase/src/test/java -name "${name}.java" -path "*/dailybuild/*" | head -1)
  if [ -z "$found" ]; then
    echo "ERROR: Cannot find class '$name' under dailybuild/" >&2
    exit 1
  fi
  # Convert path to package name
  echo "$found" | sed 's|testcase/src/test/java/||; s|/|.|g; s|\.java$||'
}

# Build XML
echo '<?xml version="1.0" encoding="UTF-8"?>' > "$DEBUG_XML"
echo '<!DOCTYPE suite SYSTEM "http://testng.org/testng-1.0.dtd">' >> "$DEBUG_XML"
echo '<suite name="Debug Test">' >> "$DEBUG_XML"

# Add setup classes (trigger @BeforeSuite)
if [ "$NO_SETUP" = false ]; then
  echo '  <test name="Setup">' >> "$DEBUG_XML"
  echo '    <classes>' >> "$DEBUG_XML"
  for cls in "${SETUP_CLASSES[@]}"; do
    echo "      <class name=\"$cls\">" >> "$DEBUG_XML"
    echo '        <methods><exclude name=".*"/></methods>' >> "$DEBUG_XML"
    echo '      </class>' >> "$DEBUG_XML"
  done
  # If setup-only, add a dummy test marker
  if [ "$SETUP_ONLY" = true ]; then
    echo '    </classes>' >> "$DEBUG_XML"
    echo '  </test>' >> "$DEBUG_XML"
    echo '</suite>' >> "$DEBUG_XML"
    echo "=== Setup-only mode: will run @BeforeSuite to establish chain state ==="
    echo "=== This takes ~12 minutes (two maintenance periods) ==="
    ./gradlew :testcase:debugTest --info 2>&1 | tee /tmp/debugtest.log
    exit $?
  fi
  echo '    </classes>' >> "$DEBUG_XML"
  echo '  </test>' >> "$DEBUG_XML"
fi

# Add target test classes
echo '  <test name="Target">' >> "$DEBUG_XML"
echo '    <classes>' >> "$DEBUG_XML"
for name in "${CLASSES[@]}"; do
  fqn=$(resolve_class "$name")
  echo "      <class name=\"$fqn\"/>" >> "$DEBUG_XML"
done
echo '    </classes>' >> "$DEBUG_XML"
echo '  </test>' >> "$DEBUG_XML"
echo '</suite>' >> "$DEBUG_XML"

echo "=== Generated debug-test.xml ==="
cat "$DEBUG_XML"
echo ""
echo "=== Running tests ==="

# Run and capture output
./gradlew :testcase:debugTest --info 2>&1 | tee /tmp/debugtest.log

# Summary
echo ""
echo "=== Results ==="
echo "PASSED: $(grep -c ' PASSED$' /tmp/debugtest.log 2>/dev/null || echo 0)"
echo "FAILED: $(grep -c ' FAILED$' /tmp/debugtest.log 2>/dev/null || echo 0)"
echo "SKIPPED: $(grep -c ' SKIPPED$' /tmp/debugtest.log 2>/dev/null || echo 0)"
grep " FAILED$" /tmp/debugtest.log 2>/dev/null || true
