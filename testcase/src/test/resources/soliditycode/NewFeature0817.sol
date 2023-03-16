contract X {
    // no "returns" on purpose
    function a(uint) public pure {}
    function b(uint) external pure {}
}

contract Base {
    function a(uint x) external pure returns (uint) { return x + 1; }
}

contract C is Base{
    function f() external {}
    function g() external {}
    function h() pure external {}
    function i() view external {}

    function comparison_operators_for_local_external_function_pointers() public returns (bool) {
        function () external f_local = this.f;
        function () external g_local = this.g;
        function () external pure h_local = this.h;
        function () external view i_local = this.i;

        assert(
            f_local == this.f &&
            g_local == this.g &&
            h_local == this.h &&
            i_local == this.i
        );
        return true;
    }

    function test_function_name_for_abi_encodeCall() public view returns (uint r) {
        bool success;
        bytes memory result;
        (success, result) = address(this).staticcall(abi.encodeCall(X.a, 1));
        require(success && result.length == 32);
        r += abi.decode(result, (uint));
        require(r == 2);

        (success, result) = address(this).staticcall(abi.encodeCall(X.b, 10));
        require(success && result.length == 32);
        r += abi.decode(result, (uint));
        require(r == 13);

        (success, result) = address(this).staticcall(abi.encodeCall(Base.a, 100));
        require(success && result.length == 32);
        r += abi.decode(result, (uint));
        require(r == 114);

        (success, result) = address(this).staticcall(abi.encodeCall(this.a, 1000));
        require(success && result.length == 32);
        r += abi.decode(result, (uint));
        require(r == 1115);

        (success, result) = address(this).staticcall(abi.encodeCall(C.b, 10000));
        require(success && result.length == 32);
        r += abi.decode(result, (uint));
        require(r == 11116);

        return r;
    }
    function b(uint x) external view returns (uint) {
        return this.a(x);
    }

    event E();
    function eventSelector() external returns (bytes32) {
        return (E.selector);
    }
}
