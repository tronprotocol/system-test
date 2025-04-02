package stest.tron.wallet.common.client.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.primitives.Longs;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.netty.util.internal.StringUtil;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.AssetIssueList;
import org.tron.api.GrpcAPI.BlockExtention;
import org.tron.api.GrpcAPI.BytesMessage;
import org.tron.api.GrpcAPI.CanDelegatedMaxSizeRequestMessage;
import org.tron.api.GrpcAPI.CanDelegatedMaxSizeResponseMessage;
import org.tron.api.GrpcAPI.CanWithdrawUnfreezeAmountRequestMessage;
import org.tron.api.GrpcAPI.CanWithdrawUnfreezeAmountResponseMessage;
import org.tron.api.GrpcAPI.DecryptNotes;
import org.tron.api.GrpcAPI.DecryptNotes.NoteTx;
import org.tron.api.GrpcAPI.DecryptNotesMarked;
import org.tron.api.GrpcAPI.DelegatedResourceList;
import org.tron.api.GrpcAPI.DelegatedResourceMessage;
import org.tron.api.GrpcAPI.EmptyMessage;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.GrpcAPI.GetAvailableUnfreezeCountRequestMessage;
import org.tron.api.GrpcAPI.GetAvailableUnfreezeCountResponseMessage;
import org.tron.api.GrpcAPI.IvkDecryptAndMarkParameters;
import org.tron.api.GrpcAPI.IvkDecryptParameters;
import org.tron.api.GrpcAPI.NfParameters;
import org.tron.api.GrpcAPI.Note;
import org.tron.api.GrpcAPI.NoteParameters;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.OvkDecryptParameters;
import org.tron.api.GrpcAPI.PrivateParameters;
import org.tron.api.GrpcAPI.PrivateParametersWithoutAsk;
import org.tron.api.GrpcAPI.ReceiveNote;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.SpendAuthSigParameters;
import org.tron.api.GrpcAPI.SpendNote;
import org.tron.api.GrpcAPI.SpendResult;
import org.tron.api.GrpcAPI.TransactionApprovedList;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.GrpcAPI.TransactionInfoList;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletGrpc.WalletBlockingStub;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;

import org.tron.protos.Protocol.Account.FreezeV2;
import org.tron.protos.Protocol.Account.Frozen;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.ChainParameters;
import org.tron.protos.Protocol.DelegatedResourceAccountIndex;
import org.tron.protos.Protocol.Exchange;
import org.tron.protos.Protocol.Key;
import org.tron.protos.Protocol.Permission;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Contract.ContractType;
import org.tron.protos.Protocol.Transaction.Result;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AccountContract.AccountCreateContract;
import org.tron.protos.contract.AccountContract.AccountPermissionUpdateContract;
import org.tron.protos.contract.AccountContract.AccountUpdateContract;
import org.tron.protos.contract.AccountContract.SetAccountIdContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.ParticipateAssetIssueContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UpdateAssetContract;
import org.tron.protos.contract.BalanceContract;
import org.tron.protos.contract.BalanceContract.DelegateResourceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.FreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.TransferContract;
import org.tron.protos.contract.BalanceContract.UnDelegateResourceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceContract;
import org.tron.protos.contract.BalanceContract.UnfreezeBalanceV2Contract;
import org.tron.protos.contract.BalanceContract.WithdrawExpireUnfreezeContract;
import org.tron.protos.contract.BalanceContract.CancelAllUnfreezeV2Contract;
import org.tron.protos.contract.ExchangeContract.ExchangeCreateContract;
import org.tron.protos.contract.ExchangeContract.ExchangeInjectContract;
import org.tron.protos.contract.ExchangeContract.ExchangeTransactionContract;
import org.tron.protos.contract.ExchangeContract.ExchangeWithdrawContract;
import org.tron.protos.contract.MarketContract;
import org.tron.protos.contract.ProposalContract.ProposalApproveContract;
import org.tron.protos.contract.ProposalContract.ProposalCreateContract;
import org.tron.protos.contract.ProposalContract.ProposalDeleteContract;
import org.tron.protos.contract.ShieldContract.IncrementalMerkleVoucherInfo;
import org.tron.protos.contract.ShieldContract.OutputPoint;
import org.tron.protos.contract.ShieldContract.OutputPointInfo;
import org.tron.protos.contract.ShieldContract.ShieldedTransferContract;
import org.tron.protos.contract.ShieldContract.SpendDescription;
import org.tron.protos.contract.SmartContractOuterClass.ClearABIContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.CreateSmartContract.Builder;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract.ABI;
import org.tron.protos.contract.SmartContractOuterClass.SmartContractDataWrapper;
import org.tron.protos.contract.SmartContractOuterClass.TriggerSmartContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateEnergyLimitContract;
import org.tron.protos.contract.SmartContractOuterClass.UpdateSettingContract;
import org.tron.protos.contract.StorageContract.BuyStorageContract;
import org.tron.protos.contract.StorageContract.SellStorageContract;
import org.tron.protos.contract.StorageContract.UpdateBrokerageContract;
import org.tron.protos.contract.WitnessContract.VoteWitnessContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.BlockCapsule.BlockId;
import stest.tron.wallet.common.client.utils.zen.address.DiversifierT;
import stest.tron.wallet.common.client.utils.zen.address.ExpandedSpendingKey;
import stest.tron.wallet.common.client.utils.zen.address.FullViewingKey;
import stest.tron.wallet.common.client.utils.zen.address.IncomingViewingKey;
import stest.tron.wallet.common.client.utils.zen.address.PaymentAddress;
import stest.tron.wallet.common.client.utils.zen.address.SpendingKey;

@Slf4j
public class PublicMethed {

  // //Wallet.setAddressPreFixByte()();
  private static final String FilePath = "Wallet";
//  private static final Logger logger = LoggerFactory.getLogger("TestLogger");
  // private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  // private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  public static Map<Long, ShieldNoteInfo> utxoMapNote = new ConcurrentHashMap();
  public static List<ShieldNoteInfo> spendUtxoList = new ArrayList<>();
  // private static List<WalletFile> walletFile = new ArrayList<>();
  private static ShieldWrapper shieldWrapper = new ShieldWrapper();
  // Wallet wallet = new Wallet();
  public static volatile Integer witnessNum;

  public static volatile String freezeV2Txid;

  public static String code;

  public static AtomicInteger randomFreezeAmount = new AtomicInteger(1);

  private static final String fullnode2 = Configuration.getByPath("testng.conf")
          .getStringList("fullnode.ip.list").get(1);
  private static ManagedChannel channelFull2 = ManagedChannelBuilder.forTarget(fullnode2).usePlaintext().build();
  private static WalletGrpc.WalletBlockingStub blockingStubFull2 = WalletGrpc.newBlockingStub(channelFull2);
  private static final String gRPCurl =
      Configuration.getByPath("testng.conf").getString("defaultParameter.gRPCurl");




  /** constructor. */
  public static Integer getWitnessNum(WalletGrpc.WalletBlockingStub blockingStubFull) {
    //if (null == witnessNum) {
      //witnessNum = PublicMethed.listWitnesses(blockingStubFull).get().getWitnessesList().size();
    //}
    witnessNum = PublicMethed.listWitnesses(blockingStubFull).get().getWitnessesList().size();
    return witnessNum;
  }

  /** constructor. */
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
      transaction = signTransaction(ecKey, transaction);

      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  /** constructor. */
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
      transaction = signTransaction(ecKey, transaction);

      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

      return response.getResult();
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /** constructor. */
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
      transaction = signTransaction(ecKey, transaction);

      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

      return response.getResult();
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /** constructor. */
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
      transaction = signTransaction(ecKey, transaction);

      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

