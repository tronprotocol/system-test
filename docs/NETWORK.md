# Network Topology Guide

How to set up java-tron nodes for running system tests. This document covers single-node and multi-node configurations with topology diagrams, port mappings, and ready-to-use configs.

---

## Overview

```
                          ┌─────────────────────────────────────┐
                          │         Test Suites                 │
                          │                                     │
                          │  smokeTest ──── Quick sanity (~30)  │
                          │  singleNodeBuild ─ Single node only │
                          │  dailyBuild ──── Full regression    │
                          └──────────┬──────────┬───────────────┘
                                     │          │
                          ┌──────────▼──┐  ┌────▼──────────┐
                          │ Single-Node │  │  Multi-Node   │
                          │  (57%)      │  │  (43%)        │
                          │  ~260 class │  │  ~195 class   │
                          └─────────────┘  └───────────────┘
```

| Suite | Command | Node Requirement | Classes | Time |
|-------|---------|-----------------|---------|------|
| Smoke | `./gradlew smokeTest` | Single | ~30 | ~5 min |
| Single Node | `./gradlew singleNodeBuild` | Single | ~260 | ~40 min |
| Daily Build | `./gradlew dailyBuild` | Multi (full) / Single (partial) | ~455 | ~1.5 h |

---

## Single-Node Topology

One java-tron process plays all roles: FullNode, Solidity, PBFT, and Witness. Tests connect to different **ports** on the same process.

```
 ┌─────────────────────────────────────────────────────────────────────┐
 │                        java-tron Process                           │
 │                   (--witness --es mode)                            │
 │                                                                     │
 │   ┌─────────────┐  ┌──────────────┐  ┌──────────────┐             │
 │   │  Witness x2  │  │  Block Store │  │  Event Sub   │             │
 │   │  (SR1 + SR2) │  │  (LevelDB)   │  │  (Native)    │             │
 │   └─────────────┘  └──────────────┘  └──────┬───────┘             │
 │                                              │                      │
 │   ┌──────────────────────────────────────────┼──────────────────┐  │
 │   │                   API Layer              │                  │  │
 │   │                                          │                  │  │
 │   │  gRPC               HTTP            JSON-RPC    Event       │  │
 │   │  ├─ :50051 Full     ├─ :8090 Full   ├─ :50545   :50096     │  │
 │   │  ├─ :50061 Sol      ├─ :8091 Sol    └─ :50555              │  │
 │   │  └─ :50071 PBFT     └─ :8098 PBFT                          │  │
 │   └─────────────────────────────────────────────────────────────┘  │
 │                                                                     │
 │   P2P: :18889                                                      │
 └─────────────────────────────────────────────────────────────────────┘
          ▲           ▲           ▲           ▲
          │           │           │           │
     ┌────┴───┐  ┌────┴───┐  ┌───┴────┐  ┌───┴────┐
     │ gRPC   │  │ HTTP   │  │JSON-RPC│  │ Event  │
     │ Tests  │  │ Tests  │  │ Tests  │  │ Tests  │
     └────────┘  └────────┘  └────────┘  └────────┘
```

### Port Map (Single-Node)

| Protocol | Service | Port | testng.conf Key |
|----------|---------|------|-----------------|
| gRPC | FullNode | 50051 | `fullnode.ip.list[0]` |
| gRPC | Solidity | 50061 | `solidityNode.ip.list[0]` |
| gRPC | PBFT | 50071 | `solidityNode.ip.list[2]` |
| HTTP | FullNode | 8090 | `httpnode.ip.list[0]` |
| HTTP | Solidity | 8091 | `httpnode.ip.list[2]` |
| HTTP | PBFT | 8098 | `httpnode.ip.list[4]` |
| JSON-RPC | FullNode | 50545 | `jsonRpcNode.ip.list[0]` |
| JSON-RPC | Solidity | 50555 | `jsonRpcNode.ip.list[1]` |
| TCP | Event Subscribe | 50096 | `eventnode.ip.list[0]` |
| TCP | P2P | 18889 | -- |

### Start Command

```bash
bin/FullNode -c config.conf --witness --es
```

### config.conf (Single-Node)

Full version at `docs/config.conf`. Minimal example:

