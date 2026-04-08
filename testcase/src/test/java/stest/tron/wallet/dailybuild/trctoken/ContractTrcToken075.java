package stest.tron.wallet.dailybuild.trctoken;

import static org.tron.protos.Protocol.TransactionInfo.code.FAILED;

import com.google.protobuf.ByteString;
import java.util.HashMap;
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
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class ContractTrcToken075 extends TronBaseTest {

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

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "TokenBalance with exception condition", groups = {"contract", "daily"})
  public void testTokenBalanceContract() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 11000_000_000L, fromAddress,
        foundationKey, blockingStubFull));
  //    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
//        PublicMethod.getFreezeBalanceCount(dev001Address, dev001Key, 130000L,
//            blockingStubFull), 0, 1,
//        ByteString.copyFrom(dev001Address), foundationKey, blockingStubFull));
//
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
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethod
        .queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());
  Long devAssetCountBefore = PublicMethod.getAssetIssueValue(dev001Address,
        assetAccountId, blockingStubFull);
    logger.info("before AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountBefore: "
        + devAssetCountBefore);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken075.sol";
  String contractName = "Dest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 200;
    long callValue = 5;
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callValue, 0, 10000, tokenId, tokenValue,
            null, dev001Key, dev001Address, blockingStubFull);

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
  Long devAssetCountAfter = PublicMethod.getAssetIssueValue(dev001Address,
        assetAccountId, blockingStubFull);
    logger.info("after AssetId: " + assetAccountId.toStringUtf8() + ", devAssetCountAfter: "
        + devAssetCountAfter);
  Long contractAssetCount = PublicMethod.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("Contract has AssetId: " + assetAccountId.toStringUtf8() + ", Count: "
        + contractAssetCount);

    Assert.assertEquals(Long.valueOf(tokenValue),
        Long.valueOf(devAssetCountBefore - devAssetCountAfter));
    Assert.assertEquals(Long.valueOf(tokenValue), contractAssetCount);
  // get and verify the msg.value and msg.id

    Long transferAssetBefore = PublicMethod.getAssetIssueValue(transferTokenContractAddress,
        assetAccountId, blockingStubFull);
    logger.info("before trigger, transferTokenContractAddress has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + transferAssetBefore);
  Long devAssetBefore = PublicMethod.getAssetIssueValue(dev001Address,
        assetAccountId, blockingStubFull);
    logger.info("before trigger, dev001Address has AssetId "
        + assetAccountId.toStringUtf8() + ", Count is " + devAssetBefore);

    tokenId = Long.toString(100_0000);
  String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "getToken(trcToken)", tokenId, false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    tokenId = Long.toString(0);
    triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "getToken(trcToken)", tokenId, false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    tokenId = Long.toString(-1);

    triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "getToken(trcToken)", tokenId, false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    tokenId = Long.toString(Long.MIN_VALUE);

    triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "getToken(trcToken)", tokenId, false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "getTokenLongMin()", "#", false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

    triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "getTokenLongMax()", "#", false, 0,
        1000000000L, "0", 0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
  //Assert.assertEquals("BigInteger out of long range",
    Assert.assertEquals("REVERT opcode executed",
        infoById.get().getResMessage().toStringUtf8());

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, foundationKey, 0, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, foundationKey, 1, dev001Address, blockingStubFull);  }
}


