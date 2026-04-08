contract Factory {
    event Deployed(address addr, uint256 salt, address sender);
    function deploy(bytes memory code, uint256 salt) public returns(address){
        address addr;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        emit Deployed(addr, salt, msg.sender);
        return addr;
    }

    event Deployed(address addr, bytes32 salt, address sender);
        function deploy(bytes memory code, bytes32 salt) public returns(address){
            address addr;
            assembly {
                addr := create2(0, add(code, 0x20), mload(code), salt)
                if iszero(extcodesize(addr)) {
                    revert(0, 0)
                }
            }
            emit Deployed(addr, salt, msg.sender);
            return addr;
    }

    function create2WithParams(bytes32 salt,uint256 h) public returns(address) {
        toCreateWithConstructor e = new toCreateWithConstructor{salt: salt}(h);
        return address(e);
    }
}

contract FactoryBytes {
    event Deployed(address addr, bytes32 salt, address sender);
    function deploy(bytes memory code, bytes32 salt) public returns(address){
        address addr;
        assembly {
            addr := create2(0, add(code, 0x20), mload(code), salt)
            if iszero(extcodesize(addr)) {
                revert(0, 0)
            }
        }
        emit Deployed(addr, salt, msg.sender);
        return addr;
    }
}

contract TestConstract {
    uint public i;
    constructor () public {
    }
    function plusOne() public returns(uint){
        i++;
        return i;
    }
}


contract toCreateWithConstructor {
    uint public i;
    constructor (uint h) public {
        i = h;
    }
    function plusOne() public returns(uint){
        i++;
        return i;
    }
}