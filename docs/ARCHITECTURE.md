# System-Test Architecture

## Overview

```
system-test/
├── protocol/           # Protobuf definitions → generated gRPC stubs
├── testcase/           # All test code
│   └── src/test/java/stest/tron/wallet/
│       ├── common/client/utils/    # Shared test infrastructure
│       ├── dailybuild/             # Daily CI regression suite
│       ├── account/                # Core account tests
│       ├── transfer/               # TRX transfer tests
│       ├── block/                  # Block query tests
│       ├── contract/               # Smart contract tests
│       ├── fuzz/                   # Property-based fuzz tests (jqwik)
│       ├── onlinestress/           # Load/stress tests
│       └── fulltest/               # Full integration tests
└── docker/             # Docker environment for private network testing
```

## Test Infrastructure

### Base Class Hierarchy

```
TronBaseTest (abstract)
├── channelFull / blockingStubFull (auto-initialized)
├── foundationKey / foundationAddress
├── witnessKey / witnessAddress
├── fullnode endpoint, maxFeeLimit
├── initSolidityChannel() / initPbftChannel()
└── closeChannels() (auto-cleanup)

TestClass extends TronBaseTest
├── @BeforeClass: test-specific setup
├── @Test(groups={"daily","contract"}): test methods
└── @AfterClass: resource cleanup (channels auto-closed by base)
```

### Helper Classes (decomposed from PublicMethod)

| Helper | Responsibility | Methods |
|--------|---------------|---------|
| AccountHelper | Account queries, creation | 28 |
| ContractHelper | Contract deploy, trigger, ABI | 42 |
| ResourceHelper | Freeze, delegate, energy/bandwidth | 57 |
| AssetHelper | TRC-10/TRC-20 token ops | 46 |
| GovernanceHelper | Proposals, voting, witness | 19 |
| TransactionHelper | Tx signing, broadcasting | 16 |
| BlockHelper | Block queries | 8 |
| ShieldHelper | Privacy/shielded operations | 17 |
| CommonHelper | Address/key utilities | 26 |

### Utility Classes

| Class | Purpose |
|-------|---------|
| TronConstants | Magic number elimination (ONE_TRX, FREEZE_ENERGY, etc.) |
| RetryUtil | Polling/retry patterns (waitUntil, pollUntilNonNull) |
| Retry | TestNG IRetryAnalyzer (configurable via system properties) |
| RetryListener | IAnnotationTransformer for auto-retry injection |

## Test Execution

| Gradle Task | Suite XML | Threads | Purpose |
|-------------|-----------|---------|---------|
| `stest` | testng.xml | 4 | Core smoke tests |
| `dailyBuild` | daily-build.xml | 8+1 | Daily regression |
| `smokeTest` | smoke-test.xml | 4 | Quick verification |
| `fuzzTest` | (JUnit Platform) | 1 | Property-based tests |
| `leveldbTest` | leveldb.xml | 1 | Database tests |
| `coverageCheck` | — | — | JaCoCo threshold verification |

## Test Groups

| Group | Meaning | Execution |
|-------|---------|-----------|
| `smoke` | Core functionality, must always pass | Parallel |
| `daily` | Daily CI regression | Parallel |
| `contract` | Smart contract related | Parallel |
| `serial` | Timing-sensitive, no parallelization | Serial |
| `stress` | Load/performance tests | Serial |
| `staking` | Stake 2.0 / FreezeV2 | Parallel |
| `shield` | Privacy/Zen token tests | Parallel |
| `multisig` | Multi-signature tests | Parallel |
| `full` | Full integration | Parallel |

## Configuration

Tests read from `testcase/src/test/resources/testng.conf` (HOCON format):
- Node endpoints (gRPC, HTTP, JSON-RPC, Solidity, PBFT)
- Foundation/witness account keys
- Default parameters (maxFeeLimit, etc.)
