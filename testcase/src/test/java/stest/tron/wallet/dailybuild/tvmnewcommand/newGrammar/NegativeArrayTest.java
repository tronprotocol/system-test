package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class NegativeArrayTest extends TronBaseTest {

  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
  }

  @Test(enabled = true, description = "Deploy contract", groups = {"contract", "daily"})
  public void test01DeployContract() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress, 100_000_000L,
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    Protocol.Account info = PublicMethod.queryAccount(dev001Key, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = accountResource.getEnergyUsed();
  Long beforeNetUsed = accountResource.getNetUsed();
  Long beforeFreeNetUsed = accountResource.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String filePath = "./src/test/resources/soliditycode/negativeArray.sol";
  String contractName = "NegativeArray";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    Protocol.Account infoafter = PublicMethod.queryAccount(dev001Key, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(dev001Address,
            blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  @Test(enabled = true, description = "Trigger contract", groups = {"contract", "daily"})
  public void test02TriggerContract() {
    // get[2]
    String methodStr = "get(uint256)";
  String argStr = "2";
  String triggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById.get().getResMessage());
    }
    logger.info("infoById" + infoById);
  String contractResult =
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray());
    logger.info("contractResult:" + contractResult);
    Assert.assertEquals(new BigInteger(contractResult, 16).intValue(), -3);
  // get[1]
    String argStr1 = "1";
  String triggerTxid1 = PublicMethod.triggerContract(contractAddress, methodStr, argStr1, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = PublicMethod
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    if (infoById1.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById1.get().getResMessage());
    }
    logger.info("infoById1" + infoById1);
  String contractResult1 =
        ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray());
    logger.info("contractResult1:" + contractResult1);
    Assert.assertEquals(new BigInteger(contractResult1, 16).intValue(), 2);
  // change array value
    String triggerTxid2 = PublicMethod.triggerContract(contractAddress, "set()", "", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethod
        .getTransactionInfoById(triggerTxid2, blockingStubFull);
    if (infoById2.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById2.get().getResMessage());
    }
    logger.info("infoById2" + infoById2);
  String log1 =
        ByteArray.toHexString(infoById2.get().getLog(0).getData().toByteArray());
    logger.info("log1:" + log1);
    Assert.assertEquals(new BigInteger(log1, 16).intValue(), -1);
  String log2 = ByteArray.toHexString(infoById2.get().getLog(1).getData().toByteArray());
    logger.info("log2:" + log2);
    Assert.assertEquals(new BigInteger(log2, 16).intValue(), 3);
  String log3 =
        ByteArray.toHexString(infoById2.get().getLog(2).getData().toByteArray());
    logger.info("log3:" + log3);
    Assert.assertEquals(new BigInteger(log3, 16).intValue(), -8);
  // get[2]
    String triggerTxid3 = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById3 = PublicMethod
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    if (infoById3.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById3.get().getResMessage());
    }
    logger.info("infoById3" + infoById3);
  String contractResult3 =
        ByteArray.toHexString(infoById3.get().getContractResult(0).toByteArray());
    logger.info("contractResult3:" + contractResult3);
    Assert.assertEquals(new BigInteger(contractResult3, 16).intValue(), -8);
  // get[1]
    String triggerTxid4 = PublicMethod.triggerContract(contractAddress, methodStr, argStr1, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById4 = PublicMethod
        .getTransactionInfoById(triggerTxid4, blockingStubFull);
    if (infoById4.get().getResultValue() != 0) {
      Assert.fail("trigger contract failed with message: " + infoById4.get().getResMessage());
    }
    logger.info("infoById4" + infoById4);
  String contractResult4 =
        ByteArray.toHexString(infoById4.get().getContractResult(0).toByteArray());
    logger.info("contractResult4:" + contractResult4);
    Assert.assertEquals(new BigInteger(contractResult4, 16).intValue(), 3);
  // get[3]
    String triggerTxid5 = PublicMethod.triggerContract(contractAddress, methodStr, "3", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById5 = PublicMethod
        .getTransactionInfoById(triggerTxid5, blockingStubFull);
    logger.info("infoById5" + infoById5);
    Assert.assertEquals(1, infoById5.get().getResultValue());
    Assert.assertEquals("REVERT opcode executed", infoById5.get()
        .getResMessage().toStringUtf8());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    PublicMethod.sendcoin(fromAddress, balance, dev001Address, dev001Key,
        blockingStubFull);  }
}


