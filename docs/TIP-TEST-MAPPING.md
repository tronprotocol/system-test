# TIP → Test Class Mapping

Maps TRON Improvement Proposals to their test coverage in system-test.

## Core Protocol TIPs

| TIP | Feature | Test Classes | Coverage |
|-----|---------|-------------|----------|
| TIP-10 | Stake 2.0 / FreezeV2 | `freezeV2/FreezeBalanceV2Test*` (8 files), `tvmnewcommand/tvmstake/*` (9 files), `tvmnewcommand/tvmFreeze/*` (3 files) | Good |
| TIP-157 | Resource Delegation V2 | `freezeV2/DelegateResourceEdgeTest001`, `freezeV2/DelegateResourceV2TimestampTest`, `account/DelegateResourceTimestampTest` | Moderate |
| TIP-271 | Vote Reward | `votereward/VoteRewardTest001` | Basic |
| TIP-467 | Rate Limiting | `ratelimit/RateLimite001`, `ratelimit/RateLimitEdgeTest001`, `http/HttpRateLimit001` | Moderate |
| TIP-541 | Market Transactions (DEX) | `assetmarket/*` (6 files) | Basic |
| TIP-543 | CancelAllUnfreezeV2 | Embedded in `freezeV2/FreezeBalanceV2Test*` | Implicit |
| TIP-586 | Dynamic Energy | `longexecutiontime/DynamicEnergyTest001`, `dynamicenergy/DynamicEnergyTest002` | Basic |

## TVM / Smart Contract TIPs

| TIP | Feature | Test Classes | Coverage |
|-----|---------|-------------|----------|
| TIP-37 | CREATE2 | `tvmnewcommand/create2/*` (27 files) | Excellent |
| TIP-43 | extCodeHash | `tvmnewcommand/extCodeHash/*` (11 files) | Excellent |
| TIP-44 | Batch Validate | `tvmnewcommand/batchValidateSignContract/*` (12 files) | Excellent |
| TIP-51 | isContract | `tvmnewcommand/isContract/*` (3 files) | Good |
| TIP-53 | Istanbul opcodes | `tvmnewcommand/istanbul/*` (3 files) | Good |
| TIP-54 | try/catch | `tvmnewcommand/tryCatch/*` (2 files) | Good |
| TIP-60 | Precompile clearABI | `tvmnewcommand/clearabi/*` (9 files) | Good |
| TIP-62 | ValidateMultiSign | `tvmnewcommand/validatemultisign/*` (3 files) | Good |
| — | TVM Freeze/Stake | `tvmnewcommand/tvmstake/*`, `tvmnewcommand/tvmFreeze/*` | Good |
| — | TVM Asset Issue | `tvmnewcommand/tvmassetissue/*` (5 files) | Good |
| — | New Grammar | `tvmnewcommand/newGrammar/*` (54 files) | Excellent |
| — | Trigger Constant | `tvmnewcommand/triggerconstant/*` (27 files) | Excellent |

## Token / Asset TIPs

| TIP | Feature | Test Classes | Coverage |
|-----|---------|-------------|----------|
| TIP-10 | TRC-10 tokens | `trctoken/*` (44 files), `assetissue/*` (25 files) | Excellent |
| — | Privacy/Shield | `zentoken/*` (11 files), `zentrc20token/*` (6 files) | Good |

## Governance TIPs

| TIP | Feature | Test Classes | Coverage |
|-----|---------|-------------|----------|
| — | Proposals | `committee/*` (4 files) | Basic |
| — | Witness/SR | `witness/*` (2 files), `account/BrokerageTest001` | Basic |
| — | Multi-signature | `multisign/*` (35 files) | Excellent |

## Coverage Legend
- **Excellent**: 10+ dedicated test files, comprehensive edge cases
- **Good**: 3-9 test files, core scenarios covered
- **Moderate**: 2-3 test files, basic + some edge cases
- **Basic**: 1-2 test files, happy path only
