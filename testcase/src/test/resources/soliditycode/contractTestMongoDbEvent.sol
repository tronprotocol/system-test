pragma solidity ^0.8.0;

contract SimpleStorage {
    uint256 favoriteNumber;

    event storedNumber(
        uint256 indexed oldNumber,
        uint256 indexed newNumber,
        uint256 addedNumber,
        address sender
    );
   event storedNumber1(
        uint256 oldNumber,
        uint256 newNumber,
        uint256 addedNumber,
        address sender
    );
    function store(uint256 _favoriteNumber) public {
        emit storedNumber(
            favoriteNumber,
            _favoriteNumber,
            _favoriteNumber + favoriteNumber,
            msg.sender
        );
        emit storedNumber1(
            favoriteNumber,
            _favoriteNumber,
            _favoriteNumber + favoriteNumber,
            msg.sender
                );
        favoriteNumber = _favoriteNumber;

    }

    function retrieve() public view returns (uint256) {
        return favoriteNumber;
    }
}
