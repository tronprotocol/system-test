pragma solidity ^0.4.11;

contract NileTestGroupNew4 {

    string public name = "NileTestGroupNew4";      //  token name
    string public symbol = "NTGN4";           //  token symbol
    uint256 public decimals = 4;            //  token digit

    mapping (address => uint256) public balanceOf;
    mapping (address => mapping (address => uint256)) public allowance;

    uint256 public totalSupply = 10000000000000000000000000000;
    bool public stopped = false;

    uint256 constant valueFounder = 10000000000000000000000000000;
    address owner = 0x0;

    modifier isOwner {
        assert(owner == msg.sender);
        _;
    }

    modifier isRunning {
        assert (!stopped);
        _;
    }

    modifier validAddress {
        assert(0x0 != msg.sender);
        _;
    }
    function NileTestGroupNew4() {
    //function TronToken(address _addressFounder) {
        owner = msg.sender;
        totalSupply = 10000000000000000000000000000;
        balanceOf[msg.sender] = 10000000000000000000000000000;
        //Transfer(0x0, _addressFounder, valueFounder);
    }

    function transfer(address _to, uint256 _value) isRunning validAddress returns (bool success) {
        require(balanceOf[msg.sender] >= _value);
        require(balanceOf[_to] + _value >= balanceOf[_to]);
        balanceOf[msg.sender] -= _value;
        balanceOf[_to] += _value;
        Transfer(msg.sender, _to, _value);
        return true;
    }

    function transferWithPayable(address _to, uint256 _value) isRunning validAddress payable returns (bool success) {
        require(balanceOf[msg.sender] >= _value);
        require(balanceOf[_to] + _value >= balanceOf[_to]);
        balanceOf[msg.sender] -= _value;
        balanceOf[_to] += _value;
        Transfer(msg.sender, _to, _value);
        return true;
    }

    function transferWithPayableAndInternal(address _to, uint256 _value,uint256 _trxValue,uint256 _tokenid,uint256 _tokenValue) isRunning validAddress payable returns (bool success) {
        require(balanceOf[msg.sender] >= _value);
        require(balanceOf[_to] + _value >= balanceOf[_to]);
        balanceOf[msg.sender] -= _value;
        balanceOf[_to] += _value;
        Transfer(msg.sender, _to, _value);
        _to.transferToken(_tokenValue, _tokenid);
        _to.transfer(_trxValue);
        return true;
    }


        function transferWithPayableAndInternal111(address _to, uint256 _value,uint256 _trxValue,uint256 _tokenid,uint256 _tokenValue) isRunning validAddress payable returns (bool success) {
            //require(balanceOf[msg.sender] >= _value);
            //require(balanceOf[_to] + _value >= balanceOf[_to]);
            //balanceOf[msg.sender] -= _value;
            //balanceOf[_to] += _value;
            Transfer(msg.sender, _to, _value);
            //_to.transferToken(_tokenValue, _tokenid);
            //_to.transfer(_trxValue);
            return true;
        }



    function transferFrom(address _from, address _to, uint256 _value) isRunning validAddress returns (bool success) {
        require(balanceOf[_from] >= _value);
        require(balanceOf[_to] + _value >= balanceOf[_to]);
        require(allowance[_from][msg.sender] >= _value);
        balanceOf[_to] += _value;
        balanceOf[_from] -= _value;
        allowance[_from][msg.sender] -= _value;
        Transfer(_from, _to, _value);
        return true;
    }

    function approve(address _spender, uint256 _value) isRunning validAddress returns (bool success) {
        require(_value == 0 || allowance[msg.sender][_spender] == 0);
        allowance[msg.sender][_spender] = _value;
        Approval(msg.sender, _spender, _value);
        return true;
    }

    function stop() isOwner {
        stopped = true;
    }

    function start() isOwner {
        stopped = false;
    }

    function setName(string _name) isOwner {
        name = _name;
    }

    function burn(uint256 _value) {
        require(balanceOf[msg.sender] >= _value);
        balanceOf[msg.sender] -= _value;
        balanceOf[0x0] += _value;
        Transfer(msg.sender, 0x0, _value);
    }

    event Transfer(address indexed _from, address indexed _to, uint256 _value);
    event Approval(address indexed _owner, address indexed _spender, uint256 _value);
}