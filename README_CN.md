# TRON System-Test

[English](README.md) | **中文**

[![CI](https://github.com/tronprotocol/system-test/actions/workflows/ci.yml/badge.svg)](https://github.com/tronprotocol/system-test/actions/workflows/ci.yml)
[![Coverage](https://github.com/tronprotocol/system-test/actions/workflows/coverage.yml/badge.svg)](https://github.com/tronprotocol/system-test/actions/workflows/coverage.yml)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](LICENSE)

[java-tron](https://github.com/tronprotocol/java-tron)（GreatVoyage-v4.8.1 Democritus）的系统级集成测试套件。覆盖全部 34 种系统合约、gRPC/HTTP/JSON-RPC API、TVM 指令、Stake 2.0、多签、DEX 和隐私功能，通过端对端测试验证运行中的 TRON 节点。

> 本仓库中的所有私钥均为随机生成的测试密钥，不包含任何敏感信息。

## 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 1.8 (x86) / 17 (ARM) | 与 java-tron 保持一致 |
| Gradle | 7.6.4 (wrapper) | 与 java-tron 保持一致 |
| TestNG | 6.14.3 | 主测试框架 |
| Protobuf | 3.25.5 | 协议序列化 |
| gRPC | 1.60.0 | 节点通信 |
| BouncyCastle | 1.78.1 | 密码学 |
| JaCoCo | 0.8.11 | 代码覆盖率 |
| jqwik | 1.7.4 | 属性测试 |

## 项目结构

```
system-test/
├── protocol/                          # Protobuf 定义 + gRPC 代码生成
├── testcase/                          # 主测试模块 (698 个测试类)
│   ├── build.gradle                   # 测试依赖与任务定义
│   └── src/test/
│       ├── java/stest/tron/wallet/
│       │   ├── account/               # 账户功能测试
│       │   ├── transfer/              # TRX 转账测试
│       │   ├── block/                 # 区块操作测试
│       │   ├── witness/               # SR/见证人测试
│       │   ├── committee/             # 治理提案测试
│       │   ├── multisign/             # 多签测试
│       │   ├── contract/              # 智能合约测试 (linkage + scenario)
│       │   ├── exchangeandtoken/      # DEX & TRC-10 测试
│       │   ├── dailybuild/            # 日常回归测试 (42 个子包)
│       │   │   ├── account/           # Freeze V2、委托、资源
│       │   │   ├── http/              # HTTP API 测试
│       │   │   ├── jsonrpc/           # JSON-RPC 接口测试
│       │   │   ├── tvmnewcommand/     # TVM 新指令测试
│       │   │   ├── security/          # 安全/攻击模拟测试
│       │   │   ├── consensus/         # DPoS 共识测试
│       │   │   ├── network/           # P2P 网络测试
│       │   │   ├── performance/       # 性能基准测试
│       │   │   └── ...                # 更多子包
│       │   ├── fulltest/              # 完整集成测试
│       │   ├── onlinestress/          # 压力测试
│       │   └── common/client/         # 共享工具
│       │       ├── utils/
│       │       │   ├── PublicMethod.java      # 核心测试辅助 (已委托至 Helper)
│       │       │   ├── AccountHelper.java     # 账户相关操作
│       │       │   ├── ContractHelper.java    # 合约相关操作
│       │       │   ├── ResourceHelper.java    # 资源相关操作
│       │       │   ├── TronBaseTest.java      # 测试基类
│       │       │   ├── TronConstants.java     # 常量定义
│       │       │   ├── TestAccountFactory.java # 测试账户工厂
│       │       │   ├── FailureClassifier.java # 失败自动分类
│       │       │   └── ...
│       └── resources/
│           ├── testng.xml             # 核心测试套件 (4 线程)
│           ├── daily-build.xml        # 日常回归套件
│           ├── testng.conf            # 主配置 (节点地址、测试账户)
│           └── soliditycode/          # 1259 个 Solidity 测试合约
├── docker/                            # Docker 环境
└── docs/                              # 文档
```

## 前置条件

- **JDK 8**（x86_64）或 **JDK 17**（ARM64），推荐 Temurin 发行版
- **Solidity 编译器**：tronprotocol fork solc v0.8.26，放在 `solcDIR/solc`（可通过 `./gradlew downloadTools` 自动下载）
- **运行中的 TRON 节点**：私有网络、Nile 测试网或 Shasta 测试网

### 测试网络选项

| 网络 | 用途 | 文档 |
|------|------|------|
| **私有网络** | 本地开发，快速迭代 | [搭建指南](https://tronprotocol.github.io/documentation-en/using_javatron/private_network/) |
| **Nile 测试网** | 前沿功能（领先于主网） | [水龙头: nileex.io](https://nileex.io) |
| **Shasta 测试网** | 参数与主网一致 | 仅通过 TronGrid 访问 |

## 快速开始

### 运行核心测试套件（4 线程并行）
```bash
./gradlew stest
```

### 运行日常回归构建（8 线程并行 + 串行）
```bash
./gradlew dailyBuild
```

### 运行冒烟测试（快速验证，约 5 分钟）
```bash
./gradlew smokeTest
```

### 运行仅单节点测试（无需多节点环境）
```bash
./gradlew singleNodeBuild
```

### 运行属性测试（Fuzz 测试）
```bash
./gradlew fuzzTest
```
使用 [jqwik](https://jqwik.net/) 对 Base58、ByteArray、Sha256、ECKey 和地址验证进行随机输入测试。

### 工具自动下载

首次运行时自动下载 grpcurl 和 solc：
```bash
./gradlew downloadTools
```

> **国内网络提示**：如果 GitHub 下载超时，请参考 [安装指南](docs/SETUP_CN.md#手动安装网络受限环境) 中的手动安装方式。

## 本地搭建 java-tron 环境运行 DailyBuild

如果需要在本地运行 `dailyBuild` 测试套件，需要搭建一个由 **2 个 Witness FullNode + 1 个 Solidity 节点** 组成的私有网络。项目已提供预配置文件：

```
testcase/src/test/resources/dailybuild-witness-conf/
├── witness1_config.conf   # Witness FullNode 1（出块节点，needSyncCheck=false）
├── witness2_config.conf   # Witness FullNode 2（出块节点，needSyncCheck=true，RocksDB + keystore）
└── solidity_config.conf   # Solidity 节点（无 witness 密钥）
```

### 端口映射

| 节点 | P2P 端口 | gRPC | HTTP Full | HTTP Solidity | HTTP PBFT | JSON-RPC Full | Prometheus |
|------|----------|------|-----------|---------------|-----------|---------------|------------|
| Witness 1 | 18889 | 50051 | 8090 | 8091 | 8098 | 50545 | 9528 |
| Witness 2 | 18892 | 50052 | 8093 | 8094 | 8099 | 50546 | 9527 |
| Solidity  | 18893 | 50053 | 8096 | 8097 | —   | 50547 | 9529 |

### 必需工具

- **Solidity 编译器 (solc)**：从 [tronprotocol/solidity/releases](https://github.com/tronprotocol/solidity/releases) 下载 tronprotocol 分支版本，将二进制文件放入 `solcDIR/` 目录，文件**必须**命名为 `solc`。当前使用版本：`v0.8.26`。
- **gRPCurl**：从 [fullstorydev/grpcurl/releases](https://github.com/fullstorydev/grpcurl/releases) 下载，将二进制文件放入 `gRPCurl/` 目录，文件**必须**命名为 `grpcurl`。
- **Slack 通知（可选）**：在测试执行环境中配置自定义 `slack` 命令，可自动接收失败用例的通知。

### MongoDB 事件插件（Witness 2 必需）

Witness 2 配置了 MongoDB 事件订阅，启动前需要：

1. 启动 MongoDB 服务
2. 从 [tronprotocol/event-plugin](https://github.com/tronprotocol/event-plugin) 下载事件插件
3. 编辑 `witness2_config.conf`，配置以下字段：
   - `event.subscribe.path` — 插件 zip 的绝对路径（如 `/path/to/plugin-mongodb-1.0.0.zip`）
   - `event.subscribe.server` — MongoDB 地址（如 `172.17.0.1:27017`）
   - `event.subscribe.dbconfig` — `数据库名|用户名|密码|版本号`

### 启动节点

```bash
# 先编译 java-tron
cd /path/to/java-tron
./gradlew build -x test

# 启动 Witness 1（首节点，负责出块）
java -jar build/libs/FullNode.jar --witness --es -c witness1_config.conf

# 启动 Witness 2（从 Witness 1 同步后参与出块）
java -jar build/libs/FullNode.jar --witness --es -c witness2_config.conf

# 启动 Solidity 节点（同步已确认区块）
java -jar build/libs/FullNode.jar --solidity -c solidity_config.conf
```

> `--es` 启用事件订阅服务。`--solidity` 以 Solidity 模式启动节点。

3 个节点全部启动并正常出块后，即可运行 dailyBuild 套件：

```bash
./gradlew dailyBuild
```

> 完整环境搭建指南（JDK、solc、grpcurl）请参考 [安装指南](docs/SETUP_CN.md)。

## 与 java-tron CI 的集成

本仓库由 java-tron 的 CI 流水线通过 [`system-test.yml`](https://github.com/tronprotocol/java-tron/blob/develop/.github/workflows/system-test.yml) 自动调用：

1. java-tron CI 构建 `FullNode.jar`
2. 检出本仓库的 `release_workflow` 分支
3. 将 `config-system-test.conf` 复制到 java-tron
4. 以后台进程启动 FullNode（`--witness` 模式）
5. 轮询 `http://localhost:8090/wallet/getblockbynum?num=1` 直到就绪（最长 5 分钟）
6. 执行 `./gradlew --info stest --no-daemon`
7. 超时时间：60 分钟

## 配置

测试配置从 `testcase/src/test/resources/testng.conf`（HOCON 格式）加载：

- **节点地址**：`fullnode.ip.list`、`solidityNode.ip.list`、`httpnode`
- **测试账户**：`foundationAccount.key1`、`witness.key1` 到 `witness.key5`
- **端口**：gRPC (50051)、HTTP (8090)、JSON-RPC (8545)

## 相关仓库

| 仓库 | 说明 |
|------|------|
| [java-tron](https://github.com/tronprotocol/java-tron) | TRON 全节点实现 |
| [protocol](https://github.com/tronprotocol/protocol) | Protobuf API 与消息定义 |
| [tron-docker](https://github.com/tronprotocol/tron-docker) | TRON 节点 Docker 自动化 |
| [tips](https://github.com/tronprotocol/tips) | TRON 改进提案 (127 个 TIP) |
| [documentation-en](https://github.com/tronprotocol/documentation-en) | 官方英文文档 |
| [trident](https://github.com/tronprotocol/trident) | 轻量级 Java SDK |
| [tronweb](https://github.com/tronprotocol/tronweb) | JavaScript/TypeScript API 库 |

## 文档

| 文档 | 说明 |
|------|------|
| [安装指南 (中文)](docs/SETUP_CN.md) | 5 分钟快速开始、私有网络搭建、工具安装 |
| [安装指南 (英文)](docs/SETUP.md) | Setup guide in English |
| [架构](docs/ARCHITECTURE.md) | 测试基础设施、基类、执行模型 |
| [网络拓扑](docs/NETWORK.md) | 单节点/多节点组网、端口映射、Docker 环境 |
| [代码规范](docs/STYLE_GUIDE.md) | 代码约定、命名规则、断言模式 |
| [常见问题](docs/FAQ.md) | 常见问题与解决方案 |
| [贡献指南](CONTRIBUTING.md) | 分支命名、提交格式、PR 流程 |

## 参与贡献

初次接触？以下是参与方式：

- **新手任务**：浏览 [精选入门任务](docs/GOOD_FIRST_ISSUES.md) 或在 GitHub 上按 [`good first issue`](https://github.com/tronprotocol/system-test/labels/good%20first%20issue) 筛选
- **贡献指南**：阅读 [CONTRIBUTING.md](CONTRIBUTING.md) 了解开发环境搭建、代码规范和 PR 流程
- **报告 Bug**：提交 [Bug 报告](https://github.com/tronprotocol/system-test/issues/new?template=bug_report.md)，附上复现步骤和环境信息
- **贡献者**：查看 [CONTRIBUTORS.md](CONTRIBUTORS.md) 了解贡献者列表和认可方式

## 许可证

本项目采用 [GNU 宽通用公共许可证 v3.0](LICENSE) 授权。
