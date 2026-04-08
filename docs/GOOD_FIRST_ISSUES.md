# Good First Issues

A curated list of beginner-friendly tasks for new contributors. Each issue is self-contained, well-scoped, and designed to help you learn the codebase while making a real contribution.

**How to get started:** Pick an issue below, comment on it (or create a new GitHub issue referencing the ID), and submit a PR. See [CONTRIBUTING.md](../CONTRIBUTING.md) for setup and PR guidelines.

---

## New Tests

### 1. Add gRPC test for `GetBandwidthPrices` API
- **Description:** The `GetBandwidthPrices` endpoint returns historical bandwidth unit prices but has no dedicated test. Write a test that calls the API and verifies the response format.
- **Labels:** `good first issue`, `test`, `enhancement`
- **Difficulty:** Easy
- **Area:** `dailybuild/account/` -- create a new test class

### 2. Add gRPC test for `GetEnergyPrices` API
- **Description:** Similar to bandwidth prices, `GetEnergyPrices` returns historical energy unit prices. Add a test that validates the response contains comma-separated price entries.
- **Labels:** `good first issue`, `test`, `enhancement`
- **Difficulty:** Easy
- **Area:** `dailybuild/account/` -- create a new test class

### 3. Add HTTP test for `GetBurnTRX` endpoint
- **Description:** The `/wallet/getburntrx` HTTP endpoint returns the total burned TRX amount but has no HTTP-level test. Add a test in the HTTP test suite.
- **Labels:** `good first issue`, `test`
- **Difficulty:** Easy
- **Area:** `dailybuild/http/`

### 4. Add negative test for `FreezeBalanceV2` with zero amount
- **Description:** Write a test that attempts to freeze 0 TRX and verifies the transaction is rejected. Covers an edge case not currently tested.
- **Labels:** `good first issue`, `test`
- **Difficulty:** Easy
- **Area:** `dailybuild/freezeV2/`

### 5. Add JSON-RPC test for `eth_chainId`
- **Description:** The JSON-RPC interface supports `eth_chainId` but it lacks a dedicated test. Verify it returns the correct chain ID for the test network.
- **Labels:** `good first issue`, `test`
- **Difficulty:** Easy
- **Area:** `dailybuild/jsonrpc/`

### 6. Add test for account permission with empty operations field
- **Description:** Test what happens when `AccountPermissionUpdate` is called with an empty `operations` byte string. The system should reject or handle it gracefully.
- **Labels:** `good first issue`, `test`
- **Difficulty:** Medium
- **Area:** `dailybuild/multisign/`

### 7. Add test for creating account with minimum TRX
- **Description:** Test the exact minimum TRX required to create a new account (0.1 TRX / 100,000 sun). Verify that sending less fails and sending exactly the minimum succeeds.
- **Labels:** `good first issue`, `test`
- **Difficulty:** Easy
- **Area:** `dailybuild/account/`

---

## Test Improvements

### 8. Replace `assertTrue(a == b)` with `assertEquals` in FreezeBalanceV2 tests
- **Description:** Several FreezeV2 tests use `Assert.assertTrue(value == expected)` instead of `Assert.assertEquals(actual, expected, "message")`. The latter gives better failure messages.
- **Labels:** `good first issue`, `test`, `enhancement`
- **Difficulty:** Easy
- **Files:** `dailybuild/freezeV2/FreezeBalanceV2Test001.java`, `FreezeBalanceV2Test003.java`, `FreezeBalanceV2Test004.java`

### 9. Add `description` to `@Test` annotations in transfer tests
- **Description:** Many `@Test` annotations in the transfer package lack a `description` parameter. Add meaningful descriptions that explain what each test validates.
- **Labels:** `good first issue`, `test`, `docs`
- **Difficulty:** Easy
- **Files:** `transfer/WalletTestTransfer001.java`, `WalletTestTransfer003.java`, `WalletTestTransfer004.java`, `WalletTestTransfer007.java`

### 10. Add assertion messages to account tests
- **Description:** Tests in `account/WalletTestAccount004.java` and `WalletTestAccount005.java` use bare `Assert.assertTrue(condition)` without failure messages. Add descriptive messages to each assertion.
- **Labels:** `good first issue`, `test`, `enhancement`
- **Difficulty:** Easy
- **Files:** `account/WalletTestAccount004.java`, `account/WalletTestAccount005.java`

### 11. Remove magic numbers in HTTP test assertions
- **Description:** Tests like `HttpTestBlock001.java` compare against hardcoded numeric values (e.g., block numbers, energy costs) without explaining what they represent. Extract these into named constants or add inline comments.
- **Labels:** `good first issue`, `test`, `enhancement`
- **Difficulty:** Easy
- **Area:** `dailybuild/http/`

### 12. Replace `assertTrue(a == b)` with `assertEquals` in HTTP market tests
- **Description:** `HttpTestMarket001.java` and `HttpTestMarket002.java` use `assertTrue` for value comparisons. Refactor to `assertEquals` with descriptive messages.
- **Labels:** `good first issue`, `test`, `enhancement`
- **Difficulty:** Easy
- **Files:** `dailybuild/http/HttpTestMarket001.java`, `dailybuild/http/HttpTestMarket002.java`

