contract A {
    constructor() public payable{}

    function memoryCopy() external returns (bytes32 x) {
        assembly {
            mstore(0x20, 0x50)  // Store 0x50 at word 1 in memory
            mcopy(0, 0x20, 0x20)  // Copies 0x50 to word 0 in memory
            x := mload(0)    // Returns 32 bytes "0x50"
        }
    }

    mapping(address => bool) sentGifts;

    modifier nonreentrant {
        assembly {
            if tload(0) { revert(0, 0) }
            tstore(0, 1)
        }
        _;
        // Unlocks the guard, making the pattern composable.
        // After the function exits, it can be called again, even in the same transaction.
        assembly {
            tstore(0, 0)
        }
    }

    function tstoreAndTload() public {
        uint256 res;
        assembly {
            res := tload(0)
        }
        require(res == 0);
        assembly {
            tstore(0, 1)
            let x := tload(0)
            let y := add(x, 1)
            tstore(0, y)
            res := tload(0)
        }
        require(res == 2);
    }

    function claimGift() nonreentrant public {
        require(address(this).balance >= 1 trx);
        require(!sentGifts[msg.sender]);
        (bool success, ) = msg.sender.call{value: 1 trx}("");
        require(success);

        // In a reentrant function, doing this last would open up the vulnerability
        sentGifts[msg.sender] = true;
    }

    function onlyTstore()  public{
        assembly {
            tstore(0, 1)
        }
    }
}

contract B {
    event receiveCalled();
    event fallbackCalled();
    constructor() public payable{}
    fallback() external payable {
        emit fallbackCalled();
    }
    receive() external payable {
        emit receiveCalled();
    }
    function claimGiftOnce(address called_address) public {
        (bool success,) = called_address.call(abi.encodeWithSignature("claimGift()"));
        require(success);
    }

    function claimGiftTwice(address called_address) public {
        (bool success,) = called_address.call(abi.encodeWithSignature("claimGift()"));
        require(success);
        (bool success1,) = called_address.call(abi.encodeWithSignature("claimGift()"));
        require(success1);
    }
    function staticcallTstore(address called_address) public  {
        (bool success,) = called_address.staticcall(
            abi.encodeWithSignature("onlyTstore()")
        );
        require(!success);

    }

}


