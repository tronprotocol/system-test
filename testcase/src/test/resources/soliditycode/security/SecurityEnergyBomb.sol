// SPDX-License-Identifier: MIT
pragma solidity ^0.8.6;

contract EnergyBomb {
    uint256 public counter;
    uint256[] public data;

    // Unbounded loop that will exhaust energy
    function infiniteLoop() external {
        while (true) {
            counter++;
        }
    }

    // Loop with configurable iterations
    function boundedLoop(uint256 iterations) external {
        for (uint256 i = 0; i < iterations; i++) {
            counter++;
        }
    }

    // Expanding storage - very expensive in energy
    function storageExpansion(uint256 count) external {
        for (uint256 i = 0; i < count; i++) {
            data.push(i);
        }
    }

    // Recursive computation that is expensive
    function expensiveComputation(uint256 n) external pure returns (uint256) {
        uint256 result = 1;
        for (uint256 i = 0; i < n; i++) {
            for (uint256 j = 0; j < n; j++) {
                result = (result * 31 + i + j) % (type(uint256).max);
            }
        }
        return result;
    }

    function getCounter() external view returns (uint256) {
        return counter;
    }

    function getDataLength() external view returns (uint256) {
        return data.length;
    }
}
