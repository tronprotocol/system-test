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
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class ContractTrcToken079 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  private byte[] transferTokenContractAddress = null;
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

  @Test(enabled = true, description = "Trigger transferToken with 0 tokenValue and tokenId", groups = {"contract", "daily"})
  public void triggerTransferTokenContract() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 1100_000_000L, fromAddress,
        foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 1000_000_000L, fromAddress,
        foundationKey, blockingStubFull));
  //    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
//        PublicMethod.getFreezeBalanceCount(dev001Address, dev001Key, 70000L,
//            blockingStubFull), 0, 1,
//        ByteString.copyFrom(dev001Address), foundationKey, blockingStubFull));
//    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress, 10_000_000L + PublicMethod.randomFreezeAmount.getAndAdd(1),
//        0, 0, ByteString.copyFrom(dev001Address), foundationKey, blockingStubFull));
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
  Long devAssetCountBefore = PublicMethod.getAssetIssueValue(dev001Address,
        assetAccountId, blockingStubFull);

    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken079.sol";
  String contractName = "tokenTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 200;
    long callValue = 0;
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key,
            dev001Address, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(transferTokenContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  Long devAssetCountAfter = PublicMethod.getAssetIssueValue(dev001Address,
        assetAccountId, blockingStubFull);
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);
  Long contractAssetCount = PublicMethod.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(200), Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(200), contractAssetCount);
  //    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
//        PublicMethod.getFreezeBalanceCount(user001Address, user001Key, 50000L,
//            blockingStubFull), 0, 1,
//        ByteString.copyFrom(user001Address), foundationKey, blockingStubFull));

    Assert.assertTrue(PublicMethod.transferAsset(user001Address,
        assetAccountId.toByteArray(), 10L, dev001Address, dev001Key, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long transferAssetBefore = PublicMethod.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);

    PublicMethod
        .sendcoin(transferTokenContractAddress, 5000000, fromAddress, foundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    tokenId = Long.toString(0);
    tokenValue = 0;
    callValue = 5;
  String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "msgTokenValueAndTokenIdTest()", "#", false, callValue,
        1000000000L, tokenId, tokenValue, user001Address, user001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    List<String> retList = PublicMethod.getStrings(infoById.get()
        .getContractResult(0).toByteArray());
    logger.info("the value: " + retList);
  Long msgId = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));
  Long msgTokenValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));
  Long msgCallValue = ByteArray.toLong(ByteArray.fromHexString(retList.get(2)));

    logger.info("msgId: " + msgId);
    logger.info("msgTokenValue: " + msgTokenValue);
    logger.info("msgCallValue: " + msgCallValue);

    Assert.assertEquals(msgId.toString(), tokenId);
    Assert.assertEquals(Long.valueOf(msgTokenValue), Long.valueOf(tokenValue));
    Assert.assertEquals(Long.valueOf(msgCallValue), Long.valueOf(callValue));

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethod.freeResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, foundationKey, 0, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, foundationKey, 1, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, foundationKey, 1, user001Address, blockingStubFull);  }
}