```hocon
net { type = mainnet }

storage {
  db.version = 2
  db.directory = "database"
  index.directory = "index"
  transHistory.switch = "on"
  balance.history.lookup = true
}

node {
  listen.port = 18889

  rpc {
    port = 50051
    solidityPort = 50061
    PBFTPort = 50071
    reflectionService = true         # Required for grpcurl tests
  }

  http {
    fullNodePort = 8090
    solidityPort = 8091
    PBFTPort = 8098
  }

  jsonrpc {
    httpFullNodeEnable = true
    httpFullNodePort = 50545
    httpSolidityEnable = true
    httpSolidityPort = 50555
    httpPBFTEnable = true
    httpPBFTPort = 50565
  }

  discovery { enable = true; persist = true }
  walletExtensionApi = true
  fullNodeAllowShieldedTransaction = true
  zenTokenId = 1000001
}

seed.node { ip.list = [ "127.0.0.1:18889" ] }

# 2 witnesses — meets 2/3 consensus threshold for single-node block production
localwitness = [
  369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77,
  291C233A5A7660FB148BAE07FCBCF885224F2DF453239BD983F859E8E5AA4602,
  9FD8E129DE181EA44C6129F727A6871440169568ADE002943EAD0E7A16D8EDAC
]

# Fast parameters for testing (20s maintenance instead of 6 hours)
block {
  needSyncCheck = false
  maintenanceTimeInterval = 20000
  proposalExpireTime = 20000
  checkFrozenTime = 0
}

vm {
  supportConstant = true
  saveInternalTx = true
  minTimeRatio = 0.0
  maxTimeRatio = 10.0
}

committee {
  allowCreationOfContracts = 1
  allowMultiSign = 1
  allowDelegateResource = 1
  allowSameTokenName = 1
  allowTvmTransferTrc10 = 1
  allowAccountStateRoot = 1
  allowTvmConstantinople = 1
  allowShieldedTransaction = 1
  allowTvmSolidity059 = 1
  changedDelegation = 1
  forbidTransferToContract = 1
  allowPBFT = 1
  allowShieldedTRC20Transaction = 1
  allowTvmIstanbul = 1
  allowTransactionFeePool = 1
  allowOptimizeBlackHole = 1
  allowBlackHoleOptimization = 1
  allowTvmFreeze = 1
  allowReceiptsMerkleRoot = 1
  allowAssetOptimization = 1
  allowTvmCompatibleEvm = 1
  allowTvmLondon = 1
  allowHigherLimitForMaxCpuTimeOfOneTx = 1
}

event.subscribe {
  native { useNativeQueue = true; bindport = 50096; sendqueuelength = 1000 }
  topics = [
    { triggerName = "block";          enable = true; topic = "block" },
    { triggerName = "transaction";    enable = true; topic = "transaction" },
    { triggerName = "contractevent";  enable = true; topic = "contractevent" },
    { triggerName = "contractlog";    enable = true; topic = "contractlog" },
    { triggerName = "solidity";       enable = true; topic = "solidity" },
    { triggerName = "solidityevent";  enable = true; topic = "solidityevent" },
    { triggerName = "soliditylog";    enable = true; topic = "soliditylog" }
  ]
}

genesis.block {
  assets = [
    { accountName = "Devaccount";  accountType = "AssetIssue"; address = "TPwJS5eC5BPGyMGtYTHNhPTB89sUWjDSSu"; balance = "10000000000000000" },
    { accountName = "Zion";        accountType = "AssetIssue"; address = "TSRNrjmrAbDdrsoqZsv7FZUtAo13fwoCzv"; balance = "15000000000000000" },
    { accountName = "Sun";         accountType = "AssetIssue"; address = "TDQE4yb3E7dvDjouvu8u7GgSnMZbxAEumV"; balance = "10000000000000000" },
    { accountName = "testng001";   accountType = "AssetIssue"; address = "TKVyqEJaq8QRPQfWE8s8WPb5c92kanAdLo"; balance = "10000000000000000" },
    { accountName = "testng002";   accountType = "AssetIssue"; address = "THph9K2M2nLvkianrMGswRhz5hjSA9fuH7"; balance = "10000000000000000" },
    { accountName = "testng003";   accountType = "AssetIssue"; address = "TV75jZpdmP2juMe1dRwGrwpV6AMU6mr1EU"; balance = "10000000000000000" },
    { accountName = "testng004";   accountType = "AssetIssue"; address = "TNUpX2x6SH36Sv8i5FtENYZJFcBCFs8ds8"; balance = "10000000000000000" },
    { accountName = "testng005";   accountType = "AssetIssue"; address = "TCKu9t3U3dgU6YD3sWKcPD17BUpU6dohTH"; balance = "10000000000000000" },
    { accountName = "Blackhole";   accountType = "AssetIssue"; address = "THmtHi1Rzq4gSKYGEKv1DPkV7au6xU1AUB"; balance = "-9223372036854775808" }
  ]
  witnesses = [
    { address: "TB4B1RMhoPeivkj4Hebm6tttHjRY9yQFes"; url = "http://Mercury.org"; voteCount = 105 },
    { address: "TT1smsmhxype64boboU8xTuNZVCKP1w6qT"; url = "http://Venus.org"; voteCount = 104 }
  ]
  timestamp = "0"
  parentHash = "0x0000000000000000000000000000000000000000000000000000000000000000"
}
```

