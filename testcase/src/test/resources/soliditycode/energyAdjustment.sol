// SPDX-License-Identifier: MIT
pragma solidity ^0.8.17;

contract C {

    constructor() payable {
    }

//    constructor(uint amount, uint res) payable {
//        freezebalancev2(amount, res);
//    }

    receive() payable external {}

    function killme(address payable target) external {
        selfdestruct(target);
    }
}

contract D {
    constructor() public payable {}
    function getPredictedAddress(bytes32 salt) view public returns(address) {
        // This complicated expression just tells you how the address
        // can be pre-computed. It is just there for illustration.
        // You actually only need ``new D{salt: salt}(arg)``.
        address predictedAddress = address(uint160(uint(keccak256(abi.encodePacked(
                bytes1(0x41),
                address(this),
                salt,
                keccak256(abi.encodePacked(
                    type(C).creationCode
                ))
            )))));
        return predictedAddress;
    }

    function createDSalted(bytes32 salt) public returns(address) {
        C e = new C{salt: salt}();
        return address(e);
    }

    function testKill(address payable create2Add,address payable receiver,address payable receiver1) public {
        address(create2Add).call(abi.encodeWithSignature("killme(address)",receiver));
        address(create2Add).call(abi.encodeWithSignature("killme(address)",receiver1));
    }

    function test(int length, int offset) external {
        assembly {
            pop(nativevote(length, offset, length, offset))
        }
    }
}



