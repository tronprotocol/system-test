//bug fix: Code Generator: Correctly encode literals used in ``abi.encodeCall`` in place of fixed bytes arguments.

contract C {
    function removeSignature(bytes calldata x) external pure returns (bytes memory) {
        return x[4:];
    }
    function g(bytes2, bytes2, bytes2) public {}
    function h(uint16, uint16) public {}
    function f() public returns (bytes memory) {
        uint16 x = 0x1234;
        return this.removeSignature(abi.encodeCall(this.g, (0x1234, "ab", bytes2(x))));
    }
    function f2() public returns (bytes memory) {
        bytes2 x = 0x1234;
        return this.removeSignature(abi.encodeCall(this.h, (0x1234, uint16(x))));
    }
}

contract D {
    function f(uint256[] memory a, uint256[1] calldata b) public returns (bytes memory) {
        return abi.encode(a, b);
    }

    function g(uint256[] memory a, uint256[1] calldata b) external returns (bytes memory) {
        return f(a, b);
    }

    function h(uint256[] memory a, uint256[1] calldata b) external returns (uint256[] memory, uint256[1] calldata) {
        return (a, b);
    }
}

// ----
// f(uint256[],uint256[1]): 0x40, 0xff, 1, 0xffff -> 0x20, 0x80, 0x40, 0xff, 1, 0xffff
// g(uint256[],uint256[1]): 0x40, 0xff, 1, 0xffff -> 0x20, 0x80, 0x40, 0xff, 1, 0xffff
// h(uint256[],uint256[1]): 0x40, 0xff, 1, 0xffff -> 0x40, 0xff, 1, 0xffff

contract E {
    event ev(uint[], uint);
    bytes s;
    constructor() {
        // The following event emission involves writing to temporary memory at the current location
        // of the free memory pointer. Several other operations (e.g. certain keccak256 calls) will
        // use temporary memory in a similar manner.
        // In this particular case, the length of the passed array will be written to temporary memory
        // exactly such that the byte after the 63 bytes allocated below will be 0x02. This dirty byte
        // will then be written to storage during the assignment and become visible with the push in ``h``.
        emit ev(new uint[](2), 0);
        bytes memory m = new bytes(63);
        s = m;
    }
    function h() external returns (bytes memory) {
        s.push();
        return s;
    }
}

//Override Checker: Allow changing data location for parameters only when overriding external functions.
abstract contract I {
    function f(uint256[] calldata a) external virtual returns (uint256[] calldata);
}

contract M is I {
    function f(uint256[] memory a) public override returns (uint256[] memory) {
        return a;
    }

    function g(uint[] calldata x) public returns (uint256[] memory) {
        return f(x);
    }
}

// f(uint256[]): 0x20, 2, 9, 8 -> 0x20, 2, 9, 8
// g(uint256[]): 0x20, 2, 9, 8 -> 0x20, 2, 9, 8


