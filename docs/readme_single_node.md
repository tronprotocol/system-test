# Single-Node Configuration

## Configuration File

| Node | Configuration File |
|------|--------------------|
| SR-1 node (carrying 3 witnesses) | `config.conf` |

## Node Startup Parameters

SR-1 node requires the following parameters at startup:

```
--es --witness
```

## Required Tools

### Compiler (solc)

- Download: <https://github.com/tronprotocol/solidity/releases>
- Save to the `solcDIR/` directory of the system-test project, named `solc`
- Current version: `v0.8.26`

### gRPCurl

- Download: <https://github.com/fullstorydev/grpcurl/releases>
- Save to the `gRPCurl/` directory of the system-test project, named `grpcurl`
- Current version: `v1.8.9`


## Notes

### MongoDB-related test cases

The single-node event configuration uses the native configuration, which causes MongoDB-related test cases to fail. If you do not need to run MongoDB test cases, you can disable them.

### Rate-limiting test cases

Some rate-limiting test cases will fail in a single-node environment. If you do not need to run these test cases, you can disable them in advance.
