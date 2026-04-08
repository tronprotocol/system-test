# 单节点配置说明

## 配置文件

| 节点 | 配置文件 |
|------|----------|
| SR-1 节点（拖 3 witness） | `config.conf` |

## 节点启动参数

SR-1 节点启动时需要添加参数：

```
--es --witness
```

## 依赖工具

### 编译器（solc）

- 下载地址：<https://github.com/tronprotocol/solidity/releases>
- 下载后保存至 system-test 项目的 `solcDIR/` 目录，命名为 `solc`
- 当前使用版本：`v0.8.26`

### gRPCurl

- 下载地址：<https://github.com/fullstorydev/grpcurl/releases>
- 下载后保存至 system-test 项目的 `gRPCurl/` 目录，命名为 `grpcurl`
- 当前使用版本：`v1.8.9`


## 注意事项

### mongo 相关用例

单节点 event 配置使用了 native 配置，会导致 mongo 相关用例失败。若无需关注 mongo 用例，可禁用此部分用例。

### 限速相关用例

部分限速用例在单节点环境下运行会失败。若不关注此类用例，可预先禁用此部分用例。