### Verify

```bash
# Wait 10-15 seconds after start, then check block production
curl -s http://127.0.0.1:8090/wallet/getnowblock | python3 -m json.tool | grep number
```

---

## Multi-Node Topology

For full `dailyBuild` coverage (including Solidity queries, PBFT finality, and multi-node sync tests), run 2 separate java-tron processes.

```
 ┌──────────────────────────────────┐    P2P     ┌──────────────────────────────────┐
 │         Node 1 (SR1)             │◄──────────►│         Node 2 (SR2)             │
 │                                  │  :18889    │                                  │
 │  Witness: SR1 (Mercury)          │  :18892    │  Witness: SR2 (Venus)            │
 │                                  │            │                                  │
 │  gRPC                            │            │  gRPC                            │
 │  ├─ :50051 FullNode              │            │  ├─ :50052 FullNode              │
 │  ├─ :50061 Solidity              │            │  ├─ :50062 Solidity              │
 │  └─ :50071 PBFT                  │            │  └─ :50072 PBFT                  │
 │                                  │            │                                  │
 │  HTTP                            │            │  HTTP                            │
 │  ├─ :8090  FullNode              │            │  ├─ :8092  FullNode              │
 │  ├─ :8091  Solidity              │            │  ├─ :8093  Solidity              │
 │  └─ :8098  PBFT                  │            │  └─ :8099  PBFT                  │
 │                                  │            │                                  │
 │  JSON-RPC                        │            │  JSON-RPC                        │
 │  ├─ :50545 FullNode              │            │  ├─ :50546 FullNode              │
 │  └─ :50555 Solidity              │            │  └─ :50556 Solidity              │
 │                                  │            │                                  │
 │  P2P: :18889                     │            │  P2P: :18892                     │
 │  Event: :50096                   │            │  Event: :50097                   │
 └──────────────────────────────────┘            └──────────────────────────────────┘
          ▲                                                ▲
          │                                                │
          └────────────── Test Process ────────────────────┘
                     (Gradle / TestNG)
```

### DPoS Consensus

```
 Time ──────────────────────────────────────────────────────►

 SR1 ──── Block 1 ────────────── Block 3 ────────────── Block 5 ──►
                   ╲            ╱          ╲            ╱
                    ── confirm ──            ── confirm ──
                   ╱            ╲          ╱            ╲
 SR2 ────────────── Block 2 ────────────── Block 4 ──────────────►

 Finality: block is "solidified" after 2/3 witnesses confirm (both SRs needed)
 Solidity queries only return solidified blocks
```

### Port Map (Multi-Node)

| Protocol | Service | Node 1 | Node 2 |
|----------|---------|--------|--------|
| gRPC | FullNode | 50051 | 50052 |
| gRPC | Solidity | 50061 | 50062 |
| gRPC | PBFT | 50071 | 50072 |
| HTTP | FullNode | 8090 | 8092 |
| HTTP | Solidity | 8091 | 8093 |
| HTTP | PBFT | 8098 | 8099 |
| JSON-RPC | FullNode | 50545 | 50546 |
| JSON-RPC | Solidity | 50555 | 50556 |
| TCP | P2P | 18889 | 18892 |
| TCP | Event | 50096 | 50097 |
| TCP | Backup | 10001 | 10002 |

