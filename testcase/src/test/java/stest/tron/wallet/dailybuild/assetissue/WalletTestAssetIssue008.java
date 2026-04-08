package stest.tron.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.math.BigInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.NumberMessage;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Block;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.contract.AssetIssueContractOuterClass.TransferAssetContract;
import org.tron.protos.contract.AssetIssueContractOuterClass.UnfreezeAssetContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TransactionUtils;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestAssetIssue008 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static String name = "assetissue008" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  String description = "test query assetissue from soliditynode";
  String url = "https://testqueryassetissue.com/from/soliditynode/";
  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] queryAssetIssueFromSoliAddress = ecKey.getAddress();
  String queryAssetIssueKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  public static String loadPubKey() {
    char[] buf = new char[0x100];
    return String.valueOf(buf, 32, 130);
  }


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    logger.info(ByteArray.toHexString(ecKey.getPrivKeyBytes()));    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    }

  @Test(enabled = true, description = "Get asset issue list from Solidity", groups = {"daily"})
  public void testGetAllAssetIssueFromSolidity() {
    Assert.assertTrue(PublicMethod.sendcoin(queryAssetIssueFromSoliAddress, 2048000000, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod.createAssetIssue(queryAssetIssueFromSoliAddress, name,
        totalSupply, 1, 100, start, end, 1, description, url, 10000L,
        10000L, 1L, 1L, queryAssetIssueKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    GrpcAPI.AssetIssueList assetIssueList = blockingStubSolidity
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    logger.info(Long.toString(assetIssueList.getAssetIssueCount()));

    if (assetIssueList.getAssetIssueCount() == 0) {
      Assert.assertTrue(PublicMethod.freezeBalance(foundationAddress, 10000000L, 3,
          foundationKey, blockingStubFull));
      Assert.assertTrue(PublicMethod.sendcoin(toAddress, 999999L, foundationAddress,
          foundationKey, blockingStubFull));
      Block currentBlock = blockingStubFull.getNowBlock(GrpcAPI.EmptyMessage.newBuilder().build());
      logger.info("fullnode block num is " + Long.toString(currentBlock.getBlockHeader()
          .getRawData().getNumber()));
      PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    }

    assetIssueList = blockingStubSolidity
        .getAssetIssueList(GrpcAPI.EmptyMessage.newBuilder().build());
    Assert.assertTrue(assetIssueList.getAssetIssueCount() >= 1);
    for (Integer j = 0; j < assetIssueList.getAssetIssueCount(); j++) {
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getOwnerAddress().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getName().isEmpty());
      Assert.assertFalse(assetIssueList.getAssetIssue(j).getUrl().isEmpty());
      Assert.assertTrue(assetIssueList.getAssetIssue(j).getTotalSupply() > 0);
      logger.info("test get all assetissue from solidity");
    }

  }

  @AfterMethod
  public void aftertest() {
    PublicMethod.freeResource(queryAssetIssueFromSoliAddress, queryAssetIssueKey, foundationAddress,
        blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }

  /**
   * constructor.
   */
  public Account queryAccount(ECKey ecKey, WalletGrpc.WalletBlockingStub blockingStubFull) {
    byte[] address;
    if (ecKey == null) {
      String pubKey = loadPubKey(); //04 PubKey[128]
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

  public byte[] getAddress(ECKey ecKey) {
    return ecKey.getAddress();
  }

  /**
   * constructor.
   */
  public Account grpcQueryAccount(byte[] address, WalletGrpc.WalletBlockingStub blockingStubFull) {
    ByteString addressBs = ByteString.copyFrom(address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    return blockingStubFull.getAccount(request);
  }

  /**
   * constructor.
   */
  public Block getBlock(long blockNum, WalletGrpc.WalletBlockingStub blockingStubFull) {
    NumberMessage.Builder builder = NumberMessage.newBuilder();
    builder.setNum(blockNum);
    return blockingStubFull.getBlockByNum(builder.build());

  }

  private Transaction signTransaction(ECKey ecKey, Transaction transaction) {
    if (ecKey == null || ecKey.getPrivKey() == null) {
      logger.warn("Warning: Can't sign,there is no private key !!");
      return null;
    }
    transaction = TransactionUtils.setTimestamp(transaction);
    return TransactionUtils.sign(transaction, ecKey);
  }

  /**
   * constructor.
   */
  public boolean transferAsset(byte[] to, byte[] assertName, long amount, byte[] address,
      String priKey) {
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
    Transaction transaction = blockingStubFull.transferAsset(contract);
    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }
    transaction = signTransaction(ecKey, transaction);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (!response.getResult()) {
      return false;
    } else {
      Account search = queryAccount(ecKey, blockingStubFull);
      return true;
    }

  }

  /**
   * constructor.
   */
  public boolean unFreezeAsset(byte[] addRess, String priKey) {
    byte[] address = addRess;
  ECKey temKey = null;
    try {
      BigInteger priK = new BigInteger(priKey, 16);
      temKey = ECKey.fromPrivate(priK);
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    final ECKey ecKey = temKey;
  //Account search = queryAccount(ecKey, blockingStubFull);

    UnfreezeAssetContract.Builder builder = UnfreezeAssetContract
        .newBuilder();
    ByteString byteAddress = ByteString.copyFrom(address);

    builder.setOwnerAddress(byteAddress);

    UnfreezeAssetContract contract = builder.build();
    Transaction transaction = blockingStubFull.unfreezeAsset(contract);

    if (transaction == null || transaction.getRawData().getContractCount() == 0) {
      return false;
    }

    transaction = TransactionUtils.setTimestamp(transaction);
    transaction = TransactionUtils.sign(transaction, ecKey);
    Return response = blockingStubFull.broadcastTransaction(transaction);
    if (!response.getResult()) {
      logger.info(ByteArray.toStr(response.getMessage().toByteArray()));
    }
    return response.getResult();
  }
}