      return response.getResult();
    } catch (Exception ex) {
      ex.printStackTrace();
      return false;
    }
  }

  /** constructor. */
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
      transaction = signTransaction(ecKey, transaction);

      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      if (response.getResult() == false) {
        return response;
      } else {
        try {
          Thread.sleep(3000);
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

  /** constructor. */
  public static Account queryAccountByAddress(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** constructor. */
  public static Account queryAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** constructor. */
  public static Protocol.Account queryAccount(
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;
    if (ecKey == null) {
      String pubKey = loadPubKey(); // 04 PubKey[128]
      if (StringUtils.isEmpty(pubKey)) {
        logger.warn("Warning: QueryAccount failed, no wallet address !!");
        return null;
      }
      byte[] pubKeyAsc = pubKey.getBytes();
      byte[] pubKeyHex = Hex.decode(pubKeyAsc);
      ecKey = ECKey.fromPublicOnly(pubKeyHex);
    }
    return grpcQueryAccount(ecKey.getAddress(), blockingStubFull);
  }

  /** constructor. */
  public static Account queryAccount(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** constructor. */
  public static Account getAccountById(
      String accountId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsAccountId = ByteString.copyFromUtf8(accountId);
    Account request = Account.newBuilder().setAccountId(bsAccountId).build();
    return blockingStubFull.getAccountById(request);
  }

  /** constructor. */
  public static Account getAccountByIdFromSolidity(
      String accountId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString bsAccountId = ByteString.copyFromUtf8(accountId);
    Account request = Account.newBuilder().setAccountId(bsAccountId).build();
    return blockingStubFull.getAccountById(request);
  }

  /** constructor. */
  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }

  /** constructor. */
  public static byte[] getAddress(ECKey ecKey) {

    return ecKey.getAddress();
  }

  /** constructor. */
  public static Protocol.Account grpcQueryAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Protocol.Account request = Protocol.Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** constructor. */
  public static Protocol.Block getBlock(
      long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());
  }

  /** constructor. */
  public static BlockExtention getBlock2(long blockNum, WalletBlockingStub blockingStubFull) {
    GrpcAPI.NumberMessage.Builder builder = GrpcAPI.NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum2(builder.build());
  }

  /** constructor. */
  public static Protocol.Transaction signTransaction(
      ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      // logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /** constructor. */
  public static Protocol.Transaction signTransactionForShield(
      ECKey ecKey, Protocol.Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      // logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    return TransactionUtils.sign(transaction, ecKey);
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
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

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return response;
    } else {
      return ret;
    }
  }

  /** constructor. */
  public static Boolean freezeBalance(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    if(getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays.getProposalName(),
        blockingStubFull) <= 0) {
      return freezeBalanceV1(addRess,freezeBalance,freezeDuration,0,priKey,blockingStubFull);
    } else {
      return freezeBalanceV2(addRess,freezeBalance,0,priKey,blockingStubFull);
    }
  }

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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }


  public static Boolean freezeBalanceV1(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return freezeBalanceV1ForReceiver(addRess,freezeBalance,freezeDuration,resourceCode,null,priKey,blockingStubFull);
  }

  /** constructor. */
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
    Protocol.Account beforeFronzen = queryAccount(priKey, blockingStubFull);
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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    if (response.getResult() == false) {
      return response;
    }

    Long afterBlockNum = 0L;

    while (afterBlockNum < beforeBlockNum) {
      Protocol.Block currentBlock1 =
          blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      afterBlockNum = currentBlock1.getBlockHeader().getRawData().getNumber();
    }

    Protocol.Account afterFronzen = queryAccount(priKey, blockingStubFull);
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

  /** constructor. */
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


  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }


  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }
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
    transaction = signTransaction(ecKey, transaction);
    String txId = ByteArray.toHexString(
            Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return txId;
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }
  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    String txId = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    broadcastTransaction(transaction, blockingStubFull);
    return txId;
  }


  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

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
    transaction = signTransaction(ecKey, transaction);
    String freezeV2Txid = ByteArray.toHexString(
            Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return freezeV2Txid;
  }


  public static Boolean freezeV2ProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethed.getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays
        .getProposalName(), blockingStubFull) > 0;
  }


  public static Boolean tronPowerProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethed.getChainParametersValue(ProposalEnum.GetAllowNewResourceModel
        .getProposalName(), blockingStubFull) == 1;
  }

  public static Boolean getAllowDynamicEnergyProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethed.getChainParametersValue(ProposalEnum.GetAllowDynamicEnergy
        .getProposalName(), blockingStubFull) == 1;
  }



  public static Long getProposalMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethed.getChainParametersValue(ProposalEnum.GetMemoFee.getProposalName(),blockingStubFull);
  }

  public static String getMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getMemoFee(EmptyMessage.newBuilder().build()).getPrices();
  }

  public static String getEnergyPrice(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getEnergyPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  public static String getEnergyPriceSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return blockingStubFull.getEnergyPrices(EmptyMessage.newBuilder().build()).getPrices();
  }


  public static String getBandwidthPrices(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getBandwidthPrices(EmptyMessage.newBuilder().build()).getPrices();
  }

  public static String getBandwidthPricesSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return blockingStubFull.getBandwidthPrices(EmptyMessage.newBuilder().build()).getPrices();
  }



  /** constructor. */
  public static Boolean sendcoin(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Integer times = 0;
    while (times++ <= 2) {

      TransferContract.Builder builder = TransferContract.newBuilder();
      ByteString bsTo = ByteString.copyFrom(to);
      ByteString bsOwner = ByteString.copyFrom(owner);
      builder.setToAddress(bsTo);
      builder.setOwnerAddress(bsOwner);
      builder.setAmount(amount);

      TransferContract contract = builder.build();
      Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction ==null");
        continue;
      }
      transaction = signTransaction(ecKey, transaction);
      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      return response.getResult();
    }
    return false;
  }

  /** constructor. */
  public static Boolean sendcoinWithScript(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      int scriptLength,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    Integer times = 0;
    while (times++ <= 2) {

      TransferContract.Builder builder = TransferContract.newBuilder();
      ByteString bsTo = ByteString.copyFrom(to);
      ByteString bsOwner = ByteString.copyFrom(owner);
      builder.setToAddress(bsTo);
      builder.setOwnerAddress(bsOwner);
      builder.setAmount(amount);

      TransferContract contract = builder.build();
      Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
      Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
      builder1.setScripts(ByteString.copyFrom(new byte[scriptLength]));
      Transaction.Builder builder2 = transaction.toBuilder();
      builder2.setRawData(builder1);
      transaction = builder2.build();
      System.out.println(transaction.getSerializedSize());
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        logger.info("transaction ==null");
        continue;
      }
      transaction = signTransaction(ecKey, transaction);
      GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
      return response.getResult();
    }
    return false;
  }

  /** constructor. */
  public static String sendcoinGetTransactionHex(
      byte[] to,
      long amount,
      byte[] owner,
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

    Integer times = 0;
    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = signTransaction(ecKey, transaction);
    logger.info(
        "HEX transaction is : "
            + "transaction hex string is "
            + ByteArray.toHexString(transaction.toByteArray()));
    return ByteArray.toHexString(transaction.toByteArray());
  }

  /** constructor. */
  public static Boolean cancelDeferredTransactionById(
      String txid, byte[] owner, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    /*Contract.CancelDeferredTransactionContract.Builder builder = Contract
      .CancelDeferredTransactionContract.newBuilder();
    builder.setTransactionId(ByteString.copyFrom(ByteArray.fromHexString(txid)));
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    Contract.CancelDeferredTransactionContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull
     .createCancelDeferredTransactionContract(contract);

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
        "Cancel transaction before sign txid = " + ByteArray.toHexString(
        transactionExtention.getTxid().toByteArray()));

    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "Cancel transaction txid = " + ByteArray.toHexString(transactionExtention
        .getTxid().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();*/
    return null;
  }

  /** constructor. */
  public static Boolean sendcoinDelayed(
      byte[] to,
      long amount,
      long delaySeconds,
      byte[] owner,
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

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);

    // transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    logger.info(
        "Txid is "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static String sendcoinDelayedGetTxid(
      byte[] to,
      long amount,
      long delaySeconds,
      byte[] owner,
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

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);

    // transaction = TransactionUtils.setDelaySeconds(transaction, delaySeconds);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    transaction = signTransaction(ecKey, transaction);
    logger.info(
        "Txid is "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
  }

  /** constructor. */
  public static Return sendcoin2(
      byte[] to,
      long amount,
      byte[] owner,
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
    // Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.createTransaction2(contract);
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

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      //      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return response;
    }
    return ret;
  }

  /** constructor. */
  public static String sendcoinGetTransactionId(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull
  ) {
    return sendcoinWithMemoGetTransactionId(to,amount,null,owner,
        priKey,blockingStubFull);
}

  /** constructor. */
  public static String sendcoinWithMemoGetTransactionId(
      byte[] to,
      long amount,
      String memo,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    // Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    // Test raw data
    if(null != memo) {
      Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
      builder1.setData(ByteString.copyFromUtf8(memo));
      Transaction.Builder builder2 = transaction.toBuilder();
      builder2.setRawData(builder1);
      transaction = builder2.build();
    }


    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** constructor. */
  public static String sendcoinGetTransactionIdForConstructData(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
    // Protocol.Account search = queryAccount(priKey, blockingStubFull);

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.createTransaction(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction ==null");
      return null;
    }
    // Test raw data
    Protocol.Transaction.raw.Builder builder1 = transaction.getRawData().toBuilder();
    StringBuffer stringBuffer = new StringBuffer();
    while (stringBuffer.length() <= 20) {
      stringBuffer.append("12345678");
    }
    builder1.setData(ByteString.copyFromUtf8(stringBuffer.toString()));
    Transaction.Builder builder2 = transaction.toBuilder();
    builder2.setRawData(builder1);
    transaction = builder2.build();

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** constructor. */
  public static Optional<Transaction> getTransactionById(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubFull.getTransactionById(request);

    return Optional.ofNullable(transaction);
  }

  /** constructor. */
  public static Optional<Transaction> getTransactionById(
      String txId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubFull.getTransactionById(request);
    return Optional.ofNullable(transaction);
  }

  /** constructor. */
  public static Long getAssetBalanceByAssetId(
      ByteString assetId, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Account assetOwnerAccount = queryAccount(priKey, blockingStubFull);
    Long assetOwnerAssetBalance = 0L;
    for (String id : assetOwnerAccount.getAssetV2Map().keySet()) {
      if (assetId.toStringUtf8().equalsIgnoreCase(id)) {
        assetOwnerAssetBalance = assetOwnerAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("asset balance is " + assetOwnerAssetBalance);
    return assetOwnerAssetBalance;
  }


  public static Long getAssetBalanceByAssetId(
      ByteString assetId, byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    Account assetOwnerAccount = queryAccount(address, blockingStubFull);
    Long assetOwnerAssetBalance = 0L;
    for (String id : assetOwnerAccount.getAssetV2Map().keySet()) {
      if (assetId.toStringUtf8().equalsIgnoreCase(id)) {
        assetOwnerAssetBalance = assetOwnerAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("asset balance is " + assetOwnerAssetBalance);
    return assetOwnerAssetBalance;
  }

  /*
  public static Optional<DeferredTransaction> getDeferredTransactionById(String txId,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    DeferredTransaction transaction = blockingStubFull.getDeferredTransactionById(request);
    if (Objects.isNull(transaction)) {
      transaction = blockingStubFull.getDeferredTransactionById(request);
    }
    return Optional.ofNullable(transaction);
  }
  */

  /** constructor. */
  public static Optional<Transaction> getTransactionByIdSolidity(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction = blockingStubSolidity.getTransactionById(request);
    return Optional.ofNullable(transaction);
  }

  /** constructor. */
  public static String printTransaction(Transaction transaction) {
    String result = "";
    result += "hash: ";
    result += "\n";
    result +=
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(), transaction.toByteArray()));
    result += "\n";
    result += "txid: ";
    result += "\n";
    result +=
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));
    result += "\n";

    if (transaction.getRawData() != null) {
      result += "raw_data: ";
      result += "\n";
      result += "{";
      result += "\n";
      result += printTransactionRow(transaction.getRawData());
      result += "}";
      result += "\n";
    }

    return result;
  }

  /** constructor. */
  public static long printTransactionRow(Transaction.raw raw) {
    long timestamp = raw.getTimestamp();

    return timestamp;
  }

  /** constructor. */
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

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
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

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return response;
    }
    return ret;
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);

    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static boolean updateAccount(
      byte[] addressBytes,
      byte[] accountNameBytes,
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

    AccountUpdateContract.Builder builder = AccountUpdateContract.newBuilder();
    ByteString basAddreess = ByteString.copyFrom(addressBytes);
    ByteString bsAccountName = ByteString.copyFrom(accountNameBytes);

    builder.setAccountName(bsAccountName);
    builder.setOwnerAddress(basAddreess);

    AccountUpdateContract contract = builder.build();
    Protocol.Transaction transaction = blockingStubFull.updateAccount(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("Please check!!! transaction == null");
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static boolean waitSolidityNodeSynFullNodeData(
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Block solidityCurrentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Integer wait = 0;
    long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    logger.info("start sync soliditynode, SR number: " + getWitnessNum(blockingStubFull));
    while (solidityCurrentBlock.getBlockHeader().getRawData().getNumber()
            <= currentBlockNum + 1
        && wait
            < ((getWitnessNum(blockingStubFull) >= 27)
                ? 27
                : getWitnessNum(blockingStubFull) + 4)) {
      try {
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      solidityCurrentBlock =
          blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      if (wait == 24) {
        logger.info("Didn't syn,skip to next case.");
        return false;
      }
      wait++;
    }
    logger.info("Fullnode number: " + currentBlockNum
    + ", solidity node number: " + solidityCurrentBlock.getBlockHeader().getRawData().getNumber());

    return true;
  }

  /** constructor. */
  public static boolean waitProduceNextBlock(WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    final Long currentNum = currentBlock.getBlockHeader().getRawData().getNumber();

    Block nextBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long nextNum = nextBlock.getBlockHeader().getRawData().getNumber();

    Integer wait = 0;
    logger.info("start wait produce block, current num: " + currentBlock.getBlockHeader().getRawData().getNumber());
    while (nextNum <= currentNum + 1 && wait <= 45) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      nextBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      nextNum = nextBlock.getBlockHeader().getRawData().getNumber();
      if (wait == 45) {
        logger.info("quit timeout, These 45 second didn't produce a block,please check.");
        return false;
      }
      wait++;
    }
    logger.info("quit normally, wait times: " + wait);
    return true;
  }



  /**
   * if tx is found, return it
   * if query timeout,assert failed then return false
   * @param blockingStubFull
   * @param txId
   * @param timeout second
   * @return
   */
  public static void WaitUntilTransactionInfoFound(WalletGrpc.WalletBlockingStub blockingStubFull, String txId, int timeout) {
    Integer wait = 0;
    while (wait++ <= timeout) {
      try {
        // wait 3 seconds
        Thread.sleep(3000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txId, blockingStubFull);
      if(infoById.get().getBlockTimeStamp() > 0){
        logger.info("quit normally, wait tx by id: " + txId + " times: " + wait);
        return;
      }
    }
    logger.info("quit timeout, wait tx by id: " + txId + " times: " + wait);

  }

  /** constructor. */
  public static AccountNetMessage getAccountNet(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountNet(request);
  }

  /*  public static byte[] addPreFix(byte[] address) {
  //Wallet.setAddressPreFixByte()();
  Config config = Configuration.getByPath("testng.conf");
  byte ADD_PRE_FIX_BYTE_MAINNET = (byte) 0x41;   //41 + address
  byte ADD_PRE_FIX_BYTE_TESTNET = (byte) 0xa0;   //a0 + address
  byte[] preFix = new byte[1];
  if (config.hasPath("net.type") && "mainnet".equalsIgnoreCase(config.getString("net.type"))) {
    WalletClient.setAddressPreFixByte(ADD_PRE_FIX_BYTE_MAINNET);
    preFix[0] = ADD_PRE_FIX_BYTE_MAINNET;
   }else {
      WalletClient.setAddressPreFixByte(ADD_PRE_FIX_BYTE_TESTNET);
      preFix[0] = ADD_PRE_FIX_BYTE_TESTNET;
    }
    byte[] finalAddress = new byte[preFix.length+address.length];
    System.arraycopy(preFix, 0, finalAddress, 0, preFix.length);
    System.arraycopy(address, 0, finalAddress, preFix.length, address.length);
    return finalAddress;

  }*/

  /** constructor. */
  public static byte[] getFinalAddress(String priKey) {
    WalletClient walletClient;
    walletClient = new WalletClient(priKey);
    // walletClient.init(0);
    return walletClient.getAddress();
  }

  /** constructor. */
  public static String createAccountGetTxid(
      byte[] ownerAddress,
      byte[] newAddress,
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

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();
    Transaction transaction = blockingStubFull.createAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
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

  /** constructor. */
  public static boolean createAccount(
      byte[] ownerAddress,
      byte[] newAddress,
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

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();
    Transaction transaction = blockingStubFull.createAccount(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static Return createAccount2(
      byte[] ownerAddress,
      byte[] newAddress,
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

    byte[] owner = ownerAddress;
    AccountCreateContract.Builder builder = AccountCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setAccountAddress(ByteString.copyFrom(newAddress));
    AccountCreateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.createAccount2(contract);

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

    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      // logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
      return response;
    }
    return ret;
  }

  /** constructor. */
  public static boolean voteWitness(
      byte[] ownerAddress,
      String priKey,
      HashMap<byte[], Long> witnessMap,
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
    VoteWitnessContract.Builder builder = VoteWitnessContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    for (byte[] address : witnessMap.keySet()) {
      VoteWitnessContract.Vote.Builder voteBuilder = VoteWitnessContract.Vote.newBuilder();
      voteBuilder.setVoteAddress(ByteString.copyFrom(address));
      voteBuilder.setVoteCount(witnessMap.get(address));
      builder.addVotes(voteBuilder.build());
    }

    VoteWitnessContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.voteWitnessAccount2(contract);
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** constructor. */
  public static boolean createProposal(
      byte[] ownerAddress,
      String priKey,
      HashMap<Long, Long> parametersMap,
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
    ProposalCreateContract.Builder builder = ProposalCreateContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.putAllParameters(parametersMap);

    ProposalCreateContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalCreate(contract);
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** constructor. */
  public static boolean approveProposal(
      byte[] ownerAddress,
      String priKey,
      long id,
      boolean isAddApproval,
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
    ProposalApproveContract.Builder builder = ProposalApproveContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);
    builder.setIsAddApproval(isAddApproval);
    ProposalApproveContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalApprove(contract);
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static boolean deleteProposal(
      byte[] ownerAddress, String priKey, long id, WalletGrpc.WalletBlockingStub blockingStubFull) {
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
    ProposalDeleteContract.Builder builder = ProposalDeleteContract.newBuilder();
    builder.setOwnerAddress(ByteString.copyFrom(owner));
    builder.setProposalId(id);

    ProposalDeleteContract contract = builder.build();
    TransactionExtention transactionExtention = blockingStubFull.proposalDelete(contract);
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static boolean printAddress(String key) {
    // Wallet.setAddressPreFixByte()();
    logger.info(key);
    logger.info(ByteArray.toHexString(getFinalAddress(key)));
    logger.info(Base58.encode58Check(getFinalAddress(key)));
    return true;
  }

  /** constructor. */
  public static String getAddressString(String key) {
    // Wallet.setAddressPreFixByte()();
    return Base58.encode58Check(getFinalAddress(key));
  }

  /** constructor. */
  public static ArrayList<String> getAddressInfo(String key) {
    // Wallet.setAddressPreFixByte()();
    ArrayList<String> accountList = new ArrayList<String>();
    accountList.add(key);
    accountList.add(ByteArray.toHexString(getFinalAddress(key)));
    accountList.add(Base58.encode58Check(getFinalAddress(key)));
    return accountList;
  }

  /** constructor. */
  public static boolean setAccountId(
      byte[] accountIdBytes,
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
    SetAccountIdContract.Builder builder = SetAccountIdContract.newBuilder();
    ByteString bsAddress = ByteString.copyFrom(owner);
    ByteString bsAccountId = ByteString.copyFrom(accountIdBytes);
    builder.setAccountId(bsAccountId);
    builder.setOwnerAddress(bsAddress);
    SetAccountIdContract contract = builder.build();
    Transaction transaction = blockingStubFull.setAccountId(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      logger.info("transaction == null");
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
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


  public static Long getChainParametersValue(String proposalName,WalletGrpc.WalletBlockingStub blockingStubFull) {
    ChainParameters chainParameters = blockingStubFull
        .getChainParameters(EmptyMessage.newBuilder().build());
    Optional<ChainParameters> getChainParameters = Optional.ofNullable(chainParameters);
    logger.info(Long.toString(getChainParameters.get().getChainParameterCount()));
    for (Integer i = 0; i < getChainParameters.get().getChainParameterCount(); i++) {
      if(getChainParameters.get().getChainParameter(i).getKey().equals(proposalName)) {
        return getChainParameters.get().getChainParameter(i).getValue();
      }
    }

    return 0L;


  }

  /** constructor. */
  public static Boolean freezeBalanceGetEnergy(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    if(getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays.getProposalName(),
        blockingStubFull) == 0) {
      return freezeBalanceV1(addRess,freezeBalance,freezeDuration,resourceCode,priKey,blockingStubFull);
    } else {
      return freezeBalanceV2(addRess,freezeBalance,resourceCode,priKey,blockingStubFull);
    }
  }

  /** constructor. */
  public static AccountResourceMessage getAccountResource(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccountResource(request);
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
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
      estimateDeployContractEnergy(PublicMethed.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return contractAddress;
    }
  }

  /** constructor. */
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
      estimateDeployContractEnergy(PublicMethed.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return contractAddress;
    }
  }

  /** constructor. */
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
      estimateDeployContractEnergy(PublicMethed.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray())));
    return transaction;
  }

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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
      estimateDeployContractEnergy(PublicMethed.code, value, "0", 0L, ownerAddress, blockingStubFull);
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));

    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      // logger.info("brodacast succesfully");
      return contractAddress;
    }
  }

  /** constructor. */
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

  /** constructor. */
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
      estimateDeployContractEnergy(PublicMethed.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    byte[] contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
  public static SmartContract getContract(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString byteString = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(byteString).build();
    logger.info("contract name is " + blockingStubFull.getContract(bytesMessage).getName());
    logger.info("contract address is " + WalletClient.encode58Check(address));
    return blockingStubFull.getContract(bytesMessage);
  }

  /** constructor. */
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
    PublicMethed.code = code;

    return Hex.decode(code);
  }

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

    PublicMethed.code = code;

    return Hex.decode(code);
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** 61 constructor. */
  public static Optional<TransactionInfo> getTransactionInfoById(
      String txId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;
    transactionInfo = blockingStubFull.getTransactionInfoById(request);
    return Optional.ofNullable(transactionInfo);
  }

  /** 61 constructor. */
  public static Optional<TransactionInfo> getTransactionInfoByIdFromSolidity(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    TransactionInfo transactionInfo;
    transactionInfo = blockingStubFull.getTransactionInfoById(request);
    return Optional.ofNullable(transactionInfo);
  }

  /** constructor. */
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNum(
      long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    TransactionInfoList transactionInfoList;
    transactionInfoList = blockingStubFull.getTransactionInfoByBlockNum(builder.build());
    return Optional.ofNullable(transactionInfoList);
  }

  /** constructor. */
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNumFromSolidity(
      long blockNum, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    TransactionInfoList transactionInfoList;
    transactionInfoList = blockingStubSolidity.getTransactionInfoByBlockNum(builder.build());
    return Optional.ofNullable(transactionInfoList);
  }

  /** constructor. */
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

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** constructor. */
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

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response =
        broadcastTransactionBoth(transaction, blockingStubFull, blockingStubFull1);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));

    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  public static Optional<ExchangeList> getExchangeList(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ExchangeList exchangeList = blockingStubFull.listExchanges(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(exchangeList);
  }

  /** constructor. */
  public static Optional<ExchangeList> getExchangeList(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ExchangeList exchangeList =
        blockingStubSolidity.listExchanges(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(exchangeList);
  }

  /** constructor. */
  public static Optional<Exchange> getExchange(
      String id, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    BytesMessage request =
        BytesMessage.newBuilder()
            .setValue(ByteString.copyFrom(ByteArray.fromLong(Long.parseLong(id))))
            .build();
    Exchange exchange = blockingStubSolidity.getExchangeById(request);
    return Optional.ofNullable(exchange);
  }

  /** constructor. */
  public static Optional<Exchange> getExchange(
      String id, WalletGrpc.WalletBlockingStub blockingStubFull) {
    BytesMessage request =
        BytesMessage.newBuilder()
            .setValue(ByteString.copyFrom(ByteArray.fromLong(Long.parseLong(id))))
            .build();
    Exchange exchange = blockingStubFull.getExchangeById(request);
    return Optional.ofNullable(exchange);
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));

    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));

    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
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

  /** constructor. */
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
      estimateDeployContractEnergy(PublicMethed.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    byte[] contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
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

  /** constructor. */
  public static Boolean freezeBalanceForReceiver(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      ByteString receiverAddressBytes,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    if(getChainParametersValue(ProposalEnum.GetUnfreezeDelayDays.getProposalName(), blockingStubFull) > 0) {
      return delegateResourceForReceiver(addRess,freezeBalance,resourceCode,receiverAddressBytes.toByteArray(),priKey,blockingStubFull);
    } else {
      return freezeBalanceV1ForReceiver(addRess,freezeBalance,freezeDuration,resourceCode,
          null == receiverAddressBytes ? null : receiverAddressBytes.toByteArray(),priKey,blockingStubFull);
    }
  }

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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


  /** constructor. */
  public static Optional<DelegatedResourceAccountIndex>
      getDelegatedResourceAccountIndexFromSolidity(
          byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {

    ByteString addressBs = ByteString.copyFrom(address);

    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();

    DelegatedResourceAccountIndex accountIndex =
        blockingStubFull.getDelegatedResourceAccountIndex(bytesMessage);
    return Optional.ofNullable(accountIndex);
  }
  /** constructor. */
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
  /** constructor. */
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
  /** constructor. */
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndexV2(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();

    DelegatedResourceAccountIndex accountIndex =
          blockingStubFull.getDelegatedResourceAccountIndexV2(bytesMessage);
    return Optional.ofNullable(accountIndex);
  }
  /** constructor. */
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndexV2Solidity(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage bytesMessage = BytesMessage.newBuilder().setValue(addressBs).build();

    DelegatedResourceAccountIndex accountIndex =
        blockingStubFull.getDelegatedResourceAccountIndexV2(bytesMessage);
    return Optional.ofNullable(accountIndex);
  }

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


  /** constructor. */
  public static AssetIssueContract getAssetIssueByName(
      String assetName, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    return blockingStubFull.getAssetIssueByName(request);
  }

  /** constructor. */
  public static AssetIssueContract getAssetIssueByNameFromSolidity(
      String assetName, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    return blockingStubFull.getAssetIssueByName(request);
  }

  /** constructor. */
  public static Optional<AssetIssueList> getAssetIssueListByName(
      String assetName, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueListByName(request);
    return Optional.ofNullable(assetIssueList);
  }

  /** constructor. */
  public static Optional<AssetIssueList> getAssetIssueListByNameFromSolidity(
      String assetName, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetNameBs = ByteString.copyFrom(assetName.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetNameBs).build();
    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueListByName(request);
    return Optional.ofNullable(assetIssueList);
  }

  /** constructor. */
  public static Optional<GrpcAPI.AssetIssueList> listAssetIssueFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    GrpcAPI.AssetIssueList assetIssueList =
        blockingStubFull.getAssetIssueList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(assetIssueList);
  }

  /** constructor. */
  public static Optional<GrpcAPI.AssetIssueList> listAssetIssuepaginatedFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull, Long offset, Long limit) {
    GrpcAPI.PaginatedMessage.Builder pageMessageBuilder = GrpcAPI.PaginatedMessage.newBuilder();
    pageMessageBuilder.setOffset(offset);
    pageMessageBuilder.setLimit(limit);
    AssetIssueList assetIssueList =
        blockingStubFull.getPaginatedAssetIssueList(pageMessageBuilder.build());
    return Optional.ofNullable(assetIssueList);
  }

  /** constructor. */
  public static Optional<GrpcAPI.WitnessList> listWitnesses(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    GrpcAPI.WitnessList witnessList =
        blockingStubFull.listWitnesses(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(witnessList);
  }

  /** constructor. */
  public static Optional<GrpcAPI.WitnessList> listWitnessesFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    GrpcAPI.WitnessList witnessList =
        blockingStubFull.listWitnesses(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(witnessList);
  }

  /** constructor. */
  public static AssetIssueContract getAssetIssueById(
      String assetId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetIdBs = ByteString.copyFrom(assetId.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetIdBs).build();
    return blockingStubFull.getAssetIssueById(request);
  }

  /** constructor. */
  public static AssetIssueContract getAssetIssueByIdFromSolidity(
      String assetId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString assetIdBs = ByteString.copyFrom(assetId.getBytes());
    BytesMessage request = BytesMessage.newBuilder().setValue(assetIdBs).build();
    return blockingStubFull.getAssetIssueById(request);
  }

  /** constructor. */
  public static Optional<AssetIssueList> getAssetIssueByAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AssetIssueList assetIssueList = blockingStubFull.getAssetIssueByAccount(request);
    return Optional.ofNullable(assetIssueList);
  }

  private static Permission json2Permission(JSONObject json) {
    Permission.Builder permissionBuilder = Permission.newBuilder();
    if (json.containsKey("type")) {
      int type = json.getInteger("type");
      permissionBuilder.setTypeValue(type);
    }
    if (json.containsKey("permission_name")) {
      String permissionName = json.getString("permission_name");
      permissionBuilder.setPermissionName(permissionName);
    }
    if (json.containsKey("threshold")) {
      // long threshold = json.getLong("threshold");
      long threshold = Long.parseLong(json.getString("threshold"));
      permissionBuilder.setThreshold(threshold);
    }
    if (json.containsKey("parent_id")) {
      int parentId = json.getInteger("parent_id");
      permissionBuilder.setParentId(parentId);
    }
    if (json.containsKey("operations")) {
      byte[] operations = ByteArray.fromHexString(json.getString("operations"));
      permissionBuilder.setOperations(ByteString.copyFrom(operations));
    }
    if (json.containsKey("keys")) {
      JSONArray keys = json.getJSONArray("keys");
      List<Key> keyList = new ArrayList<>();
      for (int i = 0; i < keys.size(); i++) {
        Key.Builder keyBuilder = Key.newBuilder();
        JSONObject key = keys.getJSONObject(i);
        String address = key.getString("address");
        long weight = Long.parseLong(key.getString("weight"));
        // long weight = key.getLong("weight");
        // keyBuilder.setAddress(ByteString.copyFrom(address.getBytes()));
        keyBuilder.setAddress(ByteString.copyFrom(WalletClient.decodeFromBase58Check(address)));
        keyBuilder.setWeight(weight);
        keyList.add(keyBuilder.build());
      }
      permissionBuilder.addAllKeys(keyList);
    }
    return permissionBuilder.build();
  }

  /** constructor. */
  public static boolean accountPermissionUpdate(
      String permissionJson,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] priKeys) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
    JSONArray activepermissions = permissions.getJSONArray("active_permissions");

    if (ownerpermission != null) {
      Permission ownerPermission = json2Permission(ownerpermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesspermission != null) {
      Permission witnessPermission = json2Permission(witnesspermission);
      builder.setWitness(witnessPermission);
    }
    if (activepermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activepermissions.size(); j++) {
        JSONObject permission = activepermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static long getFreezeBalanceCount(
      byte[] accountAddress,
      String ecKey,
      Long targetEnergy,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Precision change as the entire network freezes
    AccountResourceMessage resourceInfo = getAccountResource(accountAddress, blockingStubFull);

    Account info = queryAccount(accountAddress, blockingStubFull);

    Account getAccount = queryAccount(ecKey, blockingStubFull);

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

  /** constructor. */
  public static Long getAssetIssueValue(
      byte[] accountAddress,
      ByteString assetIssueId,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Long assetIssueCount = 0L;
    Account contractAccount = queryAccount(accountAddress, blockingStubFull);
    Map<String, Long> createAssetIssueMap = contractAccount.getAssetV2Map();
    for (Map.Entry<String, Long> entry : createAssetIssueMap.entrySet()) {
      if (assetIssueId.toStringUtf8().equals(entry.getKey())) {
        assetIssueCount = entry.getValue();
      }
    }
    return assetIssueCount;
  }

  /** constructor. */
  public static List<String> getStrings(byte[] data) {
    int index = 0;
    List<String> ret = new ArrayList<>();
    while (index < data.length) {
      ret.add(byte2HexStr(data, index, 32));
      index += 32;
    }
    return ret;
  }

  /** constructor. */
  public static String byte2HexStr(byte[] b, int offset, int length) {
    StringBuilder ssBuilder = new StringBuilder();
    for (int n = offset; n < offset + length && n < b.length; n++) {
      String stmp = Integer.toHexString(b[n] & 0xFF);
      ssBuilder.append((stmp.length() == 1) ? "0" + stmp : stmp);
    }
    return ssBuilder.toString().toUpperCase().trim();
  }

  /** constructor. */
  public static Transaction addTransactionSign(
      Transaction transaction, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    Transaction.Builder transactionBuilderSigned = transaction.toBuilder();
    byte[] hash =
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray());

    ECKey.ECDSASignature signature = ecKey.sign(hash);
    ByteString bsSign = ByteString.copyFrom(signature.toByteArray());
    transactionBuilderSigned.addSignature(bsSign);
    transaction = transactionBuilderSigned.build();
    return transaction;
  }

  /** constructor. */
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
      estimateDeployContractEnergy(PublicMethed.code, value, tokenId, tokenValue, ownerAddress, blockingStubFull);
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    byte[] contractAddress = generateContractAddress(transaction, owner);
    System.out.println(
        "Your smart contract address will be: " + WalletClient.encode58Check(contractAddress));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response;
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response;
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

  /** constructor. */
  public static GrpcAPI.Return accountPermissionUpdateForResponse(
      String permissionJson,
      byte[] owner,
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

    AccountPermissionUpdateContract.Builder builder = AccountPermissionUpdateContract.newBuilder();

    JSONObject permissions = JSONObject.parseObject(permissionJson);
    JSONObject ownerpermission = permissions.getJSONObject("owner_permission");
    JSONObject witnesspermission = permissions.getJSONObject("witness_permission");
    JSONArray activepermissions = permissions.getJSONArray("active_permissions");

    if (ownerpermission != null) {
      Permission ownerPermission = json2Permission(ownerpermission);
      builder.setOwner(ownerPermission);
    }
    if (witnesspermission != null) {
      Permission witnessPermission = json2Permission(witnesspermission);
      builder.setWitness(witnessPermission);
    }
    if (activepermissions != null) {
      List<Permission> activePermissionList = new ArrayList<>();
      for (int j = 0; j < activepermissions.size(); j++) {
        JSONObject permission = activepermissions.getJSONObject(j);
        activePermissionList.add(json2Permission(permission));
      }
      builder.addAllActives(activePermissionList);
    }
    builder.setOwnerAddress(ByteString.copyFrom(owner));

    AccountPermissionUpdateContract contract = builder.build();

    TransactionExtention transactionExtention = blockingStubFull.accountPermissionUpdate(contract);
    if (transactionExtention == null) {
      return null;
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
      return ret;
    }
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response;
  }

  public static TransactionApprovedList getTransactionApprovedList(
      Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return blockingStubFull.getTransactionApprovedList(transaction);
  }

  /** constructor. */
  public static long getFreezeBalanceNetCount(
      byte[] accountAddress,
      String ecKey,
      Long targetNet,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Precision change as the entire network freezes
    AccountResourceMessage resourceInfo = getAccountResource(accountAddress, blockingStubFull);

    Account info = queryAccount(accountAddress, blockingStubFull);

    Account getAccount = queryAccount(ecKey, blockingStubFull);

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

  /** constructor. */
  public static GrpcAPI.Return broadcastTransaction(
      Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    String txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    logger.info("broadcastTransaction: " + txid);
    int i = 10;
    GrpcAPI.Return response = blockingStubFull.broadcastTransaction(transaction);
    while (!response.getResult() && response.getCode() == response_code.SERVER_BUSY && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
      logger.info("repeate times = " + (10 - i));
    }

    if (response.getResult() == false) {
      logger.info("Code = " + response.getCode());
      logger.info("Message = " + response.getMessage().toStringUtf8());
    }
    return response;
  }

  /** constructor. */
  public static GrpcAPI.Return broadcastTransactionBoth(
      Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletGrpc.WalletBlockingStub blockingStubFull1) {
    int i = 10;
    waitProduceNextBlock(blockingStubFull1);
    GrpcAPI.Return response = blockingStubFull1.broadcastTransaction(transaction);
    GrpcAPI.Return response1 = blockingStubFull.broadcastTransaction(transaction);
    while (response.getResult() == false
        && response.getCode() == response_code.SERVER_BUSY
        && i > 0) {
      try {
        Thread.sleep(300);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      i--;
      response = blockingStubFull.broadcastTransaction(transaction);
      logger.info("repeate times = " + (10 - i));
    }

    if (response.getResult() == false) {
      logger.info("Code = " + response.getCode());
      logger.info("Message = " + response.getMessage().toStringUtf8());
    }
    return response;
  }

  /** constructor. */
  public synchronized static String exec(String command) throws InterruptedException {
    String returnString = "";
    String errReturnString = "";
    Process pro = null;
    Runtime runTime = Runtime.getRuntime();
    if (runTime == null) {
      logger.error("Create runtime false!");
    }
    try {
      pro = runTime.exec(command);
      BufferedReader input = new BufferedReader(new InputStreamReader(pro.getInputStream()));
      PrintWriter output = new PrintWriter(new OutputStreamWriter(pro.getOutputStream()));
      String line;
      while ((line = input.readLine()) != null) {
        returnString = returnString + line + "\n";
      }
      InputStream stderr = pro.getErrorStream();
      InputStreamReader errReader = new InputStreamReader(stderr);
      BufferedReader br = new BufferedReader(errReader);
      String errLine = null;
      while ((errLine = br.readLine())!=null) {
        errReturnString = errReturnString + errLine + "\n";
      }
      input.close();
      output.close();
      errReader.close();
      br.close();
      pro.destroy();
    } catch (IOException ex) {
      logger.error(null, ex);
    }
    return returnString.length() >= errReturnString.length() ? returnString : errReturnString;
  }


  /** constructor. */
  public static HashMap<String, String> getBycodeAbiNoOptimize(
      String solFile, String contractName) {
    final String compile =
        Configuration.getByPath("testng.conf").getString("defaultParameter.solidityCompile");

    String dirPath = solFile.substring(solFile.lastIndexOf("/"), solFile.lastIndexOf("."));
    String outputPath = "src/test/resources/soliditycode//output" + dirPath;

    File binFile = new File(outputPath + "/" + contractName + ".bin");
    File abiFile = new File(outputPath + "/" + contractName + ".abi");
    if (binFile.exists()) {
      binFile.delete();
    }
    if (abiFile.exists()) {
      abiFile.delete();
    }

    HashMap<String, String> retMap = new HashMap<>();
    String absolutePath = System.getProperty("user.dir");
    logger.debug("absolutePath: " + absolutePath);
    logger.debug("solFile: " + solFile);
    logger.debug("outputPath: " + outputPath);
    String cmd =
        compile
            + " --bin --abi --overwrite "
            + absolutePath
            + "/"
            + solFile
            + " -o "
            + absolutePath
            + "/"
            + outputPath;
    logger.info("cmd: " + cmd);

    String byteCode = null;
    String abI = null;

    // compile solidity file
    try {
      exec(cmd);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // get byteCode and ABI
    try {
      byteCode = fileRead(outputPath + "/" + contractName + ".bin", false);
      retMap.put("byteCode", byteCode);
      logger.debug("byteCode: " + byteCode);
      abI = fileRead(outputPath + "/" + contractName + ".abi", false);
      retMap.put("abI", abI);
      logger.debug("abI: " + abI);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return retMap;
  }

  /** constructor. */
  public synchronized static HashMap<String, String> getBycodeAbi(String solFile, String contractName) {
    final String compile =
        Configuration.getByPath("testng.conf").getString("defaultParameter.solidityCompile");

    String dirPath = solFile.substring(solFile.lastIndexOf("/"), solFile.lastIndexOf("."));
    String outputPath = "src/test/resources/soliditycode//output" + dirPath;

    File binFile = new File(outputPath + "/" + contractName + ".bin");
    File abiFile = new File(outputPath + "/" + contractName + ".abi");
    if (binFile.exists()) {
      binFile.delete();
    }
    if (abiFile.exists()) {
      abiFile.delete();
    }

    HashMap<String, String> retMap = new HashMap<>();
    String absolutePath = System.getProperty("user.dir");
    logger.debug("absolutePath: " + absolutePath);
    logger.debug("solFile: " + solFile);
    logger.debug("outputPath: " + outputPath);
    String cmd =
        compile
            + " --optimize --bin --abi --overwrite "
            + absolutePath
            + "/"
            + solFile
            + " -o "
            + absolutePath
            + "/"
            + outputPath;
    logger.info("cmd: " + cmd);

    String byteCode = null;
    String abI = null;


    // compile solidity file
    try {
      exec(cmd);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    // get byteCode and ABI
    try {
      byteCode = fileRead(outputPath + "/" + contractName + ".bin", false);
      retMap.put("byteCode", byteCode);
      logger.debug("byteCode: " + byteCode);
      abI = fileRead(outputPath + "/" + contractName + ".abi", false);
      retMap.put("abI", abI);
      logger.debug("abI: " + abI);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return retMap;
  }

  /** constructor. */
  public static String fileRead(String filePath, boolean isLibrary) throws Exception {
    File file = new File(filePath);
    FileReader reader = new FileReader(file);
    BufferedReader breader = new BufferedReader(reader);
    StringBuilder sb = new StringBuilder();
    String s = "";
    if (!isLibrary) {
      if ((s = breader.readLine()) != null) {
        sb.append(s);
      }
      breader.close();
    } else {
      String fistLine = breader.readLine();
      breader.readLine();
      if ((s = breader.readLine()) != null && !s.equals("")) {
        s = s.substring(s.indexOf("-> ") + 3);
        sb.append(s + ":");
      } else {
        s = fistLine.substring(fistLine.indexOf("__") + 2, fistLine.lastIndexOf("__"));
        sb.append(s + ":");
      }
      breader.close();
    }
    return sb.toString();
  }

  /** constructor. */
  public static HashMap<String, String> getBycodeAbiForLibrary(
      String solFile, String contractName) {
    HashMap retMap = null;
    String dirPath = solFile.substring(solFile.lastIndexOf("/"), solFile.lastIndexOf("."));
    String outputPath = "src/test/resources/soliditycode/output" + dirPath;
    try {
      retMap = PublicMethed.getBycodeAbi(solFile, contractName);
      String library = fileRead(outputPath + "/" + contractName + ".bin", true);
      retMap.put("library", library);
      logger.debug("library: " + library);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return retMap;
  }

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** constructor. */
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

  /** constructor. */
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
    transaction = signTransaction(ecKey, transaction);
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction.getRawData().toByteArray())));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    if (response.getResult() == false) {
      return null;
    } else {
      return ByteArray.toHexString(
          Sha256Hash.hash(
              CommonParameter.getInstance().isECKeyCryptoEngine(),
              transaction.getRawData().toByteArray()));
    }
  }

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
  public static String create2(String[] parameters) {
    if (parameters == null || parameters.length != 3) {
      logger.error("create2 needs 3 parameter:\ncreate2 address code salt");
      return null;
    }

    byte[] address = WalletClient.decodeFromBase58Check(parameters[0]);
    if (!WalletClient.addressValid(address)) {
      logger.error("length of address must be 21 bytes.");
      return null;
    }

    byte[] code = Hex.decode(parameters[1]);
    byte[] temp = Longs.toByteArray(Long.parseLong(parameters[2]));
    if (temp.length != 8) {
      logger.error("Invalid salt!");
      return null;
    }
    byte[] salt = new byte[32];
    System.arraycopy(temp, 0, salt, 24, 8);

    byte[] mergedData = ByteUtil.merge(address, salt, Hash.sha3(code));
    String create2Address = Base58.encode58Check(Hash.sha3omit12(mergedData));

    logger.info("create2 Address: " + create2Address);

    return create2Address;
  }

  /** constructor. */
  public static boolean sendShieldCoin(
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
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

    PrivateParameters.Builder builder = PrivateParameters.newBuilder();
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
      builder.setTransparentFromAddress(ByteString.copyFrom(publicZenTokenOwnerAddress));
      builder.setFromAmount(fromAmount);
    }
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
      builder.setTransparentToAddress(ByteString.copyFrom(publicZenTokenToAddress));
      builder.setToAmount(toAmount);
    }

    if (shieldAddressInfo != null) {
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      outPointBuild.setIndex(noteTx.getIndex());
      request.addOutPoints(outPointBuild.build());

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));

      // String shieldAddress = noteInfo.getPaymentAddress();
      // ShieldAddressInfo addressInfo =
      //    shieldWrapper.getShieldAddressInfoMap().get(shieldAddress);
      SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
      try {
        ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
        builder.setAsk(ByteString.copyFrom(expandedSpendingKey.getAsk()));
        builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
        builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));
      } catch (Exception e) {
        System.out.println(e);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

      // System.out.println("address " + noteInfo.getPaymentAddress());
      // System.out.println("value " + noteInfo.getValue());
      // System.out.println("rcm " + ByteArray.toHexString(noteInfo.getR()));
      // System.out.println("trxId " + noteInfo.getTrxId());
      // System.out.println("index " + noteInfo.getIndex());
      // System.out.println("meno " + new String(noteInfo.getMemo()));

      SpendNote.Builder spendNoteBuilder = SpendNote.newBuilder();
      spendNoteBuilder.setNote(noteBuild.build());
      try {
        spendNoteBuilder.setAlpha(
            ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
      } catch (Exception e) {
        System.out.println(e);
      }

      IncrementalMerkleVoucherInfo merkleVoucherInfo =
          blockingStubFull.getMerkleTreeVoucherInfo(request.build());
      spendNoteBuilder.setVoucher(merkleVoucherInfo.getVouchers(0));
      spendNoteBuilder.setPath(merkleVoucherInfo.getPaths(0));

      builder.addShieldedSpends(spendNoteBuilder.build());

    } else {
      byte[] ovk =
          ByteArray.fromHexString(
              "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      builder.setOvk(ByteString.copyFrom(ovk));
    }

    if (shieldOutputList.size() > 0) {
      for (int i = 0; i < shieldOutputList.size(); ++i) {
        builder.addShieldedReceives(
            ReceiveNote.newBuilder().setNote(shieldOutputList.get(i)).build());
      }
    }

    TransactionExtention transactionExtention =
        blockingStubFull.createShieldedTransaction(builder.build());
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
    Any any = transaction.getRawData().getContract(0).getParameter();

    try {
      ShieldedTransferContract shieldedTransferContract =
          any.unpack(ShieldedTransferContract.class);
      if (shieldedTransferContract.getFromAmount() > 0 || fromAmount == 321321) {
        transaction = signTransactionForShield(ecKey, transaction);
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      } else {
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    return broadcastTransaction(transaction, blockingStubFull).getResult();
  }

  /** constructor. */
  public static boolean sendShieldCoinWithoutAsk(
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
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

    PrivateParametersWithoutAsk.Builder builder = PrivateParametersWithoutAsk.newBuilder();
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
      builder.setTransparentFromAddress(ByteString.copyFrom(publicZenTokenOwnerAddress));
      builder.setFromAmount(fromAmount);
    }
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
      builder.setTransparentToAddress(ByteString.copyFrom(publicZenTokenToAddress));
      builder.setToAmount(toAmount);
    }

    byte[] ask = new byte[32];
    if (shieldAddressInfo != null) {
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      outPointBuild.setIndex(noteTx.getIndex());
      request.addOutPoints(outPointBuild.build());
      IncrementalMerkleVoucherInfo merkleVoucherInfo =
          blockingStubFull.getMerkleTreeVoucherInfo(request.build());
      if (merkleVoucherInfo.getVouchersCount() != 1) {
        System.out.println("Can't get all merkel tree, please check the notes.");
        return false;
      }

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));

      // String shieldAddress = noteInfo.getPaymentAddress();
      // ShieldAddressInfo addressInfo =
      //    shieldWrapper.getShieldAddressInfoMap().get(shieldAddress);
      String shieldAddress = noteTx.getNote().getPaymentAddress();
      SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
      try {
        ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
        System.arraycopy(expandedSpendingKey.getAsk(), 0, ask, 0, 32);
        builder.setAk(
            ByteString.copyFrom(ExpandedSpendingKey.getAkFromAsk(expandedSpendingKey.getAsk())));
        builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
        builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));
      } catch (Exception e) {
        System.out.println(e);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

      // System.out.println("address " + noteInfo.getPaymentAddress());
      // System.out.println("value " + noteInfo.getValue());
      // System.out.println("rcm " + ByteArray.toHexString(noteInfo.getR()));
      // System.out.println("trxId " + noteInfo.getTrxId());
      // System.out.println("index " + noteInfo.getIndex());
      // System.out.println("meno " + new String(noteInfo.getMemo()));

      SpendNote.Builder spendNoteBuilder = SpendNote.newBuilder();
      spendNoteBuilder.setNote(noteBuild.build());
      try {
        spendNoteBuilder.setAlpha(
            ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
      } catch (Exception e) {
        System.out.println(e);
      }

      spendNoteBuilder.setVoucher(merkleVoucherInfo.getVouchers(0));
      spendNoteBuilder.setPath(merkleVoucherInfo.getPaths(0));

      builder.addShieldedSpends(spendNoteBuilder.build());

    } else {
      byte[] ovk =
          ByteArray.fromHexString(
              "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      builder.setOvk(ByteString.copyFrom(ovk));
    }

    if (shieldOutputList.size() > 0) {
      for (int i = 0; i < shieldOutputList.size(); ++i) {
        builder.addShieldedReceives(
            ReceiveNote.newBuilder().setNote(shieldOutputList.get(i)).build());
      }
    }

    TransactionExtention transactionExtention =
        blockingStubFull.createShieldedTransactionWithoutSpendAuthSig(builder.build());
    if (transactionExtention == null) {
      System.out.println("sendShieldCoinWithoutAsk failure.");
      return false;
    }
    BytesMessage trxHash =
        blockingStubFull.getShieldTransactionHash(transactionExtention.getTransaction());
    if (trxHash == null || trxHash.getValue().toByteArray().length != 32) {
      System.out.println("sendShieldCoinWithoutAsk get transaction hash failure.");
      return false;
    }
    Transaction transaction = transactionExtention.getTransaction();
    if (transaction.getRawData().getContract(0).getType()
        != ContractType.ShieldedTransferContract) {
      System.out.println("This method only for ShieldedTransferContract, please check!");
      return false;
    }
    Any any = transaction.getRawData().getContract(0).getParameter();
    Transaction transaction1 = transactionExtention.getTransaction();
    try {
      ShieldedTransferContract shieldContract = any.unpack(ShieldedTransferContract.class);
      List<SpendDescription> spendDescList = shieldContract.getSpendDescriptionList();
      ShieldedTransferContract.Builder contractBuild =
          shieldContract.toBuilder().clearSpendDescription();
      for (int i = 0; i < spendDescList.size(); i++) {

        SpendAuthSigParameters.Builder builder1 = SpendAuthSigParameters.newBuilder();
        builder1.setAsk(ByteString.copyFrom(ask));
        builder1.setTxHash(ByteString.copyFrom(trxHash.getValue().toByteArray()));
        builder1.setAlpha(builder.getShieldedSpends(i).getAlpha());
        SpendDescription.Builder spendDescription = spendDescList.get(i).toBuilder();
        BytesMessage authSig = blockingStubFull.createSpendAuthSig(builder1.build());
        spendDescription.setSpendAuthoritySignature(
            ByteString.copyFrom(authSig.getValue().toByteArray()));

        contractBuild.addSpendDescription(spendDescription.build());
      }

      Transaction.raw.Builder rawBuilder =
          transaction.toBuilder()
              .getRawDataBuilder()
              .clearContract()
              .addContract(
                  Transaction.Contract.newBuilder()
                      .setType(ContractType.ShieldedTransferContract)
                      .setParameter(Any.pack(contractBuild.build()))
                      .build());

      transaction = transaction.toBuilder().clearRawData().setRawData(rawBuilder).build();

      transactionExtention = transactionExtention.toBuilder().setTransaction(transaction).build();

      if (transactionExtention == null) {
        return false;
      }
      Return ret = transactionExtention.getResult();
      if (!ret.getResult()) {
        System.out.println("Code = " + ret.getCode());
        System.out.println("Message = " + ret.getMessage().toStringUtf8());
        return false;
      }
      transaction1 = transactionExtention.getTransaction();
      if (transaction == null || transaction.getRawData().getContractCount() == 0) {
        System.out.println("Transaction is empty");
        return false;
      }
      System.out.println(
          "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));

      if (transaction1.getRawData().getContract(0).getType()
          != ContractType.ShieldedTransferContract) {
        transaction1 = signTransaction(ecKey, transaction1);
      } else {
        Any any1 = transaction1.getRawData().getContract(0).getParameter();
        ShieldedTransferContract shieldedTransferContract =
            any1.unpack(ShieldedTransferContract.class);
        if (shieldedTransferContract.getFromAmount() > 0) {
          transaction1 = signTransactionForShield(ecKey, transaction1);
          System.out.println(
              "trigger txid = "
                  + ByteArray.toHexString(
                      Sha256Hash.hash(
                          CommonParameter.getInstance().isECKeyCryptoEngine(),
                          transaction1.getRawData().toByteArray())));
        }
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    System.out.println(
        "trigger txid = "
            + ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    transaction1.getRawData().toByteArray())));
    return broadcastTransaction(transaction1, blockingStubFull).getResult();
  }

  /** constructor. */
  public static List<Note> addShieldOutputList(
      List<Note> shieldOutList, String shieldToAddress, String toAmountString, String menoString) {
    String shieldAddress = shieldToAddress;
    String amountString = toAmountString;
    if (menoString.equals("null")) {
      menoString = "";
    }
    long shieldAmount = 0;
    if (!StringUtil.isNullOrEmpty(amountString)) {
      shieldAmount = Long.valueOf(amountString);
    }

    Note.Builder noteBuild = Note.newBuilder();
    noteBuild.setPaymentAddress(shieldAddress);
    noteBuild.setPaymentAddress(shieldAddress);
    noteBuild.setValue(shieldAmount);
    try {
      noteBuild.setRcm(
          ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
    } catch (Exception e) {
      System.out.println(e);
    }
    noteBuild.setMemo(ByteString.copyFrom(menoString.getBytes()));
    shieldOutList.add(noteBuild.build());
    // logger.info(shieldOutList.toString());
    return shieldOutList;
  }

  /** constructor. */
  public static Optional<ShieldAddressInfo> generateShieldAddress() {
    ShieldAddressInfo addressInfo = new ShieldAddressInfo();
    try {
      DiversifierT diversifier = DiversifierT.random();
      SpendingKey spendingKey = SpendingKey.random();
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      IncomingViewingKey incomingViewingKey = fullViewingKey.inViewingKey();
      PaymentAddress paymentAddress = incomingViewingKey.address(diversifier).get();

      addressInfo.setSk(spendingKey.getValue());
      addressInfo.setD(diversifier);
      addressInfo.setIvk(incomingViewingKey.getValue());
      addressInfo.setOvk(fullViewingKey.getOvk());
      addressInfo.setPkD(paymentAddress.getPkD());

      if (addressInfo.validateCheck()) {
        return Optional.of(addressInfo);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return Optional.empty();
  }

  /** constructor. */
  public static DecryptNotes listShieldNote(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    logger.info(ByteArray.toHexString(shieldAddressInfo.get().ivk));
    IvkDecryptParameters.Builder builder = IvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum);
    builder.setEndBlockIndex(currentBlockNum + 1);
    builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
    DecryptNotes notes = blockingStubFull.scanNoteByIvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** constructor. */
  public static DecryptNotes getShieldNotesByIvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    // startBlockNum = 0L;
    logger.info("ivk:" + ByteArray.toHexString(shieldAddressInfo.get().ivk));
    IvkDecryptParameters.Builder builder = IvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum + 1);
    builder.setEndBlockIndex(currentBlockNum + 1);
    builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
    DecryptNotes notes = blockingStubFull.scanNoteByIvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** constructor. */
  public static DecryptNotesMarked getShieldNotesAndMarkByIvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    // startBlockNum = 0L;
    logger.info("ivk:" + ByteArray.toHexString(shieldAddressInfo.get().ivk));
    try {
      IvkDecryptAndMarkParameters.Builder builder = IvkDecryptAndMarkParameters.newBuilder();
      builder.setStartBlockIndex(startBlockNum + 1);
      builder.setEndBlockIndex(currentBlockNum + 1);
      builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
      builder.setAk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getAk()));
      builder.setNk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getNk()));
      DecryptNotesMarked decryptNotes = blockingStubFull.scanAndMarkNoteByIvk(builder.build());
      logger.info(decryptNotes.toString());
      return decryptNotes;
    } catch (Exception e) {
      logger.info(e.toString());
      return null;
    }
  }

  /** constructor. */
  public static DecryptNotesMarked getShieldNotesAndMarkByIvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    // startBlockNum = 0L;
    logger.info("ivk:" + ByteArray.toHexString(shieldAddressInfo.get().ivk));
    try {
      IvkDecryptAndMarkParameters.Builder builder = IvkDecryptAndMarkParameters.newBuilder();
      builder.setStartBlockIndex(startBlockNum + 1);
      builder.setEndBlockIndex(currentBlockNum + 1);
      builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
      builder.setAk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getAk()));
      builder.setNk(ByteString.copyFrom(shieldAddressInfo.get().getFullViewingKey().getNk()));
      DecryptNotesMarked decryptNotes = blockingStubSolidity.scanAndMarkNoteByIvk(builder.build());
      logger.info(decryptNotes.toString());
      return decryptNotes;
    } catch (Exception e) {
      logger.info(e.toString());
      return null;
    }
  }

  /** constructor. */
  public static DecryptNotes getShieldNotesByIvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    IvkDecryptParameters.Builder builder = IvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum);
    builder.setEndBlockIndex(currentBlockNum);
    builder.setIvk(ByteString.copyFrom(shieldAddressInfo.get().getIvk()));
    DecryptNotes notes = blockingStubSolidity.scanNoteByIvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** constructor. */
  public static DecryptNotes getShieldNotesByOvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    logger.info("ovk:" + ByteArray.toHexString(shieldAddressInfo.get().ovk));
    OvkDecryptParameters.Builder builder = OvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum + 1);
    builder.setEndBlockIndex(currentBlockNum + 1);
    builder.setOvk(ByteString.copyFrom(shieldAddressInfo.get().getOvk()));
    DecryptNotes notes = blockingStubFull.scanNoteByOvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** constructor. */
  public static DecryptNotes getShieldNotesByOvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Block currentBlock =
        blockingStubSolidity.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
    Long currentBlockNum = currentBlock.getBlockHeader().getRawData().getNumber();
    Long startBlockNum = 0L;
    if (currentBlockNum > 100) {
      startBlockNum = currentBlockNum - 100;
    }
    OvkDecryptParameters.Builder builder = OvkDecryptParameters.newBuilder();
    builder.setStartBlockIndex(startBlockNum);
    builder.setEndBlockIndex(currentBlockNum);
    builder.setOvk(ByteString.copyFrom(shieldAddressInfo.get().getOvk()));
    DecryptNotes notes = blockingStubSolidity.scanNoteByOvk(builder.build());
    logger.info(notes.toString());
    return notes;
  }

  /** constructor. */
  public static String getMemo(Note note) {
    return ZenUtils.getMemo(note.getMemo().toByteArray());
  }

  /** constructor. */
  public static SpendResult getSpendResult(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletGrpc.WalletBlockingStub blockingStubFull) {

    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
    outPointBuild.setIndex(noteTx.getIndex());
    request.addOutPoints(outPointBuild.build());
    Optional<IncrementalMerkleVoucherInfo> merkleVoucherInfo =
        Optional.of(blockingStubFull.getMerkleTreeVoucherInfo(request.build()));

    if (merkleVoucherInfo.isPresent() && merkleVoucherInfo.get().getVouchersCount() > 0) {
      NoteParameters.Builder builder = NoteParameters.newBuilder();
      try {
        builder.setAk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getAk()));
        builder.setNk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getNk()));
        logger.info("AK:" + ByteArray.toHexString(shieldAddressInfo.getFullViewingKey().getAk()));
        logger.info("NK:" + ByteArray.toHexString(shieldAddressInfo.getFullViewingKey().getNk()));
      } catch (Exception e) {
        Assert.assertTrue(1 == 1);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));
      builder.setNote(noteBuild.build());
      builder.setTxid(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      builder.setIndex(noteTx.getIndex());
      // builder.setVoucher(merkleVoucherInfo.getVouchers(0));

      SpendResult result = blockingStubFull.isSpend(builder.build());
      return result;
    }
    return null;
  }

  /** constructor. */
  public static SpendResult getSpendResultOnSolidity(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
    outPointBuild.setIndex(noteTx.getIndex());
    request.addOutPoints(outPointBuild.build());
    Optional<IncrementalMerkleVoucherInfo> merkleVoucherInfo =
        Optional.of(blockingStubSolidity.getMerkleTreeVoucherInfo(request.build()));

    if (merkleVoucherInfo.isPresent() && merkleVoucherInfo.get().getVouchersCount() > 0) {
      NoteParameters.Builder builder = NoteParameters.newBuilder();
      try {
        builder.setAk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getAk()));
        builder.setNk(ByteString.copyFrom(shieldAddressInfo.getFullViewingKey().getNk()));
      } catch (Exception e) {
        Assert.assertTrue(1 == 1);
      }
      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));
      builder.setNote(noteBuild.build());
      builder.setTxid(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      builder.setIndex(noteTx.getIndex());
      // builder.setVoucher(merkleVoucherInfo.getVouchers(0));

      SpendResult result = blockingStubSolidity.isSpend(builder.build());
      return result;
    }
    return null;
  }

  /** constructor. */
  public static String getShieldNullifier(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    OutputPointInfo.Builder request = OutputPointInfo.newBuilder();
    OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
    outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
    outPointBuild.setIndex(noteTx.getIndex());
    request.addOutPoints(outPointBuild.build());
    IncrementalMerkleVoucherInfo merkleVoucherInfo =
        blockingStubFull.getMerkleTreeVoucherInfo(request.build());
    if (merkleVoucherInfo.getVouchersCount() < 1) {
      System.out.println("get merkleVoucherInfo failure.");
      return null;
    }
    Note.Builder noteBuild = Note.newBuilder();
    noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
    noteBuild.setValue(noteTx.getNote().getValue());
    noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
    noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

    String shieldAddress = noteTx.getNote().getPaymentAddress();
    SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
    try {
      // TODO
      FullViewingKey fullViewingKey = spendingKey.fullViewingKey();
      NfParameters.Builder builder = NfParameters.newBuilder();
      builder.setNote(noteBuild.build());
      builder.setVoucher(merkleVoucherInfo.getVouchers(0));
      builder.setAk(ByteString.copyFrom(fullViewingKey.getAk()));
      builder.setNk(ByteString.copyFrom(fullViewingKey.getNk()));

      BytesMessage nullifier = blockingStubFull.createShieldNullifier(builder.build());
      return ByteArray.toHexString(nullifier.getValue().toByteArray());

    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /** constructor. */
  public static String sendShieldCoinGetTxid(
      byte[] publicZenTokenOwnerAddress,
      long fromAmount,
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      List<GrpcAPI.Note> shieldOutputList,
      byte[] publicZenTokenToAddress,
      long toAmount,
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

    PrivateParameters.Builder builder = PrivateParameters.newBuilder();
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenOwnerAddress)) {
      builder.setTransparentFromAddress(ByteString.copyFrom(publicZenTokenOwnerAddress));
      builder.setFromAmount(fromAmount);
    }
    if (!ByteUtil.isNullOrZeroArray(publicZenTokenToAddress)) {
      builder.setTransparentToAddress(ByteString.copyFrom(publicZenTokenToAddress));
      builder.setToAmount(toAmount);
    }

    if (shieldAddressInfo != null) {
      OutputPointInfo.Builder request = OutputPointInfo.newBuilder();

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));
      OutputPoint.Builder outPointBuild = OutputPoint.newBuilder();
      outPointBuild.setHash(ByteString.copyFrom(noteTx.getTxid().toByteArray()));
      outPointBuild.setIndex(noteTx.getIndex());
      request.addOutPoints(outPointBuild.build());

      // ShieldNoteInfo noteInfo = shieldWrapper.getUtxoMapNote().get(shieldInputList.get(i));

      // String shieldAddress = noteInfo.getPaymentAddress();
      // ShieldAddressInfo addressInfo =
      //    shieldWrapper.getShieldAddressInfoMap().get(shieldAddress);
      SpendingKey spendingKey = new SpendingKey(shieldAddressInfo.getSk());
      try {
        ExpandedSpendingKey expandedSpendingKey = spendingKey.expandedSpendingKey();
        builder.setAsk(ByteString.copyFrom(expandedSpendingKey.getAsk()));
        builder.setNsk(ByteString.copyFrom(expandedSpendingKey.getNsk()));
        builder.setOvk(ByteString.copyFrom(expandedSpendingKey.getOvk()));
      } catch (Exception e) {
        System.out.println(e);
      }

      Note.Builder noteBuild = Note.newBuilder();
      noteBuild.setPaymentAddress(shieldAddressInfo.getAddress());
      noteBuild.setValue(noteTx.getNote().getValue());
      noteBuild.setRcm(ByteString.copyFrom(noteTx.getNote().getRcm().toByteArray()));
      noteBuild.setMemo(ByteString.copyFrom(noteTx.getNote().getMemo().toByteArray()));

      // System.out.println("address " + noteInfo.getPaymentAddress());
      // System.out.println("value " + noteInfo.getValue());
      // System.out.println("rcm " + ByteArray.toHexString(noteInfo.getR()));
      // System.out.println("trxId " + noteInfo.getTrxId());
      // System.out.println("index " + noteInfo.getIndex());
      // System.out.println("meno " + new String(noteInfo.getMemo()));

      SpendNote.Builder spendNoteBuilder = SpendNote.newBuilder();
      spendNoteBuilder.setNote(noteBuild.build());
      try {
        spendNoteBuilder.setAlpha(
            ByteString.copyFrom(stest.tron.wallet.common.client.utils.zen.note.Note.generateR()));
      } catch (Exception e) {
        System.out.println(e);
      }

      IncrementalMerkleVoucherInfo merkleVoucherInfo =
          blockingStubFull.getMerkleTreeVoucherInfo(request.build());
      spendNoteBuilder.setVoucher(merkleVoucherInfo.getVouchers(0));
      spendNoteBuilder.setPath(merkleVoucherInfo.getPaths(0));

      builder.addShieldedSpends(spendNoteBuilder.build());

    } else {
      byte[] ovk =
          ByteArray.fromHexString(
              "030c8c2bc59fb3eb8afb047a8ea4b028743d23e7d38c6fa30908358431e2314d");
      builder.setOvk(ByteString.copyFrom(ovk));
    }

    if (shieldOutputList.size() > 0) {
      for (int i = 0; i < shieldOutputList.size(); ++i) {
        builder.addShieldedReceives(
            ReceiveNote.newBuilder().setNote(shieldOutputList.get(i)).build());
      }
    }

    TransactionExtention transactionExtention =
        blockingStubFull.createShieldedTransaction(builder.build());
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
    System.out.println(
        "Receive txid = " + ByteArray.toHexString(transactionExtention.getTxid().toByteArray()));
    Any any = transaction.getRawData().getContract(0).getParameter();

    try {
      ShieldedTransferContract shieldedTransferContract =
          any.unpack(ShieldedTransferContract.class);
      if (shieldedTransferContract.getFromAmount() > 0) {
        transaction = signTransactionForShield(ecKey, transaction);
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      } else {
        System.out.println(
            "trigger txid = "
                + ByteArray.toHexString(
                    Sha256Hash.hash(
                        CommonParameter.getInstance().isECKeyCryptoEngine(),
                        transaction.getRawData().toByteArray())));
      }
    } catch (Exception e) {
      System.out.println(e);
    }
    broadcastTransaction(transaction, blockingStubFull);
    return ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
  }

  /** constructor. */
  public static byte[] decode58Check(String input) {
    byte[] decodeCheck = Base58.decode(input);
    if (decodeCheck.length <= 4) {
      return null;
    }
    byte[] decodeData = new byte[decodeCheck.length - 4];
    System.arraycopy(decodeCheck, 0, decodeData, 0, decodeData.length);
    byte[] hash0 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), decodeData);
    byte[] hash1 = Sha256Hash.hash(CommonParameter.getInstance().isECKeyCryptoEngine(), hash0);
    if (hash1[0] == decodeCheck[decodeData.length]
        && hash1[1] == decodeCheck[decodeData.length + 1]
        && hash1[2] == decodeCheck[decodeData.length + 2]
        && hash1[3] == decodeCheck[decodeData.length + 3]) {
      return decodeData;
    }
    return null;
  }

  /** constructor. */
  public static void freedResource(
      byte[] fromAddress,
      String priKey,
      byte[] toAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    long balance = PublicMethed.queryAccount(fromAddress, blockingStubFull).getBalance();
    sendcoin(toAddress, balance - 500000, fromAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  public static String parametersString(List<Object> parameters) {
    String[] inputArr = new String[parameters.size()];
    int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');
    return input;
  }

  /** constructor. */
  public static String bytes32ToString(byte[] bytes) {
    if (bytes == null) {
      return "null";
    }
    int imax = bytes.length - 1;
    if (imax == -1) {
      return "";
    }

    StringBuilder b = new StringBuilder();
    for (int i = 0; ; i++) {
      b.append(bytes[i]);
      if (i == imax) {
        return b.toString();
      }
    }
  }

  /** constructor. */
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

  /** constructor. */
  public static Return sendcoinForReturn(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention transaction = blockingStubFull.createTransaction2(contract);
    if (transaction == null) {
      return transaction.getResult();
    }
    Return ret = transaction.getResult();
    return ret;
  }

  /** constructor. */
  public static Transaction sendcoinForTransaction(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    // Wallet.setAddressPreFixByte()();
    // String priKey = testKey002;
    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;

    TransferContract.Builder builder = TransferContract.newBuilder();
    ByteString bsTo = ByteString.copyFrom(to);
    ByteString bsOwner = ByteString.copyFrom(owner);
    builder.setToAddress(bsTo);
    builder.setOwnerAddress(bsOwner);
    builder.setAmount(amount);

    TransferContract contract = builder.build();
    TransactionExtention extention = blockingStubFull.createTransaction2(contract);
    Protocol.Transaction transaction = extention.getTransaction();
    return transaction;
  }

  /** constructor. */
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

    transaction = signTransaction(ecKey, transaction);
    broadcastTransaction(transaction, blockingStubFull);

    String txid =
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));

    System.out.println("trigger txid = " + txid);
    return txid;
  }

  /** constructor. */
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

  /** constructor. */
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

    transaction = signTransaction(ecKey, transaction);
    broadcastTransaction(transaction, blockingStubFull);

    String txid =
        ByteArray.toHexString(
            Sha256Hash.hash(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                transaction.getRawData().toByteArray()));

    System.out.println("trigger txid = " + txid);

    return txid;
  }

  /** constructor. */
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

  /** constructor. */
  public static Optional<Protocol.MarketOrderList> getMarketOrderByAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage request = BytesMessage.newBuilder().setValue(addressBs).build();

    Protocol.MarketOrderList marketOrderList;
    marketOrderList = blockingStubFull.getMarketOrderByAccount(request);
    return Optional.ofNullable(marketOrderList);
  }

  /** constructor. */
  public static Optional<Protocol.MarketOrderList> getMarketOrderByAccountSolidity(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ByteString addressBs = ByteString.copyFrom(address);
    BytesMessage request = BytesMessage.newBuilder().setValue(addressBs).build();

    Protocol.MarketOrderList marketOrderList;
    marketOrderList = blockingStubSolidity.getMarketOrderByAccount(request);
    return Optional.ofNullable(marketOrderList);
  }

  /** constructor. */
  public static Optional<Protocol.MarketOrder> getMarketOrderById(
      byte[] order, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString orderBytes = ByteString.copyFrom(order);
    BytesMessage request = BytesMessage.newBuilder().setValue(orderBytes).build();
    Protocol.MarketOrder orderPair = blockingStubFull.getMarketOrderById(request);
    return Optional.ofNullable(orderPair);
  }

  /** constructor. */
  public static Optional<Protocol.MarketOrder> getMarketOrderByIdSolidity(
      byte[] order, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    ByteString orderBytes = ByteString.copyFrom(order);
    BytesMessage request = BytesMessage.newBuilder().setValue(orderBytes).build();
    Protocol.MarketOrder orderPair = blockingStubSolidity.getMarketOrderById(request);
    return Optional.ofNullable(orderPair);
  }

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
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

  /** constructor. */
  public static Optional<Protocol.MarketOrderPairList> getMarketPairList(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    Protocol.MarketOrderPairList marketOrderList =
        blockingStubFull.getMarketPairList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(marketOrderList);
  }

  /** constructor. */
  public static Optional<Protocol.MarketOrderPairList> getMarketPairListSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    Protocol.MarketOrderPairList marketOrderList =
        blockingStubSolidity.getMarketPairList(EmptyMessage.newBuilder().build());
    return Optional.ofNullable(marketOrderList);
  }

  /** constructor. */
  public static String stringToHexString(String s) {
    String str = "";
    for (int i = 0; i < s.length(); i++) {
      int ch = s.charAt(i);
      String s4 = Integer.toHexString(ch);
      str = str + s4;
    }
    return str;
  }

  /** constructor. */
  public static String hexStringToString(String s) {
    if (s == null || s.equals("")) {
      return null;
    }
    s = s.replace(" ", "");
    byte[] baKeyword = new byte[s.length() / 2];
    for (int i = 0; i < baKeyword.length; i++) {
      try {
        baKeyword[i] = (byte) (0xff & Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      s = new String(baKeyword, "gbk");
      new String();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    return s;
  }

  /** constructor. */
  public static String removeAll0sAtTheEndOfHexStr(String s) {
    return s.replaceAll("(00)+$", "");
  }

  /** constructor. */
  public static String replaceCode(String code, String address) {
    if (code.indexOf("__$") == -1) {
      return code;
    } else {
      int index = code.indexOf("_");
      String oldStr = code.substring(index - 1, index + 39);
      Pattern p = Pattern.compile(oldStr);
      Matcher m = p.matcher(code);
      String result = m.replaceAll(address);
      return result;
    }
  }

  /** constructor. */
  public static Map<String, Long> getAllowance2(
      Long startNum, Long endNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    final String blackHole =
        Configuration.getByPath("testng.conf").getString("defaultParameter.blackHoleAddress");
    Long totalCount = 0L;
    Map<String, Integer> witnessBlockCount = new HashMap<>();
    Map<String, Long> witnessBrokerage = new HashMap<>();
    Map<String, Long> witnessVoteCount = new HashMap<>();
    Map<String, Long> witnessAllowance = new HashMap<>();
    List<Protocol.Witness> witnessList =
        PublicMethed.listWitnesses(blockingStubFull).get().getWitnessesList();
    for (Protocol.Witness witness : witnessList) {
      witnessVoteCount.put(
          ByteArray.toHexString(witness.getAddress().toByteArray()), witness.getVoteCount());
      GrpcAPI.BytesMessage bytesMessage =
          GrpcAPI.BytesMessage.newBuilder().setValue(witness.getAddress()).build();
      Long brokerager = blockingStubFull.getBrokerageInfo(bytesMessage).getNum();
      witnessBrokerage.put(ByteArray.toHexString(witness.getAddress().toByteArray()), brokerager);
      totalCount += witness.getVoteCount();
    }
    Optional<Protocol.TransactionInfo> infoById = null;
    for (Long k = startNum; k < endNum; k++) {
      String witnessAdd =
          ByteArray.toHexString(
              PublicMethed.getBlock(k, blockingStubFull)
                  .getBlockHeader()
                  .getRawData()
                  .getWitnessAddress()
                  .toByteArray());
      witnessBlockCount.put(witnessAdd, witnessBlockCount.getOrDefault(witnessAdd, 0) + 1);
      List<Transaction> transList =
          PublicMethed.getBlock(k, blockingStubFull).getTransactionsList();
      for (Transaction tem : transList) {
        String txid =
            ByteArray.toHexString(
                Sha256Hash.hash(
                    CommonParameter.getInstance().isECKeyCryptoEngine(),
                    tem.getRawData().toByteArray()));
        logger.info("----ss txid:" + txid);
        infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
        Long packingFee = infoById.get().getPackingFee();

        witnessAllowance.put(
            witnessAdd, witnessAllowance.getOrDefault(witnessAdd, 0L) + packingFee);
      }
    }

    logger.info("========totalCount:" + totalCount);
    List<Protocol.ChainParameters.ChainParameter> chainParaList =
        blockingStubFull
            .getChainParameters(EmptyMessage.newBuilder().build())
            .getChainParameterList();
    Long witness127PayPerBlock = 0L;
    Long witnessPayPerBlock = 0L;
    for (Protocol.ChainParameters.ChainParameter para : chainParaList) {
      if ("getWitness127PayPerBlock".equals(para.getKey())) {
        witness127PayPerBlock = para.getValue();
      }
      if ("getWitnessPayPerBlock".equals(para.getKey())) {
        witnessPayPerBlock = para.getValue();
      }
    }
    logger.info(
        "witness127PayPerBlock:"
            + witness127PayPerBlock
            + "\n witnessPayPerBlock:"
            + witnessPayPerBlock);

    for (Map.Entry<String, Long> entry : witnessBrokerage.entrySet()) {
      logger.info("-----witnessBrokerage   " + entry.getKey() + " : " + entry.getValue());
    }
    for (Map.Entry<String, Long> entry : witnessVoteCount.entrySet()) {
      logger.info("-----witnessVoteCount   " + entry.getKey() + " : " + entry.getValue());
    }
    for (Map.Entry<String, Integer> entry : witnessBlockCount.entrySet()) {
      logger.info("-----witnessBlockCount   " + entry.getKey() + " : " + entry.getValue());
    }

    for (Map.Entry<String, Long> entry : witnessVoteCount.entrySet()) {
      String witnessAdd = entry.getKey();
      logger.info(
          "----witnessAdd:"
              + witnessAdd
              + " block count:"
              + witnessBlockCount.get(witnessAdd)
              + "    all: "
              + witnessAllowance.getOrDefault(witnessAdd, 0L));
      Long pay =
          (witnessBlockCount.get(witnessAdd) * witnessPayPerBlock
                  + (endNum - startNum) * witness127PayPerBlock * entry.getValue() / totalCount
                  + witnessAllowance.getOrDefault(witnessAdd, 0L))
              * witnessBrokerage.get(witnessAdd)
              / 100;

      witnessAllowance.put(witnessAdd, pay);
      logger.info("******  " + witnessAdd + " : " + pay);
    }
    return witnessAllowance;
  }

  public static String getContractStringMsg(byte[] contractMsgArray) {
    int resultLenth = ByteArray.toInt(ByteArray.subArray(contractMsgArray, 32, 64));
    return ByteArray.toStr(ByteArray.subArray(contractMsgArray, 64, 64 + resultLenth));
  }

  /** constructor. */
  public static long getBrokerage(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    GrpcAPI.BytesMessage bytesMessage =
        GrpcAPI.BytesMessage.newBuilder().setValue(addressBs).build();
    Long brokerager = blockingStubFull.getBrokerageInfo(bytesMessage).getNum();
    return brokerager;
  }

  /** constructor. */
  public boolean updateBrokerage(
      byte[] owner, int brokerage, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {

    ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    ECKey ecKey = temKey;

    UpdateBrokerageContract.Builder updateBrokerageContract = UpdateBrokerageContract.newBuilder();
    updateBrokerageContract.setOwnerAddress(ByteString.copyFrom(owner)).setBrokerage(brokerage);
    TransactionExtention transactionExtention =
        blockingStubFull.updateBrokerage(updateBrokerageContract.build());
    Protocol.Transaction transaction = transactionExtention.getTransaction();
    if (transactionExtention == null || !transactionExtention.getResult().getResult()) {
      if (transactionExtention != null) {
        System.out.println("Code = " + transactionExtention.getResult().getCode());
        System.out.println(
            "Message = " + transactionExtention.getResult().getMessage().toStringUtf8());
      }
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);

    return response.getResult();
  }

  /** constructor. */
  public static Long getAccountBalance(
      Protocol.Block block, byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    final Long blockNum = block.getBlockHeader().getRawData().getNumber();
    BlockId blockId =
        new BlockId(
            Sha256Hash.of(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                block.getBlockHeader().getRawData().toByteArray()),
            block.getBlockHeader().getRawData().getNumber());

    BalanceContract.AccountIdentifier accountIdentifier =
        BalanceContract.AccountIdentifier.newBuilder()
            .setAddress(ByteString.copyFrom(address))
            .build();
    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setHash(blockId.getByteString())
            .setNumber(blockNum)
            .build();

    BalanceContract.AccountBalanceRequest accountBalanceRequest =
        BalanceContract.AccountBalanceRequest.newBuilder()
            .setAccountIdentifier(accountIdentifier)
            .setBlockIdentifier(blockIdentifier)
            .build();
    return blockingStubFull.getAccountBalance(accountBalanceRequest).getBalance();
  }

  /** constructor. */
  public static BalanceContract.BlockBalanceTrace getBlockBalance(
      Protocol.Block block, WalletGrpc.WalletBlockingStub blockingStubFull) {
    final Long blockNum = block.getBlockHeader().getRawData().getNumber();

    BlockId blockId =
        new BlockId(
            Sha256Hash.of(
                CommonParameter.getInstance().isECKeyCryptoEngine(),
                block.getBlockHeader().getRawData().toByteArray()),
            block.getBlockHeader().getRawData().getNumber());
    BalanceContract.BlockBalanceTrace.BlockIdentifier blockIdentifier =
        BalanceContract.BlockBalanceTrace.BlockIdentifier.newBuilder()
            .setHash(blockId.getByteString())
            .setNumber(blockNum)
            .build();

    return blockingStubFull.getBlockBalanceTrace(blockIdentifier);
  }

  /** 61 constructor. */
  public static Optional<Transaction> getTransactionFromPending(
      String txId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString bsTxid = ByteString.copyFrom(ByteArray.fromHexString(txId));
    BytesMessage request = BytesMessage.newBuilder().setValue(bsTxid).build();
    Transaction transaction;
    transaction = blockingStubFull.getTransactionFromPending(request);
    return Optional.ofNullable(transaction);
  }


  /** constructor. */
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
    freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }
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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return freezeV2Txid;
  }


  public static Long getFrozenV2Amount(byte[] address, int resourceCode,WalletGrpc.WalletBlockingStub blockingStubFull) {
    List<FreezeV2> list = queryAccount(address,blockingStubFull).getFrozenV2List();
    for(int i = 0; i < list.size();i++) {
      if(list.get(i).getType().getNumber() == resourceCode) {
        return list.get(i).getAmount();
      }
    }
    return 0L;

  }

  /** constructor. */
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
    waitProduceNextBlock(blockingStubFull);

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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }


  /** constructor. */
  public static Boolean delegateResourceV2(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return delegateResourceV2Lock(addressByte,delegateAmount,resourceCode,false, null, receiverAddress,priKey,
        blockingStubFull);
  }
  /** constructor. */
  public static String delegateResourceV2AndGetTxId(byte[] addressByte,
                                           long delegateAmount,
                                           int resourceCode,
                                           byte[] receiverAddress,
                                           String priKey,
                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
    return delegateResourceV2LockAndGetTxId(addressByte,delegateAmount,resourceCode,false, null, receiverAddress,priKey,
            blockingStubFull);
  }

  /** constructor. */
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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return transactionExtention;
  }



  /** constructor. */
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
    freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return freezeV2Txid;
  }



  /** constructor. */
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
    freezeV2Txid = ByteArray.toHexString(
        Sha256Hash.hash(
            CommonParameter.getInstance().isECKeyCryptoEngine(),
            transaction.getRawData().toByteArray()));
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return response.getResult();
  }

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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return freezeV2Txid;
  }
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
    GrpcAPI.Return response = broadcastTransaction(transaction, blockingStubFull);
    return transactionExtention;
  }


  /** constructor. */
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

  /** constructor. */
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
//      HttpResponse response = HttpMethed
//              .triggerConstantContractWithData(
//                      httpnode,
//                      ownerAddress, null, null, null, code, value, tokenValue, Long.parseLong(tokenId));
//      JSONObject jsonObject = HttpMethed.parseResponseContent(response);
//      HttpMethed.printJsonContent(jsonObject);
//      Long constantEnergy = jsonObject.getLong("energy_used");
//      logger.info("constantEnergy:" + constantEnergy);
//
//      TransactionExtention trx = PublicMethed
//              .triggerConstantContractDeployContract(
//                      code, ownerAddress, value, tokenId, tokenValue, blockingStubFull);
//      Long grpcConstantEnergy = trx.getEnergyUsed();
//      logger.info("grpcConstantEnergy:" + grpcConstantEnergy);
//      Assert.assertEquals(grpcConstantEnergy.longValue(), constantEnergy.longValue());
//      logger.info("triggerConstantContract ------------ end    -----------");
//
//      Long energyFee = PublicMethed.getChainParametersValue(
//              ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);
//      logger.info("energyFee:" + energyFee);


