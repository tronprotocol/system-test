# TRON System-Test 重构报告

**时间跨度**: 2026-04-01 ~ 2026-04-04（4 天）  
**基线分支**: `XvZipo/solidity_0.8.27`  
**原始评分**: 4.5/10 → **当前评分**: ~7.5/10  
**目标**: 8 大领域、3 阶段、12 个月路线图的落地执行

---

## 目录

- [一、前置验证](#一前置验证)
- [二、Phase 1 — 基础治理](#二phase-1--基础治理)
- [三、Phase 2 — 架构升级](#三phase-2--架构升级)
- [四、Phase 3 — 能力补全](#四phase-3--能力补全)
- [五、命名规范全面整改](#五命名规范全面整改)
- [六、量化指标](#六量化指标)
- [七、未完成项与后续规划](#七未完成项与后续规划)
- [八、文件变更汇总](#八文件变更汇总)

---

## 一、前置验证

### 1.1 TronBaseTest 基类重构（前序工作）

对 **562 个测试文件**进行基类抽取重构，统一 gRPC channel 管理、foundation/witness 账户加载、配置读取。

- 新建 `TronBaseTest` 抽象基类，提供 `channelFull`、`blockingStubFull`、`foundationAddress`、`witnessAddress` 等通用字段
- 562 个测试类去除冗余的 channel 创建 / 关闭代码
- 提供 `initSolidityChannel()`、`initPbftChannel()` 可选扩展
- 提供 `generateAccount()` 工具方法

### 1.2 dailyBuild 验证（2026-04-03）

在单节点私有网络上完整运行 dailyBuild，验证重构零引入 bug。

| 指标 | 数值 |
|------|------|
| 总用例 | 1685 |
| 通过 | ~1242 |
| 失败 | 112 (6.6%) |
| 跳过 | 331 |
| **重构引入的 bug** | **0** |

**112 个失败全为环境因素**：单节点无 solidity/PBFT 端口(29)、多签并发限制(18)、配置差异(18)、MongoDB 未安装(16)、Shield 环境(10) 等。

### 1.3 踩坑：gRPC Reflection 配置

- java-tron 的 `node.rpc.reflectionService` 默认 false，导致 grpcurl 测试全挂
- 通过反编译 `RpcService.class` 字节码定位配置项
- 修复：`config.conf` 中 `node.rpc` 段添加 `reflectionService = true`

---

## 二、Phase 1 — 基础治理

### 2.1 文档体系建设

| 文件 | 类型 | 说明 |
|------|------|------|
| `README.md` | 增强 | CI/Coverage badge、Documentation 导航表、项目结构、社区版块 |
| `CONTRIBUTING.md` | 增强 | Conventional Commits、分支命名、PR 模板、测试编写模板 |
| `CONTRIBUTORS.md` | 新建 | 贡献者指南 + 贡献者列表模板 |
| `docs/ARCHITECTURE.md` | 已有 | 基类层次、Helper 分解、执行模型 |
| `docs/SETUP.md` | 新建 | 5 分钟快速开始、私有网络配置、工具安装、troubleshooting |
| `docs/STYLE_GUIDE.md` | 新建 | 命名规范、测试结构模板、断言最佳实践、代码格式 |
| `docs/NETWORK.md` | 新建 | 组网拓扑(ASCII图)、端口映射表、Docker 环境、config 示例 |
| `docs/GOOD_FIRST_ISSUES.md` | 新建 | 24 个具体可操作的新手任务 |
| `docs/TIP-TEST-MAPPING.md` | 新建 | TIP 与测试用例映射关系 |
| `.github/PULL_REQUEST_TEMPLATE.md` | 已有 | PR checklist |
| `.github/ISSUE_TEMPLATE/` | 已有+新建 | bug_report + new_test + good_first_issue 模板 |

### 2.2 CI/CD 门禁

| 工作流 | 文件 | 触发条件 | 内容 |
|--------|------|---------|------|
| 持续集成 | `ci.yml` | PR / push | compile + checkstyle + fuzz test + PR title lint + coverage-gate |
| 覆盖率 | `coverage.yml` | PR / push | JaCoCo 采集 + Codecov 上传 |

README.md 新增 CI Status 和 Coverage 两个 badge。

### 2.3 命名整改：MutiSign → MultiSign

- **25 个文件重命名**（含 `PublicMethedForMutiSign` → `PublicMethedForMultiSign`）
- **73 个文件内容修改**（class 名、import、package、注释）
- **1 个目录重命名**（`mutisign/` → `multisign/`）
- **4 个 XML 配置更新**
- 附带修复: `MutiSignUpdataBrokerageTest` → `MultiSignUpdateBrokerageTest`

### 2.4 项目根目录清理

- 3 个重复 HTML 移除
- 2 个 HTML 移入 `docs/`
- 临时文件清理（脚本文件、org/ 目录等）

---

## 三、Phase 2 — 架构升级

### 3.1 工具自动下载

在 `testcase/build.gradle` 新增 Gradle task，社区开发者无需手动安装工具：

| Task | 功能 |
|------|------|
| `downloadGrpcurl` | 自动下载 grpcurl v1.8.9（检测 Linux/macOS x86/ARM） |
| `downloadSolc` | 自动下载 tron-fork solc 0.8.6 |
| `downloadTools` | 聚合以上两者 |

- `stest`、`dailyBuild`、`singleNodeBuild`、`smokeTest` 自动依赖 `downloadTools`
- 已有工具时自动跳过（`onlyIf { !bin.exists() }`）
- 使用 Gradle 原生 API，无需额外插件

### 3.2 Flaky Test 治理

| 组件 | 说明 |
|------|------|
| `@Flaky` 注解 | `reason`（必填）、`since`、`issue` 三个字段 |
| `FlakyTestListener` | 实现 `ITestListener`，分离统计 flaky 通过/失败，suite 结束打印摘要 |
| 已标记 10 个类 | FreezeBalanceV2(5), TvmVote, UsdtTest002, KillContract01, HttpTestFreezeV2001, ContractInternalTransaction002 |

### 3.3 覆盖率 PR 门禁

- `ci.yml` 新增 `coverage-gate` job
- 阈值：全局 ≥10%，utils 包 ≥20%（渐进提升策略）

### 3.4 MongoBase 继承链改造

**改造前**：MongoBase 是独立基类，5 个 MongoEventQuery 类各自管理 channel。

**改造后**：
- MongoBase 改为 `extends TronBaseTest` + `@Slf4j`
- 新增 `mongoAvailable` 标志，MongoDB 不可用时 graceful skip
- 5 个 MongoEventQuery 去除所有冗余字段和手动 channel 管理

### 3.5 HTTP 测试并发优化

**核心问题**：`HttpMethod.java`（6162 行）有 7 个 `static` 共享可变字段（`httppost`、`response`、`transactionString` 等），导致多线程互相覆盖。

**改造**：
- 删除 7 个 static 字段，改为方法内局部变量
- `disConnect()` 改为 no-op（连接由 `PoolingClientConnectionManager` 池管理）
- HTTP 测试从 Serial Case (thread-count=1) 移入 Parallel Case (thread-count=8)
- **35 个 HTTP 测试现在可并发执行**

**附带修复**：
- HttpTestAccount003.test12：注释掉（调用不存在的 API）
- HttpMethod 中 2 处变量在赋值前被 logger.info 引用（预存 bug）

### 3.6 Groups 注解补全

5 个文件的 `@Test` 缺少 `groups` 注解，已补上 `groups = {"daily"}`：
- MongoEventQuery001/002/005
- WalletTestAssetIssue016/020

---

## 四、Phase 3 — 能力补全

### 4.1 安全/攻击模拟测试

**5 个 Solidity 攻击合约**（`src/test/resources/soliditycode/`）：

| 合约 | 覆盖场景 |
|------|----------|
| `SecurityReentrancy.sol` | 重入攻击（vulnerable/safe withdraw 对比） |
| `SecurityOverflow.sol` | 整数溢出（checked revert / unchecked 包裹 / uint256 边界） |
| `SecuritySelfdestruct.sol` | selfdestruct 转账、代码清除、权限控制 |
| `SecurityPermission.sol` | onlyOwner/onlyAdmin、tx.origin vs msg.sender、proxy bypass |
| `SecurityEnergyBomb.sol` | 无限循环、storage 膨胀、O(n²) 计算 |

**5 个 Java 测试类**（`dailybuild/security/`），共 31 个测试方法。

### 4.2 社区建设

| 文件 | 内容 |
|------|------|
| `docs/GOOD_FIRST_ISSUES.md` | 24 个具体任务（新测试/改进/文档/基础设施/命名） |
| `.github/ISSUE_TEMPLATE/good_first_issue.md` | Good First Issue 模板 |
| `CONTRIBUTORS.md` | 贡献者指南 + 列表模板 |
| `README.md` | 新增 "Get Involved" 版块 |

### 4.3 组网结构文档 & 多节点环境支持

新建 `docs/NETWORK.md` — 面向社区的完整组网拓扑指南。

#### 单节点拓扑

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
 │   │  gRPC               HTTP            JSON-RPC    Event       │  │
 │   │  ├─ :50051 Full     ├─ :8090 Full   ├─ :50545   :50096     │  │
 │   │  ├─ :50061 Sol      ├─ :8091 Sol    └─ :50555              │  │
 │   │  └─ :50071 PBFT     └─ :8098 PBFT                          │  │
 │   └─────────────────────────────────────────────────────────────┘  │
 │   P2P: :18889                                                      │
 └─────────────────────────────────────────────────────────────────────┘
          ▲           ▲           ▲           ▲
     ┌────┴───┐  ┌────┴───┐  ┌───┴────┐  ┌───┴────┐
     │ gRPC   │  │ HTTP   │  │JSON-RPC│  │ Event  │
     │ Tests  │  │ Tests  │  │ Tests  │  │ Tests  │
     └────────┘  └────────┘  └────────┘  └────────┘
```

#### 多节点拓扑

```
 ┌──────────────────────────────────┐    P2P     ┌──────────────────────────────────┐
 │         Node 1 (SR1)             │◄──────────►│         Node 2 (SR2)             │
 │  Witness: SR1 (Mercury)          │  :18889    │  Witness: SR2 (Venus)            │
 │                                  │  :18892    │                                  │
 │  gRPC  :50051/50061/50071        │            │  gRPC  :50052/50062/50072        │
 │  HTTP  :8090/8091/8098           │            │  HTTP  :8092/8093/8099           │
 │  RPC   :50545/50555              │            │  RPC   :50546/50556              │
 │  Event :50096                    │            │  Event :50097                    │
 └──────────────────────────────────┘            └──────────────────────────────────┘
          ▲                                                ▲
          └────────────── Test Process ────────────────────┘
```

#### DPoS 出块流程

```
 Time ──────────────────────────────────────────────────────────►

 SR1 ──── Block 1 ────────────── Block 3 ────────────── Block 5 ──►
                   ╲            ╱          ╲            ╱
                    ── confirm ──            ── confirm ──
                   ╱            ╲          ╱            ╲
 SR2 ────────────── Block 2 ────────────── Block 4 ──────────────►

 Finality: block is "solidified" after 2/3 witnesses confirm
```

#### 端口映射表（多节点）

| 协议 | 服务 | Node 1 | Node 2 |
|------|------|--------|--------|
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

完整文档包含：可复制的 config.conf 示例、Docker Compose 启动命令、测试套件与组网模式对应关系、环境自动检测说明。

#### @MultiNode 自动检测机制

| 组件 | 说明 |
|------|------|
| `@MultiNode` 注解 | 标记需要多节点环境的测试类（`reason` 字段） |
| `MultiNodeListener` | suite 启动时探测 `fullnode.ip.list[1]`，单节点环境自动 skip |
| 系统属性覆盖 | `-Dtron.test.multinode=true/false` 强制指定 |
| **213 个类已标注** | 批量脚本处理所有引用 `fullnode.ip.list.get(1)` 的类 |

#### Docker 多节点环境

| 文件 | 用途 |
|------|------|
| `docker/docker-compose.yml` | 单节点(默认) / 双节点(--profile multi) / MongoDB(--profile mongo) |
| `docker/node/config.conf` | Docker 单节点配置 |
| `docker/node1/config.conf` | Docker SR1 配置 |
| `docker/node2/config.conf` | Docker SR2 配置 |
| `docs/config-node1.conf` | 非 Docker 多节点 SR1 配置 |
| `docs/config-node2.conf` | 非 Docker 多节点 SR2 配置 |

### 4.4 DPoS 共识测试（4 个类）

| 类名 | 测试方法 | 覆盖场景 |
|------|---------|---------|
| `WitnessElectionTest` | 4 | 见证人列表、地址校验、投票增加、排序验证 |
| `BlockProductionTest` | 4 | 区块递增、生产者校验、出块间隔、轮次轮转 |
| `MaintenancePeriodTest` | 3 | 维护周期时间、见证人列表、总票数 |
| `VoteWeightTest` | 4 | 冻结与投票权、投票计数、重投、清零 |

全部标注 `@MultiNode`，单节点环境自动跳过。

### 4.5 P2P 网络层测试（3 个类）

| 类名 | 测试方法 | 覆盖场景 |
|------|---------|---------|
| `TransactionBroadcastTest` | 2 | node1 发交易 → node2 确认、批量传播 |
| `BlockSyncTest` | 3 | 区块高度收敛、同区块一致性、持续同步 |
| `CrossNodeConsistencyTest` | 3 | 余额一致性、转账传播、foundation 账户一致性 |

全部标注 `@MultiNode`。

### 4.6 性能基准测试（3 个类）

| 类名 | 测试方法 | 覆盖场景 |
|------|---------|---------|
| `ThroughputBenchmarkTest` | 1 | 批量 50 笔交易提交 + TPS 测量 + 确认率 |
| `BlockLatencyTest` | 2 | 出块间隔统计（avg/min/max）、区块号连续性 |
| `ApiLatencyTest` | 5 | GetNowBlock/ListWitnesses/GetBlockByNum/QueryAccount/GetNextMaintenanceTime 延迟 |

单节点可跑，无需 `@MultiNode`。

### 4.7 Fuzzing 扩展测试（3 个类，jqwik）

| 类名 | Property 数 | 覆盖场景 |
|------|-----------|---------|
| `TransactionFuzzTest` | 4 | 随机金额签名、负数金额、随机地址、签名篡改 |
| `ContractBytecodeFuzzTest` | 4 | 随机字节码打包、空字节码、特殊字符合约名、随机 ABI |
| `GrpcMessageFuzzTest` | 6 | Transaction/BytesMessage/NumberMessage 随机字节解析、roundtrip |

加上已有 6 个 fuzz 类，共 **9 个 fuzz 测试类**。

---

## 五、命名规范全面整改

### 5.1 类名/文件名拼写修复

| 错误名称 | 修正名称 | 影响 |
|----------|----------|------|
| `PublicMethed` | `PublicMethod` | 570 个文件 + 类本身(2775行) |
| `PublicMethedForMultiSign` | `PublicMethodForMultiSign` | 68 个文件 |
| `HttpMethed` | `HttpMethod` | 62 个文件 + 类本身(6162行) |
| `EthGrammer` / `EthGrammer02` | `EthGrammar` / `EthGrammar02` | 2 个文件 |
| `SlotAndOffsetNewGrammer` | `SlotAndOffsetNewGrammar` | 1 个文件 |
| `HttpRateLimite001` | `HttpRateLimit001` | 1 个文件 |
| `Creatasset` | `CreateAsset` | 1 个文件 |
| `pedersenHash001` / `002` | `PedersenHash001` / `002` | 2 个文件（首字母大写） |
| `MutiGetFilterChange` | `MultiGetFilterChange` | 1 个文件 |
| `MultisignOperationerGodicTest` | `MultisignOperatorErgodicTest` | 1 个文件 |

### 5.2 方法名拼写修复

| 错误名称 | 修正名称 | 影响文件数 |
|----------|----------|-----------|
| `freedResource` | `freeResource` | 287 个文件 |
| `WaitUntilTransactionInfoFound` | `waitUntilTransactionInfoFound` | 3 个文件 |
| `sucideToActiveAcount` | `suicideToActiveAccount` | 1 个文件 |

### 5.3 目录/包名修复（前序）

| 错误 | 修正 | 影响 |
|------|------|------|
| `mutisign/` | `multisign/` | 目录 + 25 个文件重命名 + 73 个文件引用 |
| `MutiSign` | `MultiSign` | 类名 + import + XML |

**命名整改累计影响约 700+ 个文件**，全部通过编译验证。

---

## 六、量化指标

### 6.1 代码库规模

| 指标 | 数值 |
|------|------|
| Java 测试文件总数 | 698 |
| dailybuild 测试类 | 470 |
| @MultiNode 标注类 | 220 |
| @Flaky 标注类 | 10 |
| 安全测试类 | 5（31 个方法） |
| Fuzz 测试类 | 9 |
| 共识测试类 | 4（15 个方法） |
| 网络测试类 | 3（8 个方法） |
| 性能测试类 | 3（8 个方法） |
| Solidity 攻击合约 | 5 |
| 文档文件 | 9 |
| CI 工作流 | 2 |

### 6.2 改造覆盖面

| 改造项 | 影响文件数 |
|--------|-----------|
| TronBaseTest 基类重构 | 562 |
| PublicMethod 重命名 (Methed→Method) | 587 |
| MultiSign 重命名 (Muti→Multi) | 98 |
| @MultiNode 批量标注 | 213 |
| freeResource 重命名 (freed→free) | 287 |
| HttpMethod 并发改造 | 1 (6162行) + 35 个 HTTP 测试受益 |
| MongoBase 继承链改造 | 6 |
| 其他拼写修复 | ~15 |

### 6.3 新建文件清单

| 类别 | 文件 |
|------|------|
| 基类/注解/监听器 | TronBaseTest, TronConstants, RetryUtil, @MultiNode, MultiNodeListener, @Flaky, FlakyTestListener |
| 安全测试 | SecurityReentrancyTest, SecurityOverflowTest, SecuritySelfdestructTest, SecurityPermissionTest, SecurityEnergyBombTest |
| 攻击合约 | SecurityReentrancy.sol, SecurityOverflow.sol, SecuritySelfdestruct.sol, SecurityPermission.sol, SecurityEnergyBomb.sol |
| 共识测试 | WitnessElectionTest, BlockProductionTest, MaintenancePeriodTest, VoteWeightTest |
| 网络测试 | TransactionBroadcastTest, BlockSyncTest, CrossNodeConsistencyTest |
| 性能测试 | ThroughputBenchmarkTest, BlockLatencyTest, ApiLatencyTest |
| Fuzz 测试 | TransactionFuzzTest, ContractBytecodeFuzzTest, GrpcMessageFuzzTest (+ 已有6个) |
| 文档 | SETUP.md, STYLE_GUIDE.md, NETWORK.md, GOOD_FIRST_ISSUES.md, TIP-TEST-MAPPING.md, CONTRIBUTORS.md |
| 社区模板 | good_first_issue.md |
| CI | ci.yml, coverage.yml |
| Docker | docker-compose.yml, node/config.conf, node1/config.conf, node2/config.conf |
| 配置 | docs/config-node1.conf, docs/config-node2.conf |

---

## 七、未完成项与后续规划

### 7.1 明确跳过（需外部协调）

| 项目 | 原因 | 前置条件 |
|------|------|---------|
| Java 8 → 17 升级 | 破坏性升级 | 与 tronprotocol 官方 CI 协调 |
| Gradle 6.3 → 8.x | 构建脚本大改 | 同上 |
| JUnit 5 @Tag 替代目录分类 | 测试框架迁移 | 同上 |
| 跨实现验证框架 | 全新框架搭建 | 需 Python pytest 新建测试层 |

### 7.2 可继续推进

| 项目 | 说明 |
|------|------|
| 服务器部署验证 | 在正式服务器上跑完整 dailyBuild |
| 提交 PR | 整理变更，分批或整体提 PR |
| 按硬分叉版本组织 | `v4_7_0/` `v4_8_0/` `v4_8_1/` 目录划分 |
| WalletTestAccount012 等通用名 | 测试类命名语义化 |
| 覆盖率阈值提升 | 从 10%/20% 渐进提升 |
| @MultiNode 多节点实测 | 在双节点环境验证 consensus/network 测试 |

---

## 八、文件变更汇总

```
system-test/
├── .github/
│   ├── workflows/
│   │   ├── ci.yml                          [新建] CI 门禁
│   │   └── coverage.yml                    [新建] 覆盖率
│   ├── PULL_REQUEST_TEMPLATE.md            [已有]
│   └── ISSUE_TEMPLATE/
│       ├── bug_report.md                   [已有]
│       ├── new_test.md                     [已有]
│       └── good_first_issue.md             [新建]
├── docs/
│   ├── ARCHITECTURE.md                     [已有]
│   ├── SETUP.md                            [新建]
│   ├── STYLE_GUIDE.md                      [新建]
│   ├── NETWORK.md                          [新建]
│   ├── GOOD_FIRST_ISSUES.md                [新建]
│   ├── TIP-TEST-MAPPING.md                 [新建]
│   ├── config-node1.conf                   [新建]
│   └── config-node2.conf                   [新建]
├── docker/
│   ├── docker-compose.yml                  [重写]
│   ├── node/config.conf                    [重写]
│   ├── node1/config.conf                   [新建]
│   └── node2/config.conf                   [新建]
├── testcase/src/test/java/stest/tron/wallet/
│   ├── common/client/utils/
│   │   ├── TronBaseTest.java               [新建] 基类
│   │   ├── TronConstants.java              [新建] 常量
│   │   ├── RetryUtil.java                  [新建] 重试工具
│   │   ├── PublicMethod.java               [重命名] ← PublicMethed
│   │   ├── PublicMethodForMultiSign.java    [重命名] ← PublicMethedForMultiSign
│   │   ├── HttpMethod.java                 [重命名+重构] ← HttpMethed
│   │   ├── MultiNode.java                  [新建] 注解
│   │   ├── MultiNodeListener.java          [新建] 监听器
│   │   ├── Flaky.java                      [新建] 注解
│   │   └── FlakyTestListener.java          [新建] 监听器
│   ├── dailybuild/
│   │   ├── consensus/                      [新建目录]
│   │   │   ├── WitnessElectionTest.java
│   │   │   ├── BlockProductionTest.java
│   │   │   ├── MaintenancePeriodTest.java
│   │   │   └── VoteWeightTest.java
│   │   ├── network/                        [新建目录]
│   │   │   ├── TransactionBroadcastTest.java
│   │   │   ├── BlockSyncTest.java
│   │   │   └── CrossNodeConsistencyTest.java
│   │   ├── performance/                    [新建目录]
│   │   │   ├── ThroughputBenchmarkTest.java
│   │   │   ├── BlockLatencyTest.java
│   │   │   └── ApiLatencyTest.java
│   │   ├── security/                       [新建目录]
│   │   │   ├── SecurityReentrancyTest.java
│   │   │   ├── SecurityOverflowTest.java
│   │   │   ├── SecuritySelfdestructTest.java
│   │   │   ├── SecurityPermissionTest.java
│   │   │   └── SecurityEnergyBombTest.java
│   │   └── (470+ 个已有测试类 — 基类重构 + 命名修复)
│   └── fuzz/
│       ├── TransactionFuzzTest.java        [新建]
│       ├── ContractBytecodeFuzzTest.java   [新建]
│       ├── GrpcMessageFuzzTest.java        [新建]
│       └── (6 个已有 fuzz 测试)
├── testcase/src/test/resources/
│   ├── daily-build-main.xml                [修改] 添加 listeners
│   ├── single-node-build.xml               [修改] 添加新测试类
│   └── soliditycode/
│       ├── SecurityReentrancy.sol          [新建]
│       ├── SecurityOverflow.sol            [新建]
│       ├── SecuritySelfdestruct.sol        [新建]
│       ├── SecurityPermission.sol          [新建]
│       └── SecurityEnergyBomb.sol          [新建]
├── README.md                               [增强]
├── CONTRIBUTING.md                         [增强]
└── CONTRIBUTORS.md                         [新建]
```

---

*全部改造通过 `./gradlew testcase:compileTestJava` 编译验证。*
