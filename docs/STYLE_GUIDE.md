# System-Test Code Style Guide

Style conventions for the TRON system-test project. Tests run on Java 8 with
TestNG, gRPC, and Protobuf. Checkstyle is enforced via
`testcase/config/checkstyle/checkStyleAll.xml`.

---

## 1. File Organization

### Package Structure

All daily build tests live under:

```
stest.tron.wallet.dailybuild.<feature>/
```

Feature packages map to TRON capabilities: `freezeV2`, `multisign`,
`assetissue`, `account`, `tvmnewcommand`, etc.

### Test Class Naming

Use the pattern `<Feature><Scenario>Test<NNN>.java`:

```
DelegateResourceEdgeTest001.java   -- good
FreezeBalanceV2Test003.java        -- good
WalletTestAccount012.java          -- bad: generic, no feature context
```

The numeric suffix groups related scenarios. When only one class is needed,
use `001` or omit the number entirely.

### Source Layout

```
testcase/src/test/java/stest/tron/wallet/
  common/client/utils/        # PublicMethod, TronBaseTest, TronConstants
  dailybuild/<feature>/       # test classes
```

---

## 2. Test Class Structure

### Template

```java
package stest.tron.wallet.dailybuild.feature;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.TronConstants;
import stest.tron.wallet.common.client.utils.Utils;

/**
 * Brief description of what this test class covers.
 */
@Slf4j
public class FeatureScenarioTest001 extends TronBaseTest {

  private ECKey testKey = new ECKey(Utils.getRandom());
  private byte[] testAddress = testKey.getAddress();

  @BeforeClass(enabled = true)
  public void beforeClass() {
    // Fund accounts, freeze resources, deploy contracts
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(description = "Describe expected behavior", groups = {"daily"})
  public void test01DescriptiveName() {
    // arrange, act, assert
  }

  @AfterClass(enabled = true)
  public void afterClass() {
    // Return remaining TRX to foundation
    PublicMethod.sendcoin(foundationAddress, 0L,
        testAddress, testKey, blockingStubFull);
  }
}
```

### Rules

- **Always extend `TronBaseTest`.** It provides `blockingStubFull`,
  `foundationAddress`, `foundationKey`, and auto-closes gRPC channels.
- **Use `@Slf4j`** for logging. Never use `System.out.println`.
- **`@BeforeClass`** for one-time setup (funding, freezing, deploying).
- **`@AfterClass`** for cleanup (return TRX to foundation).
- **`@Test` annotations** must include both `description` and `groups`:
  ```java
  @Test(description = "Delegate zero bandwidth fails", groups = {"daily"})
  ```

---

## 3. Naming Conventions

### Methods and Variables

- `camelCase` for methods, local variables, and fields.
- Test methods: `testXxx` or `testNNDescription` (e.g., `test01DelegateZero`).

### Constants

- `UPPER_SNAKE_CASE` for constants.
- Use `TronConstants` for shared values (`TEN_TRX`, `THOUSAND_TRX`,
  `FREEZE_BANDWIDTH`, `FREEZE_ENERGY`).
- No magic numbers. Replace `1_000_000L` with a named constant.

### Spelling

Use consistent spelling throughout:

| Correct       | Wrong                        |
|---------------|------------------------------|
| `MultiSign`   | `MutiSign`, `Mutisign`       |
| `MultiSig`    | `MutiSig`                    |
| `AssetIssue`  | `AssetIssue` (already correct) |
| `SmartContract` | `Smartcontract`            |

---

## 4. Assertion Best Practices

### Always Include Messages

```java
// Good
Assert.assertEquals(balance, expectedBalance,
    "Balance mismatch after delegation");

// Bad -- no diagnostic message
Assert.assertEquals(balance, expectedBalance);
```

### Use Specific Assertions

```java
// Good
Assert.assertEquals(actual, expected, "msg");
Assert.assertTrue(condition, "msg");
Assert.assertNull(obj, "msg");
Assert.assertNotNull(obj, "msg");

// Bad -- loses type information on failure
Assert.assertTrue(actual == expected, "msg");
Assert.assertTrue(obj != null, "msg");
```

### One Logical Assertion Per Test

When practical, each `@Test` method should verify one behavior. Multiple
related assertions (e.g., checking both balance and resource count after one
operation) are fine within a single test.

---

## 5. Resource Management

### Wait for Block Production

After any on-chain transaction, wait before querying state:

```java
PublicMethod.sendcoin(toAddress, amount, fromAddress, fromKey, stub);
PublicMethod.waitProduceNextBlock(stub);
// Now safe to query
```

### Return TRX in AfterClass

Every test that receives TRX from foundation must return it:

```java
@AfterClass(enabled = true)
public void afterClass() {
  PublicMethod.sendcoin(foundationAddress, 0L,
      testAddress, testKeyStr, blockingStubFull);
}
```

Sending `0L` as the amount triggers a "send all remaining balance" pattern
in some helper methods. Verify the helper behavior you are using.

### Generate Fresh Keys

Always create new keypairs per test class to avoid cross-test interference:

```java
private ECKey ownerKey = new ECKey(Utils.getRandom());
private byte[] ownerAddress = ownerKey.getAddress();
private String ownerKeyStr = ByteArray.toHexString(ownerKey.getPrivKeyBytes());
```

### Channel Cleanup

`TronBaseTest` manages gRPC channel lifecycle. Do not create or close
`ManagedChannel` instances manually unless you have a specific reason.

---

## 6. Code Formatting

These rules are enforced by checkstyle (`checkStyleAll.xml`).

| Rule                     | Setting                          |
|--------------------------|----------------------------------|
| Indentation              | 2 spaces (basic), 4 spaces (line wrap / throws) |
| Tabs                     | Forbidden (`FileTabCharacter`)   |
| Encoding                 | UTF-8                            |
| Max line length          | 100 characters                   |
| Star imports             | Forbidden (`AvoidStarImport`)    |
| Empty catch blocks       | Variable must be named `expected` |
| Braces                   | Required (`NeedBraces`)          |
| One statement per line   | Required                         |
| Modifier order           | `public static final` (standard Java order) |

### Import Order

Checkstyle enforces `STATIC` imports first, then `THIRD_PARTY_PACKAGE`,
sorted alphabetically within each group, with a blank line between groups:

```java
import static org.testng.Assert.assertEquals;

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.utils.PublicMethod;
```

### Line Wrapping

Lines exceeding 100 characters should break with 4-space continuation indent.
Operators wrap to the next line:

```java
Assert.assertTrue(
    PublicMethod.freezeBalanceV2(ownerAddress, TronConstants.THOUSAND_TRX,
        TronConstants.FREEZE_BANDWIDTH, ownerKeyStr, blockingStubFull));
```

---

## 7. Common Pitfalls

1. **Forgetting `waitProduceNextBlock`** -- leads to flaky tests that pass
   locally but fail in CI due to timing.
2. **Hardcoded addresses or private keys** -- always generate fresh keys or
   reference values from `TronBaseTest` / config files.
3. **Not returning TRX** -- leaks test funds, eventually starving the
   foundation account on long-running test networks.
4. **Swallowing exceptions in catch blocks** -- if a test must handle an
   exception, assert on it. Never use empty catch blocks to hide failures.
5. **Wildcard imports** -- checkstyle rejects them. Use explicit imports.