### Node 1 config: `docs/config-node1.conf`

Full config file ready to use. Key differences from single-node:

```hocon
# --- Differences from single-node (full file at docs/config-node1.conf) ---

node {
  listen.port = 18889

  rpc {
    port = 50051
    solidityPort = 50061
    PBFTPort = 50071
  }

  http {
    fullNodePort = 8090
    solidityPort = 8091
    PBFTPort = 8098
  }

  jsonrpc {
    httpFullNodePort = 50545
    httpSolidityPort = 50555
    httpPBFTPort = 50565
  }

  active = [ "127.0.0.1:18892" ]
}

seed.node { ip.list = [ "127.0.0.1:18889", "127.0.0.1:18892" ] }

# Node 1 runs SR1 only
localwitness = [
  369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77
]

block {
  needSyncCheck = false    # First node: false
}

event.subscribe.native.bindport = 50096
```

### Node 2 config: `docs/config-node2.conf`

Full config file ready to use. Key differences from Node 1:

```hocon
# --- Differences from Node 1 (full file at docs/config-node2.conf) ---

node {
  listen.port = 18892

  rpc {
    port = 50052
    solidityPort = 50062
    PBFTPort = 50072
  }

  http {
    fullNodePort = 8092
    solidityPort = 8093
    PBFTPort = 8099
  }

  jsonrpc {
    httpFullNodePort = 50546
    httpSolidityPort = 50556
    httpPBFTPort = 50566
  }

  active = [ "127.0.0.1:18889" ]
}

seed.node { ip.list = [ "127.0.0.1:18889", "127.0.0.1:18892" ] }

# Node 2 runs SR2 only
localwitness = [
  291C233A5A7660FB148BAE07FCBCF885224F2DF453239BD983F859E8E5AA4602
]

block {
  needSyncCheck = true     # Non-first node: true
}

# Use different data directories to avoid conflict
storage {
  db.directory = "database2"
  index.directory = "index2"
}

event.subscribe.native.bindport = 50097
```

### testng.conf for Multi-Node

```hocon
fullnode = {
  ip.list = [
    "127.0.0.1:50051",    # [0] Node 1 FullNode
    "127.0.0.1:50052",    # [1] Node 2 FullNode
  ]
}

solidityNode = {
  ip.list = [
    "127.0.0.1:50061",    # [0] Node 1 Solidity
    "127.0.0.1:50062",    # [1] Node 2 Solidity
    "127.0.0.1:50071",    # [2] Node 1 PBFT
    "127.0.0.1:50072"     # [3] Node 2 PBFT
  ]
}

httpnode = {
  ip.list = [
    "127.0.0.1:8090",     # [0] Node 1 FullNode HTTP
    "127.0.0.1:8092",     # [1] Node 2 FullNode HTTP
    "127.0.0.1:8091",     # [2] Node 1 Solidity HTTP
    "127.0.0.1:8093",     # [3] Node 2 Solidity HTTP
    "127.0.0.1:8098",     # [4] Node 1 PBFT HTTP
    "127.0.0.1:8093",     # [5] Node 2 Solidity HTTP
    "127.0.0.1:8099"      # [6] Node 2 PBFT HTTP
  ]
}

jsonRpcNode = {
  ip.list = [
    "127.0.0.1:50545",    # [0] Node 1 FullNode JSON-RPC
    "127.0.0.1:50555",    # [1] Node 1 Solidity JSON-RPC
    "127.0.0.1:50546",    # [2] Node 2 FullNode JSON-RPC
  ]
}
```

### Start Commands

```bash
# Terminal 1 — Node 1
cd node1/
bin/FullNode -c config-node1.conf --witness --es

# Terminal 2 — Node 2
cd node2/
bin/FullNode -c config-node2.conf --witness --es

# Verify both nodes are producing and syncing
curl -s http://127.0.0.1:8090/wallet/getnowblock | python3 -c "import sys,json; print('Node1:', json.load(sys.stdin)['block_header']['raw_data']['number'])"
curl -s http://127.0.0.1:8092/wallet/getnowblock | python3 -c "import sys,json; print('Node2:', json.load(sys.stdin)['block_header']['raw_data']['number'])"
```

