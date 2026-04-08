# Contributing to TRON System-Test

Thank you for your interest in contributing! This guide aligns with [java-tron's contribution standards](https://github.com/tronprotocol/java-tron/blob/develop/CONTRIBUTING.md).

## Prerequisites

- **JDK 8** (x86_64) or **JDK 17** (ARM64)
- **Gradle 7.6.4** (use the included wrapper `./gradlew`)
- **Solidity compiler** binary:
  ```bash
  # Download and place in project root
  mkdir -p solcDIR
  # Linux:
  # Linux:
  wget -O solcDIR/solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-static-linux
  # macOS:
  # wget -O solcDIR/solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-macos
  chmod +x solcDIR/solc
  ```
- **Running TRON node**: see [README.md](README.md#test-network-options) for network options

### Environment Setup

```bash
# 1. Fork and clone
git clone https://github.com/<your-username>/system-test.git
cd system-test

# 2. Add upstream remote
git remote add upstream https://github.com/tronprotocol/system-test.git

# 3. Sync with upstream
git fetch upstream
git checkout main
git merge upstream/main

# 4. Verify build
./gradlew compileTestJava
```

## Branch Naming

Create branches from `main`:

| Prefix | Purpose | Example |
|--------|---------|---------|
| `feature/` | New test cases or capabilities | `feature/tip-650-transient-storage` |
| `fix/` | Fix flaky or broken tests | `fix/transfer001-timeout` |
| `test/` | Add tests for existing features | `test/market-order-actuator` |
| `refactor/` | Code structure improvements | `refactor/split-public-methed` |
| `docs/` | Documentation updates | `docs/update-readme` |
| `hotfix/` | Urgent fixes for release branch | `hotfix/ci-solc-path` |

## Commit Message Format

We follow [Conventional Commits](https://www.conventionalcommits.org/), aligned with java-tron's `pr-check.yml` enforcement:

```
type(scope): subject
```

### Types

`feat` | `fix` | `test` | `refactor` | `docs` | `style` | `chore` | `ci` | `perf` | `build` | `revert`

### Scopes

| Scope | Covers |
|-------|--------|
| `consensus` | DPoS, witness, voting, block production |
| `vm` | TVM instructions, smart contract execution |
| `account` | Account creation, update, permissions, multi-sign |
| `token` | TRC-10, TRC-20, asset issue, exchange/DEX |
| `api` | gRPC, HTTP, JSON-RPC endpoints |
| `resource` | Bandwidth, energy, freeze/unfreeze, delegation |
| `governance` | Proposals, committee parameters |
| `network` | P2P, node discovery, sync |
| `ci` | GitHub Actions, build configuration |
| `infra` | Test utilities, base classes, configuration |

### Rules

- **Subject**: 10-72 characters, imperative mood, no period at end
- **Description**: minimum 20 characters in PR body

### Examples

```
test(vm): add TVM MCOPY opcode validation for TIP-651
fix(api): stabilize EventQuery flaky test with polling
refactor(infra): extract AccountHelper from PublicMethod
docs(readme): add java-tron CI integration guide
ci(build): add checkstyle and PR lint workflow
```

## Pull Request Process

1. **Create branch** from `main` with proper prefix
2. **Write/update tests** following the test patterns below
3. **Verify locally**: `./gradlew compileTestJava && ./gradlew lint`
4. **Push** and create PR to `tronprotocol/system-test:main`
5. **PR title** must follow commit format: `type(scope): subject` (10-72 chars)
6. **PR description** must be at least 20 characters
7. **CI must pass**: compile + checkstyle + PR lint

## Test Writing Guidelines

### Test Class Structure

Extend `TronBaseTest` — it provides `channelFull`, `blockingStubFull`, `foundationKey/Address`, `witnessKey/Address`, `maxFeeLimit`, and handles channel lifecycle automatically.

```java
@Slf4j
public class FeatureNameTest extends TronBaseTest {

    // Test-specific accounts (generated fresh each run)
    private ECKey ecKey1 = new ECKey(Utils.getRandom());
    private byte[] testAddress = ecKey1.getAddress();
    private String testKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    @BeforeClass
    public void beforeClass() {
        // channelFull and blockingStubFull are already initialized
        PublicMethod.sendcoin(testAddress, TronConstants.TEN_TRX,
            foundationAddress, foundationKey, blockingStubFull);
        PublicMethod.waitProduceNextBlock(blockingStubFull);
    }

    @Test(description = "Clear description of what is being tested",
          groups = {"daily"})
    public void testSpecificBehavior() {
        // Arrange + Act
        Assert.assertTrue(PublicMethod.sendcoin(...));
        PublicMethod.waitProduceNextBlock(blockingStubFull);
        // Assert
        Account account = PublicMethod.queryAccount(address, blockingStubFull);
        Assert.assertEquals(account.getBalance(), expectedBalance,
            "Balance should be X after transfer");
    }

    @AfterClass
    public void afterClass() {
        // Return funds; channel shutdown is handled by TronBaseTest
        PublicMethod.freeResource(testAddress, testKey,
            foundationAddress, blockingStubFull);
    }
}
```

See `dailybuild/example/ExampleTest.java` for a complete working example.

### Conventions

- **Package**: place new tests in `stest.tron.wallet.dailybuild.<feature>/`
- **Naming**: `<Feature><Scenario>Test.java` (e.g., `DelegateResourceTest.java`)
- **Assertions**: prefer `Assert.assertEquals(actual, expected, "message")` over `Assert.assertTrue(a == b)`
- **Wait for blocks**: always call `PublicMethod.waitProduceNextBlock()` after on-chain operations
- **Resource cleanup**: return test TRX in `@AfterClass`, close gRPC channels
- **No real keys**: all test keys must be randomly generated
- **Logging**: use `logger.info()` (via `@Slf4j`), never `System.out.println`

### Test Suite Registration

- **Parallel-safe tests**: add to `daily-build.xml` under `Parallel Case`
- **Must-be-serial tests** (HTTP, JSON-RPC, events): add to `Serial Case`
- **Core smoke tests**: add to `testng.xml`

## Code Style

- **Checkstyle**: enforced via `config/checkstyle/checkStyleAll.xml`
- **Indentation**: 4 spaces (no tabs)
- **Encoding**: UTF-8
- **Java version**: 1.8 source/target compatibility (no Java 9+ features)

Run locally: `./gradlew lint`

## Questions?

- [TRON Developer Documentation](https://tronprotocol.github.io/documentation-en/)
- [TRON Improvement Proposals (TIPs)](https://github.com/tronprotocol/tips)
- [java-tron Issues](https://github.com/tronprotocol/java-tron/issues)
