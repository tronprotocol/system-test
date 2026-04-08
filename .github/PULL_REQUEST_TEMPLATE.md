## What does this PR do?
<!-- Brief description of the changes. -->

## Why are these changes needed?
<!-- Link to issue or explain the motivation. -->

## Type of Change
- [ ] New test(s)
- [ ] Test infrastructure / utilities
- [ ] Bug fix (test was incorrect)
- [ ] Documentation
- [ ] CI/CD

## Test Results
- [ ] `./gradlew compileTestJava` passes
- [ ] `./gradlew fuzzTest` passes (if touching utils)
- [ ] Relevant test suite runs successfully

## Checklist
- [ ] Tests extend `TronBaseTest` (not standalone channel management)
- [ ] Tests have `groups` annotation (smoke/daily/contract/etc.)
- [ ] No hardcoded private keys with real funds
- [ ] Helper classes used instead of raw gRPC calls where possible
