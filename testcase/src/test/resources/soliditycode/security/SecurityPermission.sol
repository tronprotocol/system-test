// SPDX-License-Identifier: MIT
pragma solidity ^0.8.6;

contract PermissionTest {
    address public owner;
    uint256 public protectedValue;
    uint256 public publicValue;
    mapping(address => bool) public admins;

    event ValueChanged(uint256 oldValue, uint256 newValue, address changedBy);
    event OwnershipTransferred(address oldOwner, address newOwner);

    modifier onlyOwner() {
        require(msg.sender == owner, "Not owner");
        _;
    }

    modifier onlyAdmin() {
        require(admins[msg.sender] || msg.sender == owner, "Not admin");
        _;
    }

    constructor() {
        owner = msg.sender;
    }

    // Only owner can call this
    function setProtectedValue(uint256 _value) external onlyOwner {
        emit ValueChanged(protectedValue, _value, msg.sender);
        protectedValue = _value;
    }

    // Only admin or owner can call this
    function setAdminValue(uint256 _value) external onlyAdmin {
        protectedValue = _value;
    }

    // Anyone can call this
    function setPublicValue(uint256 _value) external {
        publicValue = _value;
    }

    function addAdmin(address _admin) external onlyOwner {
        admins[_admin] = true;
    }

    function removeAdmin(address _admin) external onlyOwner {
        admins[_admin] = false;
    }

    function transferOwnership(address newOwner) external onlyOwner {
        require(newOwner != address(0), "Zero address");
        emit OwnershipTransferred(owner, newOwner);
        owner = newOwner;
    }

    // Returns msg.sender
    function getMsgSender() external view returns (address) {
        return msg.sender;
    }

    // Returns tx.origin
    function getTxOrigin() external view returns (address) {
        return tx.origin;
    }
}

// Contract that calls PermissionTest to demonstrate tx.origin vs msg.sender
contract PermissionCaller {
    PermissionTest public target;

    constructor(address _target) {
        target = PermissionTest(_target);
    }

    // When this calls PermissionTest, msg.sender will be this contract
    // but tx.origin will be the EOA that called this function
    function callGetMsgSender() external view returns (address) {
        return target.getMsgSender();
    }

    function callGetTxOrigin() external view returns (address) {
        return target.getTxOrigin();
    }

    // Try to call the owner-only function (should fail because msg.sender is this contract)
    function trySetProtectedValue(uint256 _value) external {
        target.setProtectedValue(_value);
    }

    function trySetPublicValue(uint256 _value) external {
        target.setPublicValue(_value);
    }
}
