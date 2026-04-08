# 测试组网方式调研：以太坊 vs java-tron

## 背景

java-tron 的 system-test 需要启动外部 FullNode 进程，通过 gRPC/HTTP 连接进行黑盒测试。完整 dailyBuild（455 个测试类）中 43% 需要多节点环境（Solidity、PBFT、第二 FullNode）。搭建和维护这个测试环境是一个持续的痛点。

本文调研以太坊（go-ethereum）的测试架构，分析 java-tron 的差距和可能的改进方向。

---

## 以太坊的五层测试架构

### 第 1 层：SimulatedBackend — 进程内嵌入节点

最轻量的方式。`simulated.NewBackend()` 在测试进程内启动一个完整的以太坊节点：

- P2P 关闭（`NoDiscovery: true, MaxPeers: 0`）
- 用 `SimulatedBeacon` 模拟共识层
- **按需出块**：调用 `Commit()` 才出块，不需要等待时间间隔
- 返回标准 `ethclient.Client` 接口，测试代码与连接真实节点无差异

```go
// 一行代码启动完整节点
sim := simulated.NewBackend(types.GenesisAlloc{
    testAddr: {Balance: big.NewInt(10000000000000000)},
})
client := sim.Client()

// 发交易 + 立即出块
client.SendTransaction(ctx, tx)
sim.Commit()  // 不等 12 秒，立即产生新块
```

**关键设计**：go-ethereum 的节点是一个可嵌入的库，`node.Node` + `eth.Ethereum` 可以在任意 Go 进程中实例化。

### 第 2 层：SimulatedBeacon — 模拟共识与可控最终性

替代真实的共识客户端（Lighthouse/Prysm 等），通过 Engine API 驱动出块：

1. 调用 `forkchoiceUpdated` 触发区块构建
2. 调用 `getPayload` 获取构建好的区块
3. 调用 `newPayload` 验证并导入区块
4. 再次调用 `forkchoiceUpdated` 设为规范链头

**最终性处理**（对应 TRON 的"固化"）：
- `devEpochLength = 32`：每 32 个块更新一次 `FinalizedBlockHash`
- `SafeBlockHash` 始终等于 `HeadBlockHash`（开发模式简化）
- 两种模式：`period=0` 手动出块（确定性测试），`period>0` 自动出块

```go
n, ethservice, simBeacon := startSimulatedBeaconEthService(t, genesis, 0)
// period=0: 只在显式调用时出块，测试完全可控
```

### 第 3 层：Mock Peers — 协议级模拟

P2P 协议和同步测试使用 mock peer 对象，不走真实网络连接：

- `downloadTesterPeer`：模拟拥有不同链状态的远程节点
- `testHandler`：创建真实区块链 + handler，但用 mock 交易池
- 测试同步、区块传播、交易广播等逻辑，无需真实 TCP 连接

```go
type downloadTester struct {
    chain      *core.BlockChain
    downloader *Downloader
    peers      map[string]*downloadTesterPeer  // 模拟的对等节点
}
// 注册 mock peer 并测试同步
tester.newPeer("peer1", protocol, blocks)
```

### 第 4 层：Hive — Docker 多客户端集成测试

以太坊官方跨客户端测试框架（github.com/ethereum/hive）：

**架构**：
- **Simulator**（Go 程序，打包为 Docker）：编排测试场景
- **Client**（Docker 镜像）：各种以太坊实现（geth/besu/nethermind/erigon/reth 等）
- **Controller**：提供 HTTP API，Simulator 通过它启停 Client 容器

**核心 Simulator**：

| Simulator | 用途 |
|-----------|------|
| `ethereum/engine` | Engine API 合规测试，CLMocker 模拟共识层 |
| `ethereum/sync` | 跨客户端链同步（source → sink 模式） |
| `ethereum/consensus` | EVM 状态转换测试 |
| `ethereum/rpc-compat` | JSON-RPC API 兼容性 |
| `devp2p` | P2P 协议一致性 |

**CLMocker**（Hive 中的共识模拟器）：
- `SlotsToFinalized = 2`：区块产出 2 个 slot 后标记为 finalized
- 可同时驱动多个执行层客户端
- 跟踪 `ExecutedPayloadHistory`、`HeadHashHistory`

**sync simulator 的多节点模式**：
1. 启动 source 客户端，导入预构建的链
2. 通过 `forkchoiceUpdated` 标记为已同步
3. 获取 source 的 enode URL
4. 启动 sink 客户端，`HIVE_BOOTNODE` 指向 source
5. 轮询 sink 直到同步到预期的 head hash（60s 超时）

### 第 5 层：Kurtosis — 全真 Devnet

完整的执行层 + 共识层多节点网络（github.com/ethpandaops/ethereum-package）：

```yaml
participants:
  - el_type: geth
    cl_type: lighthouse
    count: 3
  - el_type: nethermind
    cl_type: teku
network_params:
  seconds_per_slot: 6
  genesis_delay: 120
  electra_fork_epoch: 5
```

- 生成 EL 和 CL 的创世状态
- 分发验证者密钥到各 CL 节点
- 支持 Docker（本地）和 Kubernetes（CI/生产）
- 集成 **Assertoor** 做集成断言（交易包含、finality 验证）
- 集成 **Attacknet** 做混沌工程（网络分区、节点崩溃）
- 以太坊基金会用此框架测试硬分叉升级

---