//      logger.info("EstimateEnergy -------- start ------");
//      response = HttpMethed.getEstimateEnergyDeployContract(httpnode,
//              ownerAddress, null, null, null, code, value, tokenValue, Long.parseLong(tokenId), true);
//      jsonObject = HttpMethed.parseResponseContent(response);
//      HttpMethed.printJsonContent(jsonObject);
//      Long estimateEnergy = jsonObject.getLong("energy_required");
//      logger.info("estimateEnergy:" + estimateEnergy);


//      Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyMessage =
//              PublicMethed.estimateEnergyDeployContract(blockingStubFull2,
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

  /** constructor. */
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

  public static Long getExchangeIdByCreatorAddress(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    List<Exchange> exchangeList =  PublicMethed.getExchangeList(blockingStubFull).get().getExchangesList();
    for (int i = 0; i < exchangeList.size(); i++){
      Exchange exchange = exchangeList.get(i);
      if (exchange.getCreatorAddress().equals(ByteString.copyFrom(address))) {
        return exchange.getExchangeId();
      }
    }
    return 0L;
  }

  public static String gRPCurlRequest(String data, String requestUrl, String node) {
    String cmd = gRPCurl + " " + "-plaintext";
    if (data!=null) {
      cmd = cmd + " -d " + data;
    }
    cmd = cmd + " " + node + " " + requestUrl;
    logger.info("cmd is : " + cmd);
    try {
      return PublicMethed.exec(cmd);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (RuntimeException e) {
      throw new RuntimeException(e);
    } catch (Exception e) {
      logger.error(e.toString());
    }
    return null;
  }





}
