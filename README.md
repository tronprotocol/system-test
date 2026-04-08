# TRON System-Test

**English** | [中文](README_CN.md)

[![CI](https://github.com/tronprotocol/system-test/actions/workflows/ci.yml/badge.svg)](https://github.com/tronprotocol/system-test/actions/workflows/ci.yml)
[![Coverage](https://github.com/tronprotocol/system-test/actions/workflows/coverage.yml/badge.svg)](https://github.com/tronprotocol/system-test/actions/workflows/coverage.yml)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](LICENSE)
[![codecov](https://codecov.io/gh/tronprotocol/system-test/branch/main/graph/badge.svg)](https://codecov.io/gh/tronprotocol/system-test)

System-level integration test suite for [java-tron](https://github.com/tronprotocol/java-tron) (GreatVoyage-v4.8.1 Democritus). Covers all 34 system contracts, gRPC/HTTP/JSON-RPC APIs, TVM instructions, Stake 2.0, multi-signature, DEX, and privacy features via end-to-end testing against live TRON nodes.

> All private keys in this repository are randomized test keys with no sensitive information.

## Tech Stack

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 1.8 (x86) / 17 (ARM) | Aligned with java-tron |
| Gradle | 7.6.4 (wrapper) | Aligned with java-tron |
| TestNG | 6.14.3 | Primary test framework |
| Protobuf | 3.25.5 | Protocol serialization |
| gRPC | 1.60.0 | Node communication |
| BouncyCastle | 1.78.1 | Cryptography |
| JaCoCo | 0.8.11 | Code coverage |
| jqwik | 1.7.4 | Property-based testing |

## Project Structure

```
system-test/
├── protocol/                          # Protobuf definitions + gRPC code generation
├── testcase/                          # Main test module (649 test classes)
│   ├── build.gradle                   # Test dependencies & task definitions
│   └── src/test/
│       ├── java/stest/tron/wallet/
│       │   ├── account/               # Account functionality tests
│       │   ├── transfer/              # TRX transfer tests
│       │   ├── block/                 # Block operation tests
│       │   ├── witness/               # SR/witness tests
│       │   ├── committee/             # Governance proposal tests
│       │   ├── multisign/              # Multi-signature tests
│       │   ├── contract/              # Smart contract tests (linkage + scenario)
│       │   ├── exchangeandtoken/      # DEX & TRC-10 tests
│       │   ├── dailybuild/            # Daily regression tests (42 sub-packages)
│       │   │   ├── account/           # Freeze V2, delegation, resources
│       │   │   ├── http/              # HTTP API tests
│       │   │   ├── jsonrpc/           # JSON-RPC interface tests
│       │   │   ├── tvmnewcommand/     # TVM new instruction tests
│       │   │   ├── freezeV2/          # Stake 2.0 tests
│       │   │   ├── eventquery/        # Event system tests
│       │   │   └── ...                # 36 more sub-packages
│       │   ├── fulltest/              # Full integration tests
│       │   ├── onlinestress/          # Stress tests
│       │   └── common/client/         # Shared utilities
│       │       ├── Configuration.java # HOCON config loader
│       │       └── utils/
│       │           ├── PublicMethod.java  # Core test helper (8400+ lines)
│       │           ├── RetryListener.java # TestNG retry listener
│       │           └── ...               # 100+ utility classes
│       └── resources/
│           ├── testng.xml             # Core test suite (4 threads)
│           ├── daily-build.xml        # Daily regression suite
│           ├── testng.conf            # Main config (node addresses, test accounts)
│           └── config-system-test.conf # Config used by java-tron CI
├── docs/reference/
│   └── java-tron-notes.md            # TRON protocol reference
├── build.gradle                       # Root build config
└── LICENSE                            # LGPL-3.0
```

## Prerequisites

- **JDK 8** (x86_64) or **JDK 17** (ARM64), Temurin distribution recommended
- **Solidity compiler**: tronprotocol fork solc v0.8.26 in `solcDIR/solc` (auto-downloaded by `./gradlew downloadTools`)
- **Running TRON node(s)**: private network, Nile Testnet, or Shasta Testnet

### Test Network Options

| Network | Use Case | Docs |
|---------|----------|------|
| **Private Network** | Local development, fast iteration | [Setup Guide](https://tronprotocol.github.io/documentation-en/using_javatron/private_network/) |
| **Nile Testnet** | Forward-looking features (ahead of mainnet) | [Faucet: nileex.io](https://nileex.io) |
| **Shasta Testnet** | Mirrors mainnet parameters | Via TronGrid only |

## Quick Start

### Run Core Test Suite (4 threads parallel)
```bash
./gradlew stest
```

### Run Daily Regression Build (8 threads parallel + serial)
```bash
./gradlew dailyBuild
```

### Run Trident SDK Tests
```bash
./gradlew trident
```

### Run LevelDB Tests
```bash
./gradlew leveldbTest
```

### Run Mainnet Replay Query
```bash
./gradlew mainnetReplayQuery
```

### Run Property-Based Fuzz Tests
```bash
./gradlew fuzzTest
```
Tests Base58, ByteArray, Sha256, ECKey, and address validation with randomized inputs using [jqwik](https://jqwik.net/).

## Local java-tron Environment for DailyBuild

To run the `dailyBuild` test suite locally, you need a private network with **2 Witness FullNodes + 1 Solidity node**. Pre-built configuration files are provided in:

```
testcase/src/test/resources/dailybuild-witness-conf/
├── witness1_config.conf   # Witness FullNode 1 (block producer, needSyncCheck=false)
├── witness2_config.conf   # Witness FullNode 2 (block producer, needSyncCheck=true, RocksDB + keystore)
└── solidity_config.conf   # Solidity node (no witness key)
```

### Port Mapping

| Node | P2P Port | gRPC | HTTP Full | HTTP Solidity | HTTP PBFT | JSON-RPC Full | Prometheus |
|------|----------|------|-----------|---------------|-----------|---------------|------------|
| Witness 1 | 18889 | 50051 | 8090 | 8091 | 8098 | 50545 | 9528 |
| Witness 2 | 18892 | 50052 | 8093 | 8094 | 8099 | 50546 | 9527 |
| Solidity  | 18893 | 50053 | 8096 | 8097 | —   | 50547 | 9529 |

### Required Tools

- **Solidity Compiler (solc)**: Download the tronprotocol fork from [tronprotocol/solidity/releases](https://github.com/tronprotocol/solidity/releases), place the binary in `solcDIR/` and name it `solc`. Current version: `v0.8.26`.
- **gRPCurl**: Download from [fullstorydev/grpcurl/releases](https://github.com/fullstorydev/grpcurl/releases), place the binary in `gRPCurl/` and name it `grpcurl`.
- **Slack Notifications (Optional)**: Configure a custom `slack` command in the test execution environment to automatically receive notifications for failed test cases.


### MongoDB Event Plugin (Required for Witness 2)

Witness 2 is configured with MongoDB event subscription. Before starting:

1. Start a MongoDB service
2. Download the event plugin from [tronprotocol/event-plugin](https://github.com/tronprotocol/event-plugin)
3. Edit `witness2_config.conf` and set:
   - `event.subscribe.path` — absolute path to the plugin zip (e.g., `/path/to/plugin-mongodb-1.0.0.zip`)
   - `event.subscribe.server` — MongoDB address (e.g., `172.17.0.1:27017`)
   - `event.subscribe.dbconfig` — `dbname|username|password|version`

### Start Nodes

```bash
# Build java-tron first
cd /path/to/java-tron
./gradlew build -x test

# Start Witness 1 (first node, produces blocks)
java -jar build/libs/FullNode.jar --witness --es -c witness1_config.conf

# Start Witness 2 (syncs from Witness 1, then produces blocks)
java -jar build/libs/FullNode.jar --witness --es -c witness2_config.conf

# Start Solidity node (syncs confirmed blocks)
java -jar build/libs/FullNode.jar --solidity -c solidity_config.conf
```

> `--es` enables the event subscription service. `--solidity` starts the node in Solidity mode.

After all 3 nodes are running and producing blocks, run the dailyBuild suite:

```bash
./gradlew dailyBuild
```

> For the full environment setup guide (JDK, solc, grpcurl), see [Setup Guide](docs/SETUP.md).

## Integration with java-tron CI

This repository is automatically invoked by java-tron's CI pipeline via the [`system-test.yml`](https://github.com/tronprotocol/java-tron/blob/develop/.github/workflows/system-test.yml) workflow:

1. java-tron CI builds `FullNode.jar`
2. Checks out this repo's `release_workflow` branch
3. Copies `config-system-test.conf` into java-tron
4. Starts FullNode as a background process (`--witness` mode)
5. Polls `http://localhost:8090/wallet/getblockbynum?num=1` until ready (max 5 min)
6. Runs `./gradlew --info stest --no-daemon`
7. Timeout: 60 minutes

## Configuration

Test configuration is loaded from `testcase/src/test/resources/testng.conf` (HOCON format):

- **Node addresses**: `fullnode.ip.list`, `solidityNode.ip.list`, `httpnode`
- **Test accounts**: `foundationAccount.key1`, `witness.key1` through `witness.key5`
- **Ports**: gRPC (50051), HTTP (8090), JSON-RPC (8545)

For java-tron CI integration, use `config-system-test.conf` with fast test parameters:
- `maintenanceTimeInterval = 300000` (5 min vs production 6 hours)
- `proposalExpireTime = 600000` (10 min vs production 3 days)

## Related Repositories

| Repository | Description |
|------------|-------------|
| [java-tron](https://github.com/tronprotocol/java-tron) | TRON full node implementation |
| [protocol](https://github.com/tronprotocol/protocol) | Protobuf API & message definitions |
| [tron-docker](https://github.com/tronprotocol/tron-docker) | Docker automation for TRON nodes |
| [tips](https://github.com/tronprotocol/tips) | TRON Improvement Proposals (127 TIPs) |
| [documentation-en](https://github.com/tronprotocol/documentation-en) | Official English documentation |
| [trident](https://github.com/tronprotocol/trident) | Lightweight Java SDK |
| [tronweb](https://github.com/tronprotocol/tronweb) | JavaScript/TypeScript API library |

## Documentation

| Document | Description |
|----------|-------------|
| [Setup Guide](docs/SETUP.md) | 5-minute quick start, private network setup, tool installation |
| [安装指南 (中文)](docs/SETUP_CN.md) | 中文版快速开始、工具安装、国内镜像 |
| [Architecture](docs/ARCHITECTURE.md) | Test infrastructure, base classes, execution model |
| [Network Topology](docs/NETWORK.md) | Single/multi-node setup, port mapping, Docker |
| [Style Guide](docs/STYLE_GUIDE.md) | Code conventions, naming rules, assertion patterns |
| [FAQ](docs/FAQ.md) | Common issues and solutions |
| [Contributing](CONTRIBUTING.md) | Branch naming, commit format, PR process |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines on commit format, branch naming, and PR process.

## Get Involved

New to the project? Here is how to start contributing:

- **Good First Issues:** Browse [curated beginner tasks](docs/GOOD_FIRST_ISSUES.md) or filter by [`good first issue`](https://github.com/tronprotocol/system-test/labels/good%20first%20issue) on GitHub.
- **Contributing Guide:** Read [CONTRIBUTING.md](CONTRIBUTING.md) for setup, conventions, and PR process.
- **Report Bugs:** Open a [bug report](https://github.com/tronprotocol/system-test/issues/new?template=bug_report.md) with reproduction steps and environment details.
- **Contributors:** See [CONTRIBUTORS.md](CONTRIBUTORS.md) for the list of contributors and how to get recognized.

## License

This project is licensed under the [GNU Lesser General Public License v3.0](LICENSE).
