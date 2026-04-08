package stest.tron.wallet.common.client.utils;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.CanDelegatedMaxSizeRequestMessage;
import org.tron.api.GrpcAPI.CanDelegatedMaxSizeResponseMessage;
import org.tron.api.GrpcAPI.CanWithdrawUnfreezeAmountRequestMessage;
import org.tron.api.GrpcAPI.CanWithdrawUnfreezeAmountResponseMessage;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.DelegatedResourceMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.GetAvailableUnfreezeCountRequestMessage;
import org.tron.api.GrpcAPI.GetAvailableUnfreezeCountResponseMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.FreezeV2;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.BalanceContract.CancelAllUnfreezeV2Contract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import org.tron.protos.contract.StorageContract.BuyStorageContract;
import org.tron.protos.contract.StorageContract.SellStorageContract;

/**
 * Helper class for resource and staking related operations including freeze, unfreeze,
 * delegate, storage, and resource query methods extracted from PublicMethod.
 */
@Slf4j
public class ResourceHelper {

  // --- Freeze Balance (V1 + auto-routing) ---

  /** Freeze balance using V1 or V2 depending on whether the freeze-v2 proposal is active. */
  public static Boolean freezeBalance(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    if(PublicMethod.getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays.getProposalName(),
        blockingStubFull) <= 0) {
      return freezeBalanceV1(addRess,freezeBalance,freezeDuration,0,priKey,blockingStubFull);
    } else {
      return freezeBalanceV2(addRess,freezeBalance,0,priKey,blockingStubFull);
    }
  }

  /** Freeze balance V1 for a specific receiver address. */
  public static Boolean freezeBalanceV1ForReceiver(byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;


    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder
        .setOwnerAddress(byteAddreess)
        .setFrozenBalance(frozenBalance)
        .setResourceValue(resourceCode)
        .setFrozenDuration(frozenDuration);

    if(null != receiverAddress) {
      builder.setReceiverAddress(ByteString.copyFrom(receiverAddress));
    }

    FreezeBalanceContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.freezeBalance(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }


  /** Freeze balance V1 without a receiver (self-freeze). */
  public static Boolean freezeBalanceV1(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return freezeBalanceV1ForReceiver(addRess,freezeBalance,freezeDuration,resourceCode,null,priKey,blockingStubFull);
  }

  /** Freeze balance using the freezeBalance2 RPC and return the gRPC Return result. */
  public static Return freezeBalance2(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address = addRess;
    long frozenBalance = freezeBalance;
    long frozenDuration = freezeDuration;
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    Protocol.Block currentBlock =
        blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    final Long beforeBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Protocol.Account beforeFronzen = PublicMethod.queryAccount(priKey, blockingStubFull);
    Long beforeFrozenBalance = 0L;
    // Long beforeBandwidth     = beforeFronzen.getBandwidth();
    if (beforeFronzen.getFrozenCount() != 0) {
      beforeFrozenBalance = beforeFronzen.getFrozen(0).getFrozenBalance();
      // beforeBandwidth     = beforeFronzen.getBandwidth();
      // logger.info(Long.toString(beforeFronzen.getBandwidth()));
      logger.info(Long.toString(beforeFronzen.getFrozen(0).getFrozenBalance()));
    }

    FreezeBalanceContract.Builder builder = FreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);

    builder
        .setOwnerAddress(byteAddreess)
        .setFrozenBalance(frozenBalance)
        .setFrozenDuration(frozenDuration);

    FreezeBalanceContract contract = builder.build();

    GrpcAPI.TransactionExtention transactionExtention = blockingStubFull.freezeBalance2(contract);
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

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    if (response.getResult() == false) {
      return response;
    }

    Long afterBlockNum = 0L;

    while (afterBlockNum < beforeBlockNum) {
      Protocol.Block currentBlock1 =
          blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
    }

    Protocol.Account afterFronzen = PublicMethod.queryAccount(priKey, blockingStubFull);
    Long afterFrozenBalance = afterFronzen.getFrozen(0).getFrozenBalance();
    logger.info(Long.toString(afterFronzen.getFrozen(0).getFrozenBalance()));
    logger.info(
        "beforefronen"
            + beforeFrozenBalance.toString()
            + "    afterfronzen"
            + afterFrozenBalance.toString());
    Assert.assertTrue(afterFrozenBalance - beforeFrozenBalance == freezeBalance);
    return ret;
  }

  // --- Freeze Balance V2 ---

  /** Freeze balance V2 and return success boolean. */
  public static Boolean freezeBalanceV2(byte[] addressByte,
      long freezeBalance,
      int resourceCode,
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
    FreezeBalanceV2Contract.Builder builder =  FreezeBalanceV2Contract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    builder
        .setOwnerAddress(byteAddress)
        .setFrozenBalance(freezeBalance)
        .setResourceValue(resourceCode);
    FreezeBalanceV2Contract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.freezeBalanceV2(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    PublicMethod.freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Freeze balance V2 and return the transaction ID string. */
  public static String freezeBalanceV2AndGetTxId(byte[] addressByte,
                                        long freezeBalance,
                                        int resourceCode,
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
    FreezeBalanceV2Contract.Builder builder =  FreezeBalanceV2Contract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    builder
            .setOwnerAddress(byteAddress)
            .setFrozenBalance(freezeBalance)
            .setResourceValue(resourceCode);
    FreezeBalanceV2Contract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.freezeBalanceV2(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return null;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    String freezeV2Txid = ByteArray.toHexString(
            Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return freezeV2Txid;
  }

  /** Get the frozen V2 amount for a specific resource code. */
  public static Long getFrozenV2Amount(byte[] address, int resourceCode,WalletGrpc.WalletBlockingStub blockingStubFull) {
    List<FreezeV2> list = PublicMethod.queryAccount(address,blockingStubFull).getFrozenV2List();
    for(int i = 0; i < list.size();i++) {
      if(list.get(i).getType().getNumber() == resourceCode) {
        return list.get(i).getAmount();
      }
    }
    return 0L;

  }

  // --- Unfreeze Balance ---

  /** Unfreeze balance using V1 or V2 depending on whether the freeze-v2 proposal is active. */
  public static Boolean unFreezeBalance(
      byte[] address,
      String priKey,
      int resourceCode,
      byte[] receiverAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    if(freezeV2ProposalIsOpen(blockingStubFull)) {
      return unFreezeBalanceV2(address,priKey,0,resourceCode,blockingStubFull);
    } else {
      return unFreezeBalanceV1(address,priKey,resourceCode,receiverAddress,blockingStubFull);
    }
  }


  /** Unfreeze balance V1 with optional receiver address. */
  public static Boolean unFreezeBalanceV1(
      byte[] address,
      String priKey,
      int resourceCode,
      byte[] receiverAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UnfreezeBalanceContract.Builder builder = UnfreezeBalanceContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess).setResourceValue(resourceCode);
    if (receiverAddress != null) {
      ByteString receiverAddressBytes = ByteString.copyFrom(receiverAddress);
      builder.setReceiverAddress(receiverAddressBytes);
    }

    UnfreezeBalanceContract contract = builder.build();
    Transaction transaction = blockingStubFull.unfreezeBalance(contract);
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }


  /** Unfreeze balance V2 with specified amount and resource code. */
  public static Boolean unFreezeBalanceV2(
      byte[] address,
      String priKey,
      long unFreezeBalanceAmount,
      int resourceCode,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UnfreezeBalanceV2Contract.Builder builder = UnfreezeBalanceV2Contract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess)
        .setResourceValue(resourceCode).setUnfreezeBalance(unFreezeBalanceAmount);

    UnfreezeBalanceV2Contract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.unfreezeBalanceV2(contract);
    Transaction transaction = transactionExtention.getTransaction();
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    PublicMethod.freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** Unfreeze balance V2 and return the transaction ID string. */
  public static String unFreezeBalanceV2AndGetTxId(
          byte[] address,
          String priKey,
          long unFreezeBalanceAmount,
          int resourceCode,
          WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    UnfreezeBalanceV2Contract.Builder builder = UnfreezeBalanceV2Contract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess)
            .setResourceValue(resourceCode).setUnfreezeBalance(unFreezeBalanceAmount);

    UnfreezeBalanceV2Contract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.unfreezeBalanceV2(contract);
    Transaction transaction = transactionExtention.getTransaction();
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    String txId = ByteArray.toHexString(
            Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return txId;
  }

  // --- Cancel All Unfreeze V2 ---

  /** Cancel all pending unfreeze V2 operations and return success boolean. */
  public static Boolean cancelAllUnFreezeBalanceV2(
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
    CancelAllUnfreezeV2Contract.Builder builder = CancelAllUnfreezeV2Contract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);
    CancelAllUnfreezeV2Contract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.cancelAllUnfreezeV2(contract);
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("cancel unfreeze transaction ==null");
      return false;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Cancel all pending unfreeze V2 operations and return the transaction ID. */
  public static String cancelAllUnFreezeBalanceV2AndGetTxid(
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
    CancelAllUnfreezeV2Contract.Builder builder = CancelAllUnfreezeV2Contract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);
    CancelAllUnfreezeV2Contract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.cancelAllUnfreezeV2(contract);
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("cancel unfreeze transaction ==null");
      return null;
    }
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    String txId = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return txId;
  }

  // --- Withdraw Expire Unfreeze ---

  /** Withdraw expired unfrozen balance and return success boolean. */
  public static Boolean withdrawExpireUnfreeze(
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
    WithdrawExpireUnfreezeContract.Builder builder = WithdrawExpireUnfreezeContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);

    WithdrawExpireUnfreezeContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.withdrawExpireUnfreeze(contract);
    Transaction transaction = transactionExtention.getTransaction();
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    PublicMethod.freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** Withdraw expired unfrozen balance and return the transaction ID. */
  public static String withdrawExpireUnfreezeAndGetTxId(
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
    WithdrawExpireUnfreezeContract.Builder builder = WithdrawExpireUnfreezeContract.newBuilder();
    ByteString byteAddreess = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddreess);

    WithdrawExpireUnfreezeContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.withdrawExpireUnfreeze(contract);
    Transaction transaction = transactionExtention.getTransaction();
    transaction = PublicMethod.signTransaction(ecKey, transaction);
    String freezeV2Txid = ByteArray.toHexString(
            Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);

    return freezeV2Txid;
  }

  // --- Proposal Checks ---

  /** Check whether the freeze V2 proposal (unfreeze delay days) is active. */
  public static Boolean freezeV2ProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethod.getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays
        .getProposalName(), blockingStubFull) > 0;
  }


  /** Check whether the TRON Power proposal (new resource model) is active. */
  public static Boolean tronPowerProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethod.getChainParametersValue(ProposalEnum.GetAllowNewResourceModel
        .getProposalName(), blockingStubFull) == 1;
  }

  /** Check whether the dynamic energy proposal is active. */
  public static Boolean getAllowDynamicEnergyProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethod.getChainParametersValue(ProposalEnum.GetAllowDynamicEnergy
        .getProposalName(), blockingStubFull) == 1;
  }

  // --- Price Queries ---

  /** Get the memo fee from chain parameters. */
  public static Long getProposalMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethod.getChainParametersValue(ProposalEnum.GetMemoFee.getProposalName(),blockingStubFull);
  }

  /** Get the memo fee prices string from the full node. */
  public static String getMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getMemoFee(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Get the energy price history string from the full node. */
  public static String getEnergyPrice(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getEnergyPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Get the energy price history string from the solidity node. */
  public static String getEnergyPriceSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return blockingStubFull.getEnergyPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Get the bandwidth price history string from the full node. */
  public static String getBandwidthPrices(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getBandwidthPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  /** Get the bandwidth price history string from the solidity node. */
  public static String getBandwidthPricesSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return blockingStubFull.getBandwidthPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  // --- Freeze Balance with TRON Power / Energy ---

  /** Freeze balance to get TRON Power, routing to V1 or V2 based on active proposals. */
  public static Boolean freezeBalanceGetTronPower(
      byte[] address,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      ByteString receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    if(tronPowerProposalIsOpen(blockingStubFull) && !freezeV2ProposalIsOpen(blockingStubFull)) {
      return freezeBalanceForReceiver(
          address,
          freezeBalance,
          freezeDuration,
          resourceCode,
          receiverAddress,
          priKey,
          blockingStubFull);
    }


    if(!tronPowerProposalIsOpen(blockingStubFull) && !freezeV2ProposalIsOpen(blockingStubFull)) {
      return freezeBalanceForReceiver(
          address,
          freezeBalance,
          freezeDuration,
          0,
          receiverAddress,
          priKey,
          blockingStubFull);
    }


    if(!tronPowerProposalIsOpen(blockingStubFull) && freezeV2ProposalIsOpen(blockingStubFull)
        && null == receiverAddress) {
      return freezeBalanceV2(address,freezeBalance,0,priKey,blockingStubFull);
    }

    if(tronPowerProposalIsOpen(blockingStubFull)
        && freezeV2ProposalIsOpen(blockingStubFull)
        && null == receiverAddress) {
      return freezeBalanceV2(address,freezeBalance,resourceCode,priKey,blockingStubFull);
    }
    return false;
  }

  /** Freeze balance to get energy, routing to V1 or V2 based on active proposals. */
  public static Boolean freezeBalanceGetEnergy(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    if(PublicMethod.getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays.getProposalName(),
        blockingStubFull) == 0) {
      return freezeBalanceV1(addRess,freezeBalance,freezeDuration,resourceCode,priKey,blockingStubFull);
    } else {
      return freezeBalanceV2(addRess,freezeBalance,resourceCode,priKey,blockingStubFull);
    }
  }

  /** Freeze balance for a receiver, routing to V1 or delegate based on active proposals. */
  public static Boolean freezeBalanceForReceiver(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      ByteString receiverAddressBytes,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    if(PublicMethod.getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays.getProposalName(), blockingStubFull) > 0) {
      return delegateResourceForReceiver(addRess,freezeBalance,resourceCode,receiverAddressBytes.toByteArray(),priKey,blockingStubFull);
    } else {
      return freezeBalanceV1ForReceiver(addRess,freezeBalance,freezeDuration,resourceCode,
          null == receiverAddressBytes ? null : receiverAddressBytes.toByteArray(),priKey,blockingStubFull);
    }
  }

  // --- Account Resource / Net Queries ---

  /** Get the account resource information for the given address. */
  public static AccountResourceMessage getAccountResource(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountResource(request);
  }

  /** Get the account net (bandwidth) information for the given address. */
  public static AccountNetMessage getAccountNet(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountNet(request);
  }

  // --- Storage ---

  /** Buy storage by spending TRX. */
  public static boolean buyStorage(
      long quantity,
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

    BuyStorageContract.Builder builder = BuyStorageContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress).setQuant(quantity);
    BuyStorageContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.buyStorage(contract);
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

  /** Sell storage bytes back for TRX. */
  public static boolean sellStorage(
      long quantity,
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

    SellStorageContract.Builder builder = SellStorageContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);
    builder.setOwnerAddress(byteAddress).setStorageBytes(quantity);
    SellStorageContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.sellStorage(contract);
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

  // --- Freeze Balance Count Calculators ---

  /** Calculate the amount of TRX needed to freeze to obtain a target energy amount. */
  public static long getFreezeBalanceCount(
      byte[] accountAddress,
      String ecKey,
      Long targetEnergy,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Precision change as the entire network freezes
    AccountResourceMessage resourceInfo = getAccountResource(accountAddress, blockingStubFull);

    Account info = PublicMethod.queryAccount(accountAddress, blockingStubFull);

    Account getAccount = PublicMethod.queryAccount(ecKey, blockingStubFull);

    long balance = info.getBalance();
    long frozenBalance = info.getAccountResource().getFrozenBalanceForEnergy().getFrozenBalance();
    long totalEnergyLimit = resourceInfo.getTotalEnergyLimit();
    long totalEnergyWeight = resourceInfo.getTotalEnergyWeight();
    long energyUsed = resourceInfo.getEnergyUsed();
    long energyLimit = resourceInfo.getEnergyLimit();

    if (energyUsed > energyLimit) {
      targetEnergy = energyUsed - energyLimit + targetEnergy;
    }

    if (totalEnergyWeight == 0) {
      return 1000_000L;
    }

    // totalEnergyLimit / (totalEnergyWeight + needBalance) = needEnergy / needBalance
    final BigInteger totalEnergyWeightBi = BigInteger.valueOf(totalEnergyWeight);
    long needBalance =
        totalEnergyWeightBi
            .multiply(BigInteger.valueOf(1_000_000))
            .multiply(BigInteger.valueOf(targetEnergy))
            .divide(BigInteger.valueOf(totalEnergyLimit - targetEnergy))
            .longValue();

    logger.info("getFreezeBalanceCount, needBalance: " + needBalance);

    if (needBalance < 1000000L) {
      needBalance = 2000000L;
      logger.info("getFreezeBalanceCount, needBalance less than 1 TRX, modify to: " + needBalance);
    }
    return needBalance * 2;
  }

  /** Calculate the amount of TRX needed to freeze to obtain a target net (bandwidth) amount. */
  public static long getFreezeBalanceNetCount(
      byte[] accountAddress,
      String ecKey,
      Long targetNet,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Precision change as the entire network freezes
    AccountResourceMessage resourceInfo = getAccountResource(accountAddress, blockingStubFull);

    Account info = PublicMethod.queryAccount(accountAddress, blockingStubFull);

    Account getAccount = PublicMethod.queryAccount(ecKey, blockingStubFull);

    long balance = info.getBalance();
    long totalNetLimit = resourceInfo.getTotalNetLimit();
    long totalNetWeight = resourceInfo.getTotalNetWeight();
    long netUsed = resourceInfo.getNetUsed();
    long netLimit = resourceInfo.getNetLimit();

    if (netUsed > netLimit) {
      targetNet = netUsed - netLimit + targetNet;
    }

    if (totalNetWeight == 0) {
      return 1000_000L;
    }

    // totalNetLimit / (totalNetWeight + needBalance) = needNet / needBalance
    final BigInteger totalNetWeightBi = BigInteger.valueOf(totalNetWeight);
    long needBalance =
        totalNetWeightBi
            .multiply(BigInteger.valueOf(1_000_000))
            .multiply(BigInteger.valueOf(targetNet))
            .divide(BigInteger.valueOf(totalNetLimit - targetNet))
            .longValue();

    logger.info("getFreezeBalanceNetCount, needBalance: " + needBalance);

    if (needBalance < 1000000L) {
      needBalance = 1000000L;
      logger.info(
          "getFreezeBalanceNetCount, needBalance less than 1 TRX, modify to: " + needBalance);
    }
    return needBalance;
  }

  // --- Delegated Resource ---

  /** Get delegated resource list between two addresses, routing to V1 or V2. */
  public static Optional<DelegatedResourceList> getDelegatedResource(
      byte[] fromAddress, byte[] toAddress, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString fromAddressBs = ByteString.copyFrom(fromAddress);
    ByteString toAddressBs = ByteString.copyFrom(toAddress);

    DelegatedResourceMessage request =
        DelegatedResourceMessage.newBuilder()
            .setFromAddress(fromAddressBs)
            .setToAddress(toAddressBs)
            .build();
    if(freezeV2ProposalIsOpen(blockingStubFull)) {
      DelegatedResourceList delegatedResource = blockingStubFull.getDelegatedResourceV2(request);
      return Optional.ofNullable(delegatedResource);
    } else {
      DelegatedResourceList delegatedResource = blockingStubFull.getDelegatedResource(request);
      return Optional.ofNullable(delegatedResource);
    }

  }

  /** Get delegated resource list from solidity node. */
  public static Optional<DelegatedResourceList> getDelegatedResourceFromSolidity(
      byte[] fromAddress,
      byte[] toAddress,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString fromAddressBs = ByteString.copyFrom(fromAddress);
    ByteString toAddressBs = ByteString.copyFrom(toAddress);

    DelegatedResourceMessage request =
        DelegatedResourceMessage.newBuilder()
            .setFromAddress(fromAddressBs)
            .setToAddress(toAddressBs)
            .build();
    DelegatedResourceList delegatedResource = blockingStubFull.getDelegatedResource(request);
    return Optional.ofNullable(delegatedResource);
  }

  /** Get delegated resource account index, routing to V1 or V2. */
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();
    if(freezeV2ProposalIsOpen(blockingStubFull)) {
      DelegatedResourceAccountIndex accountIndex =
          blockingStubFull.getDelegatedResourceAccountIndexV2(bytesMessage);
      return Optional.ofNullable(accountIndex);
    } else {
      DelegatedResourceAccountIndex accountIndex =
          blockingStubFull.getDelegatedResourceAccountIndex(bytesMessage);
      return Optional.ofNullable(accountIndex);
    }
  }


  /** Get delegated resource account index from solidity node. */
  public static Optional<DelegatedResourceAccountIndex>
      getDelegatedResourceAccountIndexFromSolidity(
          byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {

    ByteString addressBs = ByteString.copyFrom(address);

    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();

    DelegatedResourceAccountIndex accountIndex =
        blockingStubFull.getDelegatedResourceAccountIndex(bytesMessage);
    return Optional.ofNullable(accountIndex);
  }

  /** Get delegated resource V2 list between two addresses from full node. */
  public static Optional<DelegatedResourceList> getDelegatedResourceV2(
      byte[] fromAddress, byte[] toAddress, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString fromAddressBs = ByteString.copyFrom(fromAddress);
    ByteString toAddressBs = ByteString.copyFrom(toAddress);

    DelegatedResourceMessage request =
        DelegatedResourceMessage.newBuilder()
            .setFromAddress(fromAddressBs)
            .setToAddress(toAddressBs)
            .build();

    DelegatedResourceList delegatedResource = blockingStubFull.getDelegatedResourceV2(request);
    return Optional.ofNullable(delegatedResource);
  }

  /** Get delegated resource V2 list between two addresses from solidity node. */
  public static Optional<DelegatedResourceList> getDelegatedResourceV2Solidity(
      byte[] fromAddress, byte[] toAddress, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString fromAddressBs = ByteString.copyFrom(fromAddress);
    ByteString toAddressBs = ByteString.copyFrom(toAddress);

    DelegatedResourceMessage request =
        DelegatedResourceMessage.newBuilder()
            .setFromAddress(fromAddressBs)
            .setToAddress(toAddressBs)
            .build();

    DelegatedResourceList delegatedResource = blockingStubFull.getDelegatedResourceV2(request);
    return Optional.ofNullable(delegatedResource);
  }

  /** Get delegated resource account index V2 from full node. */
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndexV2(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();

    DelegatedResourceAccountIndex accountIndex =
          blockingStubFull.getDelegatedResourceAccountIndexV2(bytesMessage);
    return Optional.ofNullable(accountIndex);
  }

  /** Get delegated resource account index V2 from solidity node. */
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndexV2Solidity(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();

    DelegatedResourceAccountIndex accountIndex =
        blockingStubFull.getDelegatedResourceAccountIndexV2(bytesMessage);
    return Optional.ofNullable(accountIndex);
  }

  // --- Can Delegated Max Size ---

  /** Get the maximum delegatable resource size for the given address and type from full node. */
  public static Optional<CanDelegatedMaxSizeResponseMessage> getCanDelegatedMaxSize(byte[] ownerAddress, int type,
      WalletGrpc.WalletBlockingStub blockingStub) {
    ByteString ownerAddressBS = ByteString.copyFrom(ownerAddress);
    CanDelegatedMaxSizeRequestMessage request = CanDelegatedMaxSizeRequestMessage.newBuilder()
        .setOwnerAddress(ownerAddressBS)
        .setType(type)
        .build();
    CanDelegatedMaxSizeResponseMessage canDelegatedMaxSizeResponseMessage;
    canDelegatedMaxSizeResponseMessage = blockingStub.getCanDelegatedMaxSize(request);
    return Optional.ofNullable(canDelegatedMaxSizeResponseMessage);
  }

  /** Get the maximum delegatable resource size for the given address and type from solidity node. */
  public static Optional<CanDelegatedMaxSizeResponseMessage> getCanDelegatedMaxSizeSolidity(byte[] ownerAddress, int type,
                                                                                            WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString ownerAddressBS = ByteString.copyFrom(ownerAddress);
    CanDelegatedMaxSizeRequestMessage request = CanDelegatedMaxSizeRequestMessage.newBuilder()
        .setOwnerAddress(ownerAddressBS)
        .setType(type)
        .build();
    CanDelegatedMaxSizeResponseMessage canDelegatedMaxSizeResponseMessage;
    canDelegatedMaxSizeResponseMessage = blockingStubFull.getCanDelegatedMaxSize(request);
    return Optional.ofNullable(canDelegatedMaxSizeResponseMessage);
  }

  // --- Can Withdraw Unfreeze Amount ---

  /** Get the withdrawable unfrozen amount for the given address and timestamp from full node. */
  public static Optional<CanWithdrawUnfreezeAmountResponseMessage> getCanWithdrawUnfreezeAmount(
      byte[] ownerAddress, long timestamp,WalletGrpc.WalletBlockingStub blockingStub) {
    ByteString ownerAddressBS = ByteString.copyFrom(ownerAddress);
    CanWithdrawUnfreezeAmountRequestMessage request = CanWithdrawUnfreezeAmountRequestMessage.newBuilder()
        .setOwnerAddress(ownerAddressBS)
        .setTimestamp(timestamp)
        .build();
    CanWithdrawUnfreezeAmountResponseMessage canDelegatedMaxSizeResponseMessage;
    canDelegatedMaxSizeResponseMessage = blockingStub.getCanWithdrawUnfreezeAmount(request);
    return Optional.ofNullable(canDelegatedMaxSizeResponseMessage);
  }

  /** Get the withdrawable unfrozen amount for the given address and timestamp from solidity node. */
  public static Optional<CanWithdrawUnfreezeAmountResponseMessage> getCanWithdrawUnfreezeAmountSolidity(
      byte[] ownerAddress, long timestamp,WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString ownerAddressBS = ByteString.copyFrom(ownerAddress);
    CanWithdrawUnfreezeAmountRequestMessage request = CanWithdrawUnfreezeAmountRequestMessage.newBuilder()
        .setOwnerAddress(ownerAddressBS)
        .setTimestamp(timestamp)
        .build();
    CanWithdrawUnfreezeAmountResponseMessage canDelegatedMaxSizeResponseMessage;
    canDelegatedMaxSizeResponseMessage = blockingStubFull.getCanWithdrawUnfreezeAmount(request);
    return Optional.ofNullable(canDelegatedMaxSizeResponseMessage);
  }

  // --- Available Unfreeze Count ---

  /** Get the available unfreeze count for the given address from full node. */
  public static Optional<GetAvailableUnfreezeCountResponseMessage> getAvailableUnfreezeCount(
      byte[] ownerAddress,WalletGrpc.WalletBlockingStub blockingStub) {
    ByteString ownerAddressBS = ByteString.copyFrom(ownerAddress);
    GetAvailableUnfreezeCountRequestMessage request = GetAvailableUnfreezeCountRequestMessage.newBuilder()
        .setOwnerAddress(ownerAddressBS)
        .build();
    GetAvailableUnfreezeCountResponseMessage getAvailableUnfreezeCountResponseMessage;
    getAvailableUnfreezeCountResponseMessage = blockingStub.getAvailableUnfreezeCount(request);
    return Optional.ofNullable(getAvailableUnfreezeCountResponseMessage);
  }

  /** Get the available unfreeze count for the given address from solidity node. */
  public static Optional<GetAvailableUnfreezeCountResponseMessage> getAvailableUnfreezeCountSolidity(
      byte[] ownerAddress,WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString ownerAddressBS = ByteString.copyFrom(ownerAddress);
    GetAvailableUnfreezeCountRequestMessage request = GetAvailableUnfreezeCountRequestMessage.newBuilder()
        .setOwnerAddress(ownerAddressBS)
        .build();
    GetAvailableUnfreezeCountResponseMessage getAvailableUnfreezeCountResponseMessage;
    getAvailableUnfreezeCountResponseMessage = blockingStubFull.getAvailableUnfreezeCount(request);
    return Optional.ofNullable(getAvailableUnfreezeCountResponseMessage);
  }

  // --- Delegate Resource ---

  /** Freeze V2 and delegate resource to a receiver address. */
  public static Boolean delegateResourceForReceiver(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      byte[] receiverAddress,
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
    Assert.assertTrue(freezeBalanceV2(addressByte,delegateAmount,resourceCode,priKey,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    DelegateResourceContract.Builder builder =  DelegateResourceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    ByteString byteReceiverAddress = ByteString.copyFrom(receiverAddress);
    builder
        .setOwnerAddress(byteAddress)
        .setBalance(delegateAmount / 2)
        .setReceiverAddress(byteReceiverAddress)
        .setResourceValue(resourceCode);
    DelegateResourceContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.delegateResource(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }


  /** Delegate resource V2 to a receiver (without lock). */
  public static Boolean delegateResourceV2(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return delegateResourceV2Lock(addressByte,delegateAmount,resourceCode,false, null, receiverAddress,priKey,
        blockingStubFull);
  }

  /** Delegate resource V2 and return the transaction ID. */
  public static String delegateResourceV2AndGetTxId(byte[] addressByte,
                                           long delegateAmount,
                                           int resourceCode,
                                           byte[] receiverAddress,
                                           String priKey,
                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
    return delegateResourceV2LockAndGetTxId(addressByte,delegateAmount,resourceCode,false, null, receiverAddress,priKey,
            blockingStubFull);
  }

  /** Delegate resource V2 and return the full TransactionExtention. */
  public static TransactionExtention delegateResourceV2AndGetTransactionExtention(
      byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      boolean lock,
      Long lockPeriod,
      byte[] receiverAddress,
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

    DelegateResourceContract.Builder builder =  DelegateResourceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    ByteString byteReceiverAddress = ByteString.copyFrom(receiverAddress);
    builder
        .setOwnerAddress(byteAddress)
        .setBalance(delegateAmount)
        .setReceiverAddress(byteReceiverAddress)
        .setResourceValue(resourceCode)
        .setLock(lock);
    if (null != lockPeriod) {
      builder.setLockPeriod(lockPeriod);
    }
    DelegateResourceContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.delegateResource(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return transactionExtention;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    String freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return transactionExtention;
  }



  /** Delegate resource V2 with optional lock and lock period, return success boolean. */
  public static Boolean delegateResourceV2Lock(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      boolean lock,
      Long lockPeriod,
      byte[] receiverAddress,
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

    DelegateResourceContract.Builder builder =  DelegateResourceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    ByteString byteReceiverAddress = ByteString.copyFrom(receiverAddress);
    builder
        .setOwnerAddress(byteAddress)
        .setBalance(delegateAmount)
        .setReceiverAddress(byteReceiverAddress)
        .setResourceValue(resourceCode)
        .setLock(lock);
    if (null != lockPeriod) {
      builder.setLockPeriod(lockPeriod);
    }
    DelegateResourceContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.delegateResource(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    PublicMethod.freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Delegate resource V2 with optional lock and lock period, return the transaction ID. */
  public static String delegateResourceV2LockAndGetTxId(byte[] addressByte,
                                               long delegateAmount,
                                               int resourceCode,
                                               boolean lock,
                                               Long lockPeriod,
                                               byte[] receiverAddress,
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

    DelegateResourceContract.Builder builder =  DelegateResourceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    ByteString byteReceiverAddress = ByteString.copyFrom(receiverAddress);
    builder
            .setOwnerAddress(byteAddress)
            .setBalance(delegateAmount)
            .setReceiverAddress(byteReceiverAddress)
            .setResourceValue(resourceCode)
            .setLock(lock);
    if (null != lockPeriod) {
      builder.setLockPeriod(lockPeriod);
    }
    DelegateResourceContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.delegateResource(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return null;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    String freezeV2Txid = ByteArray.toHexString(
            Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return freezeV2Txid;
  }

  // --- Undelegate Resource ---

  /** Undelegate resource V2 from a receiver address, return success boolean. */
  public static Boolean unDelegateResourceV2(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      byte[] receiverAddress,
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

    UnDelegateResourceContract.Builder builder =  UnDelegateResourceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    ByteString byteReceiverAddress = ByteString.copyFrom(receiverAddress);
    builder
        .setOwnerAddress(byteAddress)
        .setBalance(delegateAmount)
        .setReceiverAddress(byteReceiverAddress)
        .setResourceValue(resourceCode);
    UnDelegateResourceContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.unDelegateResource(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return false;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    PublicMethod.freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** Undelegate resource V2 and return the transaction ID. */
  public static String unDelegateResourceV2AndGetTxId(byte[] addressByte,
                                             long delegateAmount,
                                             int resourceCode,
                                             byte[] receiverAddress,
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

    UnDelegateResourceContract.Builder builder =  UnDelegateResourceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    ByteString byteReceiverAddress = ByteString.copyFrom(receiverAddress);
    builder
            .setOwnerAddress(byteAddress)
            .setBalance(delegateAmount)
            .setReceiverAddress(byteReceiverAddress)
            .setResourceValue(resourceCode);
    UnDelegateResourceContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.unDelegateResource(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return null;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    String freezeV2Txid = ByteArray.toHexString(
            Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return freezeV2Txid;
  }

  /** Undelegate resource V2 and return the full TransactionExtention. */
  public static TransactionExtention unDelegateResourceV2AndGetTransactionExtention(byte[] addressByte,
                                                      long delegateAmount,
                                                      int resourceCode,
                                                      byte[] receiverAddress,
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

    UnDelegateResourceContract.Builder builder =  UnDelegateResourceContract.newBuilder();
    ByteString byteAddress = ByteString.copyFrom(addressByte);
    ByteString byteReceiverAddress = ByteString.copyFrom(receiverAddress);
    builder
        .setOwnerAddress(byteAddress)
        .setBalance(delegateAmount)
        .setReceiverAddress(byteReceiverAddress)
        .setResourceValue(resourceCode);
    UnDelegateResourceContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.unDelegateResource(contract);
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction = null");
      return transactionExtention;
    }
    transaction = TransactionUtils.sign(transaction, ecKey);
    String freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    return transactionExtention;
  }

  // --- Freed Resource ---

  /** Transfer all balance minus 500000 sun from one address to another to free up resources. */
  public static void freeResource(
      byte[] fromAddress,
      String priKey,
      byte[] toAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    long balance = PublicMethod.queryAccount(fromAddress, blockingStubFull).getBalance();
    PublicMethod.sendcoin(toAddress, balance - 500000, fromAddress, priKey, blockingStubFull);
  }
}
