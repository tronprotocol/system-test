package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class ContractTrcToken066 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] transferTokenContractAddress = null;
  private byte[] resultContractAddress = null;
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    PublicMethod.printAddress(user001Key);
  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, deploy transferContract", groups = {"contract", "daily"})
  public void test01DeployTransferTokenContract() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 5048_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 4048_000_000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        10000, start, end, 1, description, url, 100000L, 100000L,
        1L, 1L, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountBefore = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before energyLimit is " + energyLimit);
    logger.info("before energyUsage is " + energyUsage);
    logger.info("before balanceBefore is " + balanceBefore);
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken066.sol";
  String contractName = "transferTokenContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            assetAccountId.toStringUtf8(), 100, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountAfter = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("after energyLimit is " + energyLimit);
    logger.info("after energyUsage is " + energyUsage);
    logger.info("after balanceAfter is " + balanceAfter);
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);
  Long contractAssetCount = PublicMethod.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, deploy receive contract", groups = {"contract", "daily"})
  public void test02DeployRevContract() {

    Long devAssetCountBefore = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken066.sol";
  String contractName = "Result";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String recieveTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 1000, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(recieveTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (recieveTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy receive failed with message: " + infoById.get().getResMessage());
    }

    resultContractAddress = infoById.get().getContractAddress().toByteArray();

    SmartContract smartContract = PublicMethod
        .getContract(resultContractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  Long devAssetCountAfter = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);
  Long contractAssetCount = PublicMethod.getAssetIssueValue(resultContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(100), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(100), contractAssetCount);
  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, trigger transferContract", groups = {"contract", "daily"})
  public void test03TriggerContract() {

    Assert.assertTrue(PublicMethod.transferAsset(user001Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long transferAssetBefore = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);
  Long receiveAssetBefore = PublicMethod.getAssetIssueValue(resultContractAddress, assetAccountId,
        blockingStubFull);
    logger.info("before trigger, resultContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + receiveAssetBefore);
  String tokenId = assetAccountId.toStringUtf8();
  Long tokenValue = Long.valueOf(0);
  Long callValue = Long.valueOf(0);
  String param = "\"" + Base58.encode58Check(resultContractAddress)
        + "\",\"" + tokenValue + "\"," + tokenId;
  String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "transferTokenTest(address,uint256,trcToken)", param, false, callValue,
        1000000000L, assetAccountId.toStringUtf8(), 2, user001Address, user001Key,
        blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    TransactionInfo transactionInfo = infoById.get();
    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    List<String> retList = PublicMethod
        .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray());

    logger.info("the value: " + retList);
  Long msgId = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));
  Long msgTokenValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));
  Long msgCallValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(2)));

    logger.info("msgId: " + msgId);
    logger.info("msgTokenValue: " + msgTokenValue);
    logger.info("msgCallValue: " + msgCallValue);

    Assert.assertEquals(tokenId, msgId.toString());
    Assert.assertEquals(tokenValue, msgTokenValue);
    Assert.assertEquals(callValue, msgCallValue);
  Long transferAssetAfter = PublicMethod.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", transferAssetAfter is " + transferAssetAfter);
  Long receiveAssetAfter = PublicMethod.getAssetIssueValue(resultContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("after trigger, resultContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", receiveAssetAfter is " + receiveAssetAfter);

    Assert.assertEquals(receiveAssetAfter - receiveAssetBefore,
        transferAssetBefore + 2L - transferAssetAfter);

  }

  @Test(enabled = true, description = "TransferToken with 0 tokenValue, get contract tokenBalance", groups = {"contract", "daily"})
  public void test04TriggerTokenBalanceContract() {


    String param = "\"" + Base58.encode58Check(resultContractAddress) + "\",\""
        + assetAccountId.toStringUtf8() + "\"";
  String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "getTokenBalnce(address,trcToken)",
        param, false, 0, 1000000000L, user001Address,
        user001Key, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(triggerTxid,
        blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    logger.info("the receivercontract token: " + ByteArray
        .toLong(infoById.get().getContractResult(0).toByteArray()));
  Long assetIssueCount = PublicMethod.getAssetIssueValue(resultContractAddress, assetAccountId,
        blockingStubFull);
    logger.info("the receivercontract token(getaccount): " + assetIssueCount);

    Assert.assertTrue(assetIssueCount == ByteArray
        .toLong(ByteArray.fromHexString(
            ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()))));

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethod.freeResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 0, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 1, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 1, user001Address, blockingStubFull);  }
}


