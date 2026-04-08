contract TCtoken{
    uint256 number = 0;
    string tokenName = "TC Token";
    event EmployeeClockIn(address indexed employee, string  place, uint256 timestamp, bool late, uint256 fine);
    function writeNumber(uint256 n) public returns(uint256){
        number = n;
        return number;
    }
    function payMeTRX() public payable returns(uint256){
        return msg.value;
    }
    function clockOut(address employee, string memory employeePlace, uint256 currentTime, bool isLate, uint256 fine) public returns(bool){
        emit EmployeeClockIn(employee, employeePlace, currentTime, isLate, fine);
        return false;
    }

    uint a;
    function test() payable external {
        payable(msg.sender).transfer(msg.value);
        //         a = msg.value;
    }

    function test1() payable external {
        payable(msg.sender).transfer(msg.value);
        a = msg.value;
    }

    event LogFallback(uint256 ind);
    fallback() payable external{
        emit LogFallback(2);
    }

    function testUseCpu(int256 count) external{
        for(int i = 0; i < count;i++){
            for(int j = 0; j < count;j++){
                int r = 2*2;
                r = r * 2;
            }
        }
    }

}