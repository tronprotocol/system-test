package stest.tron.wallet.common.client.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.contract.SmartContractOuterClass.ClearABIContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract.Builder;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContractDataWrapper;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateEnergyLimitContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateSettingContract;
import stest.tron.wallet.common.client.WalletClient;

@Slf4j
/** Helper for smart contract operations: deployment, triggering, ABI management, and constant calls. */
public class ContractHelper {

  // ---------------------------------------------------------------------------
  // Deploy Smart Contract
  // ---------------------------------------------------------------------------

  /** Deploy a smart contract that supports fallback and receive functions. */
  public static byte[] deployContractFallbackReceive(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      long originEnergyLimit,
      String tokenId,
      long tokenValue,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi2(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    // byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract =
        contractBuilder.setNewContract(builder.build()).build();


    //estimateEnergyDeployContract
    if (null != libraryAddress) {
      estimateDeployContractEnergy(PublicMethod.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    } else {
      estimateDeployContractEnergy(code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    }


    TransactionExtention transactionExtention =
        blockingStubFull.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction(), owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return contractAddress;
    }
  }

  /** Deploy a smart contract with full parameters including token support. */
  public static byte[] deployContract(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      long originEnergyLimit,
      String tokenId,
      long tokenValue,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    // byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract =
        contractBuilder.setNewContract(builder.build()).build();

    //estimateEnergyDeployContract
    if (null != libraryAddress) {
      estimateDeployContractEnergy(PublicMethod.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    } else {
      estimateDeployContractEnergy(code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    }

    TransactionExtention transactionExtention =
        blockingStubFull.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction(), owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return contractAddress;
    }
  }

  /** Deploy a smart contract with simplified parameters (defaults for token and energy limit). */
  public static byte[] deployContract(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return deployContract(
        contractName,
        abiString,
        code,
        data,
        feeLimit,
        value,
        consumeUserResourcePercent,
        1000L,
        "0",
        0L,
        libraryAddress,
        priKey,
        ownerAddress,
        blockingStubFull);
  }

  /** Deploy a smart contract without broadcasting the transaction. */
  public static Transaction deployContractWithoutBroadcast(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      long originEnergyLimit,
      String tokenId,
      long tokenValue,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    // byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract =
        contractBuilder.setNewContract(builder.build()).build();

    //estimateEnergyDeployContract
    if (null != libraryAddress) {
      estimateDeployContractEnergy(PublicMethod.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    } else {
      estimateDeployContractEnergy(code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    }

    TransactionExtention transactionExtention =
        blockingStubFull.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction(), owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray())));
    return transaction;
  }

  /** Deploy a smart contract with fallback support (simplified overload). */
  public static byte[] deployContractFallback(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return deployContractFallbackReceive(
        contractName,
        abiString,
        code,
        data,
        feeLimit,
        value,
        consumeUserResourcePercent,
        1000L,
        "0",
        0L,
        libraryAddress,
        priKey,
        ownerAddress,
        blockingStubFull);
  }

  /** Deploy a smart contract that uses library addresses with compiler version support. */
  public static byte[] deployContractForLibrary(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      String compilerVersion,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    // byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(1000L);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      if (compilerVersion.equals("v5") || compilerVersion.equals("V5")) {
        byteCode = replaceLibraryAddresscompilerVersion(code, libraryAddress, "v5");
      } else {
        // old version
        byteCode = replaceLibraryAddresscompilerVersion(code, libraryAddress, null);
      }

    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(0);
    contractBuilder.setTokenId(Long.parseLong("0"));
    CreateSmartContract contractDeployContract =
        contractBuilder.setNewContract(builder.build()).build();

    //estimateEnergyDeployContract
    if (null != libraryAddress) {
      estimateDeployContractEnergy(PublicMethod.code, value, "0", 0L, ownerAddress, blockingStubFull);
    } else {
      estimateDeployContractEnergy(code, value, "0", 0L, ownerAddress, blockingStubFull);
    }

    TransactionExtention transactionExtention =
        blockingStubFull.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    byte[] contractAddress = generateContractAddress(transactionExtention.getTransaction(), owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return contractAddress;
    }
  }

  // ---------------------------------------------------------------------------
  // Deploy and Get Transaction Info
  // ---------------------------------------------------------------------------

  /** Deploy a smart contract and return the transaction ID (simplified overload). */
  public static String deployContractAndGetTransactionInfoById(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return deployContractAndGetTransactionInfoById(
        contractName,
        abiString,
        code,
        data,
        feeLimit,
        value,
        consumeUserResourcePercent,
        1000L,
        "0",
        0L,
        libraryAddress,
        priKey,
        ownerAddress,
        blockingStubFull);
  }

  /** Deploy a smart contract and return the transaction ID. */
  public static String deployContractAndGetTransactionInfoById(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      long originEnergyLimit,
      String tokenId,
      long tokenValue,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    // byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract =
        contractBuilder.setNewContract(builder.build()).build();

    //estimateEnergyDeployContract
    if (null != libraryAddress) {
      estimateDeployContractEnergy(PublicMethod.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    } else {
      estimateDeployContractEnergy(code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    }


    TransactionExtention transactionExtention =
        blockingStubFull.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    byte[] contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Deploy a smart contract and return the broadcast response. */
  public static GrpcAPI.Return deployContractAndGetResponse(
      String contractName,
      String abiString,
      String code,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      long originEnergyLimit,
      String tokenId,
      long tokenValue,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }
    // byte[] codeBytes = Hex.decode(code);
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract =
        contractBuilder.setNewContract(builder.build()).build();


    //estimateEnergyDeployContract
    if (null != libraryAddress) {
      estimateDeployContractEnergy(PublicMethod.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    } else {
      estimateDeployContractEnergy(code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    }


    TransactionExtention transactionExtention =
        blockingStubFull.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    byte[] contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response;
  }

  // ---------------------------------------------------------------------------
  // Deploy with Constructor Parameters
  // ---------------------------------------------------------------------------

  /** Deploy a smart contract with constructor parameters (simplified overload). */
  public static String deployContractWithConstantParame(
      String contractName,
      String abiString,
      String code,
      String constructorStr,
      String argsStr,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return deployContractWithConstantParame(
        contractName,
        abiString,
        code,
        constructorStr,
        argsStr,
        data,
        feeLimit,
        value,
        consumeUserResourcePercent,
        1000L,
        "0",
        0L,
        libraryAddress,
        priKey,
        ownerAddress,
        blockingStubFull);
  }

  /** Deploy a smart contract with constructor parameters. */
  public static String deployContractWithConstantParame(
      String contractName,
      String abiString,
      String code,
      String constructorStr,
      String argsStr,
      String data,
      Long feeLimit,
      long value,
      long consumeUserResourcePercent,
      long originEnergyLimit,
      String tokenId,
      long tokenValue,
      String libraryAddress,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    SmartContract.ABI abi = jsonStr2Abi(abiString);
    if (abi == null) {
      logger.error("abi is null");
      return null;
    }

    code += Hex.toHexString(AbiUtil.encodeInput(constructorStr, argsStr));
    byte[] owner = ownerAddress;
    SmartContract.Builder builder = SmartContract.newBuilder();
    builder.setName(contractName);
    builder.setOriginAddress(ByteString.copyFrom(owner));
    builder.setAbi(abi);
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);
    builder.setOriginEnergyLimit(originEnergyLimit);

    if (value != 0) {

      builder.setCallValue(value);
    }

    byte[] byteCode;
    if (null != libraryAddress) {
      byteCode = replaceLibraryAddress(code, libraryAddress);
    } else {
      byteCode = Hex.decode(code);
    }
    builder.setBytecode(ByteString.copyFrom(byteCode));

    Builder contractBuilder = CreateSmartContract.newBuilder();
    contractBuilder.setOwnerAddress(ByteString.copyFrom(owner));
    contractBuilder.setCallTokenValue(tokenValue);
    contractBuilder.setTokenId(Long.parseLong(tokenId));
    CreateSmartContract contractDeployContract =
        contractBuilder.setNewContract(builder.build()).build();

    //estimateEnergyDeployContract
    if (null != libraryAddress) {
      estimateDeployContractEnergy(PublicMethod.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    } else {
      estimateDeployContractEnergy(code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
    }

    TransactionExtention transactionExtention =
        blockingStubFull.deployContract(contractDeployContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();

    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    byte[] contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  // ---------------------------------------------------------------------------
  // ABI Utilities
  // ---------------------------------------------------------------------------

  /** Parse a JSON ABI string into a SmartContract.ABI protobuf object. */
  public static SmartContract.ABI jsonStr2Abi(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous =
          abiItem.getAsJsonObject().get("anonymous") != null
              && abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
      final boolean constant =
          abiItem.getAsJsonObject().get("constant") != null
              && abiItem.getAsJsonObject().get("constant").getAsBoolean();
      final String name =
          abiItem.getAsJsonObject().get("name") != null
              ? abiItem.getAsJsonObject().get("name").getAsString()
              : null;
      JsonArray inputs =
          abiItem.getAsJsonObject().get("inputs") != null
              ? abiItem.getAsJsonObject().get("inputs").getAsJsonArray()
              : null;
      final JsonArray outputs =
          abiItem.getAsJsonObject().get("outputs") != null
              ? abiItem.getAsJsonObject().get("outputs").getAsJsonArray()
              : null;
      String type =
          abiItem.getAsJsonObject().get("type") != null
              ? abiItem.getAsJsonObject().get("type").getAsString()
              : null;
      final boolean payable =
          abiItem.getAsJsonObject().get("payable") != null
              && abiItem.getAsJsonObject().get("payable").getAsBoolean();
      final String stateMutability =
          abiItem.getAsJsonObject().get("stateMutability") != null
              ? abiItem.getAsJsonObject().get("stateMutability").getAsString()
              : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback") && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (inputs != null) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null
              || inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = inputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null
              || outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder =
              SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = outputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }

      entryBuilder.setType(getEntryType(type));
      entryBuilder.setPayable(payable);
      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }

  /** Parse a JSON ABI string into a protobuf ABI, supporting receive entry type. */
  public static SmartContract.ABI jsonStr2Abi2(String jsonStr) {
    if (jsonStr == null) {
      return null;
    }

    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElementRoot = jsonParser.parse(jsonStr);
    JsonArray jsonRoot = jsonElementRoot.getAsJsonArray();
    SmartContract.ABI.Builder abiBuilder = SmartContract.ABI.newBuilder();
    for (int index = 0; index < jsonRoot.size(); index++) {
      JsonElement abiItem = jsonRoot.get(index);
      boolean anonymous =
          abiItem.getAsJsonObject().get("anonymous") != null
              && abiItem.getAsJsonObject().get("anonymous").getAsBoolean();
      final boolean constant =
          abiItem.getAsJsonObject().get("constant") != null
              && abiItem.getAsJsonObject().get("constant").getAsBoolean();
      final String name =
          abiItem.getAsJsonObject().get("name") != null
              ? abiItem.getAsJsonObject().get("name").getAsString()
              : null;
      JsonArray inputs =
          abiItem.getAsJsonObject().get("inputs") != null
              ? abiItem.getAsJsonObject().get("inputs").getAsJsonArray()
              : null;
      final JsonArray outputs =
          abiItem.getAsJsonObject().get("outputs") != null
              ? abiItem.getAsJsonObject().get("outputs").getAsJsonArray()
              : null;
      String type =
          abiItem.getAsJsonObject().get("type") != null
              ? abiItem.getAsJsonObject().get("type").getAsString()
              : null;
      final boolean payable =
          abiItem.getAsJsonObject().get("payable") != null
              && abiItem.getAsJsonObject().get("payable").getAsBoolean();
      final String stateMutability =
          abiItem.getAsJsonObject().get("stateMutability") != null
              ? abiItem.getAsJsonObject().get("stateMutability").getAsString()
              : null;
      if (type == null) {
        logger.error("No type!");
        return null;
      }
      if (!type.equalsIgnoreCase("fallback")
          && !type.equalsIgnoreCase("receive")
          && null == inputs) {
        logger.error("No inputs!");
        return null;
      }

      SmartContract.ABI.Entry.Builder entryBuilder = SmartContract.ABI.Entry.newBuilder();
      entryBuilder.setAnonymous(anonymous);
      entryBuilder.setConstant(constant);
      if (name != null) {
        entryBuilder.setName(name);
      }

      /* { inputs : optional } since fallback function not requires inputs*/
      if (inputs != null) {
        for (int j = 0; j < inputs.size(); j++) {
          JsonElement inputItem = inputs.get(j);
          if (inputItem.getAsJsonObject().get("name") == null
              || inputItem.getAsJsonObject().get("type") == null) {
            logger.error("Input argument invalid due to no name or no type!");
            return null;
          }
          String inputName = inputItem.getAsJsonObject().get("name").getAsString();
          String inputType = inputItem.getAsJsonObject().get("type").getAsString();
          ABI.Entry.Param.Builder paramBuilder = SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = inputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(inputName);
          paramBuilder.setType(inputType);
          entryBuilder.addInputs(paramBuilder.build());
        }
      }

      /* { outputs : optional } */
      if (outputs != null) {
        for (int k = 0; k < outputs.size(); k++) {
          JsonElement outputItem = outputs.get(k);
          if (outputItem.getAsJsonObject().get("name") == null
              || outputItem.getAsJsonObject().get("type") == null) {
            logger.error("Output argument invalid due to no name or no type!");
            return null;
          }
          String outputName = outputItem.getAsJsonObject().get("name").getAsString();
          String outputType = outputItem.getAsJsonObject().get("type").getAsString();
          SmartContract.ABI.Entry.Param.Builder paramBuilder =
              SmartContract.ABI.Entry.Param.newBuilder();
          JsonElement indexed = outputItem.getAsJsonObject().get("indexed");

          paramBuilder.setIndexed((indexed != null) && indexed.getAsBoolean());
          paramBuilder.setName(outputName);
          paramBuilder.setType(outputType);
          entryBuilder.addOutputs(paramBuilder.build());
        }
      }
      entryBuilder.setType(getEntryType2(type));

      if (stateMutability != null) {
        entryBuilder.setStateMutability(getStateMutability(stateMutability));
      }

      abiBuilder.addEntrys(entryBuilder.build());
    }

    return abiBuilder.build();
  }

  /** Map an ABI entry type string to its protobuf enum value. */
  public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      case "error":
        return SmartContract.ABI.Entry.EntryType.Error;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  /** Map an ABI entry type string to its protobuf enum value, including receive type. */
  public static SmartContract.ABI.Entry.EntryType getEntryType2(String type) {
    switch (type) {
      case "constructor":
        return SmartContract.ABI.Entry.EntryType.Constructor;
      case "function":
        return SmartContract.ABI.Entry.EntryType.Function;
      case "event":
        return SmartContract.ABI.Entry.EntryType.Event;
      case "fallback":
        return SmartContract.ABI.Entry.EntryType.Fallback;
      case "receive":
        return SmartContract.ABI.Entry.EntryType.Receive;
      default:
        return SmartContract.ABI.Entry.EntryType.UNRECOGNIZED;
    }
  }

  /** Map a state mutability string to its protobuf enum value. */
  public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
      String stateMutability) {
    switch (stateMutability) {
      case "pure":
        return SmartContract.ABI.Entry.StateMutabilityType.Pure;
      case "view":
        return SmartContract.ABI.Entry.StateMutabilityType.View;
      case "nonpayable":
        return SmartContract.ABI.Entry.StateMutabilityType.Nonpayable;
      case "payable":
        return SmartContract.ABI.Entry.StateMutabilityType.Payable;
      default:
        return SmartContract.ABI.Entry.StateMutabilityType.UNRECOGNIZED;
    }
  }

  // ---------------------------------------------------------------------------
  // Contract Address Generation and Query
  // ---------------------------------------------------------------------------

  /** Generate a contract address from a transaction and owner address. */
  public static byte[] generateContractAddress(Transaction trx, byte[] owneraddress) {

    // get owner address
    // this address should be as same as the onweraddress in trx, DONNOT modify it
    byte[] ownerAddress = owneraddress;

    // get tx hash
    byte[] txRawDataHash =
        Sha256Hash.of(
                CommonParameter.getInstance().isECKeyCryptoEngine(), trx.getRawData().toByteArray())
            .getBytes();

    // combine
    byte[] combined = new byte[txRawDataHash.length + ownerAddress.length];
    System.arraycopy(txRawDataHash, 0, combined, 0, txRawDataHash.length);
    System.arraycopy(ownerAddress, 0, combined, txRawDataHash.length, ownerAddress.length);

    return Hash.sha3omit12(combined);
  }

  /** Query a deployed smart contract by address. */
  public static SmartContract getContract(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString byteString = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
    logger.info("contract name is " + blockingStubFull.getContract(bytesMessage).getName());
    logger.info("contract address is " + WalletClient.encode58Check(address));
    return blockingStubFull.getContract(bytesMessage);
  }

  /** Query detailed contract information including runtime code by address. */
  public static SmartContractDataWrapper getContractInfo(
      byte[] address, WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString byteString = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
    logger.info(
        "contract name is "
            + blockingStubFull.getContractInfo(bytesMessage).getSmartContract().getName());
    logger.info("contract address is " + WalletClient.encode58Check(address));
    return blockingStubFull.getContractInfo(bytesMessage);
  }

  // ---------------------------------------------------------------------------
  // Trigger Smart Contract
  // ---------------------------------------------------------------------------

  /** Trigger a smart contract method (simplified overload without token parameters). */
  public static String triggerContract(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return triggerContract(
        contractAddress,
        method,
        argsStr,
        isHex,
        callValue,
        feeLimit,
        "0",
        0,
        ownerAddress,
        priKey,
        blockingStubFull);
  }

  /** Trigger a smart contract method with full parameters. */
  public static String triggerContract(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = new byte[0];
    if (!method.equalsIgnoreCase("#")) {
      input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));
    }

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    builder.setWrongFixed64(0x0000000000989680);
    builder.setWrongFixed32(0x0000000000989680);
    builder.setWrongSfixed64(0x0000000000989680);
    builder.setWrongSint64(0x0000000000989680);
    builder.setWrongDouble(0.111111111);
    builder.setWrongFloat(0.11f);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println(
          "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0
        && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);

    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());

    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Trigger a smart contract and broadcast to two nodes (simplified overload). */
  public static String triggerContractBoth(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletGrpc.WalletBlockingStub blockingStubFull1) {
    return triggerContractBoth(
        contractAddress,
        method,
        argsStr,
        isHex,
        callValue,
        feeLimit,
        "0",
        0,
        ownerAddress,
        priKey,
        blockingStubFull,
        blockingStubFull1);
  }

  /** Trigger a smart contract and broadcast to two nodes simultaneously. */
  public static String triggerContractBoth(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletGrpc.WalletBlockingStub blockingStubFull1) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println(
          "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0
        && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response =
        PublicMethod.broadcastTransactionBoth(transaction, blockingStubFull, blockingStubFull1);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Trigger a smart contract method using a parameter list instead of a string. */
  public static String triggerParamListContract(
      byte[] contractAddress,
      String method,
      List<Object> params,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, params));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println(
          "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0
        && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Trigger a smart contract and return the broadcast response object. */
  public static GrpcAPI.Return triggerContractAndGetResponse(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println(
          "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0
        && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response;
  }

  // ---------------------------------------------------------------------------
  // Trigger Constant Contract and Extensions
  // ---------------------------------------------------------------------------

  /** Trigger a constant (read-only) smart contract call. */
  public static String triggerConstantContract(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention =
        blockingStubFull.triggerConstantContract(triggerContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println(
          "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0
        && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    rawBuilder.setFeeLimit(feeLimit);
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Trigger a constant contract call on a solidity node, returning the TransactionExtention. */
  public static TransactionExtention triggerConstantContractForExtentionOnSolidity(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention =
        blockingStubSolidity.triggerConstantContract(triggerContract);
    return transactionExtention;
  }

  // ---------------------------------------------------------------------------
  // Clear Contract ABI
  // ---------------------------------------------------------------------------

  /** Clear the ABI of a deployed smart contract. */
  public static String clearContractAbi(
      byte[] contractAddress,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;

    ClearABIContract.Builder builder = ClearABIContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));

    ClearABIContract clearAbiContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.clearContractABI(clearAbiContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create call trx failed!");
      System.out.println("Code = " + transactionExtention.getResult().getCode());
      System.out.println(
          "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      return null;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRetCount() != 0
        && transactionExtention.getConstantResult(0) != null
        && transactionExtention.getResult() != null) {
      byte[] result = transactionExtention.getConstantResult(0).toByteArray();
      System.out.println("message:" + transaction.getRet(0).getRet());
      System.out.println(
          ":" + ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()));
      System.out.println("Result:" + Hex.toHexString(result));
      return null;
    }

    final TransactionExtention.Builder texBuilder = TransactionExtention.newBuilder();
    Transaction.Builder transBuilder = Transaction.newBuilder();
    Transaction.raw.Builder rawBuilder =
        transactionExtention.getTransaction().getRawData().toBuilder();
    transBuilder.setRawData(rawBuilder);
    for (int i = 0; i < transactionExtention.getTransaction().getSignatureCount(); i++) {
      ByteString s = transactionExtention.getTransaction().getSignature(i);
      transBuilder.setSignature(i, s);
    }
    for (int i = 0; i < transactionExtention.getTransaction().getRetCount(); i++) {
      Result r = transactionExtention.getTransaction().getRet(i);
      transBuilder.setRet(i, r);
    }
    texBuilder.setTransaction(transBuilder);
    texBuilder.setResult(transactionExtention.getResult());
    texBuilder.setTxid(transactionExtention.getTxid());
    transactionExtention = texBuilder.build();
    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return null;
    }
    transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** Clear the ABI of a deployed smart contract, returning the raw TransactionExtention. */
  public static TransactionExtention clearContractAbiForExtention(
      byte[] contractAddress,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;

    ClearABIContract.Builder builder = ClearABIContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));

    ClearABIContract clearAbiContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.clearContractABI(clearAbiContract);
    return transactionExtention;
  }

  /** Trigger a constant contract on a full node, returning the raw TransactionExtention. */
  public static TransactionExtention triggerConstantContractForExtention(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }
    if (tokenId.equalsIgnoreCase("") || tokenId.equalsIgnoreCase("#")) {
      logger.info("tokenid is 0");
      tokenId = "0";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));
    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();
    TransactionExtention transactionExtention =
        blockingStubFull.triggerConstantContract(triggerContract);
    return transactionExtention;
  }

  /** Trigger a constant contract on a solidity node, returning the raw TransactionExtention. */
  public static TransactionExtention triggerSolidityContractForExtention(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletSolidityGrpc.WalletSolidityBlockingStub solidityBlockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention =
        solidityBlockingStubFull.triggerConstantContract(triggerContract);
    return transactionExtention;
  }

  /** Trigger a contract on a full node, returning the raw TransactionExtention. */
  public static TransactionExtention triggerContractForExtention(
      byte[] contractAddress,
      String method,
      String argsStr,
      Boolean isHex,
      long callValue,
      long feeLimit,
      String tokenId,
      long tokenValue,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    if (argsStr.equalsIgnoreCase("#")) {
      logger.info("argsstr is #");
      argsStr = "";
    }

    byte[] owner = ownerAddress;
    byte[] input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));

    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    builder.setTokenId(Long.parseLong(tokenId));
    builder.setCallTokenValue(tokenValue);
    TriggerSmartContract triggerContract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.triggerContract(triggerContract);
    return transactionExtention;
  }

  // ---------------------------------------------------------------------------
  // Update Contract Settings
  // ---------------------------------------------------------------------------

  /** Update the consume-user-resource-percent setting of a deployed contract. */
  public static boolean updateSetting(
      byte[] contractAddress,
      long consumeUserResourcePercent,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    UpdateSettingContract.Builder builder = UpdateSettingContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setConsumeUserResourcePercent(consumeUserResourcePercent);

    UpdateSettingContract updateSettingContract = builder.build();
    TransactionExtention transactionExtention =
        blockingStubFull.updateSetting(updateSettingContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Update the origin energy limit of a deployed contract. */
  public static boolean updateEnergyLimit(
      byte[] contractAddress,
      long originEnergyLimit,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    byte[] owner = ownerAddress;
    UpdateEnergyLimitContract.Builder builder = UpdateEnergyLimitContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setContractAddress(ByteString.copyFrom(contractAddress));
    builder.setOriginEnergyLimit(originEnergyLimit);

    UpdateEnergyLimitContract updateEnergyLimitContract = builder.build();
    TransactionExtention transactionExtention =
        blockingStubFull.updateEnergyLimit(updateEnergyLimitContract);
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      System.out.println("RPC create trx failed!");
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    if (transactionExtention == null) {
      return false;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return false;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  // ---------------------------------------------------------------------------
  // Contract String Utility
  // ---------------------------------------------------------------------------

  /** Extract a string message from a contract return byte array. */
  public static String getContractStringMsg(byte[] contractMsgArray) {
    int resultLenth = ByteArray.toInt(ByteArray.subArray(contractMsgArray, 32, 64));
    return ByteArray.toStr(ByteArray.subArray(contractMsgArray, 64, 64 + resultLenth));
  }

  // ---------------------------------------------------------------------------
  // Estimate Energy
  // ---------------------------------------------------------------------------

  /** Estimate energy required to trigger a smart contract on a full node. */
  public static Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergy(
      WalletGrpc.WalletBlockingStub blockingStubFull,
      byte[] owner,
      byte[] contractAddress,
      long callValue,
      String method,
      String argsStr,
      Boolean isHex,
      long tokenValue,
      String tokenId
      ) {
    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    if (contractAddress != null) {
      builder.setContractAddress(ByteString.copyFrom(contractAddress));
    }
    byte[] input = new byte[0];
    if (!method.equalsIgnoreCase("#")) {
      input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));
    }
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    if (tokenId != null && tokenId != "") {
      builder.setCallTokenValue(tokenValue);
      builder.setTokenId(Long.parseLong(tokenId));
    }
    GrpcAPI.EstimateEnergyMessage estimateEnergyMessage = blockingStubFull.estimateEnergy(builder.build());
    return Optional.ofNullable(estimateEnergyMessage);
  }

  /** Estimate energy required to trigger a smart contract on a solidity node. */
  public static Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergySolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull,
      byte[] owner,
      byte[] contractAddress,
      long callValue,
      String method,
      String argsStr,
      Boolean isHex,
      long tokenValue,
      String tokenId
  ) {
    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    if (contractAddress != null) {
      builder.setContractAddress(ByteString.copyFrom(contractAddress));
    }
    byte[] input = new byte[0];
    if (!method.equalsIgnoreCase("#")) {
      input = Hex.decode(AbiUtil.parseMethod(method, argsStr, isHex));
    }
    builder.setData(ByteString.copyFrom(input));
    builder.setCallValue(callValue);
    if (tokenId != null && tokenId != "") {
      builder.setCallTokenValue(tokenValue);
      builder.setTokenId(Long.parseLong(tokenId));
    }
    GrpcAPI.EstimateEnergyMessage estimateEnergyMessage = blockingStubFull.estimateEnergy(builder.build());
    return Optional.ofNullable(estimateEnergyMessage);
  }

  /** Estimate energy required to deploy a smart contract. */
  public static Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyDeployContract(
          WalletGrpc.WalletBlockingStub blockingStubFull,
          byte[] owner,
          long callValue,
          long tokenValue,
          String tokenId,
          String code
  ) {
    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setData(ByteString.copyFrom(Hex.decode(code)));
    builder.setCallValue(callValue);
    if (tokenId != null && tokenId != "") {
      builder.setCallTokenValue(tokenValue);
      builder.setTokenId(Long.parseLong(tokenId));
    }
    GrpcAPI.EstimateEnergyMessage estimateEnergyMessage = blockingStubFull.estimateEnergy(builder.build());
    return Optional.ofNullable(estimateEnergyMessage);
  }

  /** Estimate and log energy for a contract deployment (currently a no-op placeholder). */
  public static void estimateDeployContractEnergy(
          String code,
          long value,
          String tokenId,
          long tokenValue,
          byte[] ownerAddress,
          WalletGrpc.WalletBlockingStub blockingStubFull
  ) {
//    String httpnode = Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list")
//            .get(1);
    try {
      logger.info("temp skip estimateDeployContractEnergy");

//      logger.info("triggerConstantContract ------------ start -----------");
//      HttpResponse response = HttpMethod
//              .triggerConstantContractWithData(
//                      httpnode,
//                      ownerAddress, null, null, null, code, value, tokenValue, Long.parseLong(tokenId));
//      JSONObject jsonObject = HttpMethod.parseResponseContent(response);
//      HttpMethod.printJsonContent(jsonObject);
//      Long constantEnergy = jsonObject.getLong("energy_used");
//      logger.info("constantEnergy:" + constantEnergy);
//
//      TransactionExtention trx = PublicMethod
//              .triggerConstantContractDeployContract(
//                      code, ownerAddress, value, tokenId, tokenValue, blockingStubFull);
//      Long grpcConstantEnergy = trx.getEnergyUsed();
//      logger.info("grpcConstantEnergy:" + grpcConstantEnergy);
//      Assert.assertEquals(grpcConstantEnergy.longValue(), constantEnergy.longValue());
//      logger.info("triggerConstantContract ------------ end    -----------");
//
//      Long energyFee = PublicMethod.getChainParametersValue(
//              ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);
//      logger.info("energyFee:" + energyFee);


//      logger.info("EstimateEnergy -------- start ------");
//      response = HttpMethod.getEstimateEnergyDeployContract(httpnode,
//              ownerAddress, null, null, null, code, value, tokenValue, Long.parseLong(tokenId), true);
//      jsonObject = HttpMethod.parseResponseContent(response);
//      HttpMethod.printJsonContent(jsonObject);
//      Long estimateEnergy = jsonObject.getLong("energy_required");
//      logger.info("estimateEnergy:" + estimateEnergy);


//      Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
//              PublicMethod.estimateEnergyDeployContract(blockingStubFull2,
//                      ownerAddress,
//                      value,
//                      tokenValue,
//                      tokenId,
//                      code);
//      logger.info(estimateEnergyMessage.get().toString());
//      Long grpcEstimateEnergy = estimateEnergyMessage.get().getEnergyRequired();
//      logger.info("grpcEstimateEnergy: " + grpcEstimateEnergy);
//      logger.info("EstimateEnergy ------------ end    -----------");

//      logger.info("(estimateEnergy - constantEnergy) * energyFee: "
//              + (estimateEnergy - constantEnergy) * energyFee);
//      Assert.assertEquals(grpcEstimateEnergy.longValue(), estimateEnergy.longValue());
//      Assert.assertTrue((estimateEnergy - constantEnergy) * energyFee < 1000000L);

    } catch (Exception e) {
      logger.error("EnergyEstimateDeploy: catch Exception!!");
      e.printStackTrace();
    }

  }

  /** Trigger a constant contract call for deploying a contract (used for energy estimation). */
  public static TransactionExtention triggerConstantContractDeployContract(
          String code,
          byte[] ownerAddress,
          long callValue,
          String tokenId,
          long tokenValue,
          WalletGrpc.WalletBlockingStub blockingStubFull) {

    byte[] owner = ownerAddress;
    TriggerSmartContract.Builder builder = TriggerSmartContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setData(ByteString.copyFrom(Hex.decode(code)));
    builder.setCallValue(callValue);
    if (tokenId != null && !tokenId.equals("")) {
      builder.setTokenId(Long.parseLong(tokenId));
      builder.setCallTokenValue(tokenValue);
    }
    TriggerSmartContract triggerContract = builder.build();
    TransactionExtention transactionExtention =
            blockingStubFull.triggerConstantContract(triggerContract);
    return transactionExtention;
  }

  // ---------------------------------------------------------------------------
  // Library Address Replacement (private helpers)
  // ---------------------------------------------------------------------------

  /** Replace library placeholders in bytecode with actual addresses. */
  private static byte[] replaceLibraryAddress(String code, String libraryAddressPair) {

    String[] libraryAddressList = libraryAddressPair.split("[,]");

    for (int i = 0; i < libraryAddressList.length; i++) {
      String cur = libraryAddressList[i];

      int lastPosition = cur.lastIndexOf(":");
      if (-1 == lastPosition) {
        throw new RuntimeException("libraryAddress delimit by ':'");
      }
      String libraryName = cur.substring(0, lastPosition);
      String addr = cur.substring(lastPosition + 1);
      String libraryAddressHex =
          ByteArray.toHexString(WalletClient.decodeFromBase58Check(addr)).substring(2);

      String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
      String beReplaced = "__" + libraryName + repeated;
      Matcher m = Pattern.compile(beReplaced).matcher(code);
      code = m.replaceAll(libraryAddressHex);
    }
    PublicMethod.code = code;

    return Hex.decode(code);
  }

  /** Replace library placeholders in bytecode with compiler version awareness. */
  private static byte[] replaceLibraryAddresscompilerVersion(
      String code, String libraryAddressPair, String compilerVersion) {

    String[] libraryAddressList = libraryAddressPair.split("[,]");

    for (int i = 0; i < libraryAddressList.length; i++) {
      String cur = libraryAddressList[i];

      int lastPosition = cur.lastIndexOf(":");
      if (-1 == lastPosition) {
        throw new RuntimeException("libraryAddress delimit by ':'");
      }
      String libraryName = cur.substring(0, lastPosition);
      String addr = cur.substring(lastPosition + 1);
      String libraryAddressHex;
      libraryAddressHex =
          (new String(
                  Hex.encode(WalletClient.decodeFromBase58Check(addr)), StandardCharsets.US_ASCII))
              .substring(2);

      String beReplaced;
      if (compilerVersion == null) {
        // old version
        String repeated = new String(new char[40 - libraryName.length() - 2]).replace("\0", "_");
        beReplaced = "__" + libraryName + repeated;
      } else if (compilerVersion.equalsIgnoreCase("v5")) {
        // 0.5.4 version
        String libraryNameKeccak256 =
            ByteArray.toHexString(Hash.sha3(ByteArray.fromString(libraryName))).substring(0, 34);
        beReplaced = "__\\$" + libraryNameKeccak256 + "\\$__";
      } else {
        throw new RuntimeException("unknown compiler version.");
      }

      Matcher m = Pattern.compile(beReplaced).matcher(code);
      code = m.replaceAll(libraryAddressHex);
    }

    PublicMethod.code = code;

    return Hex.decode(code);
  }
}
