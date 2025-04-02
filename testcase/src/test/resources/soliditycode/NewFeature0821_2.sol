//pragma experimental solidity;  //this can only be set at the beginning
contract A
{
    uint256 public immutable variable;

    constructor()
    {
        B b = new B();
        try b.foo(variable = 1)
        {
            variable = 2;
        }
        catch Panic(uint)
        {
            variable = 3;
        }
        catch Error(string memory)
        {
            variable = 4;
        }
        catch
        {
            variable = 5;
        }
    }
}

contract B
{
    function foo(uint256) external pure
    {
        revert();
    }
}
// ====
// EVMVersion: >=byzantium



//contract C {
//    function max(bool isUint) pure public returns (uint8) {
//        return (isUint ? type(uint8) : type(int8)).max;
//    }
//}
// ----
// TypeError 9717: (98-109): Invalid mobile type in true expression.
// TypeError 3703: (112-122): Invalid mobile type in false expression.

//contract C {
//    function f() public pure returns (uint x) {
//        x = (true ? addmod : addmod)(3, 4, 5);
//    }
//}
// ----
// TypeError 9717: (81-87): Invalid mobile type in true expression.
// TypeError 3703: (90-96): Invalid mobile type in false expression.


//contract D {
//    function f() external {}
//    function g() external {}
//}
//contract C {
//    function f(bool c) public pure {
//        (c ? D.f : D.g);
//    }
//}
// ----
// TypeError 9717: (121-124): Invalid mobile type in true expression.
// TypeError 3703: (127-130): Invalid mobile type in false expression.



//contract C {
//    function f() external pure { }
//}
//
//contract D {
//    function g() external pure { }
//}
//
//contract A {
//    function test(bool b) public returns(bytes4) {
//        (b ? C.f : D.g).selector;
//    }
//}
// ----
// TypeError 9717: (179-182): Invalid mobile type in true expression.
// TypeError 3703: (185-188): Invalid mobile type in false expression.

//built-ins
//contract C {
//    function f(uint a) public {
//        (a == 1) ? abi.encodePacked : abi.encode;
//    }
//}

