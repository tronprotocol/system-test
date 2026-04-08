package stest.tron.wallet.dailybuild.freezeV2;

import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.UnFreezeV2;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class FreezeBalanceV2Test005 extends TronBaseTest {
  private static final long sendAmount = 10000000000L;
  private final byte[] witnessAddress = PublicMethod.getFinalAddress(witnessKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenBandwidthAddress = ecKey1.getAddress();
  String frozenBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] frozenEnergyAddress = ecKey2.getAddress();
  String frozenEnergyKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] frozenEnergyRevertAddress = ecKey3.getAddress();
  String frozenEnergyRevertKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  Long freezeBandwidthBalance = 4000000000L;
  Long unfreezeBalance = 10000000L;
  Long freezeEnergyBalance = 3000000000L;
  Integer maxUnfreezeListCount = 32;
  byte[] contractAddress;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception{
    PublicMethod.printAddress(frozenBandwidthKey);
    PublicMethod.printAddress(frozenEnergyKey);    if(!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
    }
    Assert.assertTrue(PublicMethod.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(frozenEnergyAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(frozenEnergyRevertAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/contractLinkage005.sol";
  String contractName = "timeoutTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 100, null, frozenEnergyKey,
        frozenEnergyAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    contractAddress = PublicMethod.getTransactionInfoById(txid,blockingStubFull).get().getContractAddress().toByteArray();
    Assert.assertEquals(PublicMethod.queryAccount(contractAddress,blockingStubFull).getType(),AccountType.Contract);

    Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenBandwidthAddress,
        freezeBandwidthBalance,0,frozenBandwidthKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenEnergyAddress,
        freezeEnergyBalance,1,frozenEnergyKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(frozenEnergyRevertAddress,
        freezeEnergyBalance,1,frozenEnergyRevertKey,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "Bandwidth NetUsage change net_window_size", groups = {"daily", "staking"})
  public void test01BandwidthNetUsageChangeNetWindowSIze() throws Exception{
    Account account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    AccountResourceMessage accountResourceMessage = PublicMethod.getAccountResource(frozenBandwidthAddress,blockingStubFull);
  Long beforeNetUsage = account.getNetUsage();
  Long beforeNetUsageFromAccountResource = accountResourceMessage.getNetUsed();
  Long beforeNetWindowSize = account.getNetWindowSize();
    Assert.assertTrue(beforeNetWindowSize == 28800 * 1000);
    Assert.assertEquals(beforeNetUsage,beforeNetUsageFromAccountResource);
    Assert.assertTrue(beforeNetUsage == 0);
  String txid = PublicMethod.sendcoinGetTransactionId(foundationAddress,1L,
        frozenBandwidthAddress,frozenBandwidthKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    accountResourceMessage = PublicMethod.getAccountResource(frozenBandwidthAddress,blockingStubFull);
  Long afterNetUsage = account.getNetUsage();
  Long afterNetUsageFromAccountResource = accountResourceMessage.getNetUsed();
    Assert.assertEquals(afterNetUsage,afterNetUsageFromAccountResource);
  Long transactionNetUsage = PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getNetUsage();
    Assert.assertTrue(afterNetUsage <= transactionNetUsage
        && afterNetUsage + 2 >= transactionNetUsage
        && transactionNetUsage > 200);
  Long beforeLatestConsumeTime = account.getLatestConsumeTime();

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
  Long afterLatestConsumeTime = account.getLatestConsumeTime();
  //getLatestConsumeTime means this user latest use consume net Time,not current timestamp
    Assert.assertEquals(beforeLatestConsumeTime,afterLatestConsumeTime);
    beforeNetWindowSize = account.getNetWindowSize();
    logger.info("beforeNetWindowSize:" + beforeNetWindowSize);
    Assert.assertTrue(beforeNetWindowSize < 28800 * 1000);
    beforeNetUsage = account.getNetUsage();

    Assert.assertTrue(beforeNetUsage > 200);
    txid = PublicMethod.sendcoinGetTransactionId(foundationAddress,1L,
        frozenBandwidthAddress,frozenBandwidthKey,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
  Long afterNetWindowSize = account.getNetWindowSize();
    logger.info("afterNetWindowSize:" + afterNetWindowSize);
    Assert.assertTrue(
        afterNetWindowSize > beforeNetWindowSize
        && afterNetWindowSize <= 28795 * 1000);
    afterNetUsage = account.getNetUsage();
    transactionNetUsage = PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getNetUsage();
    Assert.assertTrue(afterNetUsage - beforeNetUsage <= transactionNetUsage
        && afterNetUsage - beforeNetUsage + 2 >= transactionNetUsage
        && transactionNetUsage > 200 && afterNetUsage > 400);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EnergyUsage change energy_window_size when trigger is success", groups = {"daily", "staking"})
  public void test02EnergyUsageChangeEnergyWindowSizeWithTriggerSuccess() throws Exception{
    Account account = PublicMethod.queryAccount(frozenEnergyAddress,blockingStubFull);
    AccountResourceMessage accountResourceMessage = PublicMethod.getAccountResource(frozenEnergyAddress,blockingStubFull);
  Long beforeEnergyUsage = account.getAccountResource().getEnergyUsage();
  Long beforeEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
  Long beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    Assert.assertTrue(beforeEnergyWindowSize == 28800 * 1000);
    Assert.assertEquals(beforeEnergyUsage,beforeEnergyUsageFromAccountResource);
    Assert.assertTrue(beforeEnergyUsage == 0);
  Long cycleTime = 1L;
  String txid = PublicMethod.triggerContract(contractAddress,
        "testUseCpu(uint256)", cycleTime.toString(), false,
        0, maxFeeLimit,frozenEnergyAddress, frozenEnergyKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenEnergyAddress,blockingStubFull);
    accountResourceMessage = PublicMethod.getAccountResource(frozenEnergyAddress,blockingStubFull);
  Long afterEnergyUsage = account.getAccountResource().getEnergyUsage();
  Long afterEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
    Assert.assertEquals(afterEnergyUsage,afterEnergyUsageFromAccountResource);
  Long transactionEnergyUsage = PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertEquals(PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.SUCCESS);
    logger.info("transactionEnergyUsage:" + transactionEnergyUsage);
    Assert.assertTrue(afterEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage + 2 >= transactionEnergyUsage
        && transactionEnergyUsage > 550);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenEnergyAddress,blockingStubFull);
    beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyUsage:" + beforeEnergyUsage);
    Assert.assertTrue(beforeEnergyWindowSize < 28800 * 1000);
    beforeEnergyUsage = account.getAccountResource().getEnergyUsage();
    logger.info("beforeEnergyUsage2:" + beforeEnergyUsage);
    Assert.assertTrue(beforeEnergyUsage > 550);
    txid = PublicMethod.triggerContract(contractAddress,
        "testUseCpu(uint256)",  cycleTime.toString(), false,
        0, 100000000L,frozenEnergyAddress, frozenEnergyKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.SUCCESS);
    account = PublicMethod.queryAccount(frozenEnergyAddress,blockingStubFull);
  Long afterEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyWindowSize:" + beforeEnergyWindowSize);
    logger.info("afterEnergyWindowSize:" + afterEnergyWindowSize);
    Assert.assertTrue(afterEnergyWindowSize <= 28795 * 1000
        && afterEnergyWindowSize > beforeEnergyWindowSize);
    afterEnergyUsage = account.getAccountResource().getEnergyUsage();
    transactionEnergyUsage = PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertTrue(afterEnergyUsage - beforeEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage - beforeEnergyUsage + 8 >= transactionEnergyUsage
        && transactionEnergyUsage > 550 && afterEnergyUsage > 550 * 2);

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EnergyUsage change energy_window_size when trigger is revert", groups = {"daily", "staking"})
  public void test03EnergyUsageChangeEnergyWindowSizeWithTriggerRevert() throws Exception{
    Account account = PublicMethod.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
    AccountResourceMessage accountResourceMessage = PublicMethod.getAccountResource(frozenEnergyRevertAddress,blockingStubFull);
  Long beforeEnergyUsage = account.getAccountResource().getEnergyUsage();
  Long beforeEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
  Long beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    Assert.assertTrue(beforeEnergyWindowSize == 28800 * 1000);
    Assert.assertEquals(beforeEnergyUsage,beforeEnergyUsageFromAccountResource);
    Assert.assertTrue(beforeEnergyUsage == 0);
  //Wrong method cause result is revert
    String txid = PublicMethod.triggerContract(contractAddress,
        "getRevertTransaction()", "", false,
        0, maxFeeLimit,frozenEnergyRevertAddress, frozenEnergyRevertKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
    accountResourceMessage = PublicMethod.getAccountResource(frozenEnergyRevertAddress,blockingStubFull);
  Long afterEnergyUsage = account.getAccountResource().getEnergyUsage();
  Long afterEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
    Assert.assertEquals(afterEnergyUsage,afterEnergyUsageFromAccountResource);
  Long transactionEnergyUsage = PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertEquals(PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.REVERT);
    logger.info("transactionEnergyUsage:" + transactionEnergyUsage);
    Assert.assertTrue(afterEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage + 2 >= transactionEnergyUsage
        && transactionEnergyUsage > 150);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
    beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyUsage:" + beforeEnergyUsage);
    Assert.assertTrue(beforeEnergyWindowSize < 28800 * 1000);
    beforeEnergyUsage = account.getAccountResource().getEnergyUsage();

    Assert.assertTrue(beforeEnergyUsage > 50);
    txid = PublicMethod.triggerContract(contractAddress,
        "getRevertTransaction()",  "", false,
        0, maxFeeLimit,frozenEnergyRevertAddress, frozenEnergyRevertKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.REVERT);
    account = PublicMethod.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
  Long afterEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyWindowSize:" + beforeEnergyWindowSize);
    logger.info("afterEnergyWindowSize:" + afterEnergyWindowSize);
    Assert.assertTrue(afterEnergyWindowSize <= 28795 * 1000
        && afterEnergyWindowSize > beforeEnergyWindowSize);
    afterEnergyUsage = account.getAccountResource().getEnergyUsage();
    transactionEnergyUsage = PublicMethod.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertTrue(afterEnergyUsage - beforeEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage - beforeEnergyUsage + 8 >= transactionEnergyUsage
        && transactionEnergyUsage > 150 && afterEnergyUsage > 150 * 2);

    Assert.assertTrue(account.getNetWindowOptimized());
    Assert.assertTrue(account.getAccountResource().getEnergyWindowOptimized());

  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(frozenBandwidthAddress, frozenBandwidthKey, foundationAddress, blockingStubFull);  }
}

