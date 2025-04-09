interface I {
    event ForeignEvent(bytes);
}

contract C {
    event ForeignEvent(uint, string indexed);
}

    event E(int);

contract D {
    function f() public {
        // Emitting a foreign event would trigger an internal error on 0.8.21
        emit I.ForeignEvent("abc");
        emit C.ForeignEvent(8, "xyz");

        // Emitting a file-level event. New feature.
        emit E(6);
    }

    uint acc;
    uint r;
    uint[3] array = [1,2,3];
    function inc() public {
        for (uint i = 0; i < array.length; i++) {
            acc += array[i]; // i is not modified by the loop body
            r = i;
        }
    }

    function incUcheck() public{
        for (uint i = 0; i < array.length;) {
            acc += array[i];
            r = i;
            unchecked { i++; }
        }
    }
}


interface G {
    event Event(address indexed _from, uint256 _value);
}

contract M {
    function emitEvent(uint256 _value) public {
        emit G.Event(msg.sender, _value);
    }
}

// ----
// emitEvent(uint256): 100 ->
// ~ emit Event(address,uint256): #0x1212121212121212121212121212120000000012, 0x64


contract A {
    uint acc;
    uint r;
    uint[3] array = [1,2,3];
    function inc() public{
        for (uint i = 0; i < array.length;i++) {
            acc += array[i]; // i is not modified by the loop body
            r = i;

        }
    }

}

contract B {
    uint acc;
    uint r;
    uint[3] array = [1,2,3];
    function inc() public{
        for (uint i = 0; i < array.length;) {
            acc += array[i]; // i is not modified by the loop body
            r = i;
            unchecked { i++; }
        }
    }
}

