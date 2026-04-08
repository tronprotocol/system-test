// SPDX-License-Identifier: MIT
pragma solidity ^0.8.6;

contract SelfdestructTarget {
    // This contract receives TRX from selfdestruct
    function getBalance() external view returns (uint256) {
        return address(this).balance;
    }

    fallback() external payable {}
}

contract SelfdestructTest {
    address public owner;
    uint256 public value;

    constructor() payable {
        owner = msg.sender;
    }

    function setValue(uint256 _value) external {
        value = _value;
    }

    function getBalance() external view returns (uint256) {
        return address(this).balance;
    }

    // Selfdestruct sending TRX to a target address
    function destroyAndSend(address payable target) external {
        require(msg.sender == owner, "Only owner");
        selfdestruct(target);
    }

    // Selfdestruct sending TRX to self
    function destroySelf() external {
        require(msg.sender == owner, "Only owner");
        selfdestruct(payable(address(this)));
    }

    fallback() external payable {}
}

contract SelfdestructFactory {
    event ContractCreated(address contractAddress);

    function createAndFund() external payable returns (address) {
        SelfdestructTest child = new SelfdestructTest{value: msg.value}();
        emit ContractCreated(address(child));
        return address(child);
    }

    function getBalance() external view returns (uint256) {
        return address(this).balance;
    }

    fallback() external payable {}
}
