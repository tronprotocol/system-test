package stest.tron.wallet.dailybuild.longexecutiontime;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class FreezeBalanceV2Test007 extends TronBaseTest {
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
    PublicMethod.printAddress(frozenBandwidthKey);    if(!PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {      throw new SkipException("Skipping freezeV2 test case");
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
  @Test(enabled = true, description = "Bandwidth NetUsage change net_window_size", groups = {"daily"})
  public void test01BandwidthNetUsageChangeNetWindowSIze() throws Exception{
    Account account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
    AccountResourceMessage accountResourceMessage = PublicMethod.getAccountResource(frozenBandwidthAddress,blockingStubFull);
  Long beforeNetUsage = account.getNetUsage();
  Long beforeNetUsageFromAccountResource = accountResourceMessage.getNetUsed();
  Long beforeNetWindowSize = account.getNetWindowSize();
    System.out.println("beforeNetWindowSize: " + beforeNetWindowSize);
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
  @Test(enabled = true, description = "Free net can not make netWindowSize change", groups = {"daily"})
  public void test02FreeNetCanNotMakeNetWindowSizeChange() throws Exception{
    AccountResourceMessage accountResourceMessage = PublicMethod.getAccountResource(frozenBandwidthAddress,blockingStubFull);
  Long currentNetPrice = (1000000L * accountResourceMessage.getTotalNetWeight()) / accountResourceMessage.getTotalNetLimit();
    currentNetPrice = Math.max(1L,currentNetPrice);
  Long needUnfreezeBalance = PublicMethod.getFrozenV2Amount(frozenBandwidthAddress,0,blockingStubFull) - 275 * currentNetPrice - currentNetPrice * 100
        - accountResourceMessage.getNetUsed() * currentNetPrice;
    needUnfreezeBalance = Math.max(1L,needUnfreezeBalance);
    logger.info("needUnfreezeBalance:" + needUnfreezeBalance);
    Assert.assertTrue(PublicMethod.unFreezeBalanceV2(frozenBandwidthAddress,frozenBandwidthKey,needUnfreezeBalance,
        0,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Thread.sleep(600000);

    Account account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
  Long beforeUseFreeNetWindowSize = account.getNetWindowSize();
  Long beforeNetUsed = account.getFreeNetUsage();
    logger.info("beforeUseFreeNetWindowSize:" + beforeUseFreeNetWindowSize);

    PublicMethod.sendcoinGetTransactionId(foundationAddress,1L,frozenBandwidthAddress,frozenBandwidthKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    account = PublicMethod.queryAccount(frozenBandwidthAddress,blockingStubFull);
  Long afterUseFreeNetWindowSize = account.getNetWindowSize();
  Long afterNetUsed = account.getFreeNetUsage();
    logger.info("afterUseFreeNetWindowSize:" + afterUseFreeNetWindowSize);
    Assert.assertTrue(afterNetUsed - beforeNetUsed > 200);
    Assert.assertTrue(beforeUseFreeNetWindowSize - afterUseFreeNetWindowSize <= 50 * 1000);
  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(frozenBandwidthAddress, frozenBandwidthKey, foundationAddress, blockingStubFull);  }
}

