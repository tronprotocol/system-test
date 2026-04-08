

contract testConstantContract{
function testPure(uint256 x,uint256 y) public pure returns (uint256 z) {
uint256 i=1;
return i + x + y;
}

    function testCallValue() public payable returns (uint256 z) {
        require(msg.value > 3);
        return msg.value;
    }

    function killme(address payable target) external {
        selfdestruct(target);
    }

    constructor() payable {
    }
}