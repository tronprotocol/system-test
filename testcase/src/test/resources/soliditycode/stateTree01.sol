
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

    function transferAssetInCall(address callBAddress,address callCAddress, address toAddress ,uint256 amount,trcToken id) payable public{
        callBAddress.call(abi.encodeWithSignature("transC(address,address,uint256,trcToken)",callCAddress,toAddress,amount,id));
    }
    function transferAssetIndelegateCall(address callBddress,address callAddressC, address toAddress,uint256 amount, trcToken id) payable public{
        callBddress.delegatecall(abi.encodeWithSignature("transC(address,address,uint256,trcToken)",callAddressC,toAddress,amount,id));
    }
    function getBalance(address add) view public returns (uint256 r) {
        r = add.balance;
    }

    function getTokenBalance(address toAddress, trcToken tokenId) public payable returns(uint256){
        return toAddress.tokenBalance(tokenId);
    }

    function destroy(address payable inheritor) external {
        selfdestruct(inheritor);
    }

}
contract calledContract {
        fallback() external payable{}
       constructor() public payable {}
       function transferTo(address payable toAddress)public payable{
           toAddress.transfer(5);
       }

       function  transC(address callCAddress,address toAddress,uint256 amount, trcToken id) payable public{
           callCAddress.call(abi.encodeWithSignature("trans(address,uint256,trcToken)",toAddress,amount,id));
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
    function  trans(address payable toAddress,uint256 amount, trcToken id) payable public{
        toAddress.transferToken(amount,id);
    }
    function getPredictedAddress(bytes32 salt) view public returns(address) {
       // This complicated expression just tells you how the address
       // can be pre-computed. It is just there for illustration.
       // You actually only need ``new D{salt: salt}(arg)``.
       address predictedAddress = address(uint160(uint(keccak256(abi.encodePacked(
               bytes1(0x41),
               address(this),
               salt,
               keccak256(abi.encodePacked(
                   type(callerContract).creationCode
               ))
           )))));
       return predictedAddress;
    }

    function createWithSalted(bytes32 salt) public returns(address) {
        callerContract e = new callerContract{salt: salt}();
        return address(e);
    }
}