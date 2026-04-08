package stest.tron.wallet.common.client.utils;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UpdateAssetContract;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;
import org.tron.protos.contract.ExchangeContract.ExchangeInjectContract;
import org.tron.protos.contract.ExchangeContract.ExchangeTransactionContract;
import org.tron.protos.contract.ExchangeContract.ExchangeWithdrawContract;
import org.tron.protos.contract.MarketContract;

@Slf4j
/** Helper for TRC-10/TRC-20 asset operations: issue, transfer, participate, and exchange. */
public class AssetHelper {

  // ======================== TRC-10 Asset Issue ========================

  /** Create a TRC-10 asset issue and return the transaction ID. */
  public static String createAssetIssueGetTxid(
      byte[] address,
      String name,
      String abbreviation,
      Long totalSupply,
      Integer trxNum,
      Integer icoNum,
      Long startTime,
      Long endTime,
      Integer voteScore,
      String description,
      String url,
      Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit,
      Long fronzenAmount,
      Long frozenDay,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    //// Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setAbbr(ByteString.copyFrom(abbreviation.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return null;
      }
      transaction = PublicMethod.signTransaction(ecKey, transaction);

      GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /** Create a TRC-10 asset issue without abbreviation. */
  public static Boolean createAssetIssue(
      byte[] address,
      String name,
      Long totalSupply,
      Integer trxNum,
      Integer icoNum,
      Long startTime,
      Long endTime,
      Integer voteScore,
      String description,
      String url,
      Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit,
      Long fronzenAmount,
      Long frozenDay,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    //// Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = PublicMethod.signTransaction(ecKey, transaction);

      GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

      return response.getResult();
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /** Create a TRC-10 asset issue with abbreviation. */
  public static Boolean createAssetIssue(
      byte[] address,
      String name,
      String abbreviation,
      Long totalSupply,
      Integer trxNum,
      Integer icoNum,
      Long startTime,
      Long endTime,
      Integer voteScore,
      String description,
      String url,
      Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit,
      Long fronzenAmount,
      Long frozenDay,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setAbbr(ByteString.copyFrom(abbreviation.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = PublicMethod.signTransaction(ecKey, transaction);

      GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

      return response.getResult();
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /** Create a TRC-10 asset issue with precision. */
  public static Boolean createAssetIssue(
      byte[] address,
      String name,
      Long totalSupply,
      Integer trxNum,
      Integer icoNum,
      int precision,
      Long startTime,
      Long endTime,
      Integer voteScore,
      String description,
      String url,
      Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit,
      Long fronzenAmount,
      Long frozenDay,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setPrecision(precision);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      Protocol.Transaction transaction = blockingStubFull.createAssetIssue(builder.build());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction == null");
        return false;
      }
      transaction = PublicMethod.signTransaction(ecKey, transaction);

      GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

      return response.getResult();
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /** Create a TRC-10 asset issue using extension API and return the gRPC Return. */
  public static Return createAssetIssue2(
      byte[] address,
      String name,
      Long totalSupply,
      Integer trxNum,
      Integer icoNum,
      Long startTime,
      Long endTime,
      Integer voteScore,
      String description,
      String url,
      Long freeAssetNetLimit,
      Long publicFreeAssetNetLimit,
      Long fronzenAmount,
      Long frozenDay,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    // Protocol.Account search = queryAccount(ecKey, blockingStubFull);
    try {
      AssetIssueContract.Builder builder = AssetIssueContract.newBuilder();
      builder.setOwnerAddress(ByteString.copyFrom(address));
      builder.setName(ByteString.copyFrom(name.getBytes()));
      builder.setTotalSupply(totalSupply);
      builder.setTrxNum(trxNum);
      builder.setNum(icoNum);
      builder.setStartTime(startTime);
      builder.setEndTime(endTime);
      builder.setVoteScore(voteScore);
      builder.setDescription(ByteString.copyFrom(description.getBytes()));
      builder.setUrl(ByteString.copyFrom(url.getBytes()));
      builder.setFreeAssetNetLimit(freeAssetNetLimit);
      builder.setPublicFreeAssetNetLimit(publicFreeAssetNetLimit);
      // builder.setPublicFreeAssetNetUsage();
      // builder.setPublicLatestFreeNetTime();
      AssetIssueContract.FrozenSupply.Builder frozenBuilder =
          AssetIssueContract.FrozenSupply.newBuilder();
      frozenBuilder.setFrozenAmount(fronzenAmount);
      frozenBuilder.setFrozenDays(frozenDay);
      builder.addFrozenSupply(0, frozenBuilder);

      TransactionExtention transactionExtention =
          blockingStubFull.createAssetIssue2(builder.build());

      if (transactionExtention == null) {
        return transactionExtention.getResult();
      }
      Return ret = transactionExtention.getResult();
      if (!ret.getResult()) {
        System.out.println("Code = " + ret.getCode());
        System.out.println("Message = " + ret.getMessage().toStringUtf8());
        return ret;
      } else {
        System.out.println("Code = " + ret.getCode());
        System.out.println("Message = " + ret.getMessage().toStringUtf8());
      }
      Transaction transaction = transactionExtention.getTransaction();
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        System.out.println("Transaction is empty");
        return transactionExtention.getResult();
      }
      System.out.println(
          "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
      transaction = PublicMethod.signTransaction(ecKey, transaction);

      GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
      if (response.getResult() == false) {
        return response;
      } else {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      return ret;
    } catch (Exception ex) {
      ex.printStackTrace();
      // return false;
      return Return.getDefaultInstance();
    }
  }

  // ======================== Asset Transfer & Participate ========================

  /** Participate in a TRC-10 asset issue. */
  public static boolean participateAssetIssue(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] from,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    ParticipateAssetIssueContract.Builder builder = ParticipateAssetIssueContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    ParticipateAssetIssueContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.participateAssetIssue(contract);
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Participate in a TRC-10 asset issue using extension API and return the gRPC Return. */
  public static Return participateAssetIssue2(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] from,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    ParticipateAssetIssueContract.Builder builder = ParticipateAssetIssueContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(from);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);
    ParticipateAssetIssueContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.participateAssetIssue2(contract);
    if (transactionExtention == null) {
      return transactionExtention.getResult();
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return transactionExtention.getResult();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    // Protocol.Transaction transaction = blockingStubFull.participateAssetIssue(contract);

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return response;
    } else {
      return ret;
    }
  }

  /** Transfer TRC-10 asset tokens to another address. */
  public static boolean transferAsset(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.transferAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      if (transaction == null) {
        logger.info("transaction == null");
      } else {
        logger.info("transaction.getRawData().getContractCount() == 0");
      }
      return false;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);

    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Transfer TRC-10 asset using extension API and return the gRPC Return. */
  public static Return transferAssetForReturn(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] address,
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

    TransferAssetContract.Builder builder = TransferAssetContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsName = ByteString.copyFrom(assertName);
    ByteString bsOwner = ByteString.copyFrom(address);
    builder.setToAddress(bsTo);
    builder.setAssetName(bsName);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferAssetContract contract = builder.build();
    TransactionExtention transaction = blockingStubFull.transferAsset2(contract);

    if (transaction == null) {
      return transaction.getResult();
    }
    Return ret = transaction.getResult();
    return ret;
  }

  // ======================== Asset Update ========================

  /** Update a TRC-10 asset's description, URL, and bandwidth limits. */
  public static boolean updateAsset(
      byte[] address,
      byte[] description,
      byte[] url,
      long newLimit,
      long newPublicLimit,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UpdateAssetContract.Builder builder = UpdateAssetContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(address);
    builder.setDescription(ByteString.copyFrom(description));
    builder.setUrl(ByteString.copyFrom(url));
    builder.setNewLimit(newLimit);
    builder.setNewPublicLimit(newPublicLimit);
    builder.setOwnerAddress(basAddreess);

    UpdateAssetContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Update a TRC-10 asset using extension API and return the gRPC Return. */
  public static Return updateAsset2(
      byte[] address,
      byte[] description,
      byte[] url,
      long newLimit,
      long newPublicLimit,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UpdateAssetContract.Builder builder = UpdateAssetContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(address);
    builder.setDescription(ByteString.copyFrom(description));
    builder.setUrl(ByteString.copyFrom(url));
    builder.setNewLimit(newLimit);
    builder.setNewPublicLimit(newPublicLimit);
    builder.setOwnerAddress(basAddreess);

    UpdateAssetContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.updateAsset2(contract);
    if (transactionExtention == null) {
      return transactionExtention.getResult();
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret;
    } else {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return transactionExtention.getResult();
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return response;
    }
    return ret;
  }

  // ======================== Asset Query ========================

  /** Get the TRC-10 asset balance for an account by asset ID using private key. */
  public static Long getAssetBalanceByAssetId(
      ByteString assetId, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Account assetOwnerAccount = PublicMethod.queryAccount(priKey, blockingStubFull);
    Long assetOwnerAssetBalance = 0L;
    for (String id : assetOwnerAccount.getAssetV2Map().keySet()) {
      if (assetId.toStringUtf8().equalsIgnoreCase(id)) {
        assetOwnerAssetBalance = assetOwnerAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("asset balance is " + assetOwnerAssetBalance);
    return assetOwnerAssetBalance;
  }

  /** Get the TRC-10 asset balance for an account by asset ID using address bytes. */
  public static Long getAssetBalanceByAssetId(
      ByteString assetId, byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Account assetOwnerAccount = PublicMethod.queryAccount(address, blockingStubFull);
    Long assetOwnerAssetBalance = 0L;
    for (String id : assetOwnerAccount.getAssetV2Map().keySet()) {
      if (assetId.toStringUtf8().equalsIgnoreCase(id)) {
        assetOwnerAssetBalance = assetOwnerAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("asset balance is " + assetOwnerAssetBalance);
    return assetOwnerAssetBalance;
  }

  /** Get the count of a specific asset held by an account. */
  public static Long getAssetIssueValue(
      byte[] accountAddress,
      ByteString assetIssueId,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Long assetIssueCount = 0L;
    Account contractAccount = PublicMethod.queryAccount(accountAddress, blockingStubFull);
    Map<String, Long> createAssetIssueMap = contractAccount.getAssetV2Map();
    for (Map.Entry<String, Long> entry : createAssetIssueMap.entrySet()) {
      if (assetIssueId.toStringUtf8().equals(entry.getKey())) {
        assetIssueCount = entry.getValue();
      }
    }
    return assetIssueCount;
  }

  /** Get an asset issue contract by its name from fullnode. */
  public static AssetIssueContract getAssetIssueByName(
      String assetName, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    return blockingStubFull.getAssetIssueByName(request);
  }

  /** Get an asset issue contract by its name from solidity node. */
  public static AssetIssueContract getAssetIssueByNameFromSolidity(
      String assetName, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    return blockingStubFull.getAssetIssueByName(request);
  }

  /** Get a list of asset issues by name from fullnode. */
  public static Optional<AssetIssueList> getAssetIssueListByName(
      String assetName, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueListByName(request);
    return Optional.ofNullable(assetIssueList);
  }

  /** Get a list of asset issues by name from solidity node. */
  public static Optional<AssetIssueList> getAssetIssueListByNameFromSolidity(
      String assetName, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueListByName(request);
    return Optional.ofNullable(assetIssueList);
  }

  /** List all asset issues from solidity node. */
  public static Optional<GrpcAPI.AssetIssueList> listAssetIssueFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    GrpcAPI.AssetIssueList assetIssueList =
        blockingStubFull.getAssetIssueList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(assetIssueList);
  }

  /** List asset issues with pagination from solidity node. */
  public static Optional<GrpcAPI.AssetIssueList> listAssetIssuepaginatedFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull, Long offset, Long limit) {
    GrpcAPI.PaginatedMessage.Builder pageMessageBuilder = GrpcAPI.PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    AssetIssueList assetIssueList =
        blockingStubFull.getPaginatedAssetIssueList(pageMessageBuilder.build());
    return Optional.ofNullable(assetIssueList);
  }

  /** Get an asset issue contract by its ID from fullnode. */
  public static AssetIssueContract getAssetIssueById(
      String assetId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetIdBs = ByteString.copyFrom(assetId.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetIdBs).build();
    return blockingStubFull.getAssetIssueById(request);
  }

  /** Get an asset issue contract by its ID from solidity node. */
  public static AssetIssueContract getAssetIssueByIdFromSolidity(
      String assetId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetIdBs = ByteString.copyFrom(assetId.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetIdBs).build();
    return blockingStubFull.getAssetIssueById(request);
  }

  /** Get all asset issues created by an account. */
  public static Optional<AssetIssueList> getAssetIssueByAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueByAccount(request);
    return Optional.ofNullable(assetIssueList);
  }

  // ======================== DEX Exchange ========================

  /** Create a new DEX exchange pair. */
  public static Boolean exchangeCreate(
      byte[] firstTokenId,
      long firstTokenBalance,
      byte[] secondTokenId,
      long secondTokenBalance,
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

    ExchangeCreateContract.Builder builder = ExchangeCreateContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setFirstTokenId(ByteString.copyFrom(firstTokenId))
        .setFirstTokenBalance(firstTokenBalance)
        .setSecondTokenId(ByteString.copyFrom(secondTokenId))
        .setSecondTokenBalance(secondTokenBalance);
    ExchangeCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeCreate(contract);
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
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));

    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** Inject tokens into an existing DEX exchange. */
  public static Boolean injectExchange(
      long exchangeId,
      byte[] tokenId,
      long quant,
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

    ExchangeInjectContract.Builder builder = ExchangeInjectContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    ExchangeInjectContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeInject(contract);
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
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** Withdraw tokens from a DEX exchange. */
  public static boolean exchangeWithdraw(
      long exchangeId,
      byte[] tokenId,
      long quant,
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

    ExchangeWithdrawContract.Builder builder = ExchangeWithdrawContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant);
    ExchangeWithdrawContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeWithdraw(contract);
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
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));

    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Execute a trade on a DEX exchange. */
  public static boolean exchangeTransaction(
      long exchangeId,
      byte[] tokenId,
      long quant,
      long expected,
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

    ExchangeTransactionContract.Builder builder = ExchangeTransactionContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setExchangeId(exchangeId)
        .setTokenId(ByteString.copyFrom(tokenId))
        .setQuant(quant)
        .setExpected(expected);
    ExchangeTransactionContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.exchangeTransaction(contract);
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
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));

    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  // ======================== Exchange Query ========================

  /** Get all DEX exchanges from fullnode. */
  public static Optional<ExchangeList> getExchangeList(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ExchangeList exchangeList = blockingStubFull.listExchanges(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(exchangeList);
  }

  /** Get all DEX exchanges from solidity node. */
  public static Optional<ExchangeList> getExchangeList(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ExchangeList exchangeList =
        blockingStubSolidity.listExchanges(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(exchangeList);
  }

  /** Get a DEX exchange by ID from solidity node. */
  public static Optional<Exchange> getExchange(
      String id, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    BytesMessage request =
        BytesMessage.newBuilder()
            .setValue(ByteString.copyFrom(ByteArray.fromLong(Long.parseLong(id))))
            .build();
    Exchange exchange = blockingStubSolidity.getExchangeById(request);
    return Optional.ofNullable(exchange);
  }

  /** Get a DEX exchange by ID from fullnode. */
  public static Optional<Exchange> getExchange(
      String id, WalletGrpc.WalletBlockingStub blockingStubFull) {
    BytesMessage request =
        BytesMessage.newBuilder()
            .setValue(ByteString.copyFrom(ByteArray.fromLong(Long.parseLong(id))))
            .build();
    Exchange exchange = blockingStubFull.getExchangeById(request);
    return Optional.ofNullable(exchange);
  }

  /** Get the exchange ID for exchanges created by a specific address. */
  public static Long getExchangeIdByCreatorAddress(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    List<Exchange> exchangeList =  PublicMethod.getExchangeList(blockingStubFull).get().getExchangesList();
    for (int i = 0; i < exchangeList.size(); i++){
      Exchange exchange = exchangeList.get(i);
      if (exchange.getCreatorAddress().equals(ByteString.copyFrom(address))) {
        return exchange.getExchangeId();
      }
    }
    return 0L;
  }

  // ======================== Market Orders ========================

  /** Place a sell order on the built-in DEX market. */
  public static String marketSellAsset(
      byte[] owner,
      String priKey,
      byte[] sellTokenId,
      long sellTokenQuantity,
      byte[] buyTokenId,
      long buyTokenQuantity,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    MarketContract.MarketSellAssetContract.Builder builder =
        MarketContract.MarketSellAssetContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setSellTokenQuantity(sellTokenQuantity)
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
        .setBuyTokenQuantity(buyTokenQuantity);

    TransactionExtention transactionExtention = blockingStubFull.marketSellAsset(builder.build());
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

    if (transaction.getRawData().getContract(0).getType()
        == ContractType.ShieldedTransferContract) {
      return null;
    }

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    String txid =
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));

    System.out.println("trigger txid = " + txid);
    return txid;
  }

  /** Place a sell order on the built-in DEX market and return the gRPC Return. */
  public static Return marketSellAssetGetResposne(
      byte[] owner,
      String priKey,
      byte[] sellTokenId,
      long sellTokenQuantity,
      byte[] buyTokenId,
      long buyTokenQuantity,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    MarketContract.MarketSellAssetContract.Builder builder =
        MarketContract.MarketSellAssetContract.newBuilder();
    builder
        .setOwnerAddress(ByteString.copyFrom(owner))
        .setSellTokenId(ByteString.copyFrom(sellTokenId))
        .setSellTokenQuantity(sellTokenQuantity)
        .setBuyTokenId(ByteString.copyFrom(buyTokenId))
        .setBuyTokenQuantity(buyTokenQuantity);

    TransactionExtention transactionExtention = blockingStubFull.marketSellAsset(builder.build());

    return transactionExtention.getResult();
  }

  /** Cancel an existing market order by order ID. */
  public static String marketCancelOrder(
      byte[] owner, String priKey, byte[] orderId, WalletGrpc.WalletBlockingStub blockingStubFull) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    MarketContract.MarketCancelOrderContract.Builder builder =
        MarketContract.MarketCancelOrderContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setOrderId(ByteString.copyFrom(orderId));

    TransactionExtention transactionExtention = blockingStubFull.marketCancelOrder(builder.build());

    if (transactionExtention == null) {
      return null;
    }
    Return ret = transactionExtention.getResult();
    if (!ret.getResult()) {
      System.out.println("Code = " + ret.getCode());
      System.out.println("Message = " + ret.getMessage().toStringUtf8());
      return ret.getMessage().toStringUtf8();
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      System.out.println("Transaction is empty");
      return null;
    }

    if (transaction.getRawData().getContract(0).getType()
        == ContractType.ShieldedTransferContract) {
      return null;
    }

    transaction = PublicMethod.signTransaction(ecKey, transaction);
    PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    String txid =
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));

    System.out.println("trigger txid = " + txid);

    return txid;
  }

  /** Cancel a market order and return the gRPC Return. */
  public static Return marketCancelOrderGetResposne(
      byte[] owner, String priKey, byte[] orderId, WalletGrpc.WalletBlockingStub blockingStubFull) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    MarketContract.MarketCancelOrderContract.Builder builder =
        MarketContract.MarketCancelOrderContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner)).setOrderId(ByteString.copyFrom(orderId));

    TransactionExtention transactionExtention = blockingStubFull.marketCancelOrder(builder.build());

    if (transactionExtention == null) {
      return null;
    }
    return transactionExtention.getResult();
  }

  // ======================== Market Query ========================

  /** Get all market orders placed by an account from fullnode. */
  public static Optional<Protocol.MarketOrderList> getMarketOrderByAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage request = BytesMessage.newBuilder().setValue(addressBs).build();

    Protocol.MarketOrderList marketOrderList;
    marketOrderList = blockingStubFull.getMarketOrderByAccount(request);
    return Optional.ofNullable(marketOrderList);
  }

  /** Get all market orders placed by an account from solidity node. */
  public static Optional<Protocol.MarketOrderList> getMarketOrderByAccountSolidity(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage request = BytesMessage.newBuilder().setValue(addressBs).build();

    Protocol.MarketOrderList marketOrderList;
    marketOrderList = blockingStubSolidity.getMarketOrderByAccount(request);
    return Optional.ofNullable(marketOrderList);
  }

  /** Get a specific market order by its ID from fullnode. */
  public static Optional<Protocol.MarketOrder> getMarketOrderById(
      byte[] order, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString orderBytes = ByteString.copyFrom(order);
    BytesMessage request = BytesMessage.newBuilder().setValue(orderBytes).build();
    Protocol.MarketOrder orderPair = blockingStubFull.getMarketOrderById(request);
    return Optional.ofNullable(orderPair);
  }

  /** Get a specific market order by its ID from solidity node. */
  public static Optional<Protocol.MarketOrder> getMarketOrderByIdSolidity(
      byte[] order, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ByteString orderBytes = ByteString.copyFrom(order);
    BytesMessage request = BytesMessage.newBuilder().setValue(orderBytes).build();
    Protocol.MarketOrder orderPair = blockingStubSolidity.getMarketOrderById(request);
    return Optional.ofNullable(orderPair);
  }

  /** Get market prices for a specific trading pair from fullnode. */
  public static Optional<Protocol.MarketPriceList> getMarketPriceByPair(
      byte[] sellTokenId, byte[] buyTokenId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Protocol.MarketOrderPair request =
        Protocol.MarketOrderPair.newBuilder()
            .setSellTokenId(ByteString.copyFrom(sellTokenId))
            .setBuyTokenId(ByteString.copyFrom(buyTokenId))
            .build();

    Protocol.MarketPriceList marketPriceList = blockingStubFull.getMarketPriceByPair(request);
    return Optional.ofNullable(marketPriceList);
  }

  /** Get market order list for a specific trading pair from fullnode. */
  public static Optional<Protocol.MarketOrderList> getMarketOrderListByPair(
      byte[] sellTokenId, byte[] buyTokenId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Protocol.MarketOrderPair request =
        Protocol.MarketOrderPair.newBuilder()
            .setSellTokenId(ByteString.copyFrom(sellTokenId))
            .setBuyTokenId(ByteString.copyFrom(buyTokenId))
            .build();

    Protocol.MarketOrderList marketOrderList = blockingStubFull.getMarketOrderListByPair(request);
    return Optional.ofNullable(marketOrderList);
  }

  /** Get market order list for a specific trading pair from solidity node. */
  public static Optional<Protocol.MarketOrderList> getMarketOrderListByPairSolidity(
      byte[] sellTokenId,
      byte[] buyTokenId,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Protocol.MarketOrderPair request =
        Protocol.MarketOrderPair.newBuilder()
            .setSellTokenId(ByteString.copyFrom(sellTokenId))
            .setBuyTokenId(ByteString.copyFrom(buyTokenId))
            .build();

    Protocol.MarketOrderList marketOrderList =
        blockingStubSolidity.getMarketOrderListByPair(request);
    return Optional.ofNullable(marketOrderList);
  }

  /** Get all available market trading pairs from fullnode. */
  public static Optional<Protocol.MarketOrderPairList> getMarketPairList(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Protocol.MarketOrderPairList marketOrderList =
        blockingStubFull.getMarketPairList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(marketOrderList);
  }

  /** Get all available market trading pairs from solidity node. */
  public static Optional<Protocol.MarketOrderPairList> getMarketPairListSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Protocol.MarketOrderPairList marketOrderList =
        blockingStubSolidity.getMarketPairList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(marketOrderList);
  }
}
