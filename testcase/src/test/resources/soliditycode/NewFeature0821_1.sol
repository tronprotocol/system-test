contract H {
    uint x;
    function f() public returns (uint256) {
        m().f.selector;
        return x;
    }
    function m() public returns (H) {
        x = 42;
        return this;
    }
}
// ----
// f() -> 42

library L {
    event E(bytes32, bool, uint32);
}

interface I {
    event E(bytes32, bool, uint32);
}

contract A {
    event E(bytes32, bool, uint32);
}

contract B {
    event E(bytes32, bool, uint32);
}

contract C is B {
    bytes32 public librarySelector = L.E.selector;
    bytes32 public interfaceSelector = I.E.selector;
    bytes32 public foreignContractSelector = A.E.selector;
    bytes32 inheritedSelector = E.selector;

    function f() public {
        assert(librarySelector == interfaceSelector);
        assert(librarySelector == foreignContractSelector);
        assert(librarySelector == B.E.selector);
        assert(E.selector == B.E.selector);

        emit E(E.selector, true, 123);
        emit I.E((B.E.selector), true, 123);
        emit A.E((B.E.selector), true, 123);
        emit L.E((B.E.selector), true, 123);
    }
}
// ----


contract K {
    uint immutable public x;
    uint immutable public y;
    uint immutable public z; // Not initialized. Will be 0

    constructor(bool condition, uint value) {
        x = value;
        if (condition) {
            x = 42; // Overwriting already assigned value
            z = 42;
        }
        else
            z = 24; // On 0.8.20 initialization inside an if is not allowed
        // even if there's no way for z to remain uninitialized
    }
}

contract N {
    uint immutable x;
    constructor() {
        // immutable variable init in loop, v0.8.21 is ok, v0.8.20 not ok
        while (true)
            x = 1;
    }
}
