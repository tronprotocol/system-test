type T is uint;
using Lib for T global;
library Lib {
function inc(T x) internal pure returns (T) {
return T.wrap(T.unwrap(x) + 1);
}
function dec(T x) external pure returns (T) {
return T.wrap(T.unwrap(x) - 1);
}
}