---

## Automatic Environment Detection

The test framework auto-detects whether a multi-node environment is available and skips tests accordingly.

### How It Works

```
 Suite starts
     │
     ▼
 MultiNodeListener.onStart()
     │
     ├─ Read fullnode.ip.list from testng.conf
     │
     ├─ [0] == [1] ?  ──yes──► SINGLE-NODE (same endpoint)
     │       │
     │      no
     │       │
     │       ▼
     │   TCP probe [1] ──fail──► SINGLE-NODE (unreachable)
     │       │
     │     success
     │       │
     │       ▼
     │   MULTI-NODE
     │
     ▼
 For each test class:
     Has @MultiNode annotation?
     ├─ no  → run normally
     └─ yes → SINGLE-NODE? → SkipException (clear message in report)
              MULTI-NODE?  → run normally
```

### `@MultiNode` Annotation

Mark test classes that need multiple nodes:

```java
@MultiNode(reason = "Needs second FullNode for cross-node broadcast verification")
public class WalletTestTransfer003 extends TronBaseTest {
    // ...
}
```

When running on a single node, the test is **automatically skipped** with a message:

```
SKIP WalletTestTransfer003 — Needs second FullNode for cross-node broadcast verification (single-node detected)
```

### Override via System Property

Force a mode regardless of auto-detection:

```bash
# Force multi-node mode (tests fail instead of skip if node missing)
./gradlew dailyBuild -Dtron.test.multinode=true

# Force single-node mode (skip all @MultiNode tests)
./gradlew dailyBuild -Dtron.test.multinode=false
```

### Detection Criteria

| testng.conf State | Detection | Result |
|-------------------|-----------|--------|
| `ip.list[0]` == `ip.list[1]` (same address) | Identical endpoints | **Single-node** |
| `ip.list[1]` is different but unreachable | TCP probe fails (3s timeout) | **Single-node** |
| `ip.list[1]` is different and reachable | TCP probe succeeds | **Multi-node** |
| Only 1 entry in `ip.list` | Too few nodes | **Single-node** |

### For Test Authors

When writing a new test that requires multi-node:

1. Add `@MultiNode(reason = "...")` to the class
2. The test will automatically run in multi-node environments and skip in single-node
3. No need to manually add/remove classes from `single-node-build.xml`

```java
import stest.tron.wallet.common.client.utils.MultiNode;

@MultiNode(reason = "Tests cross-node transaction broadcast and confirmation")
public class MyNewMultiNodeTest extends TronBaseTest {

    private String fullnode1 = config.getStringList("fullnode.ip.list").get(1);

    // ... test methods
}
```

For runtime checks within a test method:

```java
if (!MultiNodeListener.isMultiNodeAvailable()) {
    throw new SkipException("This specific test needs multi-node");
}
```

---

## Which Tests Need Multi-Node?

Of the ~455 test classes in `dailyBuild`, about 195 (43%) require multi-node. The dependency breaks down as:

```
 ┌─────────────────────────────────────────────────────────┐
 │              dailyBuild Test Classes (~455)              │
 │                                                         │
 │  ┌─────────────────────────────┐  ┌─────────────────┐  │
 │  │    Single-Node OK (57%)     │  │ Multi-Node (43%)│  │
 │  │                             │  │                 │  │
 │  │  TVM / Create2 / Grammar   │  │  Solidity: 145  │  │
 │  │  TRC Token                  │  │  FullNode2: 104 │  │
 │  │  Asset Issue / Market       │  │  PBFT: 21       │  │
 │  │  Freeze V2 / Staking       │  │  HTTP Sol: 37   │  │
 │  │  Account / Transaction      │  │                 │  │
 │  │  Security / Example         │  │  (overlapping)  │  │
 │  └─────────────────────────────┘  └─────────────────┘  │
 └─────────────────────────────────────────────────────────┘
```

