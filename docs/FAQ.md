# FAQ

---

## 1. Block number stays at 0 after starting the node

**Cause**: Witness private keys are not configured correctly, or `needSyncCheck` is set incorrectly.

**Solution**:
```bash
# Check config.conf:
localwitness = [
  369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77,
  291C233A5A7660FB148BAE07FCBCF885224F2DF453239BD983F859E8E5AA4602
]

block {
  needSyncCheck = false    # Must be false for single-node setup
}

# Confirm that the addresses corresponding to the private keys
# are listed in genesis.block.witnesses
```

---

## 2. Tests report "Connection refused" / "UNAVAILABLE"

**Cause**: The java-tron node is not running, or the ports do not match those in `testng.conf`.

**Solution**:
```bash
# 1. Verify the node is running
curl -s http://127.0.0.1:8090/wallet/getnowblock | head -1

# 2. If the port is occupied
lsof -i :50051
lsof -i :8090
kill -9 <PID>

# 3. Check that testng.conf ports match config.conf
grep "50051" testcase/src/test/resources/testng.conf
```

---

## 3. All grpcurl tests fail

**Cause**: The gRPC Reflection service in java-tron is disabled by default.

**Solution**: Add the following to the `node.rpc` section in `config.conf`:
```hocon
node {
  rpc {
    reflectionService = true
  }
}
```
Restart the node for the change to take effect.

---

## 4. Contract deployment fails / solc errors

**Cause**: solc is not installed, lacks execution permission, or is the standard Ethereum version of solc.

**Solution**:
```bash
# Check that solc exists and is executable
ls -la solcDIR/solc
chmod +x solcDIR/solc

# Verify it is the tronprotocol fork version
./solcDIR/solc --version
# Expected output: solc.tron ... Version: 0.8.26+...
# If it shows "solc, the solidity compiler" (without .tron), the version is wrong

# Re-download the correct version
# macOS:
wget -O solcDIR/solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-macos
# Linux:
wget -O solcDIR/solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-static-linux
chmod +x solcDIR/solc
```

---

## 5. `downloadTools` times out (slow network)

**Cause**: GitHub Releases may be slow to access depending on your region.

**Solution**: Download manually and place the files in the designated directories:

```bash
# grpcurl — place at gRPCurl/grpcurl
# Download: https://github.com/fullstorydev/grpcurl/releases/tag/v1.8.9

# solc — place at solcDIR/solc
# Download: https://github.com/tronprotocol/solidity/releases/tag/tv_0.8.26

# Once placed, Gradle will detect them automatically and skip re-downloading
```

If you have a proxy, you can also configure Gradle proxy settings:
```bash
# Add to gradle.properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

---

## 6. Solidity queries always return empty

**Cause**: A single-node environment cannot produce solidified blocks (requires 2/3 witness confirmation).

**Solution**:
- For single-node testing, use `./gradlew singleNodeBuild`, which automatically skips tests that require Solidity
- Or set up a dual-node environment (see [NETWORK.md](NETWORK.md)) for real DPoS consensus

On a single node, you can use the PBFT port (`:50071`) to obtain approximately solidified data.

---

## 7. All MongoEventQuery tests are skipped

**Cause**: MongoDB is not installed. This is **expected behavior** and does not affect other tests.

**Solution** (optional):
```bash
# macOS
brew tap mongodb/brew && brew install mongodb-community
brew services start mongodb-community

# Linux (Ubuntu)
sudo apt install mongodb
sudo systemctl start mongodb

# Verify the MongoDB address in testng.conf
mongonode.ip.list = ["127.0.0.1:27017"]
```

---

## 8. Build reports "unsupported class file version"

**Cause**: An incorrect JDK version is being used.

**Solution**:
```bash
# Check JDK version
java -version
# Should be 1.8.x (x86 platform) or 17.x (ARM platform)

# macOS — install JDK 8
brew install --cask temurin@8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)

# Switch between multiple versions
# macOS:
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
# Linux:
sudo update-alternatives --config java
```

---

## 9. dailyBuild runs out of memory (OOM)

**Cause**: dailyBuild defaults to `maxHeapSize = 2048m`, which may exceed available memory.

**Solution**:
```bash
# Option 1: Use single-node build instead
./gradlew singleNodeBuild

# Option 2: Reduce thread count (edit testcase/build.gradle)
# Change threadCount from 2 to 1

# Option 3: Increase Gradle JVM memory (edit gradle.properties)
org.gradle.jvmargs=-Xmx3072m
```

---

## 10. How to distinguish environment issues from real bugs in failure reports?

After `dailyBuild` completes, it automatically prints a **Failure Classification Summary**:

```
=== Failure Classification Summary ===
  ENV_NODE_DOWN:     29  (gRPC connection refused)
  ENV_SINGLE_NODE:  195  (@MultiNode skipped)
  ENV_NO_MONGO:       5  (MongoEventQuery001 ...)
  FLAKY:              3  (FreezeBalanceV2Test001 ...)
  BUG:                2  (SecurityOverflowTest.test01 ...)
  ────────────────────────
  Total Failures:   234
  Actionable Bugs:    2  <<< Focus only on this
=======================================
```

- **ENV_***: Environment issues — will pass once the environment is properly set up
- **FLAKY**: Known unstable tests, annotated with `@Flaky`
- **BUG**: Real issues that need to be fixed

---

## Still have questions?

- [TRON Developer Documentation](https://tronprotocol.github.io/documentation-en/)
- [TRON Improvement Proposals (TIPs)](https://github.com/tronprotocol/tips)
- [java-tron Issues](https://github.com/tronprotocol/java-tron/issues)
- [Network Topology Guide](NETWORK.md) — Detailed networking and port configuration
