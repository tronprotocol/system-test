pragma solidity ^0.8.0;

contract C {

    function depositForLog() public payable {
    bytes32 _id = hex"420042";
    bytes32 t1 = bytes32(uint256(uint160(msg.value)));
    bytes32 t2 = bytes32(uint256(uint160(msg.sender)));
    bytes32 t3 = bytes32(0x50cb9fe53daa9737b786ab3646f04d0150dc50ef4e75f59509d83667ad5adb20);

        assembly{
        log3(t1,0x20, t3, t2,_id)
        }
    }
}

