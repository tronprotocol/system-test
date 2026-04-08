contract C {
    function assemblyBlobhash() public  returns (bytes32 ret) {
        assembly {
            ret := blobhash(0)
        }
    }

    function assemblyBlobbasefee() public  returns (uint ret) {
        assembly {
            ret := blobbasefee()
        }
    }

    function globalBlobHash(uint index) public  returns (bytes32) {
        // Directly access the blob hash using the "blobhash" opcode
        return blobhash(index);
    }

    function globalBlobbasefee() public returns (uint) {
        return block.blobbasefee;
    }

//    function blobhashNoParams() external returns (bytes32 ret) {
//        assembly {
//            ret := blobhash() //need one param
//        }
//    }

    function assemblyBlobhashWithParam(uint index) public  returns (bytes32 ret) {
        assembly {
            ret := blobhash(index)
        }
    }

    function assemblySstoreBlobbasefee() public {
        assembly {
            sstore(0, blobbasefee())
        }
    }


    // Function declared as pure, but this expression (potentially) reads from the environment or state and thus requires "view".
//    function pureBlobbasefee() public pure {
//        assembly { pop(blobbasefee()) }
//    }
    // Function declared as pure, but this expression (potentially) reads from the environment or state and thus requires "view".
//    function pureGlobalBlobbasefee() public pure returns (uint) {
//        return block.blobbasefee;
//    }



    address constant KZG_POINT_EVALUATION = address(0x02000A);

    error KZGVerificationFailed();

    function validateInputLengths(
        bytes calldata version,
        bytes calldata commitment,
        bytes calldata z,
        bytes calldata y,
        bytes calldata proof
    ) internal pure {
        require(version.length == 32, "Invalid versionhash length");
        require(commitment.length == 48, "Invalid commitment length");
        require(z.length == 32, "Invalid evaluation point length");
        require(y.length == 32, "Invalid claimed value length");
        require(proof.length == 48, "Invalid proof length");
    }

    function verifyKZG(
        bytes calldata version,
        bytes calldata commitment,
        bytes calldata z,
        bytes calldata y,
        bytes calldata proof
    ) external view returns (bool success, bytes memory result) {
        validateInputLengths(version, commitment, z, y, proof);

        // Prepare the call data for the precompile
        bytes memory callData = abi.encodePacked(
            version,
            z,
            y,
            commitment,
            proof
        );

        // Call the KZG point evaluation precompile
        (success, result) = KZG_POINT_EVALUATION.staticcall(callData);

        return (success, result);
    }

    function verifyKZG1(
        bytes calldata version,
        bytes calldata commitment,
        bytes calldata z,
        bytes calldata y,
        bytes calldata proof
    ) external returns (bool success, bytes memory result) {
        validateInputLengths(version, commitment, z, y, proof);

        // Prepare the call data for the precompile
        bytes memory callData = abi.encodePacked(
            version,
            z,
            y,
            commitment,
            proof
        );

        // Call the KZG point evaluation precompile
        (success, result) = KZG_POINT_EVALUATION.staticcall(callData);

        return (success, result);
    }

    function verifyKZGCall(
        bytes calldata version,
        bytes calldata commitment,
        bytes calldata z,
        bytes calldata y,
        bytes calldata proof
    ) external returns (bool success, bytes memory result) {
        validateInputLengths(version, commitment, z, y, proof);

        // Prepare the call data for the precompile
        bytes memory callData = abi.encodePacked(
            version,
            z,
            y,
            commitment,
            proof
        );

        // Call the KZG point evaluation precompile
        (success, result) = KZG_POINT_EVALUATION.call(callData);

        return (success, result);
    }

    function verifyKZGdelegatecall(
        bytes calldata version,
        bytes calldata commitment,
        bytes calldata z,
        bytes calldata y,
        bytes calldata proof
    ) external returns (bool success, bytes memory result) {
        validateInputLengths(version, commitment, z, y, proof);

        // Prepare the call data for the precompile
        bytes memory callData = abi.encodePacked(
            version,
            z,
            y,
            commitment,
            proof
        );

        // Call the KZG point evaluation precompile
        (success, result) = KZG_POINT_EVALUATION.delegatecall(callData);

        return (success, result);
    }

}

contract D {
    function staticcallBlobhash(address called_address) public  returns(uint256){
        (bool success, bytes memory result) = called_address.staticcall(
            abi.encodeWithSignature("assemblyBlobhash()")
        );
        require(success);
        return abi.decode(result, (uint256));
    }
    function callBlobhash(address called_address) public  returns(uint256){
        (bool success, bytes memory result) = called_address.call(
            abi.encodeWithSignature("assemblyBlobhash()")
        );
        require(success);
        return abi.decode(result, (uint256));
    }
    function delegatecallBlobhash(address called_address) public  returns(uint256){
        (bool success, bytes memory result) = called_address.delegatecall(
            abi.encodeWithSignature("assemblyBlobhash()")
        );
        require(success);
        return abi.decode(result, (uint256));
    }
    function staticcallBlobBaseFee(address called_address) public returns(uint256) {
        (bool success,bytes memory result) = called_address.staticcall(
            abi.encodeWithSignature("assemblyBlobbasefee()")
        );
        require(success);
        return abi.decode(result, (uint256));
    }

    function callBlobBaseFee(address called_address) public returns(uint256) {
        (bool success,bytes memory result) = called_address.call(
            abi.encodeWithSignature("assemblyBlobbasefee()")
        );
        require(success);
        return abi.decode(result, (uint256));
    }
    function delegatecallBlobBaseFee(address called_address) public  returns(uint256){
        (bool success, bytes memory result) = called_address.delegatecall(
            abi.encodeWithSignature("assemblyBlobbasefee()")
        );
        require(success);
        return abi.decode(result, (uint256));
    }

}