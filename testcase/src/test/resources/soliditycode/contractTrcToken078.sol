
contract callerContract {
    constructor() public payable{}
    fallback() external payable{}
    function sendToB(address called_address, address c) public payable{
       called_address.delegatecall(abi.encodeWithSignature("transferTo(address)",c));
    }
    function sendToB2(address called_address,address c) public payable{
        called_address.call(abi.encodeWithSignature("transferTo(address)",c));
    }
    function sendToB3(address called_address,address c) public payable{
        called_address.delegatecall(abi.encodeWithSignature("transferTo(address)",c));
    }

    function getBalance(address add) view public returns (uint256 r) {
        r = add.balance;
    }

}
   contract calledContract {
        fallback() external payable{}
       constructor() public payable {}
       function transferTo(address payable toAddress)public payable{
           toAddress.transfer(5);
       }

       function setIinC(address c) public payable{
           c.call{value:5}(abi.encode(bytes4(keccak256("setI()"))));
       }

   }
   contract c{
    address public origin;
    address public sender;
    constructor() public payable{}
    event log(address,address);
    fallback() payable external{
         emit log(tx.origin,msg.sender);
    }
   }