### 13. Replace `System.out.println` with `logger.info` in TVM tests
- **Description:** Several TVM test classes use `System.out.println` for debugging output. Replace with `logger.info()` (available via `@Slf4j`) for consistent logging.
- **Labels:** `good first issue`, `test`, `enhancement`
- **Difficulty:** Easy
- **Files:** `dailybuild/tvmnewcommand/newGrammar/EnergyAdjustmentTest.java`, `dailybuild/tvmnewcommand/newGrammar/Opcode.java`, and others in the `tvmnewcommand/` package

---

## Documentation

### 14. Add Javadoc to `JsonRpcBase.java`
- **Description:** The `JsonRpcBase` utility class provides helper methods for JSON-RPC testing but has minimal documentation. Add Javadoc to public methods explaining parameters, return values, and usage.
- **Labels:** `good first issue`, `docs`
- **Difficulty:** Easy
- **File:** `common/client/utils/JsonRpcBase.java`

### 15. Add Javadoc to `HttpMethod.java`
- **Description:** `HttpMethod` is the primary HTTP test utility class. Many of its public methods lack Javadoc. Add documentation for at least 10 commonly used methods.
- **Labels:** `good first issue`, `docs`
- **Difficulty:** Medium
- **File:** `common/client/utils/HttpMethod.java`

### 16. Document test suite XML files
- **Description:** Add XML comments to `testng.xml` and `daily-build.xml` explaining the purpose of each test group, thread count choices, and which tests are serial vs. parallel.
- **Labels:** `good first issue`, `docs`
- **Difficulty:** Easy
- **Files:** `testcase/src/test/resources/testng.xml`, `testcase/src/test/resources/daily-build.xml`

### 17. Add inline comments to `ShieldHelper.java`
- **Description:** The shield (privacy) helper class uses complex cryptographic operations with little explanation. Add inline comments explaining the purpose of key steps.
- **Labels:** `good first issue`, `docs`
- **Difficulty:** Medium
- **File:** `common/client/utils/ShieldHelper.java`

---

## Infrastructure

### 18. Add a `TronConstants` entry for common energy limits
- **Description:** Many tests hardcode energy limits like `10000000L` or `1000000000L`. Add well-named constants (e.g., `DEFAULT_ENERGY_LIMIT`, `MAX_FEE_LIMIT`) to reduce magic numbers across the codebase.
- **Labels:** `good first issue`, `enhancement`, `infra`
- **Difficulty:** Easy
- **File:** `common/client/utils/TronConstants.java` (or create if needed)

### 19. Improve error messages in `PublicMethod.sendcoin`
- **Description:** When `sendcoin` fails, the error message does not include the amount or recipient. Enhance the logging to include transaction details for easier debugging.
- **Labels:** `good first issue`, `enhancement`, `infra`
- **Difficulty:** Easy
- **File:** `common/client/utils/PublicMethod.java`

### 20. Create a `TestAccountFactory` utility
- **Description:** Many test classes repeat the same ECKey generation + funding pattern in `@BeforeClass`. Create a small utility that generates and funds a test account in one call.
- **Labels:** `good first issue`, `enhancement`, `infra`
- **Difficulty:** Medium
- **Area:** `common/client/utils/`

---

## Naming Cleanup

### 21. Rename `WalletTestAccount012` to descriptive name
- **Description:** `WalletTestAccount012` does not convey what it tests. Read the test methods, determine the feature being tested, and rename to a descriptive class name (e.g., `AccountPermissionUpdateTest`).
- **Labels:** `good first issue`, `enhancement`
- **Difficulty:** Easy
- **Files:** `dailybuild/manual/WalletTestAccount012.java`, `dailybuild/account/WalletTestAccount012.java`

### 22. Rename `WalletTestBlock004` and `WalletTestBlock005`
- **Description:** These numbered block test classes should have descriptive names. Review the test content and rename accordingly (e.g., `BlockQueryByNumberTest`).
- **Labels:** `good first issue`, `enhancement`
- **Difficulty:** Easy
- **Files:** `dailybuild/transaction/WalletTestBlock004.java`, `dailybuild/transaction/WalletTestBlock005.java`

### 23. Rename `WalletTestTransfer005` and `WalletTestTransfer006`
- **Description:** Rename these numbered transfer test classes to descriptive names based on their actual test scenarios.
- **Labels:** `good first issue`, `enhancement`
- **Difficulty:** Easy
- **Files:** `dailybuild/manual/WalletTestTransfer005.java`, `dailybuild/manual/WalletTestTransfer006.java`

### 24. Rename `ContractScenario004` to descriptive name
- **Description:** `ContractScenario004.java` uses `System.out.println` and a generic name. Rename to describe the contract scenario it tests and fix the logging.
- **Labels:** `good first issue`, `enhancement`
- **Difficulty:** Easy
- **File:** `contract/scenario/ContractScenario004.java`

---

## How to Claim an Issue

1. Check [GitHub Issues](https://github.com/tronprotocol/system-test/issues) to see if someone is already working on it.
2. Open a new issue (or comment on an existing one) saying you want to work on it.
3. Fork the repo, create a branch (`feature/issue-title` or `fix/issue-title`), and submit a PR.
4. Reference this document and the issue number in your PR description.

All file paths above are relative to `testcase/src/test/java/stest/tron/wallet/`.
