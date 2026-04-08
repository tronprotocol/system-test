type U8 is uint8;
//using {add as +} for U8 ;
using {pickFirst as +} for U8 global;

function pickFirst(U8 x, U8 y) pure returns (U8) {
    return x; // FIXME: should detect possible overflow here
}

contract C {
    function pick(uint8 x, uint8 y) public pure returns (U8){
        return U8.wrap(x) + U8.wrap(y); // FIXME: should detect possible overflow here
    }
}

