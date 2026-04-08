package stest.tron.wallet.dailybuild.assetissue;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountNetMessage;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;

import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class WalletTestAssetIssue016 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long netCostMeasure = 200L;
  private static String name = "AssetIssue016_" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  Long freeAssetNetLimit = 30000L;
  Long publicFreeAssetNetLimit = 30000L;
  String description = "for case assetissue016";
  String url = "https://stest.assetissue016.url";
  ByteString assetAccountId;
  //get account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] asset016Address = ecKey1.getAddress();
  String testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] transferAssetAddress = ecKey2.getAddress();
  String transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());  private ManagedChannel channelSoliInFull = null;
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

  @Test(enabled = true, description = "Get asset issue net resource", groups = {"daily"})
  public void test01GetAssetIssueNet() {
    initPbftChannel();
    initSolidityChannel();
    //get account
    ecKey1 = new ECKey(Utils.getRandom());
    asset016Address = ecKey1.getAddress();
    testKeyForAssetIssue016 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    transferAssetAddress = ecKey2.getAddress();
    transferAssetCreateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethod.printAddress(testKeyForAssetIssue016);
    PublicMethod.printAddress(transferAssetCreateKey);

    Assert.assertTrue(PublicMethod
        .sendcoin(asset016Address, sendAmount, foundationAddress2, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Long start = System.currentTimeMillis() + 2000;
    Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod
        .createAssetIssue(asset016Address, name, totalSupply, 1, 1, start, end, 1, description,
            url, freeAssetNetLimit, publicFreeAssetNetLimit, 1L, 1L, testKeyForAssetIssue016,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod.queryAccount(asset016Address, blockingStubFull);
    assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();

    AccountNetMessage assetIssueInfo = PublicMethod
        .getAccountNet(asset016Address, blockingStubFull);
    Assert.assertTrue(assetIssueInfo.getAssetNetLimitCount() == 1);
    Assert.assertTrue(assetIssueInfo.getAssetNetUsedCount() == 1);
    Assert.assertFalse(assetIssueInfo.getAssetNetLimitMap().isEmpty());
    Assert.assertFalse(assetIssueInfo.getAssetNetUsedMap().isEmpty());

    GrpcAPI.BytesMessage request = GrpcAPI.BytesMessage.newBuilder()
        .setValue(assetAccountId).build();
    AssetIssueContract assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getFreeAssetNetLimit() == freeAssetNetLimit);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetLimit() == publicFreeAssetNetLimit);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() == 0);
    assetIssueInfo.hashCode();
    assetIssueInfo.getSerializedSize();
    assetIssueInfo.equals(assetIssueInfo);

    PublicMethod.transferAsset(transferAssetAddress, assetAccountId.toByteArray(), 1000L,
        asset016Address, testKeyForAssetIssue016, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 100L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() == 0);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetUsage() == 0);

    Assert.assertTrue(PublicMethod.freezeBalance(asset016Address, 30000000L,
        3, testKeyForAssetIssue016, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.transferAsset(toAddress, assetAccountId.toByteArray(), 100L,
        transferAssetAddress, transferAssetCreateKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetIssueByName = blockingStubFull.getAssetIssueByName(request);
    Assert.assertTrue(assetIssueByName.getPublicLatestFreeNetTime() > 0);
    Assert.assertTrue(assetIssueByName.getPublicFreeAssetNetUsage() > 150);

    PublicMethod
        .freeResource(asset016Address, testKeyForAssetIssue016, fromAddress, blockingStubFull);

  }

  @Test(enabled = true, description = "Get asset issue by name from Solidity", groups = {"daily"})
  public void test02GetAssetIssueByNameFromSolidity() {
    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);
    Assert.assertEquals(PublicMethod.getAssetIssueByNameFromSolidity(name,
        blockingStubSolidity).getTotalSupply(), totalSupply);
  }

  @Test(enabled = true, description = "Get asset issue by name from PBFT", groups = {"daily"})
  public void test03GetAssetIssueByNameFromPbft() {
    Assert.assertEquals(PublicMethod.getAssetIssueByNameFromSolidity(name,
        blockingStubPbft).getTotalSupply(), totalSupply);
  }

  @Test(enabled = true, description = "Get asset issue list from PBFT", groups = {"daily"})
  public void test04GetAssetIssueListFromPbft() {
    Assert.assertTrue(PublicMethod.listAssetIssueFromSolidity(
        blockingStubPbft).get().getAssetIssueCount() >= 1);
  }

  @Test(enabled = true, description = "Get asset issue list from Solidity", groups = {"daily"})
  public void test05GetAssetIssueListFromSolidity() {
    Assert.assertTrue(PublicMethod.listAssetIssueFromSolidity(
        blockingStubSoliInFull).get().getAssetIssueCount() >= 1);
    Assert.assertTrue(PublicMethod.listAssetIssueFromSolidity(
        blockingStubSolidity).get().getAssetIssueCount() >= 1);
  }

  @Test(enabled = true, description = "Get asset issue list paginated from PBFT", groups = {"daily"})
  public void test06GetAssetIssetListPaginatedFromPbft() {
    Assert.assertTrue(PublicMethod.listAssetIssuepaginatedFromSolidity(
        blockingStubPbft, 0L, 1L).get().getAssetIssueCount() == 1);
  }

  @Test(enabled = true, description = "Get asset issue list paginated from Solidity", groups = {"daily"})
  public void test05GetAssetIssueListPaginatedFromSolidity() {
    Assert.assertTrue(PublicMethod.listAssetIssuepaginatedFromSolidity(
        blockingStubSolidity, 0L, 1L).get().getAssetIssueCount() == 1);
    Assert.assertTrue(PublicMethod.listAssetIssuepaginatedFromSolidity(
        blockingStubSoliInFull, 0L, 1L).get().getAssetIssueCount() == 1);
  }
}