| Dependency | Test Count | Why |
|------------|-----------|-----|
| Solidity queries | 145 | Need solidified blocks (requires 2/3 witness confirmation) |
| Second FullNode | 104 | Test cross-node sync, broadcast, different node views |
| PBFT | 21 | Test PBFT consensus finality endpoint |
| HTTP Solidity/PBFT | 37 | HTTP variants of the above |

If you only have a single node, use `singleNodeBuild` to run the 260 compatible tests:

```bash
./gradlew singleNodeBuild    # ~40 min, no multi-node needed
```

---

## Optional: MongoDB for Event Query Tests

5 test classes (`MongoEventQuery001-005`) require MongoDB. Without it, these tests are gracefully skipped.

```
 java-tron ──event──► Native Queue (:50096)
                          │
                          ▼
                    Event Plugin ──► MongoDB (:27017)
                                        ▲
                                        │
                                  MongoEventQuery Tests
```

```bash
# macOS
brew tap mongodb/brew && brew install mongodb-community
brew services start mongodb-community

# Linux
sudo apt install mongodb
sudo systemctl start mongodb
```

Update `testng.conf`:
```hocon
mongonode = { ip.list = [ "127.0.0.1:27017" ] }
```

---

## Docker Environment

All configurations are provided as Docker Compose services. Place `FullNode.jar` in `docker/node/` before starting.

```bash
# Build FullNode.jar from java-tron source (or download from releases)
cp /path/to/FullNode.jar docker/node/FullNode.jar
```

### Docker Architecture

```
 docker compose up (default)              docker compose --profile multi up
 ┌───────────────────────┐                ┌───────────────────────┐
 │  Single-Node Mode     │                │  Multi-Node Mode      │
 │                       │                │                       │
 │  tron-node            │                │  tron-node1 (SR1)     │
 │  ├─ node/config.conf  │                │  ├─ node1/config.conf │
 │  ├─ 2 SR keys         │                │  ├─ 1 SR key          │
 │  └─ all ports         │                │  └─ ports: 50051...   │
 │                       │                │                       │
 │                       │                │  tron-node2 (SR2)     │
 │                       │                │  ├─ node2/config.conf │
 │                       │                │  ├─ 1 SR key          │
 │                       │                │  └─ ports: 50052...   │
 └───────────────────────┘                └───────────────────────┘
         ▲                                        ▲
         │         ┌──────────┐                   │
         └─────────┤  system  ├───────────────────┘
                   │  -test   │
                   └──────────┘
                   network_mode: host

 Optional:  docker compose --profile mongo up
            └─ mongo (MongoDB :27017) for event query tests
```

### File Layout

```
docker/
├── docker-compose.yml       # All services defined here
├── Dockerfile               # Multi-stage build for test runner
├── .dockerignore
├── node/
│   ├── config.conf          # Single-node config (2 SRs, all ports)
│   └── FullNode.jar         # <-- You provide this
├── node1/
│   └── config.conf          # Multi-node SR1 config (ports 50051/8090/...)
└── node2/
    └── config.conf          # Multi-node SR2 config (ports 50052/8092/...)
```

### Commands

**Single-Node (smoke / singleNodeBuild):**

```bash
# Start node
docker compose up -d tron-node

# Wait for healthy (block production)
docker compose ps    # STATUS should show "healthy"

# Run tests
docker compose run system-test smokeTest --no-daemon
docker compose run system-test singleNodeBuild --no-daemon

# Stop
docker compose down
```

**Multi-Node (full dailyBuild):**

```bash
# Start both SR nodes
docker compose --profile multi up -d

# Verify both nodes producing and syncing
curl -s http://127.0.0.1:8090/wallet/getnowblock | python3 -c "import sys,json; print('SR1:', json.load(sys.stdin)['block_header']['raw_data']['number'])"
curl -s http://127.0.0.1:8092/wallet/getnowblock | python3 -c "import sys,json; print('SR2:', json.load(sys.stdin)['block_header']['raw_data']['number'])"

# Run full regression
docker compose run system-test dailyBuild --no-daemon

# Stop
docker compose --profile multi down
```

**With MongoDB (event query tests):**

