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

/**
 * Legacy facade class — delegates to specialized Helper classes.
 *
 * <p><b>For new test code, use the Helper classes directly:</b>
 * <ul>
 *   <li>{@link AccountHelper} — account queries, creation, permissions, sendcoin</li>
 *   <li>{@link ContractHelper} — contract deploy, trigger, ABI operations</li>
 *   <li>{@link ResourceHelper} — freeze, delegate, energy/bandwidth</li>
 *   <li>{@link AssetHelper} — TRC-10/TRC-20 token operations</li>
 *   <li>{@link GovernanceHelper} — proposals, voting, witness management</li>
 *   <li>{@link TransactionHelper} — transaction signing, broadcasting</li>
 *   <li>{@link BlockHelper} — block queries</li>
 *   <li>{@link ShieldHelper} — privacy/shielded operations</li>
 *   <li>{@link CommonHelper} — address/key utilities</li>
 * </ul>
 *
 * <p>This class is kept for backward compatibility with existing 600+ test classes.
 * All delegating methods will be gradually marked {@code @Deprecated}.
 */
@Slf4j
public class PublicMethod {

  public static AtomicInteger randomTimeOffset = new AtomicInteger(1);

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
      //witnessNum = PublicMethod.listWitnesses(blockingStubFull).get().getWitnessesList().size();
    //}
    witnessNum = PublicMethod.listWitnesses(blockingStubFull).get().getWitnessesList().size();
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
    return AssetHelper.createAssetIssueGetTxid(address, name, abbreviation, totalSupply, trxNum,
        icoNum, startTime, endTime, voteScore, description, url, freeAssetNetLimit,
        publicFreeAssetNetLimit, fronzenAmount, frozenDay, priKey, blockingStubFull);
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
    return AssetHelper.createAssetIssue(address, name, totalSupply, trxNum, icoNum, startTime,
        endTime, voteScore, description, url, freeAssetNetLimit, publicFreeAssetNetLimit,
        fronzenAmount, frozenDay, priKey, blockingStubFull);
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
    return AssetHelper.createAssetIssue(address, name, abbreviation, totalSupply, trxNum,
        icoNum, startTime, endTime, voteScore, description, url, freeAssetNetLimit,
        publicFreeAssetNetLimit, fronzenAmount, frozenDay, priKey, blockingStubFull);
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
    return AssetHelper.createAssetIssue(address, name, totalSupply, trxNum, icoNum, precision,
        startTime, endTime, voteScore, description, url, freeAssetNetLimit,
        publicFreeAssetNetLimit, fronzenAmount, frozenDay, priKey, blockingStubFull);
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
    return AssetHelper.createAssetIssue2(address, name, totalSupply, trxNum, icoNum, startTime,
        endTime, voteScore, description, url, freeAssetNetLimit, publicFreeAssetNetLimit,
        fronzenAmount, frozenDay, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Account queryAccountByAddress(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.queryAccountByAddress(address, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Account queryAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.queryAccount(address, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Protocol.Account queryAccount(
      String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.queryAccount(priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Account queryAccount(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return AccountHelper.queryAccount(address, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Account getAccountById(
      String accountId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.getAccountById(accountId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Account getAccountByIdFromSolidity(
      String accountId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return AccountHelper.getAccountByIdFromSolidity(accountId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String loadPubKey() {
    return CommonHelper.loadPubKey();
  }

  /** constructor. */
  @Deprecated
  public static byte[] getAddress(ECKey ecKey) {
    return CommonHelper.getAddress(ecKey);
  }

  /** constructor. */
  public static Protocol.Account grpcQueryAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Protocol.Account request = Protocol.Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /** constructor. */
  @Deprecated
  public static Protocol.Block getBlock(
      long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return BlockHelper.getBlock(blockNum, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static BlockExtention getBlock2(long blockNum, WalletBlockingStub blockingStubFull) {
    return BlockHelper.getBlock2(blockNum, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Protocol.Transaction signTransaction(
      ECKey ecKey, Protocol.Transaction transaction) {
    return TransactionHelper.signTransaction(ecKey, transaction);
  }

  /** constructor. */
  @Deprecated
  public static Protocol.Transaction signTransactionForShield(
      ECKey ecKey, Protocol.Transaction transaction) {
    return TransactionHelper.signTransactionForShield(ecKey, transaction);
  }

  /** constructor. */
  @Deprecated
  public static boolean participateAssetIssue(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] from,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.participateAssetIssue(to, assertName, amount, from, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return participateAssetIssue2(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] from,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.participateAssetIssue2(to, assertName, amount, from, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean freezeBalance(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalance(addRess, freezeBalance, freezeDuration, priKey, blockingStubFull);
  }

  @Deprecated
  public static Boolean freezeBalanceV1ForReceiver(byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalanceV1ForReceiver(addRess, freezeBalance, freezeDuration, resourceCode, receiverAddress, priKey, blockingStubFull);
  }


  @Deprecated
  public static Boolean freezeBalanceV1(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalanceV1(addRess, freezeBalance, freezeDuration, resourceCode, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return freezeBalance2(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalance2(addRess, freezeBalance, freezeDuration, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean unFreezeBalance(
      byte[] address,
      String priKey,
      int resourceCode,
      byte[] receiverAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.unFreezeBalance(address, priKey, resourceCode, receiverAddress, blockingStubFull);
  }


  /** constructor. */
  @Deprecated
  public static Boolean unFreezeBalanceV1(
      byte[] address,
      String priKey,
      int resourceCode,
      byte[] receiverAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.unFreezeBalanceV1(address, priKey, resourceCode, receiverAddress, blockingStubFull);
  }


  /** constructor. */
  @Deprecated
  public static Boolean unFreezeBalanceV2(
      byte[] address,
      String priKey,
      long unFreezeBalanceAmount,
      int resourceCode,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.unFreezeBalanceV2(address, priKey, unFreezeBalanceAmount, resourceCode, blockingStubFull);
  }
  @Deprecated
  public static String unFreezeBalanceV2AndGetTxId(
          byte[] address,
          String priKey,
          long unFreezeBalanceAmount,
          int resourceCode,
          WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.unFreezeBalanceV2AndGetTxId(address, priKey, unFreezeBalanceAmount, resourceCode, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean cancelAllUnFreezeBalanceV2(
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.cancelAllUnFreezeBalanceV2(address, priKey, blockingStubFull);
  }
  /** constructor. */
  @Deprecated
  public static String cancelAllUnFreezeBalanceV2AndGetTxid(
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.cancelAllUnFreezeBalanceV2AndGetTxid(address, priKey, blockingStubFull);
  }


  /** constructor. */
  @Deprecated
  public static Boolean withdrawExpireUnfreeze(
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.withdrawExpireUnfreeze(address, priKey, blockingStubFull);
  }

  @Deprecated
  public static String withdrawExpireUnfreezeAndGetTxId(
          byte[] address,
          String priKey,
          WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.withdrawExpireUnfreezeAndGetTxId(address, priKey, blockingStubFull);
  }


  @Deprecated
  public static Boolean freezeV2ProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeV2ProposalIsOpen(blockingStubFull);
  }


  @Deprecated
  public static Boolean tronPowerProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.tronPowerProposalIsOpen(blockingStubFull);
  }

  @Deprecated
  public static Boolean getAllowDynamicEnergyProposalIsOpen(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getAllowDynamicEnergyProposalIsOpen(blockingStubFull);
  }



  @Deprecated
  public static Long getProposalMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getProposalMemoFee(blockingStubFull);
  }

  @Deprecated
  public static String getMemoFee(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getMemoFee(blockingStubFull);
  }

  @Deprecated
  public static String getEnergyPrice(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getEnergyPrice(blockingStubFull);
  }

  @Deprecated
  public static String getEnergyPriceSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getEnergyPriceSolidity(blockingStubFull);
  }


  @Deprecated
  public static String getBandwidthPrices(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getBandwidthPrices(blockingStubFull);
  }

  @Deprecated
  public static String getBandwidthPricesSolidity(WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getBandwidthPricesSolidity(blockingStubFull);
  }



  /** constructor. */
  @Deprecated
  public static Boolean sendcoin(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoin(to, amount, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean sendcoinWithScript(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      int scriptLength,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinWithScript(to, amount, owner, priKey, scriptLength, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String sendcoinGetTransactionHex(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinGetTransactionHex(to, amount, owner, priKey, blockingStubFull);
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
  @Deprecated
  public static Boolean sendcoinDelayed(
      byte[] to,
      long amount,
      long delaySeconds,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinDelayed(to, amount, delaySeconds, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String sendcoinDelayedGetTxid(
      byte[] to,
      long amount,
      long delaySeconds,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinDelayedGetTxid(to, amount, delaySeconds, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return sendcoin2(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoin2(to, amount, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String sendcoinGetTransactionId(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinGetTransactionId(to, amount, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String sendcoinWithMemoGetTransactionId(
      byte[] to,
      long amount,
      String memo,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinWithMemoGetTransactionId(to, amount, memo, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String sendcoinGetTransactionIdForConstructData(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinGetTransactionIdForConstructData(to, amount, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Transaction> getTransactionById(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return TransactionHelper.getTransactionById(txId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Transaction> getTransactionById(
      String txId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return TransactionHelper.getTransactionById(txId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Long getAssetBalanceByAssetId(
      ByteString assetId, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getAssetBalanceByAssetId(assetId, priKey, blockingStubFull);
  }


  @Deprecated
  public static Long getAssetBalanceByAssetId(
      ByteString assetId, byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getAssetBalanceByAssetId(assetId, address, blockingStubFull);
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
  @Deprecated
  public static Optional<Transaction> getTransactionByIdSolidity(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return TransactionHelper.getTransactionByIdSolidity(txId, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static String printTransaction(Transaction transaction) {
    return TransactionHelper.printTransaction(transaction);
  }

  /** constructor. */
  @Deprecated
  public static long printTransactionRow(Transaction.raw raw) {
    return TransactionHelper.printTransactionRow(raw);
  }

  /** constructor. */
  @Deprecated
  public static boolean updateAsset(
      byte[] address,
      byte[] description,
      byte[] url,
      long newLimit,
      long newPublicLimit,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.updateAsset(address, description, url, newLimit, newPublicLimit, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return updateAsset2(
      byte[] address,
      byte[] description,
      byte[] url,
      long newLimit,
      long newPublicLimit,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.updateAsset2(address, description, url, newLimit, newPublicLimit, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean transferAsset(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.transferAsset(to, assertName, amount, address, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean updateAccount(
      byte[] addressBytes,
      byte[] accountNameBytes,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.updateAccount(addressBytes, accountNameBytes, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean waitSolidityNodeSynFullNodeData(
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return BlockHelper.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static boolean waitProduceNextBlock(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return BlockHelper.waitProduceNextBlock(blockingStubFull);
  }



  /**
   * if tx is found, return it
   * if query timeout,assert failed then return false
   * @param blockingStubFull
   * @param txId
   * @param timeout second
   * @return
   */
  public static void waitUntilTransactionInfoFound(WalletGrpc.WalletBlockingStub blockingStubFull, String txId, int timeout) {
    BlockHelper.waitUntilTransactionInfoFound(blockingStubFull, txId, timeout);
  }

  /** constructor. */
  @Deprecated
  public static AccountNetMessage getAccountNet(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getAccountNet(address, blockingStubFull);
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
  @Deprecated
  public static byte[] getFinalAddress(String priKey) {
    return CommonHelper.getFinalAddress(priKey);
  }

  /** constructor. */
  @Deprecated
  public static String createAccountGetTxid(
      byte[] ownerAddress,
      byte[] newAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.createAccountGetTxid(ownerAddress, newAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean createAccount(
      byte[] ownerAddress,
      byte[] newAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.createAccount(ownerAddress, newAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return createAccount2(
      byte[] ownerAddress,
      byte[] newAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.createAccount2(ownerAddress, newAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean voteWitness(
      byte[] ownerAddress,
      String priKey,
      HashMap<byte[], Long> witnessMap,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.voteWitness(ownerAddress, priKey, witnessMap, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean createProposal(
      byte[] ownerAddress,
      String priKey,
      HashMap<Long, Long> parametersMap,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.createProposal(ownerAddress, priKey, parametersMap, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean approveProposal(
      byte[] ownerAddress,
      String priKey,
      long id,
      boolean isAddApproval,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.approveProposal(ownerAddress, priKey, id, isAddApproval, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean deleteProposal(
      byte[] ownerAddress, String priKey, long id, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.deleteProposal(ownerAddress, priKey, id, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean printAddress(String key) {
    return CommonHelper.printAddress(key);
  }

  /** constructor. */
  @Deprecated
  public static String getAddressString(String key) {
    return CommonHelper.getAddressString(key);
  }

  /** constructor. */
  @Deprecated
  public static ArrayList<String> getAddressInfo(String key) {
    return CommonHelper.getAddressInfo(key);
  }

  /** constructor. */
  @Deprecated
  public static boolean setAccountId(
      byte[] accountIdBytes,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.setAccountId(accountIdBytes, ownerAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean freezeBalanceGetTronPower(
      byte[] address,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      ByteString receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalanceGetTronPower(address, freezeBalance, freezeDuration, resourceCode, receiverAddress, priKey, blockingStubFull);
  }


  @Deprecated
  public static Long getChainParametersValue(String proposalName,WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.getChainParametersValue(proposalName, blockingStubFull);
  }

  public static Boolean allowTvmSelfdestructRestrictionIsActive(WalletGrpc.WalletBlockingStub blockingStubFull) {
    return PublicMethod.getChainParametersValue(ProposalEnum.GetAllowTvmSelfdestructRestriction.getProposalName(),
            blockingStubFull) == 1;
  }

  // TODO: Uncomment when proto supports getPaginatedNowWitnessList (v4.8.1+ API)
  // public static GrpcAPI.WitnessList getPaginatedNowWitnessList(Long offset, Long limit, WalletGrpc.WalletBlockingStub blockingStubFull){
  //   return blockingStubFull.getPaginatedNowWitnessList(GrpcAPI.PaginatedMessage.newBuilder().setLimit(limit).setOffset(offset).build());
  // }
  //
  // public static GrpcAPI.WitnessList getPaginatedNowWitnessListSolidity(Long offset, Long limit, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity){
  //   return blockingStubSolidity.getPaginatedNowWitnessList(GrpcAPI.PaginatedMessage.newBuilder().setLimit(limit).setOffset(offset).build());
  // }

  /** constructor. */
  @Deprecated
  public static Boolean freezeBalanceGetEnergy(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalanceGetEnergy(addRess, freezeBalance, freezeDuration, resourceCode, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static AccountResourceMessage getAccountResource(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getAccountResource(address, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean buyStorage(
      long quantity,
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.buyStorage(quantity, address, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean sellStorage(
      long quantity,
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.sellStorage(quantity, address, priKey, blockingStubFull);
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
    return ContractHelper.deployContractFallbackReceive(contractName, abiString, code, data,
        feeLimit, value, consumeUserResourcePercent, originEnergyLimit, tokenId, tokenValue,
        libraryAddress, priKey, ownerAddress, blockingStubFull);
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
    return ContractHelper.deployContract(contractName, abiString, code, data,
        feeLimit, value, consumeUserResourcePercent, originEnergyLimit, tokenId, tokenValue,
        libraryAddress, priKey, ownerAddress, blockingStubFull);
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
    return ContractHelper.deployContractWithoutBroadcast(contractName, abiString, code, data,
        feeLimit, value, consumeUserResourcePercent, originEnergyLimit, tokenId, tokenValue,
        libraryAddress, priKey, ownerAddress, blockingStubFull);
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
  @Deprecated
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
    return ContractHelper.deployContractFallback(contractName, abiString, code, data,
        feeLimit, value, consumeUserResourcePercent, libraryAddress, priKey,
        ownerAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.deployContractForLibrary(contractName, abiString, code, data,
        feeLimit, value, consumeUserResourcePercent, libraryAddress, priKey,
        ownerAddress, compilerVersion, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.deployContractAndGetTransactionInfoById(contractName, abiString,
        code, data, feeLimit, value, consumeUserResourcePercent, libraryAddress, priKey,
        ownerAddress, blockingStubFull);
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
    return ContractHelper.deployContractAndGetTransactionInfoById(contractName, abiString, code,
        data, feeLimit, value, consumeUserResourcePercent, originEnergyLimit, tokenId, tokenValue,
        libraryAddress, priKey, ownerAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static SmartContract.ABI jsonStr2Abi(String jsonStr) {
    return ContractHelper.jsonStr2Abi(jsonStr);
  }

  /** constructor. */
  @Deprecated
  public static SmartContract.ABI jsonStr2Abi2(String jsonStr) {
    return ContractHelper.jsonStr2Abi2(jsonStr);
  }

  /** constructor. */
  @Deprecated
  public static SmartContract.ABI.Entry.EntryType getEntryType(String type) {
    return ContractHelper.getEntryType(type);
  }

  /** constructor. */
  @Deprecated
  public static SmartContract.ABI.Entry.EntryType getEntryType2(String type) {
    return ContractHelper.getEntryType2(type);
  }

  /** constructor. */
  @Deprecated
  public static SmartContract.ABI.Entry.StateMutabilityType getStateMutability(
      String stateMutability) {
    return ContractHelper.getStateMutability(stateMutability);
  }

  /** constructor. */
  @Deprecated
  public static byte[] generateContractAddress(Transaction trx, byte[] owneraddress) {
    return ContractHelper.generateContractAddress(trx, owneraddress);
  }

  /** constructor. */
  @Deprecated
  public static SmartContract getContract(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ContractHelper.getContract(address, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static SmartContractDataWrapper getContractInfo(
      byte[] address, WalletBlockingStub blockingStubFull) {
    return ContractHelper.getContractInfo(address, blockingStubFull);
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
    PublicMethod.code = code;

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

    PublicMethod.code = code;

    return Hex.decode(code);
  }

  /** constructor. */
  @Deprecated
  public static boolean updateSetting(
      byte[] contractAddress,
      long consumeUserResourcePercent,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ContractHelper.updateSetting(contractAddress, consumeUserResourcePercent,
        priKey, ownerAddress, blockingStubFull);
  }

  /** 61 constructor. */
  @Deprecated
  public static Optional<TransactionInfo> getTransactionInfoById(
      String txId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return TransactionHelper.getTransactionInfoById(txId, blockingStubFull);
  }

  /** 61 constructor. */
  @Deprecated
  public static Optional<TransactionInfo> getTransactionInfoByIdFromSolidity(
      String txId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return TransactionHelper.getTransactionInfoByIdFromSolidity(txId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNum(
      long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return BlockHelper.getTransactionInfoByBlockNum(blockNum, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<TransactionInfoList> getTransactionInfoByBlockNumFromSolidity(
      long blockNum, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return BlockHelper.getTransactionInfoByBlockNumFromSolidity(blockNum, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerContract(contractAddress, method, argsStr, isHex,
        callValue, feeLimit, ownerAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerContract(contractAddress, method, argsStr, isHex,
        callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerContractBoth(contractAddress, method, argsStr, isHex,
        callValue, feeLimit, ownerAddress, priKey, blockingStubFull, blockingStubFull1);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerContractBoth(contractAddress, method, argsStr, isHex,
        callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey, blockingStubFull, blockingStubFull1);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerParamListContract(contractAddress, method, params, isHex,
        callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean exchangeCreate(
      byte[] firstTokenId,
      long firstTokenBalance,
      byte[] secondTokenId,
      long secondTokenBalance,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.exchangeCreate(firstTokenId, firstTokenBalance, secondTokenId,
        secondTokenBalance, ownerAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean injectExchange(
      long exchangeId,
      byte[] tokenId,
      long quant,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.injectExchange(exchangeId, tokenId, quant, ownerAddress, priKey, blockingStubFull);
  }

  @Deprecated
  public static Optional<ExchangeList> getExchangeList(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getExchangeList(blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<ExchangeList> getExchangeList(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return AssetHelper.getExchangeList(blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Exchange> getExchange(
      String id, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return AssetHelper.getExchange(id, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Exchange> getExchange(
      String id, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getExchange(id, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean exchangeWithdraw(
      long exchangeId,
      byte[] tokenId,
      long quant,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.exchangeWithdraw(exchangeId, tokenId, quant, ownerAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean exchangeTransaction(
      long exchangeId,
      byte[] tokenId,
      long quant,
      long expected,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.exchangeTransaction(exchangeId, tokenId, quant, expected, ownerAddress,
        priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.deployContractWithConstantParame(contractName, abiString, code,
        constructorStr, argsStr, data, feeLimit, value, consumeUserResourcePercent,
        libraryAddress, priKey, ownerAddress, blockingStubFull);
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
    return ContractHelper.deployContractWithConstantParame(contractName, abiString, code,
        constructorStr, argsStr, data, feeLimit, value, consumeUserResourcePercent,
        originEnergyLimit, tokenId, tokenValue, libraryAddress, priKey, ownerAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean freezeBalanceForReceiver(
      byte[] addRess,
      long freezeBalance,
      long freezeDuration,
      int resourceCode,
      ByteString receiverAddressBytes,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalanceForReceiver(addRess, freezeBalance, freezeDuration, resourceCode, receiverAddressBytes, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceList> getDelegatedResource(
      byte[] fromAddress, byte[] toAddress, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResource(fromAddress, toAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceList> getDelegatedResourceFromSolidity(
      byte[] fromAddress,
      byte[] toAddress,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResourceFromSolidity(fromAddress, toAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndex(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResourceAccountIndex(address, blockingStubFull);
  }


  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceAccountIndex>
      getDelegatedResourceAccountIndexFromSolidity(
          byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResourceAccountIndexFromSolidity(address, blockingStubFull);
  }
  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceList> getDelegatedResourceV2(
      byte[] fromAddress, byte[] toAddress, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResourceV2(fromAddress, toAddress, blockingStubFull);
  }
  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceList> getDelegatedResourceV2Solidity(
      byte[] fromAddress, byte[] toAddress, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResourceV2Solidity(fromAddress, toAddress, blockingStubFull);
  }
  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndexV2(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResourceAccountIndexV2(address, blockingStubFull);
  }
  /** constructor. */
  @Deprecated
  public static Optional<DelegatedResourceAccountIndex> getDelegatedResourceAccountIndexV2Solidity(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getDelegatedResourceAccountIndexV2Solidity(address, blockingStubFull);
  }

  @Deprecated
  public static Optional<CanDelegatedMaxSizeResponseMessage> getCanDelegatedMaxSize(byte[] ownerAddress, int type,
      WalletGrpc.WalletBlockingStub blockingStub) {
    return ResourceHelper.getCanDelegatedMaxSize(ownerAddress, type, blockingStub);
  }

  @Deprecated
  public static Optional<CanDelegatedMaxSizeResponseMessage> getCanDelegatedMaxSizeSolidity(byte[] ownerAddress, int type,
                                                                                            WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getCanDelegatedMaxSizeSolidity(ownerAddress, type, blockingStubFull);
  }


  @Deprecated
  public static Optional<CanWithdrawUnfreezeAmountResponseMessage> getCanWithdrawUnfreezeAmount(
      byte[] ownerAddress, long timestamp,WalletGrpc.WalletBlockingStub blockingStub) {
    return ResourceHelper.getCanWithdrawUnfreezeAmount(ownerAddress, timestamp, blockingStub);
  }
  @Deprecated
  public static Optional<CanWithdrawUnfreezeAmountResponseMessage> getCanWithdrawUnfreezeAmountSolidity(
      byte[] ownerAddress, long timestamp,WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getCanWithdrawUnfreezeAmountSolidity(ownerAddress, timestamp, blockingStubFull);
  }


  @Deprecated
  public static Optional<GetAvailableUnfreezeCountResponseMessage> getAvailableUnfreezeCount(
      byte[] ownerAddress,WalletGrpc.WalletBlockingStub blockingStub) {
    return ResourceHelper.getAvailableUnfreezeCount(ownerAddress, blockingStub);
  }
  @Deprecated
  public static Optional<GetAvailableUnfreezeCountResponseMessage> getAvailableUnfreezeCountSolidity(
      byte[] ownerAddress,WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return ResourceHelper.getAvailableUnfreezeCountSolidity(ownerAddress, blockingStubFull);
  }


  /** constructor. */
  @Deprecated
  public static AssetIssueContract getAssetIssueByName(
      String assetName, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueByName(assetName, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static AssetIssueContract getAssetIssueByNameFromSolidity(
      String assetName, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueByNameFromSolidity(assetName, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<AssetIssueList> getAssetIssueListByName(
      String assetName, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueListByName(assetName, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<AssetIssueList> getAssetIssueListByNameFromSolidity(
      String assetName, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueListByNameFromSolidity(assetName, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<GrpcAPI.AssetIssueList> listAssetIssueFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return AssetHelper.listAssetIssueFromSolidity(blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<GrpcAPI.AssetIssueList> listAssetIssuepaginatedFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull, Long offset, Long limit) {
    return AssetHelper.listAssetIssuepaginatedFromSolidity(blockingStubFull, offset, limit);
  }

  /** constructor. */
  @Deprecated
  public static Optional<GrpcAPI.WitnessList> listWitnesses(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.listWitnesses(blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<GrpcAPI.WitnessList> listWitnessesFromSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return GovernanceHelper.listWitnessesFromSolidity(blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static AssetIssueContract getAssetIssueById(
      String assetId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueById(assetId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static AssetIssueContract getAssetIssueByIdFromSolidity(
      String assetId, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueByIdFromSolidity(assetId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<AssetIssueList> getAssetIssueByAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueByAccount(address, blockingStubFull);
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
  @Deprecated
  public static boolean accountPermissionUpdate(
      String permissionJson,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      String[] priKeys) {
    return AccountHelper.accountPermissionUpdate(permissionJson, owner, priKey, blockingStubFull, priKeys);
  }

  /** constructor. */
  @Deprecated
  public static long getFreezeBalanceCount(
      byte[] accountAddress,
      String ecKey,
      Long targetEnergy,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getFreezeBalanceCount(accountAddress, ecKey, targetEnergy, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Long getAssetIssueValue(
      byte[] accountAddress,
      ByteString assetIssueId,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getAssetIssueValue(accountAddress, assetIssueId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static List<String> getStrings(byte[] data) {
    return CommonHelper.getStrings(data);
  }

  /** constructor. */
  @Deprecated
  public static String byte2HexStr(byte[] b, int offset, int length) {
    return CommonHelper.byte2HexStr(b, offset, length);
  }

  /** constructor. */
  @Deprecated
  public static Transaction addTransactionSign(
      Transaction transaction, String priKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return TransactionHelper.addTransactionSign(transaction, priKey, blockingStubFull);
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
    return ContractHelper.deployContractAndGetResponse(contractName, abiString, code, data,
        feeLimit, value, consumeUserResourcePercent, originEnergyLimit, tokenId, tokenValue,
        libraryAddress, priKey, ownerAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerContractAndGetResponse(contractAddress, method, argsStr,
        isHex, callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey,
        blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static boolean updateEnergyLimit(
      byte[] contractAddress,
      long originEnergyLimit,
      String priKey,
      byte[] ownerAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ContractHelper.updateEnergyLimit(contractAddress, originEnergyLimit,
        priKey, ownerAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static GrpcAPI.Return accountPermissionUpdateForResponse(
      String permissionJson,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.accountPermissionUpdateForResponse(permissionJson, owner, priKey, blockingStubFull);
  }

  @Deprecated
  public static TransactionApprovedList getTransactionApprovedList(
      Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return TransactionHelper.getTransactionApprovedList(transaction, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static long getFreezeBalanceNetCount(
      byte[] accountAddress,
      String ecKey,
      Long targetNet,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getFreezeBalanceNetCount(accountAddress, ecKey, targetNet, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static GrpcAPI.Return broadcastTransaction(
      Transaction transaction, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return TransactionHelper.broadcastTransaction(transaction, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static GrpcAPI.Return broadcastTransactionBoth(
      Transaction transaction,
      WalletGrpc.WalletBlockingStub blockingStubFull,
      WalletGrpc.WalletBlockingStub blockingStubFull1) {
    return TransactionHelper.broadcastTransactionBoth(transaction, blockingStubFull, blockingStubFull1);
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
  public synchronized static HashMap<String, String> getBycodeAbiWithParam(String solFile, String contractName, String param) {
    final String compile =
            Configuration.getByPath("testng.conf").getString("defaultParameter.solidityCompile");

    String dirPath = solFile.substring(solFile.lastIndexOf("/"), solFile.lastIndexOf("."));
    String outputPath = "src/test/resources/soliditycode/output" + dirPath;

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
                    + " --optimize " + param + " --bin --abi --overwrite "
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
  @Deprecated
  public static String fileRead(String filePath, boolean isLibrary) throws Exception {
    return CommonHelper.fileRead(filePath, isLibrary);
  }

  /** constructor. */
  public static HashMap<String, String> getBycodeAbiForLibrary(
      String solFile, String contractName) {
    HashMap retMap = null;
    String dirPath = solFile.substring(solFile.lastIndexOf("/"), solFile.lastIndexOf("."));
    String outputPath = "src/test/resources/soliditycode/output" + dirPath;
    try {
      retMap = PublicMethod.getBycodeAbi(solFile, contractName);
      String library = fileRead(outputPath + "/" + contractName + ".bin", true);
      retMap.put("library", library);
      logger.debug("library: " + library);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return retMap;
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerConstantContract(contractAddress, method, argsStr, isHex,
        callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerConstantContractForExtentionOnSolidity(contractAddress,
        method, argsStr, isHex, callValue, feeLimit, tokenId, tokenValue, ownerAddress,
        priKey, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static String clearContractAbi(
      byte[] contractAddress,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ContractHelper.clearContractAbi(contractAddress, ownerAddress, priKey,
        blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static TransactionExtention clearContractAbiForExtention(
      byte[] contractAddress,
      byte[] ownerAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ContractHelper.clearContractAbiForExtention(contractAddress, ownerAddress,
        priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerConstantContractForExtention(contractAddress, method,
        argsStr, isHex, callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey,
        blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerSolidityContractForExtention(contractAddress, method,
        argsStr, isHex, callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey,
        solidityBlockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.triggerContractForExtention(contractAddress, method, argsStr,
        isHex, callValue, feeLimit, tokenId, tokenValue, ownerAddress, priKey,
        blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String create2(String[] parameters) {
    return CommonHelper.create2(parameters);
  }

  /** constructor. */
  @Deprecated
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
    return ShieldHelper.sendShieldCoin(publicZenTokenOwnerAddress, fromAmount, shieldAddressInfo, noteTx, shieldOutputList, publicZenTokenToAddress, toAmount, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ShieldHelper.sendShieldCoinWithoutAsk(publicZenTokenOwnerAddress, fromAmount, shieldAddressInfo, noteTx, shieldOutputList, publicZenTokenToAddress, toAmount, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static List<Note> addShieldOutputList(
      List<Note> shieldOutList, String shieldToAddress, String toAmountString, String menoString) {
    return ShieldHelper.addShieldOutputList(shieldOutList, shieldToAddress, toAmountString, menoString);
  }

  /** constructor. */
  @Deprecated
  public static Optional<ShieldAddressInfo> generateShieldAddress() {
    return ShieldHelper.generateShieldAddress();
  }

  /** constructor. */
  @Deprecated
  public static DecryptNotes listShieldNote(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ShieldHelper.listShieldNote(shieldAddressInfo, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static DecryptNotes getShieldNotesByIvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ShieldHelper.getShieldNotesByIvk(shieldAddressInfo, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static DecryptNotesMarked getShieldNotesAndMarkByIvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ShieldHelper.getShieldNotesAndMarkByIvk(shieldAddressInfo, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static DecryptNotesMarked getShieldNotesAndMarkByIvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return ShieldHelper.getShieldNotesAndMarkByIvkOnSolidity(shieldAddressInfo, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static DecryptNotes getShieldNotesByIvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return ShieldHelper.getShieldNotesByIvkOnSolidity(shieldAddressInfo, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static DecryptNotes getShieldNotesByOvk(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ShieldHelper.getShieldNotesByOvk(shieldAddressInfo, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static DecryptNotes getShieldNotesByOvkOnSolidity(
      Optional<ShieldAddressInfo> shieldAddressInfo,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return ShieldHelper.getShieldNotesByOvkOnSolidity(shieldAddressInfo, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static String getMemo(Note note) {
    return ShieldHelper.getMemo(note);
  }

  /** constructor. */
  @Deprecated
  public static SpendResult getSpendResult(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ShieldHelper.getSpendResult(shieldAddressInfo, noteTx, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static SpendResult getSpendResultOnSolidity(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return ShieldHelper.getSpendResultOnSolidity(shieldAddressInfo, noteTx, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static String getShieldNullifier(
      ShieldAddressInfo shieldAddressInfo,
      NoteTx noteTx,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ShieldHelper.getShieldNullifier(shieldAddressInfo, noteTx, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
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
    return ShieldHelper.sendShieldCoinGetTxid(publicZenTokenOwnerAddress, fromAmount, shieldAddressInfo, noteTx, shieldOutputList, publicZenTokenToAddress, toAmount, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static byte[] decode58Check(String input) {
    return CommonHelper.decode58Check(input);
  }

  /** constructor. */
  public static void freeResource(
      byte[] fromAddress,
      String priKey,
      byte[] toAddress,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    ResourceHelper.freeResource(fromAddress, priKey, toAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String parametersString(List<Object> parameters) {
    return CommonHelper.parametersString(parameters);
  }

  /** constructor. */
  @Deprecated
  public static String bytes32ToString(byte[] bytes) {
    return CommonHelper.bytes32ToString(bytes);
  }

  /** constructor. */
  @Deprecated
  public static Return transferAssetForReturn(
      byte[] to,
      byte[] assertName,
      long amount,
      byte[] address,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.transferAssetForReturn(to, assertName, amount, address, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return sendcoinForReturn(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinForReturn(to, amount, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Transaction sendcoinForTransaction(
      byte[] to,
      long amount,
      byte[] owner,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.sendcoinForTransaction(to, amount, owner, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String marketSellAsset(
      byte[] owner,
      String priKey,
      byte[] sellTokenId,
      long sellTokenQuantity,
      byte[] buyTokenId,
      long buyTokenQuantity,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.marketSellAsset(owner, priKey, sellTokenId, sellTokenQuantity,
        buyTokenId, buyTokenQuantity, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return marketSellAssetGetResposne(
      byte[] owner,
      String priKey,
      byte[] sellTokenId,
      long sellTokenQuantity,
      byte[] buyTokenId,
      long buyTokenQuantity,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.marketSellAssetGetResposne(owner, priKey, sellTokenId, sellTokenQuantity,
        buyTokenId, buyTokenQuantity, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static String marketCancelOrder(
      byte[] owner, String priKey, byte[] orderId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.marketCancelOrder(owner, priKey, orderId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Return marketCancelOrderGetResposne(
      byte[] owner, String priKey, byte[] orderId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.marketCancelOrderGetResposne(owner, priKey, orderId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrderList> getMarketOrderByAccount(
      byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getMarketOrderByAccount(address, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrderList> getMarketOrderByAccountSolidity(
      byte[] address, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return AssetHelper.getMarketOrderByAccountSolidity(address, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrder> getMarketOrderById(
      byte[] order, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getMarketOrderById(order, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrder> getMarketOrderByIdSolidity(
      byte[] order, WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return AssetHelper.getMarketOrderByIdSolidity(order, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketPriceList> getMarketPriceByPair(
      byte[] sellTokenId, byte[] buyTokenId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getMarketPriceByPair(sellTokenId, buyTokenId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrderList> getMarketOrderListByPair(
      byte[] sellTokenId, byte[] buyTokenId, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getMarketOrderListByPair(sellTokenId, buyTokenId, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrderList> getMarketOrderListByPairSolidity(
      byte[] sellTokenId,
      byte[] buyTokenId,
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return AssetHelper.getMarketOrderListByPairSolidity(sellTokenId, buyTokenId, blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrderPairList> getMarketPairList(
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getMarketPairList(blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Optional<Protocol.MarketOrderPairList> getMarketPairListSolidity(
      WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity) {
    return AssetHelper.getMarketPairListSolidity(blockingStubSolidity);
  }

  /** constructor. */
  @Deprecated
  public static String stringToHexString(String s) {
    return CommonHelper.stringToHexString(s);
  }

  /** constructor. */
  @Deprecated
  public static String hexStringToString(String s) {
    return CommonHelper.hexStringToString(s);
  }

  /** constructor. */
  @Deprecated
  public static String removeAll0sAtTheEndOfHexStr(String s) {
    return CommonHelper.removeAll0sAtTheEndOfHexStr(s);
  }

  /** constructor. */
  @Deprecated
  public static String replaceCode(String code, String address) {
    return CommonHelper.replaceCode(code, address);
  }

  /** constructor. */
  @Deprecated
  public static Map<String, Long> getAllowance2(
      Long startNum, Long endNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.getAllowance2(startNum, endNum, blockingStubFull);
  }

  @Deprecated
  public static String getContractStringMsg(byte[] contractMsgArray) {
    return ContractHelper.getContractStringMsg(contractMsgArray);
  }

  /** constructor. */
  @Deprecated
  public static long getBrokerage(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return GovernanceHelper.getBrokerage(address, blockingStubFull);
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
  @Deprecated
  public static Long getAccountBalance(
      Protocol.Block block, byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AccountHelper.getAccountBalance(block, address, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static BalanceContract.BlockBalanceTrace getBlockBalance(
      Protocol.Block block, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return BlockHelper.getBlockBalance(block, blockingStubFull);
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
  @Deprecated
  public static Boolean freezeBalanceV2(byte[] addressByte,
      long freezeBalance,
      int resourceCode,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalanceV2(addressByte, freezeBalance, resourceCode, priKey, blockingStubFull);
  }
  @Deprecated
  public static String freezeBalanceV2AndGetTxId(byte[] addressByte,
                                        long freezeBalance,
                                        int resourceCode,
                                        String priKey,
                                        WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.freezeBalanceV2AndGetTxId(addressByte, freezeBalance, resourceCode, priKey, blockingStubFull);
  }


  @Deprecated
  public static Long getFrozenV2Amount(byte[] address, int resourceCode,WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.getFrozenV2Amount(address, resourceCode, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static Boolean delegateResourceForReceiver(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.delegateResourceForReceiver(addressByte, delegateAmount, resourceCode, receiverAddress, priKey, blockingStubFull);
  }


  /** constructor. */
  @Deprecated
  public static Boolean delegateResourceV2(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.delegateResourceV2(addressByte, delegateAmount, resourceCode, receiverAddress, priKey, blockingStubFull);
  }
  /** constructor. */
  @Deprecated
  public static String delegateResourceV2AndGetTxId(byte[] addressByte,
                                           long delegateAmount,
                                           int resourceCode,
                                           byte[] receiverAddress,
                                           String priKey,
                                           WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.delegateResourceV2AndGetTxId(addressByte, delegateAmount, resourceCode, receiverAddress, priKey, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static TransactionExtention delegateResourceV2AndGetTransactionExtention(
      byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      boolean lock,
      Long lockPeriod,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.delegateResourceV2AndGetTransactionExtention(addressByte, delegateAmount, resourceCode, lock, lockPeriod, receiverAddress, priKey, blockingStubFull);
  }



  /** constructor. */
  @Deprecated
  public static Boolean delegateResourceV2Lock(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      boolean lock,
      Long lockPeriod,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.delegateResourceV2Lock(addressByte, delegateAmount, resourceCode, lock, lockPeriod, receiverAddress, priKey, blockingStubFull);
  }

  @Deprecated
  public static String delegateResourceV2LockAndGetTxId(byte[] addressByte,
                                               long delegateAmount,
                                               int resourceCode,
                                               boolean lock,
                                               Long lockPeriod,
                                               byte[] receiverAddress,
                                               String priKey,
                                               WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.delegateResourceV2LockAndGetTxId(addressByte, delegateAmount, resourceCode, lock, lockPeriod, receiverAddress, priKey, blockingStubFull);
  }



  /** constructor. */
  @Deprecated
  public static Boolean unDelegateResourceV2(byte[] addressByte,
      long delegateAmount,
      int resourceCode,
      byte[] receiverAddress,
      String priKey,
      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.unDelegateResourceV2(addressByte, delegateAmount, resourceCode, receiverAddress, priKey, blockingStubFull);
  }

  @Deprecated
  public static String unDelegateResourceV2AndGetTxId(byte[] addressByte,
                                             long delegateAmount,
                                             int resourceCode,
                                             byte[] receiverAddress,
                                             String priKey,
                                             WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.unDelegateResourceV2AndGetTxId(addressByte, delegateAmount, resourceCode, receiverAddress, priKey, blockingStubFull);
  }
  @Deprecated
  public static TransactionExtention unDelegateResourceV2AndGetTransactionExtention(byte[] addressByte,
                                                      long delegateAmount,
                                                      int resourceCode,
                                                      byte[] receiverAddress,
                                                      String priKey,
                                                      WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ResourceHelper.unDelegateResourceV2AndGetTransactionExtention(addressByte, delegateAmount, resourceCode, receiverAddress, priKey, blockingStubFull);
  }


  /** constructor. */
  @Deprecated
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
    return ContractHelper.estimateEnergy(blockingStubFull, owner, contractAddress,
        callValue, method, argsStr, isHex, tokenValue, tokenId);
  }

  /** constructor. */
  @Deprecated
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
    return ContractHelper.estimateEnergySolidity(blockingStubFull, owner, contractAddress,
        callValue, method, argsStr, isHex, tokenValue, tokenId);
  }

  @Deprecated
  public static Optional<GrpcAPI.EstimateEnergyMessage> estimateEnergyDeployContract(
          WalletGrpc.WalletBlockingStub blockingStubFull,
          byte[] owner,
          long callValue,
          long tokenValue,
          String tokenId,
          String code
  ) {
    return ContractHelper.estimateEnergyDeployContract(blockingStubFull, owner,
        callValue, tokenValue, tokenId, code);
  }


  public static void estimateDeployContractEnergy(
          String code,
          long value,
          String tokenId,
          long tokenValue,
          byte[] ownerAddress,
          WalletGrpc.WalletBlockingStub blockingStubFull
  ) {
    ContractHelper.estimateDeployContractEnergy(code, value, tokenId, tokenValue,
        ownerAddress, blockingStubFull);
  }

  /** constructor. */
  @Deprecated
  public static TransactionExtention triggerConstantContractDeployContract(
          String code,
          byte[] ownerAddress,
          long callValue,
          String tokenId,
          long tokenValue,
          WalletGrpc.WalletBlockingStub blockingStubFull) {
    return ContractHelper.triggerConstantContractDeployContract(code, ownerAddress,
        callValue, tokenId, tokenValue, blockingStubFull);
  }

  @Deprecated
  public static Long getExchangeIdByCreatorAddress(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    return AssetHelper.getExchangeIdByCreatorAddress(address, blockingStubFull);
  }

  public static String gRPCurlRequest(String data, String requestUrl, String node) {
    String cmd = gRPCurl + " " + "-plaintext";
    if (data!=null) {
      cmd = cmd + " -d " + data;
    }
    cmd = cmd + " " + node + " " + requestUrl;
    logger.info("cmd is : " + cmd);
    try {
      return PublicMethod.exec(cmd);
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
