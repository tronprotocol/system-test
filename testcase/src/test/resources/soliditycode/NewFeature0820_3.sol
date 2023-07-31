library L {
    function externalFunction(uint a) external pure returns (uint) { return a * 1; }
    function publicFunction(uint b) public pure returns (uint) { return b * 2; }
    function internalFunction(uint c) internal pure returns (uint) { return c * 3; }
    event E();
    function f() internal { emit E(); }
}

//Also allow (external,public,internal) library functions in ``using for``.
contract C {
    using {L.externalFunction} for uint;
    using {L.publicFunction} for uint;
    using {L.internalFunction} for uint;

    function f() public pure returns (uint) {
        uint x = 1;
        return x.externalFunction();
    }

    function g() public pure returns (uint) {
        uint x = 1;
        return x.publicFunction();
    }

    function h() public pure returns (uint) {
        uint x = 1;
        return x.internalFunction();
    }

//ABI: Include events in the ABI that are emitted by a contract but defined outside of it.
    event H();
    function i() public { L.f(); emit H(); }
}
// ----
// library: L
// f() -> 1
// g() -> 2
// h() -> 3



//mmutables: Disallow initialization of immutables in try/catch statements.
/*contract A
{
uint256 public immutable variable;

constructor()
{
B b;
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
}*/

