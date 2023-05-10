contract C {
    function test_push() pure public {
        assembly {  //disallow push i
//            push0()
//            push1()
//            push2()
        }
    }

    function zero() external pure returns (uint) {
        return 0;
    }
}