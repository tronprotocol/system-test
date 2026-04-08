# TRON System-Test Setup Guide

**English** | [中文](SETUP_CN.md)

Integration test suite for [java-tron](https://github.com/tronprotocol/java-tron). Tests gRPC, HTTP, JSON-RPC APIs, smart contracts, staking, multi-signature, and more against a running java-tron node.

---

## Prerequisites

| Requirement | Details |
|-------------|---------|
| **JDK 8** (x86/Intel) | Recommended. The project targets `sourceCompatibility = 1.8`. |
| **JDK 17** (ARM/Apple Silicon) | Use Rosetta or an ARM-compatible JDK 8 alternative. |
| **Gradle** | Included via `./gradlew` wrapper -- no separate install needed. |
| **OS** | Linux or macOS. Windows is not officially supported. |
| **Disk** | ~2 GB for java-tron data directory during tests. |
| **RAM** | At least 4 GB free (java-tron itself needs ~2 GB heap). |

Install JDK 8 on macOS:

```bash
brew install --cask temurin@8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
```

On Linux (Ubuntu/Debian):

```bash
sudo apt install openjdk-8-jdk
```

---

## 5-Minute Quick Start

### Step 1: Download java-tron

Download the latest release (GreatVoyage-v4.8.1 or newer):

```bash
wget https://github.com/tronprotocol/java-tron/releases/download/GreatVoyage-v4.8.1/java-tron-GreatVoyage-v4.8.1.zip
unzip java-tron-GreatVoyage-v4.8.1.zip
cd java-tron-GreatVoyage-v4.8.1
```

### Step 2: Configure a Private Network

Copy the provided test configuration as your starting point:

```bash
cp <system-test-repo>/docs/config.conf ./config.conf
```

The test config (`docs/config.conf`) is pre-configured for local testing. Key settings to verify:

```hocon
# Ports (must match testng.conf)
node.rpc.port = 50051
node.rpc.solidityPort = 50061
node.rpc.PBFTPort = 50071
node.http.fullNodePort = 8090
node.http.solidityPort = 8091
node.http.PBFTPort = 8098
node.jsonrpc.httpFullNodePort = 50545
node.jsonrpc.httpSolidityPort = 50555

# Fast maintenance cycle for testing (20 seconds instead of 6 hours)
block.maintenanceTimeInterval = 20000
block.proposalExpireTime = 20000
block.checkFrozenTime = 0

# No sync needed for single-node private network
block.needSyncCheck = false

# Enable features needed by tests
vm.supportConstant = true
vm.saveInternalTx = true
node.walletExtensionApi = true
node.fullNodeAllowShieldedTransaction = true
node.jsonrpc.httpFullNodeEnable = true
node.jsonrpc.httpSolidityEnable = true
```

**Important:** If your tests use grpcurl (gRPC reflection), add this to the node config:

```hocon
node.rpc {
  reflectionService = true
}
```

The genesis block in `docs/config.conf` already contains pre-funded test accounts and witness keys. Do not modify the genesis section unless you also update `testng.conf`.

### Step 3: Start the Node

```bash
bin/FullNode -c config.conf --es --witness
```

Flags:
- `--witness` -- Enable block production (SR mode)
- `--es` -- Enable event subscription

### Step 4: Verify Block Production

Wait 10-15 seconds, then check:

```bash
curl http://127.0.0.1:8090/wallet/getnowblock
```

You should see a JSON response with `block_header` containing an increasing `number` field. If the block number stays at 0, check that the witness private keys in `config.conf` match the genesis witness addresses.

### Step 5: Run Tests

From the `system-test/` directory:

```bash
./gradlew smokeTest
```

If smoke tests pass, your environment is working correctly.

---

## Tool Installation

Two external tools are needed for the full test suite. Place them in the expected directories.

### grpcurl

Used by gRPC reflection tests. Download from [fullstorydev/grpcurl](https://github.com/fullstorydev/grpcurl/releases).

```bash
# macOS (Intel)
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_osx_x86_64.tar.gz
tar xzf grpcurl_1.8.9_osx_x86_64.tar.gz

# macOS (Apple Silicon)
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_osx_arm64.tar.gz
tar xzf grpcurl_1.8.9_osx_arm64.tar.gz

# Linux (x86_64)
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_linux_x86_64.tar.gz
tar xzf grpcurl_1.8.9_linux_x86_64.tar.gz

# Place in expected location
cp grpcurl <system-test-repo>/gRPCurl/grpcurl
chmod +x <system-test-repo>/gRPCurl/grpcurl
```

### solc (Solidity Compiler)

Tests compile Solidity contracts on the fly. Use the **tronprotocol fork** of solc which supports TVM-specific opcodes.

```bash
# Download tronprotocol fork solc v0.8.26
# From: https://github.com/tronprotocol/solidity/releases/tag/tv_0.8.26

# Place in expected location
cp solc <system-test-repo>/solcDIR/solc
chmod +x <system-test-repo>/solcDIR/solc

# Verify
./solcDIR/solc --version
```

Both `gRPCurl/` and `solcDIR/` directories already exist in the repository; you just need to place the binaries inside them.

---

## Running Tests

All test tasks are defined in `testcase/build.gradle` and run via the Gradle wrapper from the `system-test/` root.

| Command | Description | Tests | Time |
|---------|-------------|-------|------|
| `./gradlew smokeTest` | Core functionality verification | ~30 | ~5 min |
| `./gradlew stest` | Standard TestNG suite | ~200 | ~20 min |
| `./gradlew dailyBuild` | Full daily regression | ~1685 | ~1.5 h |
| `./gradlew singleNodeBuild` | Tests requiring only one FullNode | ~260 | ~40 min |
| `./gradlew fuzzTest` | Property-based tests (jqwik) | varies | ~10 min |
| `./gradlew lint` | Checkstyle code analysis | -- | ~1 min |

### Running a Single Test Class

```bash
./gradlew debugTest
# Edit testcase/src/test/resources/debug-test.xml to specify the class
```

### Test Parallelism

- `stest` runs with 4 threads
- `dailyBuild` runs with 2 threads (higher memory usage)
- `smokeTest` runs with 4 threads, parallel by class

---

## Configuration

### testng.conf

Located at `testcase/src/test/resources/testng.conf`. This file tells the test suite where to find the running java-tron node(s).

```hocon
# gRPC endpoints
fullnode.ip.list = ["127.0.0.1:50051"]
solidityNode.ip.list = ["127.0.0.1:50061", "127.0.0.1:50071"]

# HTTP endpoints
httpnode.ip.list = [
  "127.0.0.1:8090",   # FullNode HTTP
  "127.0.0.1:8090",   # FullNode HTTP (duplicate for test indexing)
  "127.0.0.1:8091",   # Solidity HTTP
  "127.0.0.1:8091",   # Solidity HTTP
  "127.0.0.1:8098",   # PBFT HTTP
  "127.0.0.1:8091",   # Witness2 solidity (same for single-node)
  "127.0.0.1:8098"    # Witness2 PBFT (same for single-node)
]

# JSON-RPC endpoints
jsonRpcNode.ip.list = ["127.0.0.1:50545", "127.0.0.1:50555"]

# MongoDB (optional, only needed for event query tests)
mongonode.ip.list = ["172.17.0.1:27017"]

# Test account private keys (must match genesis block accounts)
foundationAccount.key1 = FC8BF0238748587B9617EB6D15D47A66C0E07C1A1959033CF249C6532DC29FE6
foundationAccount.key2 = 6815B367FDDE637E53E9ADC8E69424E07724333C9A2B973CFA469975E20753FC
```

For single-node testing, all endpoints point to the same node. The duplicate entries in `ip.list` are intentional -- test code references them by array index.

### TestNG XML Suites

Test suites are defined in `testcase/src/test/resources/`:

| File | Purpose |
|------|---------|
| `testng.xml` | Standard suite (stest task) |
| `daily-build.xml` | Full regression (dailyBuild task) |
| `smoke-test.xml` | Quick verification |
| `single-node-build.xml` | Tests that need only one FullNode |
| `debug-test.xml` | Ad-hoc single class runs |

---

## Network Options

### Private Network (Recommended for Development)

Use the config from `docs/config.conf`. Single node, fast block times, pre-funded accounts. Best for local development and CI.

### Nile Testnet

Public test network. Update `testng.conf` endpoints:

```hocon
fullnode.ip.list = ["grpc.nile.trongrid.io:50051"]
httpnode.ip.list = ["https://nile.trongrid.io"]
```

Note: Many tests assume local control (witness keys, fast maintenance cycles) and will fail on public testnets. Use testnets only for manual verification.

### Shasta Testnet

Similar to Nile. Endpoints:

```hocon
fullnode.ip.list = ["grpc.shasta.trongrid.io:50051"]
httpnode.ip.list = ["https://api.shasta.trongrid.io"]
```

### Multi-Node Network

About 43% of dailyBuild tests (195/455 test classes) require a multi-node setup with separate Solidity and PBFT nodes. For full dailyBuild coverage, run a 2-SR witness network. See `docs/config.conf` for the seed node and active peer configuration.

---

## Troubleshooting

### grpcurl tests fail with "reflection not supported"

Add `node.rpc.reflectionService = true` to your java-tron `config.conf` and restart the node.

### Port already in use

Check for existing java-tron processes or other services:

```bash
lsof -i :50051
lsof -i :8090
# Kill stale processes
kill -9 <PID>
```

Default ports used by the test environment:

| Port | Service |
|------|---------|
| 50051 | gRPC FullNode |
| 50061 | gRPC Solidity |
| 50071 | gRPC PBFT |
| 8090 | HTTP FullNode |
| 8091 | HTTP Solidity |
| 8098 | HTTP PBFT |
| 50545 | JSON-RPC FullNode |
| 50555 | JSON-RPC Solidity |
| 18889 | P2P listen |

### MongoDB connection errors

MongoDB is only needed for event query tests. If you are not running those tests, these errors can be safely ignored. To install MongoDB for full test coverage:

```bash
# macOS
brew tap mongodb/brew && brew install mongodb-community
brew services start mongodb-community

# Linux
sudo apt install mongodb
sudo systemctl start mongodb
```

### Block production stops or block number stays at 0

- Verify witness private keys in `config.conf` under `localwitness` match the genesis witness addresses.
- Ensure `block.needSyncCheck = false` for single-node setups.
- Check java-tron logs in `logs/tron.log` for errors.

### Out of memory during dailyBuild

The `dailyBuild` task sets `maxHeapSize = 2048m`. If your machine is constrained, reduce thread count by editing `testcase/build.gradle` or run `singleNodeBuild` instead.

### Tests fail with "connection refused"

The java-tron node is not running or not ready yet. After starting, wait until you see block production in the logs before running tests:

```bash
# Watch for block production
tail -f logs/tron.log | grep "pushBlock"
```

### solc not found or wrong version

Ensure the binary is at `system-test/solcDIR/solc` and is executable. The tronprotocol fork is required -- standard Ethereum solc will not work for TVM-specific opcodes.

### Rate limiter tests fail

The test config includes rate limiter rules. If you see throttling-related failures, verify that `rate.limiter` in your node's `config.conf` matches what the tests expect (see `docs/config.conf`).

---

## Project Structure

```
system-test/
  build.gradle          # Root build file
  gradlew               # Gradle wrapper
  docs/                 # Documentation and reference configs
    config.conf         # Reference node configuration for testing
  gRPCurl/              # Place grpcurl binary here
  solcDIR/              # Place solc binary here
  testcase/
    build.gradle        # Test task definitions (stest, dailyBuild, etc.)
    src/test/
      java/stest/tron/  # Test source code
      resources/
        testng.conf     # Test endpoint configuration
        testng.xml      # TestNG suite definitions
        daily-build.xml
        smoke-test.xml
        ...
  protocol/             # Protobuf definitions
```
