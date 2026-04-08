// SPDX-License-Identifier: MIT
pragma solidity ^0.8.6;

contract OverflowTest {

    // These functions will revert in Solidity 0.8+ due to built-in overflow checks

    function addOverflow() external pure returns (uint256) {
        uint256 max = type(uint256).max;
        uint256 result = max + 1; // Should revert
        return result;
    }

    function subUnderflow() external pure returns (uint256) {
        uint256 zero = 0;
        uint256 result = zero - 1; // Should revert
        return result;
    }

    function mulOverflow() external pure returns (uint256) {
        uint256 max = type(uint256).max;
        uint256 result = max * 2; // Should revert
        return result;
    }

    // Using unchecked block - wraps around without reverting
    function uncheckedAddOverflow() external pure returns (uint256) {
        unchecked {
            uint256 max = type(uint256).max;
            uint256 result = max + 1; // Wraps to 0
            return result;
        }
    }

    function uncheckedSubUnderflow() external pure returns (uint256) {
        unchecked {
            uint256 zero = 0;
            uint256 result = zero - 1; // Wraps to type(uint256).max
            return result;
        }
    }

    function uncheckedMulOverflow() external pure returns (uint256) {
        unchecked {
            uint256 large = type(uint128).max;
            uint256 result = large * large; // Wraps
            return result;
        }
    }

    // Safe operations for comparison
    function safeAdd(uint256 a, uint256 b) external pure returns (uint256) {
        return a + b;
    }

    function safeSub(uint256 a, uint256 b) external pure returns (uint256) {
        return a - b;
    }

    function getMaxUint256() external pure returns (uint256) {
        return type(uint256).max;
    }

    function getMaxUint128() external pure returns (uint128) {
        return type(uint128).max;
    }
}
