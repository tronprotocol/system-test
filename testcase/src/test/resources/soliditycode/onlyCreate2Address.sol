// SPDX-License-Identifier: GPL-3.0
pragma solidity >=0.7.0 <0.9.0;
contract D {
    constructor() payable {}
    function destroy(address payable inheritor) external {
        selfdestruct(inheritor);
    }
    uint a;

    function testOutOfEnergy() external {
        for (uint i = 0; i < 100; i++) {
            a = i;
        }
    }
}

contract C {
    function getPredictedAddress(bytes32 salt) view public returns(address) {
        // This complicated expression just tells you how the address
        // can be pre-computed. It is just there for illustration.
        // You actually only need ``new D{salt: salt}(arg)``.
        address predictedAddress = address(uint160(uint(keccak256(abi.encodePacked(
                bytes1(0x41),
                address(this),
                salt,
                keccak256(abi.encodePacked(
                    type(D).creationCode
                ))
            )))));
        return predictedAddress;
    }

    function createDSalted(bytes32 salt) public returns(address) {
        D e = new D{salt: salt}();
        return address(e);
    }

    function createDeployEf(bytes memory code) public returns(address addr){
        address addr;
        assembly {
            addr := create(0, add(code, 0x20), mload(code))
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        return addr;
    }

    function create2DeployEf(bytes memory code,uint256 salt) public returns(address addr){
        address addr;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        return addr;
    }
}