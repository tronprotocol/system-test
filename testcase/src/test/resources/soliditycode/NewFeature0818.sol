contract C {
//Disallow several ``indexed`` attributes for the same event parameter.
//    event e(uint indexed a, bytes3 indexed indexed s, bool indexed indexed indexed b);
//    event e2(uint indexed indexed a, bytes3 indexed s);

//Disallow usage of the ``indexed`` attribute for modifier parameters.
//    modifier mod1(uint indexed a) { _; }
    mapping(address a => uint b ) balance;
    function add(address to, uint count) public returns(uint){
        balance[to] += count;
        return balance[to];
    }

    function prevrandao() public view returns (uint256) {
        return block.prevrandao;
    }

    function assemblyPrevrandao() public view returns (uint ret) {
        assembly {
            ret := prevrandao()
        }
    }

//"selfdestruct" has been deprecated. The underlying opcode will eventually undergo breaking changes, and its use is not recommended.
//    function killme(address payable target) external {
//        selfdestruct(target);
//    }
}
