# FAQ — 常见问题

[English](#faq) | 中文

---

## 1. 启动节点后区块号一直为 0

**原因**：见证人私钥未正确配置，或 `needSyncCheck` 设置错误。

**解决方案**：
```bash
# 检查 config.conf 中：
localwitness = [
  369F095838EB6EED45D4F6312AF962D5B9DE52927DA9F04174EE49F9AF54BC77,
  291C233A5A7660FB148BAE07FCBCF885224F2DF453239BD983F859E8E5AA4602
]

block {
  needSyncCheck = false    # 单节点必须设为 false
}

# 确认私钥对应的地址在 genesis.block.witnesses 中
```

---

## 2. 测试报 "Connection refused" / "UNAVAILABLE"

**原因**：java-tron 节点未启动，或端口与 `testng.conf` 不匹配。

**解决方案**：
```bash
# 1. 确认节点正在运行
curl -s http://127.0.0.1:8090/wallet/getnowblock | head -1

# 2. 如果端口被占用
lsof -i :50051
lsof -i :8090
kill -9 <PID>

# 3. 检查 testng.conf 的端口与 config.conf 是否一致
grep "50051" testcase/src/test/resources/testng.conf
```

---

## 3. grpcurl 测试全部失败

**原因**：java-tron 的 gRPC Reflection 服务默认关闭。

**解决方案**：在 `config.conf` 的 `node.rpc` 段添加：
```hocon
node {
  rpc {
    reflectionService = true
  }
}
```
重启节点后生效。

---

## 4. 合约部署失败 / solc 报错

**原因**：solc 未安装、权限不足、或使用了标准 Ethereum 版 solc。

**解决方案**：
```bash
# 检查 solc 是否存在且可执行
ls -la solcDIR/solc
chmod +x solcDIR/solc

# 确认是 tronprotocol fork 版本
./solcDIR/solc --version
# 应输出: solc.tron ... Version: 0.8.26+...
# 如果显示 "solc, the solidity compiler"（没有 .tron），则版本不对

# 重新下载正确版本
# macOS:
wget -O solcDIR/solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-macos
# Linux:
wget -O solcDIR/solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-static-linux
chmod +x solcDIR/solc
```

---

## 5. `downloadTools` 下载超时（国内网络）

**原因**：GitHub Release 在国内访问较慢。

**解决方案**：手动下载后放到指定目录：

```bash
# grpcurl — 放到 gRPCurl/grpcurl
# 下载地址: https://github.com/fullstorydev/grpcurl/releases/tag/v1.8.9

# solc — 放到 solcDIR/solc
# 下载地址: https://github.com/tronprotocol/solidity/releases/tag/tv_0.8.26

# 放好后 Gradle 会自动检测到，不再重复下载
```

如果有代理，也可以设置 Gradle 代理：
```bash
# 在 gradle.properties 中添加
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

---

## 6. Solidity 查询始终返回空

**原因**：单节点环境无法产生 solidified block（需要 2/3 见证人确认）。

**解决方案**：
- 单节点测试使用 `./gradlew singleNodeBuild`，自动跳过需要 Solidity 的测试
- 或搭建双节点环境（参考 [NETWORK.md](NETWORK.md)），实现真正的 DPoS 共识

单节点下可以使用 PBFT 端口（`:50071`）获取近似 solidified 的数据。

---

## 7. MongoEventQuery 测试全部跳过

**原因**：MongoDB 未安装。这是**正常行为**，不影响其他测试。

**解决方案**（可选）：
```bash
# macOS
brew tap mongodb/brew && brew install mongodb-community
brew services start mongodb-community

# Linux (Ubuntu)
sudo apt install mongodb
sudo systemctl start mongodb

# 确认 testng.conf 中的 MongoDB 地址
mongonode.ip.list = ["127.0.0.1:27017"]
```

---

## 8. 编译报 "unsupported class file version"

**原因**：使用了错误的 JDK 版本。

**解决方案**：
```bash
# 确认 JDK 版本
java -version
# 应为 1.8.x（x86 平台）或 17.x（ARM 平台）

# macOS 安装 JDK 8
brew install --cask temurin@8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)

# 多版本共存时切换
# macOS:
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
# Linux:
sudo update-alternatives --config java
```

---

## 9. dailyBuild 内存不足 (OOM)

**原因**：dailyBuild 默认 `maxHeapSize = 2048m`，机器内存不足。

**解决方案**：
```bash
# 方案 1：用单节点构建代替
./gradlew singleNodeBuild

# 方案 2：减少线程数（编辑 testcase/build.gradle）
# threadCount 从 2 改为 1

# 方案 3：增加 Gradle JVM 内存（编辑 gradle.properties）
org.gradle.jvmargs=-Xmx3072m
```

---

## 10. 失败报告中如何区分环境问题和真正的 bug？

`dailyBuild` 结束后会自动打印 **Failure Classification Summary**：

```
=== Failure Classification Summary ===
  ENV_NODE_DOWN:     29  (gRPC connection refused)
  ENV_SINGLE_NODE:  195  (@MultiNode skipped)
  ENV_NO_MONGO:       5  (MongoEventQuery001 ...)
  FLAKY:              3  (FreezeBalanceV2Test001 ...)
  BUG:                2  (SecurityOverflowTest.test01 ...)
  ────────────────────────
  Total Failures:   234
  Actionable Bugs:    2  <<< 只需关注这里
=======================================
```

- **ENV_*** — 环境问题，搭好环境就能过
- **FLAKY** — 已知不稳定测试，标注了 `@Flaky`
- **BUG** — 真正需要修复的问题

---

## 还有问题？

- [TRON 开发者文档](https://tronprotocol.github.io/documentation-en/)
- [TRON 改进提案 (TIPs)](https://github.com/tronprotocol/tips)
- [java-tron Issues](https://github.com/tronprotocol/java-tron/issues)
- [网络拓扑指南](NETWORK.md) — 详细的组网和端口配置
