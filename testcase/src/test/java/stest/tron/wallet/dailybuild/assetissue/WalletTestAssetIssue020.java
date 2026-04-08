package stest.tron.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.AssetIssueContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;

import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class WalletTestAssetIssue020 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final String name = "Assetissue020_" + Long.toString(now);
  private static final String char33Name = "To_long_asset_name_a" + Long.toString(now);
  private static final long totalSupply = now;
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
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
  String asset020SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());  private ManagedChannel channelSoliInFull = null;
  private ManagedChannel channelPbft = null;  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubPbft = null;  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext()
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext()
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);
  }

  @Test(enabled = true, description = "Asset issue support precision", groups = {"daily"})
  public void test01AssetIssueSupportPrecision() {
    initPbftChannel();
    initSolidityChannel();
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset020Address = ecKey1.getAddress();
    asset020Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(asset020Key);

    ecKey2 = new ECKey(Utils.getRandom());
    asset020SecondAddress = ecKey2.getAddress();
    asset020SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethod.printAddress(asset020SecondKey);
    logger.info(name);

    Assert.assertTrue(PublicMethod.sendcoin(asset020Address, 204800000000L, foundationAddress2,
        foundationKey2, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(asset020SecondAddress, 204800000000L, foundationAddress2,
        foundationKey2, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    //Can create 32 char token name.
    Long start = System.currentTimeMillis() + 2000000;
    Long end = System.currentTimeMillis() + 1000000000;

    //When precision is -1, can not create asset issue
    Assert.assertFalse(PublicMethod.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, -1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    //When precision is 7, can not create asset issue
    Assert.assertFalse(PublicMethod.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, 7, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    //When precision is 6, is equal to default.
    Assert.assertTrue(PublicMethod.createAssetIssue(asset020Address,
        name, totalSupply, 1, 1, 6, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020Key, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod.queryAccount(asset020Address, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    AssetIssueContractOuterClass.AssetIssueContract assetIssueInfo = PublicMethod
        .getAssetIssueByName(name, blockingStubFull);
    final Integer preCisionByName = assetIssueInfo.getPrecision();
    final Long TotalSupplyByName = assetIssueInfo.getTotalSupply();

    assetIssueInfo = PublicMethod.getAssetIssueById(ByteArray.toStr(assetAccountId
        .toByteArray()), blockingStubFull);
    final Integer preCisionById = assetIssueInfo.getPrecision();
    final Long TotalSupplyById = assetIssueInfo.getTotalSupply();

    assetIssueInfo = PublicMethod.getAssetIssueListByName(name, blockingStubFull)
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
    Assert.assertTrue(PublicMethod.createAssetIssue(asset020SecondAddress,
        name, totalSupply, 1, 1, 1, start, end, 1, description, url,
        2000L, 2000L, 1L, 1L, asset020SecondKey, blockingStubFull));
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSoliInFull);
    assetIssueInfo = PublicMethod.getAssetIssueByName(name, blockingStubFull);
    Assert.assertTrue(assetIssueInfo.getName().isEmpty());

  }

  @Test(enabled = true, description = "Get asset issue by id from Solidity", groups = {"daily"})
  public void test02GetAssetIssueByidFromSolidity() {
    Assert.assertEquals(PublicMethod.getAssetIssueById(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubFull),
        PublicMethod.getAssetIssueByIdFromSolidity(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubSolidity));
    Assert.assertEquals(PublicMethod.getAssetIssueById(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubFull),
        PublicMethod.getAssetIssueByIdFromSolidity(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubSoliInFull));
  }

  @Test(enabled = true, description = "Get asset issue by id from PBFT", groups = {"daily"})
  public void test03GetAssetIssueByIdFromPbft() {
    Assert.assertEquals(PublicMethod.getAssetIssueById(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubFull),
        PublicMethod.getAssetIssueByIdFromSolidity(ByteArray.toStr(assetAccountId
            .toByteArray()), blockingStubPbft));
  }

  @Test(enabled = true, description = "Get asset issue list by name from Solidity", groups = {"daily"})
  public void test04GetAssetIssueListByNameFromSolidity() {
    Assert.assertEquals(PublicMethod.getAssetIssueListByNameFromSolidity(name,
        blockingStubSolidity).get().getAssetIssueList().get(0).getTotalSupply(), totalSupply);
  }

  @Test(enabled = true, description = "Get asset issue list by name from PBFT", groups = {"daily"})
  public void test05GetAssetIssueListByNameFromPbft() {
    Assert.assertEquals(PublicMethod.getAssetIssueListByNameFromSolidity(name,
        blockingStubPbft).get().getAssetIssue(0).getTotalSupply(), totalSupply);
  }
  @Test(enabled = true, description = "freeAssetNetUsed decreasing "
      + "in getAssetIssueById and getAccount", groups = {"daily"})
  public void test06NetUsedDecrease() {
    ECKey ecKeyOwner = new ECKey(Utils.getRandom());
    byte[] assetOwnerAddress = ecKeyOwner.getAddress();
    String assetOwnerKey = ByteArray.toHexString(ecKeyOwner.getPrivKeyBytes());
    PublicMethod.printAddress(assetOwnerKey);
    Assert.assertTrue(PublicMethod.sendcoin(assetOwnerAddress, 200000000000L, foundationAddress2,
        foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(foundationAddress2,
        PublicMethod.getFreezeBalanceCount(assetOwnerAddress, assetOwnerKey, 500000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(assetOwnerAddress), foundationKey2, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(foundationAddress2, 10_0000_00000L,
        0, 0, ByteString.copyFrom(assetOwnerAddress), foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 2000000;
    Long end = System.currentTimeMillis() + 1000000000;
    String name1 = "Assetissue020_" + Long.toString(now);
    Assert.assertTrue(PublicMethod.createAssetIssue(assetOwnerAddress,
        name1, totalSupply, 1, 200, 6, start, end, 1, description, url,
        7000L, 7000L, 1L, 1L, assetOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount = PublicMethod
        .queryAccount(assetOwnerAddress, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    final String strAssetId = assetAccountId.toStringUtf8();

    ECKey ecKeyFrom = new ECKey(Utils.getRandom());
    byte[] assetFromAddress = ecKeyFrom.getAddress();
    String assetFromKey = ByteArray.toHexString(ecKeyFrom.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod.sendcoin(assetFromAddress, 100000000000L, foundationAddress2,
        foundationKey2, blockingStubFull));
    PublicMethod.transferAsset(assetFromAddress,
        assetAccountId.toByteArray(), 100L, assetOwnerAddress, assetOwnerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    ECKey ecKeyTo = new ECKey(Utils.getRandom());
    byte[] assetToAddress = ecKeyTo.getAddress();
    String assetToKey = ByteArray.toHexString(ecKeyTo.getPrivKeyBytes());
    for (int i = 0; i < 16; i++) {
      PublicMethod.transferAsset(assetToAddress,
          assetAccountId.toByteArray(), 1L, assetFromAddress, assetFromKey, blockingStubFull);
    }
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Long fromBeginNetUsed = PublicMethod.queryAccount(assetFromAddress, blockingStubFull)
        .getFreeAssetNetUsageV2Map().get(strAssetId).longValue();
    Long assetBeginNetUsed = PublicMethod.getAssetIssueById(strAssetId, blockingStubFull)
        .getPublicFreeAssetNetUsage();
    Long fromEndNetUsed = fromBeginNetUsed;
    Long assetEndNetUsed = assetBeginNetUsed;
    System.out.println("fromBeginNetUsed: " + fromBeginNetUsed);
    System.out.println("assetBeginNetUsed: " + assetBeginNetUsed);
    int count = 0;
    while (count < 11) {
      fromEndNetUsed = PublicMethod.queryAccount(assetFromAddress, blockingStubFull)
          .getFreeAssetNetUsageV2Map().get(strAssetId).longValue();
      assetEndNetUsed = PublicMethod.getAssetIssueById(strAssetId, blockingStubFull)
          .getPublicFreeAssetNetUsage();
      System.out.println("fromEndNetUsed: " + fromEndNetUsed);
      System.out.println("assetEndNetUsed: " + assetEndNetUsed);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
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
}