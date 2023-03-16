import {T} from "./NewFeature0817_1.sol";


library L {
    function id1(uint x) internal pure returns(uint) {
        return x;
    }
}

function id(uint x) pure returns (uint) {
    return x;
}

function plus(uint x) pure returns (uint) {
    return x+1;
}

function zero(uint) pure returns (uint) {
    return 0;
}
using {id,plus} for uint;

contract C {
    using {zero} for uint;

    function g(uint z) pure external returns(uint,uint,uint) {
        return (z.zero(), z.id(), z.plus());
    }

    using {L.id1} for uint;
    function f(uint x) external pure returns (uint){
        return x.id1();
    }

    function glob() public pure returns (T r1, T r2) {
        r1 = r1.inc().inc();
        r2 = r1.dec();
    }
}