## java-tron 的现状

### 当前 system-test 架构

```
[测试进程 (Gradle/TestNG)]
       │
       ├── gRPC ──→ FullNode :50051 (主节点)
       ├── gRPC ──→ FullNode :50052 (第二节点)
       ├── gRPC ──→ Solidity :50053 (固化数据查询)
       ├── gRPC ──→ PBFT    :50072 (PBFT 查询)
       ├── HTTP ──→ 各节点 HTTP 端口
       └── JSON-RPC ──→ :50545
```

- 测试进程是纯客户端，不包含任何节点逻辑
- 必须预先启动外部 FullNode 进程
- 出块依赖真实时间间隔（3 秒）
- 固化依赖真实 DPoS 共识（2/3 SR 确认）
- `@BeforeSuite` 通过提案机制设置链参数（约 2 分钟）

### 对比差距

| 能力 | go-ethereum | java-tron |
|------|------------|-----------|
| 节点嵌入测试进程 | `simulated.NewBackend()` | 不支持 |
| 共识可模拟 | `SimulatedBeacon` | 不支持，DPoS 写死在节点内 |
| 按需出块 | `sim.Commit()` | 不支持，必须等 3s 出块间隔 |
| 最终性可控 | `devEpochLength` 可配 | 不支持，固化依赖真实 2/3 SR |
| 测试所需外部进程数 | 0 | 1-2 个 FullNode |
| 单次测试耗时 | 毫秒级 | 秒-分钟级（等出块+固化） |
| 完整测试环境搭建 | 一行代码 | 多节点配置+组网+等待提案生效 |

### 根因分析

**go-ethereum 能做到是因为节点被设计为库**：
- `node.Node`、`eth.Ethereum`、`core.BlockChain` 是可组合的模块
- 共识层通过 Engine API 解耦，可以用 mock 替换
- P2P 层可选禁用

**java-tron 做不到是因为节点是单体应用**：
- `FullNode` 是唯一入口，没有暴露可编程的内部接口
- DPoS 共识紧耦合在节点生命周期中
- 没有"开发模式"或"测试模式"的出块/固化控制

---

## dailyBuild 多节点需求

### 455 个测试类的节点依赖分布

| 类别 | 数量 | 占比 |
|------|------|------|
| 仅需单 FullNode | 260 | 57% |
| 需要 Solidity (gRPC :50053) | 145 | 32% |
| 需要第二 FullNode (gRPC :50052) | 104 | 23% |
| 需要 PBFT (gRPC :50072) | 21 | 5% |
| 需要 HTTP Solidity/PBFT | 37 | 8% |
| **去重后：需要多节点** | **196** | **43%** |

### 端口映射（testng.conf）

**gRPC：**
```
fullnode[0]  = 127.0.0.1:50051   # 主 FullNode
fullnode[1]  = 127.0.0.1:50052   # 第二 FullNode
solidity[0]  = 127.0.0.1:50053   # Solidity
solidity[1]  = 127.0.0.1:50062   # RealSolidity
solidity[2]  = 127.0.0.1:50071   # PBFT (Node 1)
solidity[3]  = 127.0.0.1:50072   # PBFT (Node 2)
```

**HTTP (httpnode.ip.list)：**
```
[0] 8090   # Node 1 FullNode
[1] 8093   # Node 2 FullNode
[2] 8097   # httpSoliditynode（测试代码中的变量名）
[3] 8091   # （用途待确认）
[4] 8098   # httpPbftNode（测试代码中的变量名）
[5] 8094   # Node 2 Solidity
[6] 8099   # Node 2 PBFT
```

**JSON-RPC：**
```
[0] 50545  # FullNode
[1] 50555  # Solidity
[2] 50546  # PBFT
```

### 组网关键约束

TRON 使用 DPoS，固化需要超过 2/3 的 SR 确认。当前 genesis block 配置 2 个 witness：
- 固化阈值 = 2/3 × 2 = 1.33，即**两个 witness 都必须确认**
- 单节点配置中 `localwitness` 包含两个私钥，同一进程内两个 SR 轮流出块并互相确认，固化正常工作
- 拆成两个节点后，每个节点各持一个 SR 私钥，**两节点必须互相连通并同步**，否则固化卡住，所有依赖 Solidity 的测试超时失败

---

## 可能的改进方向

### 短期：正确搭建多节点测试环境

在 java-tron 架构不变的前提下，用两个 FullNode 实例组网：

- Node 1（witness key1）：对外暴露 FullNode + Solidity + PBFT + JSON-RPC 端口
- Node 2（witness key2）：对外暴露第二套 FullNode + Solidity + PBFT 端口
- 两节点通过 P2P 互相发现和同步
- 端口对齐 testng.conf 的期望

### 中期：Docker 编排

参考 Hive 的模式，用 docker-compose 管理测试环境：

- 节点作为 Docker 容器启动
- 测试进程通过 network_mode: host 连接
- CI 中自动化搭建/销毁

### 长期：java-tron 可测试性改造

参考 go-ethereum 的 SimulatedBackend 模式：

1. **提供进程内嵌入能力**：让 FullNode 核心模块可以在测试进程中实例化
2. **共识层解耦**：提供测试用的 DPoS mock，支持按需出块和可控固化
3. **开发模式**：添加 `--dev` 启动参数，单节点即可满足所有 API（包括 Solidity/PBFT 查询），无需组网
