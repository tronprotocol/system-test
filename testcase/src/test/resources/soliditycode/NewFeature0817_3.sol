uint256 constant MAX = 1;
library L1 {
    uint256 public constant INT = 100;
}

contract C1 {
    uint256 public constant LIMIT = MAX * L1.INT;  // same file & external library constant
}

