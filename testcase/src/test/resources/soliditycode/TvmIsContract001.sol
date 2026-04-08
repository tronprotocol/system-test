contract testIsContract{
bool public isContrct;
constructor () public {
    isContrct = address(this).isContract;
}
function testIsContractCommand(address a) public returns (bool) {
return (a.isContract);
}

function testIsContractView(address a) view public returns (bool) {
return (a.isContract);
}

function selfdestructContract(address payable a) public {
    selfdestruct(a);
}
function testConstructor() public returns(bool){
    return isContrct;
}

function testConstructorView() public view returns(bool){
    return isContrct;
}
}


contract A{
    bool public isContrct;
    constructor (address B) public {
        (bool success, bytes memory data) = address(B).call(abi.encodeWithSignature("testIsContractView(address)",address(this)));
        isContrct = success;

    }
    function testConstructorView() public view returns(bool){
        return isContrct;
    }
}

contract C{

    function create2(bytes memory code, uint256 salt) public returns(address){
        address addr;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        return addr;
    }

    function create(bytes memory code) public returns(address){
        address addr;
        assembly {
            addr := create(0, add(code, 0x20), mload(code))
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        return addr;
    }
}
