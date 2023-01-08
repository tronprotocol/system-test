contract TCtoken{
    uint256 number = 0;
    string tokenName = "TC Token";
    //员工上班打卡， 记录： 打卡人钱包地址，打卡地点，打卡时间，是否迟到，罚款金额
    event EmployeeClockIn(address indexed employee, string  place, uint256 timestamp, bool late, uint256 fine);
    function getBlockChainId() view public returns(uint256) {
        uint256 id;
        assembly {
            id := chainid()
        }
        assert(block.chainid == id);
        return block.chainid;
    }
    function writeNumber(uint256 n) public returns(uint256){
        number = n;
        return number;
    }
    function payMeTRX() public payable returns(uint256){
        return msg.value;
    }
    function getRandom(uint256 seed) view public returns(uint256){
        return uint256(keccak256(abi.encodePacked(blockhash(block.number - 5),block.timestamp,seed)));
    }
    function getMax(uint256 x,uint256 y) pure public returns(uint256){
        if(x>y){
            return x;
        }else{
            return y;
        }
    }
    function clockIn(address employee, string memory employeePlace, uint256 currentTime, bool isLate) payable public returns(bool){
        emit EmployeeClockIn(employee, employeePlace, currentTime, isLate, msg.value);
        return true;
    }
    function clockOut(address employee, string memory employeePlace, uint256 currentTime, bool isLate, uint256 fine) public returns(bool){
        emit EmployeeClockIn(employee, employeePlace, currentTime, isLate, fine);
        return false;
    }
}