```bash
# Single-node + MongoDB
docker compose --profile mongo up -d tron-node mongo
docker compose run system-test singleNodeBuild --no-daemon

# Full: multi-node + MongoDB
docker compose --profile multi --profile mongo up -d
docker compose run system-test dailyBuild --no-daemon
```

**Clean up volumes (reset chain data):**

```bash
docker compose down -v    # Removes all data volumes
```

### Config Differences Summary

| Setting | Single-Node (`node/`) | Node 1 (`node1/`) | Node 2 (`node2/`) |
|---------|----------------------|--------------------|--------------------|
| localwitness | SR1 + SR2 + extra | SR1 only | SR2 only |
| needSyncCheck | false | false | **true** |
| gRPC FullNode | 50051 | 50051 | **50052** |
| gRPC Solidity | 50061 | 50061 | **50062** |
| gRPC PBFT | 50071 | 50071 | **50072** |
| HTTP FullNode | 8090 | 8090 | **8092** |
| HTTP Solidity | 8091 | 8091 | **8093** |
| HTTP PBFT | 8098 | 8098 | **8099** |
| JSON-RPC Full | 50545 | 50545 | **50546** |
| JSON-RPC Sol | 50555 | 50555 | **50556** |
| P2P listen | 18889 | 18889 | **18892** |
| Event port | 50096 | 50096 | **50097** |
| Backup port | 10001 | 10001 | **10002** |
| Prometheus | 9528 | 9528 | **9529** |
| Storage dir | database | database | **database2** |
| active peers | [] | [127.0.0.1:18892] | [127.0.0.1:18889] |
| Genesis | identical | identical | identical |
| Committee | identical | identical | identical |

### testng.conf Mapping

For **single-node** tests (default `testng.conf`), all entries point to 127.0.0.1 with the same node's ports.

For **multi-node** tests, update `testng.conf` so array indices reference the correct node:

```hocon
# testng.conf — multi-node version
fullnode.ip.list = [
  "127.0.0.1:50051",    # [0] Node 1 FullNode
  "127.0.0.1:50052"     # [1] Node 2 FullNode
]

solidityNode.ip.list = [
  "127.0.0.1:50061",    # [0] Node 1 Solidity
  "127.0.0.1:50062",    # [1] Node 2 Solidity
  "127.0.0.1:50071",    # [2] Node 1 PBFT
  "127.0.0.1:50072"     # [3] Node 2 PBFT
]

httpnode.ip.list = [
  "127.0.0.1:8090",     # [0] Node 1 FullNode HTTP
  "127.0.0.1:8092",     # [1] Node 2 FullNode HTTP
  "127.0.0.1:8091",     # [2] Node 1 Solidity HTTP
  "127.0.0.1:8093",     # [3] Node 2 Solidity HTTP
  "127.0.0.1:8098",     # [4] Node 1 PBFT HTTP
  "127.0.0.1:8093",     # [5] Node 2 Solidity HTTP
  "127.0.0.1:8099"      # [6] Node 2 PBFT HTTP
]

jsonRpcNode.ip.list = [
  "127.0.0.1:50545",    # [0] Node 1 FullNode JSON-RPC
  "127.0.0.1:50555",    # [1] Node 1 Solidity JSON-RPC
  "127.0.0.1:50546"     # [2] Node 2 FullNode JSON-RPC
]
```

---

## Quick Reference

### I want to... | Do this

| Goal | Command | Nodes |
|------|---------|-------|
| Quick sanity check | `./gradlew smokeTest` | 1 |
| Run single-node tests only | `./gradlew singleNodeBuild` | 1 |
| Full regression | `./gradlew dailyBuild` | 2 |
| Run one specific test | `./run-test.sh ClassName` | 1 or 2 |
| Fuzz testing | `./gradlew fuzzTest` | 1 |

### Checklist Before Running Tests

- [ ] java-tron node(s) started with `--witness --es`
- [ ] Block number increasing (`curl http://127.0.0.1:8090/wallet/getnowblock`)
- [ ] Ports match between `config.conf` and `testng.conf`
- [ ] `grpcurl` and `solc` in place (or run `./gradlew downloadTools`)
- [ ] For multi-node: both nodes synced to same block height
- [ ] For event tests: MongoDB running (optional, tests skip gracefully)
