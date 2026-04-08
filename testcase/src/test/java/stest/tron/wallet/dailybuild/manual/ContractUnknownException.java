package stest.tron.wallet.dailybuild.manual;

import static org.hamcrest.core.StringContains.containsString;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractUnknownException extends TronBaseTest {
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] grammarAddress2 = ecKey2.getAddress();
  String testKeyForGrammarAddress2 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] grammarAddress3 = ecKey3.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] grammarAddress4 = ecKey4.getAddress();
  String testKeyForGrammarAddress4 = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(testKeyForGrammarAddress);    logger.info(Long.toString(PublicMethod.queryAccount(foundationKey, blockingStubFull)
        .getBalance()));
  }

  @Test(enabled = true, description = "trigger selfdestruct method", groups = {"daily"})
  public void testGrammar001() {
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress, 1000000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(grammarAddress, 204800000,
        0, 1, testKeyForGrammarAddress, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(grammarAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(grammarAddress, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
  String filePath = "src/test/resources/soliditycode/contractUnknownException.sol";
  String contractName = "testA";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress,
            grammarAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  final String s = infoById.get().getResMessage().toStringUtf8();
    long fee = infoById.get().getFee();
    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
    long energyFee = infoById.get().getReceipt().getEnergyFee();
    Account infoafter = PublicMethod.queryAccount(grammarAddress, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(grammarAddress,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfo.getNetUsed();
  Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    PublicMethod.unFreezeBalance(grammarAddress, testKeyForGrammarAddress, 1, grammarAddress,
        blockingStubFull);
    PublicMethod.freeResource(grammarAddress, testKeyForGrammarAddress, foundationAddress,
        blockingStubFull);
  }

  @Test(enabled = true, description = "trigger revert method", groups = {"daily"})
  public void testGrammar002() {
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress2, 100000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(grammarAddress2, 10000000L,
        0, 1, testKeyForGrammarAddress2, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(grammarAddress2,
        blockingStubFull);
    info = PublicMethod.queryAccount(grammarAddress2, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
  String filePath = "src/test/resources/soliditycode/contractUnknownException.sol";
  String contractName = "testB";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress2,
            grammarAddress2, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  final long fee = infoById.get().getFee();
  final long energyUsage = infoById.get().getReceipt().getEnergyUsage();
  final long energyFee = infoById.get().getReceipt().getEnergyFee();
  final String s = infoById.get().getResMessage().toStringUtf8();

    Account infoafter = PublicMethod.queryAccount(grammarAddress2, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(grammarAddress2,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfo.getNetUsed();
  Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    Assert.assertFalse(energyFee == 1000000000);

    Assert.assertTrue(beforeBalance - fee == afterBalance);
    PublicMethod.unFreezeBalance(grammarAddress2, testKeyForGrammarAddress2, 1, grammarAddress2,
        blockingStubFull);
    PublicMethod.freeResource(grammarAddress2, testKeyForGrammarAddress2, foundationAddress,
        blockingStubFull);

  }

  @Test(enabled = true, description = "trigger assert method", groups = {"daily"})
  public void testGrammar003() {
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress3, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(grammarAddress3, 1000000000L,
        0, 1, testKeyForGrammarAddress3, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(grammarAddress3,
        blockingStubFull);
    info = PublicMethod.queryAccount(grammarAddress3, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
  String filePath = "src/test/resources/soliditycode/contractUnknownException.sol";
  String contractName = "testC";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  final long fee = infoById.get().getFee();
  final long energyUsage = infoById.get().getReceipt().getEnergyUsage();
  final long energyFee = infoById.get().getReceipt().getEnergyFee();
  String s = infoById.get().getResMessage().toStringUtf8();
    Account infoafter = PublicMethod.queryAccount(grammarAddress3, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(grammarAddress3,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfo.getNetUsed();
  Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    logger.info("s:" + s);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    Assert.assertTrue(beforeBalance - fee == afterBalance);
    PublicMethod.unFreezeBalance(grammarAddress3, testKeyForGrammarAddress3, 1, grammarAddress3,
        blockingStubFull);
    PublicMethod.freeResource(grammarAddress3, testKeyForGrammarAddress3, foundationAddress,
        blockingStubFull);

  }


  @Test(enabled = true, description = "trigger require method", groups = {"daily"})
  public void testGrammar004() {
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress4, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(grammarAddress4, 100000000L,
        0, 1, testKeyForGrammarAddress4, blockingStubFull));
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(grammarAddress4,
        blockingStubFull);
    info = PublicMethod.queryAccount(grammarAddress4, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    long beforeenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeenergyLimit:" + beforeenergyLimit);
  String filePath = "src/test/resources/soliditycode/contractUnknownException.sol";
  String contractName = "testD";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            20L, 100, null, testKeyForGrammarAddress4,
            grammarAddress4, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  final String s = infoById.get().getResMessage().toStringUtf8();
  final long fee = infoById.get().getFee();
    long energyUsage = infoById.get().getReceipt().getEnergyUsage();
  final long energyFee = infoById.get().getReceipt().getEnergyFee();

    Account infoafter = PublicMethod.queryAccount(grammarAddress4, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(grammarAddress4,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfo.getNetUsed();
  Long afterFreeNetUsed = resourceInfo.getFreeNetUsed();
    long aftereenergyLimit = resourceInfo.getEnergyLimit();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterenergyLimit:" + aftereenergyLimit);
    Assert.assertThat(s, containsString("REVERT opcode executed"));
    Assert.assertTrue(beforeBalance - fee == afterBalance);
    Assert.assertFalse(energyFee == 1000000000);
    PublicMethod.unFreezeBalance(grammarAddress4, testKeyForGrammarAddress4, 1, grammarAddress4,
        blockingStubFull);
    PublicMethod.freeResource(grammarAddress4, testKeyForGrammarAddress4, foundationAddress,
        blockingStubFull);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {  }

}
