# TRON System-Test 安装指南

[English](SETUP.md) | **中文**

[java-tron](https://github.com/tronprotocol/java-tron) 的集成测试套件。通过 gRPC、HTTP、JSON-RPC API 测试智能合约、质押、多签等功能。

---

## 前置条件

| 要求 | 说明 |
|------|------|
| **JDK 8**（x86/Intel） | 推荐。项目目标 `sourceCompatibility = 1.8` |
| **JDK 17**（ARM/Apple Silicon） | 使用 Rosetta 或 ARM 兼容的 JDK 8 |
| **Gradle** | 通过 `./gradlew` 包装器使用，无需单独安装 |
| **操作系统** | Linux 或 macOS。Windows 不受官方支持 |
| **磁盘** | 约 2 GB（java-tron 数据目录） |
| **内存** | 至少 4 GB 可用（java-tron 本身需要约 2 GB 堆内存） |

macOS 安装 JDK 8：

```bash
brew install --cask temurin@8
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8)
```

Linux (Ubuntu/Debian)：

```bash
sudo apt install openjdk-8-jdk
```

---

## 5 分钟快速开始

### 第 1 步：下载 java-tron

下载最新版本（GreatVoyage-v4.8.1 或更新）：

```bash
wget https://github.com/tronprotocol/java-tron/releases/download/GreatVoyage-v4.8.1/java-tron-GreatVoyage-v4.8.1.zip
unzip java-tron-GreatVoyage-v4.8.1.zip
cd java-tron-GreatVoyage-v4.8.1
```

### 第 2 步：配置私有网络

使用仓库提供的测试配置：

```bash
cp <system-test-repo>/docs/config.conf ./config.conf
```

关键配置项（已在 `docs/config.conf` 中预设）：

```hocon
# 端口（必须与 testng.conf 匹配）
node.rpc.port = 50051
node.http.fullNodePort = 8090

# 快速维护周期（20 秒而非 6 小时）
block.maintenanceTimeInterval = 20000
block.needSyncCheck = false

# 启用测试所需功能
vm.supportConstant = true
vm.saveInternalTx = true
node.rpc.reflectionService = true    # grpcurl 测试需要
```

### 第 3 步：启动节点

```bash
bin/FullNode -c config.conf --es --witness
```

- `--witness` — 启用出块（SR 模式）
- `--es` — 启用事件订阅

### 第 4 步：验证出块

等待 10-15 秒后检查：

```bash
curl -s http://127.0.0.1:8090/wallet/getnowblock | python3 -m json.tool | grep number
```

应该看到递增的区块号。如果区块号一直为 0，检查 `config.conf` 中 `localwitness` 的私钥是否与创世区块的见证人地址匹配。

### 第 5 步：运行测试

在 `system-test/` 目录下：

```bash
# 冒烟测试（约 5 分钟，快速验证环境正确）
./gradlew smokeTest

# 通过后可以跑更完整的测试
./gradlew singleNodeBuild    # 仅单节点测试，约 40 分钟
./gradlew dailyBuild          # 全量回归，约 1.5 小时（建议双节点）
```

---

## 工具安装

完整测试套件需要两个外部工具。首次运行测试时会自动下载：

```bash
./gradlew downloadTools
```

### 手动安装（网络受限环境）

如果 GitHub 下载超时（国内常见），手动下载后放到指定目录。

#### grpcurl (v1.8.9)

下载地址：https://github.com/fullstorydev/grpcurl/releases/tag/v1.8.9

```bash
# macOS (Apple Silicon)
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_osx_arm64.tar.gz
tar xzf grpcurl_1.8.9_osx_arm64.tar.gz

# macOS (Intel)
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_osx_x86_64.tar.gz
tar xzf grpcurl_1.8.9_osx_x86_64.tar.gz

# Linux (x86_64)
wget https://github.com/fullstorydev/grpcurl/releases/download/v1.8.9/grpcurl_1.8.9_linux_x86_64.tar.gz
tar xzf grpcurl_1.8.9_linux_x86_64.tar.gz

# 放到指定位置
cp grpcurl <system-test>/gRPCurl/grpcurl
chmod +x <system-test>/gRPCurl/grpcurl
```

#### solc (tronprotocol fork v0.8.26)

下载地址：https://github.com/tronprotocol/solidity/releases/tag/tv_0.8.26

```bash
# Linux
wget -O solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-static-linux

# macOS
wget -O solc https://github.com/tronprotocol/solidity/releases/download/tv_0.8.26/solc-macos

# 放到指定位置
cp solc <system-test>/solcDIR/solc
chmod +x <system-test>/solcDIR/solc

# 验证
./solcDIR/solc --version
# 应输出: solc.tron, the solidity compiler ... Version: 0.8.26+...
```

> **注意**：必须使用 tronprotocol fork 版本的 solc，标准 Ethereum solc 不支持 TVM 特有的操作码。

---

## 测试命令一览

| 命令 | 说明 | 测试数 | 时间 |
|------|------|--------|------|
| `./gradlew smokeTest` | 核心功能验证 | ~30 | ~5 分钟 |
| `./gradlew stest` | 标准 TestNG 套件 | ~200 | ~20 分钟 |
| `./gradlew singleNodeBuild` | 仅单节点测试 | ~260 | ~40 分钟 |
| `./gradlew dailyBuild` | 全量日常回归 | ~1685 | ~1.5 小时 |
| `./gradlew fuzzTest` | 属性测试 (jqwik) | 可变 | ~10 分钟 |
| `./gradlew lint` | Checkstyle 代码检查 | -- | ~1 分钟 |

### 运行单个测试类

```bash
# 编辑 debug-test.xml 指定要跑的类
./gradlew debugTest

# 或使用脚本
./run-test.sh SecurityOverflowTest
```

---

## 单节点 vs 多节点

| 模式 | 测试覆盖 | 需要的节点数 |
|------|----------|-------------|
| 单节点 | 57% (~260 个类) | 1 个 FullNode |
| 多节点 | 100% (~455 个类) | 2 个 FullNode (SR1 + SR2) |

单节点环境中，需要多节点的测试会**自动跳过**（通过 `@MultiNode` 注解检测），不会报错。

多节点搭建详见 [NETWORK.md](NETWORK.md)。

---

## 常见问题

详见 [FAQ.md](FAQ.md)。

快速排查：

| 现象 | 解决方案 |
|------|---------|
| 区块号一直为 0 | 检查 `localwitness` 私钥和 `needSyncCheck = false` |
| `Connection refused` | 节点未启动或端口不匹配 |
| grpcurl 测试全部失败 | 配置 `node.rpc.reflectionService = true` |
| 合约部署失败 | 确认 solc 在 `solcDIR/solc` 且可执行 |
| MongoDB 测试跳过 | 正常现象，MongoDB 是可选的 |
| 内存不足 | 改用 `singleNodeBuild` 或减少线程数 |

---

## 相关文档

| 文档 | 说明 |
|------|------|
| [README (中文)](../README_CN.md) | 项目概览 |
| [网络拓扑](NETWORK.md) | 单节点/多节点组网、端口映射、Docker |
| [架构](ARCHITECTURE.md) | 测试基类、Helper 分解、执行模型 |
| [代码规范](STYLE_GUIDE.md) | 命名规则、断言模式 |
| [常见问题](FAQ.md) | 高频问题与解决方案 |
