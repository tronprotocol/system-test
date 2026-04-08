// SPDX-License-Identifier: MIT
pragma solidity ^0.8.26;

contract RequireErrorTest {
    enum ErrorType {
        Unknown,       // 0
        Validation,    // 1
        Permission,    // 2
        Logic          // 3
    }
    error NotPositive(uint256 code, string msg, ErrorType errType);
    function double(uint256 x) public pure returns (uint256) {
        require(x > 0, NotPositive(1001, "x must be positive", ErrorType.Validation));
        return x * 2;
    }
}