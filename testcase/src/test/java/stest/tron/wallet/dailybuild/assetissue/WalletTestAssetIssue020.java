package stest.tron.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class WalletTestAssetIssue020 {

  private static final long now = System.currentTimeMillis();
  private static final String name = "Assetissue020_" + Long.toString(now);
  private static final String char33Name = "To_long_asset_name_a" + Long.toString(now);
  private static final long totalSupply = now;
  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  Account assetIssue020Account;
  ByteString assetAccountId;
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset020Address = ecKey1.getAddress();
  String asset020Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] asset020SecondAddress = ecKey2.getAddress();
  String asset020SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private ManagedChannel channelSolidity = null;
  private ManagedChannel channelSoliInFull = null;
  private ManagedChannel channelPbft = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext()
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext()
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
  }

  @Test(enabled = true, description = "Asset issue support precision")
  public void test01AssetIssueSupportPrecision() {
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset020Address = ecKey1.getAddress();
    asset020Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(asset020Key);

    ecKey2 = new ECKey(Utils.getRandom());
    asset020SecondAddress = ecKey2.getAddress();
    asset020SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(asset020SecondKey);
    logger.info(name);

    Assert.assertTrue(PublicMethed.sendcoin(asset020Address, 204800000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(asset020SecondAddress, 204800000000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    //Can create 32 char token name.
    Long start = System.currentTimeMillis() + 2000000;
    Long end = System.currentTimeMillis() + 1000000000;

    //When precision is -1, can not create asset issue
    Assert.assertFalse(PublicMethed.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, -1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    //When precision is 7, can not create asset issue
    Assert.assertFalse(PublicMethed.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, 7, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    //When precision is 6, is equal to default.
    Assert.assertTrue(PublicMethed.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, 6, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethed.queryAccount(asset020Address, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    AssetIssueContractOuterClass.AssetIssueContract assetIssueInfo = PublicMethed
        .getAssetIssueByName(name, blockingStubFull);
    final Integer preCisionByName = assetIssueInfo.getPrecision();
    final Long TotalSupplyByName = assetIssueInfo.getTotalSupply();

    assetIssueInfo = PublicMethed.getAssetIssueById(ByteArray.toStr(assetAccountId
        .toByteArray()), blockingStubFull);
    final Integer preCisionById = assetIssueInfo.getPrecision();
    final Long TotalSupplyById = assetIssueInfo.getTotalSupply();

    assetIssueInfo = PublicMethed.getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    final Integer preCisionByListName = assetIssueInfo.getPrecision();
    final Long TotalSupplyByListName = assetIssueInfo.getTotalSupply();

    logger.info("precision is " + preCisionByName);
    logger.info("precision is " + preCisionById);
    logger.info("precision is " + preCisionByListName);
    logger.info("totalsupply is " + TotalSupplyByName);
    logger.info("totalsupply is " + TotalSupplyById);
    logger.info("totalsupply is " + TotalSupplyByListName);
    Assert.assertEquals(preCisionById, preCisionByListName);
    Assert.assertEquals(preCisionById, preCisionByName);
    Assert.assertEquals(TotalSupplyById, TotalSupplyByListName);
    Assert.assertEquals(TotalSupplyById, TotalSupplyByName);

    //When precision is 6, is equal to default.
    Assert.assertTrue(PublicMethed.createAssetIssue(asset020SecondAddress,
        name, totalSupply, 1, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020SecondKey, blockingStubFull));
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
    assetIssueInfo = PublicMethed.getAssetIssueByName(name, blockingStubFull);
    Assert.assertTrue(assetIssueInfo.getName().isEmpty());

  }

  @Test(enabled = true, description = "Get asset issue by id from Solidity")
  public void test02GetAssetIssueByidFromSolidity() {
    Assert.assertEquals(PublicMethed.getAssetIssueById(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubFull),
        PublicMethed.getAssetIssueByIdFromSolidity(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubSolidity));
    Assert.assertEquals(PublicMethed.getAssetIssueById(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubFull),
        PublicMethed.getAssetIssueByIdFromSolidity(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubSoliInFull));
  }

  @Test(enabled = true, description = "Get asset issue by id from PBFT")
  public void test03GetAssetIssueByIdFromPbft() {
    Assert.assertEquals(PublicMethed.getAssetIssueById(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubFull),
        PublicMethed.getAssetIssueByIdFromSolidity(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubPbft));
  }

  @Test(enabled = true, description = "Get asset issue list by name from Solidity")
  public void test04GetAssetIssueListByNameFromSolidity() {
    Assert.assertEquals(PublicMethed.getAssetIssueListByNameFromSolidity(name,
        blockingStubSolidity).get().getAssetIssueList().get(0).getTotalSupply(), totalSupply);
  }

  @Test(enabled = true, description = "Get asset issue list by name from PBFT")
  public void test05GetAssetIssueListByNameFromPbft() {
    Assert.assertEquals(PublicMethed.getAssetIssueListByNameFromSolidity(name,
        blockingStubPbft).get().getAssetIssue(0).getTotalSupply(), totalSupply);
  }
  @Test(enabled = true, description = "freeAssetNetUsed decreasing "
      + "in getAssetIssueById and getAccount")
  public void test06NetUsedDecrease() {
    ECKey ecKeyOwner = new ECKey(Utils.getRandom());
    byte[] assetOwnerAddress = ecKeyOwner.getAddress();
    String assetOwnerKey = ByteArray.toHexString(ecKeyOwner.getPrivKeyBytes());
    PublicMethed.printAddress(assetOwnerKey);
    Assert.assertTrue(PublicMethed.sendcoin(assetOwnerAddress, 200000000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress,
        PublicMethed.getFreezeBalanceCount(assetOwnerAddress, assetOwnerKey, 500000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(assetOwnerAddress), testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceForReceiver(fromAddress, 10_0000_00000L,
        0, 0, ByteString.copyFrom(assetOwnerAddress), testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 2000000;
    Long end = System.currentTimeMillis() + 1000000000;
    String name1 = "Assetissue020_" + Long.toString(now);
    Assert.assertTrue(PublicMethed.createAssetIssue(assetOwnerAddress,
        name1, totalSupply, 1, 200, 6, start, end, 1, description, url,
        7000L, 7000L, 1L, 1L, assetOwnerKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount = PublicMethed
        .queryAccount(assetOwnerAddress, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    final String strAssetId = assetAccountId.toStringUtf8();

    ECKey ecKeyFrom = new ECKey(Utils.getRandom());
    byte[] assetFromAddress = ecKeyFrom.getAddress();
    String assetFromKey = ByteArray.toHexString(ecKeyFrom.getPrivKeyBytes());
    Assert.assertTrue(PublicMethed.sendcoin(assetFromAddress, 100000000000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.transferAsset(assetFromAddress,
        assetAccountId.toByteArray(), 100L, assetOwnerAddress, assetOwnerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    ECKey ecKeyTo = new ECKey(Utils.getRandom());
    byte[] assetToAddress = ecKeyTo.getAddress();
    String assetToKey = ByteArray.toHexString(ecKeyTo.getPrivKeyBytes());
    for (int i = 0; i < 16; i++) {
      PublicMethed.transferAsset(assetToAddress,
          assetAccountId.toByteArray(), 1L, assetFromAddress, assetFromKey, blockingStubFull);
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Long fromBeginNetUsed = PublicMethed.queryAccount(assetFromAddress, blockingStubFull)
        .getFreeAssetNetUsageV2Map().get(strAssetId).longValue();
    Long assetBeginNetUsed = PublicMethed.getAssetIssueById(strAssetId, blockingStubFull)
        .getPublicFreeAssetNetUsage();
    Long fromEndNetUsed = fromBeginNetUsed;
    Long assetEndNetUsed = assetBeginNetUsed;
    System.out.println("fromBeginNetUsed: " + fromBeginNetUsed);
    System.out.println("assetBeginNetUsed: " + assetBeginNetUsed);
    int count = 0;
    while (count < 11) {
      fromEndNetUsed = PublicMethed.queryAccount(assetFromAddress, blockingStubFull)
          .getFreeAssetNetUsageV2Map().get(strAssetId).longValue();
      assetEndNetUsed = PublicMethed.getAssetIssueById(strAssetId, blockingStubFull)
          .getPublicFreeAssetNetUsage();
      System.out.println("fromEndNetUsed: " + fromEndNetUsed);
      System.out.println("assetEndNetUsed: " + assetEndNetUsed);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if ((fromBeginNetUsed > fromEndNetUsed) && (assetBeginNetUsed > assetEndNetUsed)) {
        Assert.assertTrue(true);
        break;
      }
      System.out.println("count: " + count);
      count++;
    }
    if (count == 11) {
      Assert.assertTrue(false);
    }
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSolidity != null) {
      channelSolidity.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelPbft != null) {
      channelPbft.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}