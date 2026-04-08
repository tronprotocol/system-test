pragma experimental ABIEncoderV2;

contract Demo {
  bytes32 public result;

  constructor (bytes32 hash, bytes[] memory signatures, address[] memory addresses) public {
      result = batchvalidatesign(hash, signatures, addresses);
      bytes32 expected = 0x0101010101010101010101010101010100000000000000000000000000000000;
      require(result == expected, "Invalid result value");
  }

  function testConstructor() public returns(bytes32){
      return result;
  }

  function testConstructorPure() public view returns(bytes32){
      return result;
  }
}