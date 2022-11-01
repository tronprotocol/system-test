package stest.tron.wallet.dailybuild.freezeV2;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Account.UnFreezeV2;
import org.tron.protos.Protocol.AccountType;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class FreezeBalanceV2Test005 {
  private static final long sendAmount = 10000000000L;

  private final String foundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private final String witness1Key = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witnessAddress = PublicMethed.getFinalAddress(witness1Key);

  private final String witness2Key = Configuration.getByPath("testng.conf")
      .getString("witness.key1");
  private final byte[] witness2Address = PublicMethed.getFinalAddress(witness2Key);

  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] frozenBandwidthAddress = ecKey1.getAddress();
  String frozenBandwidthKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());


  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] frozenEnergyAddress = ecKey2.getAddress();
  String frozenEnergyKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());


  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] frozenEnergyRevertAddress = ecKey3.getAddress();
  String frozenEnergyRevertKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  Long freezeBandwidthBalance = 8000000000L;

  Long unfreezeBalance = 10000000L;
  Long freezeEnergyBalance = 3000000000L;
  Integer maxUnfreezeListCount = 32;

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list")
      .get(0);
  byte[] contractAddress;

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() throws Exception{
    PublicMethed.printAddress(frozenBandwidthKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    if(!PublicMethed.freezeV2ProposalIsOpen(blockingStubFull)) {
      if (channelFull != null) {
        channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
      }
      throw new SkipException("Skipping freezeV2 test case");
    }
    Assert.assertTrue(PublicMethed.sendcoin(frozenBandwidthAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(frozenEnergyAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethed.sendcoin(frozenEnergyRevertAddress, sendAmount,
        foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    String filePath = "./src/test/resources/soliditycode/contractLinkage005.sol";
    String contractName = "timeoutTest";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    String txid = PublicMethed.deployContractAndGetTransactionInfoById(contractName, abi, code,
        "", maxFeeLimit, 0L, 100, null, frozenEnergyKey,
        frozenEnergyAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    contractAddress = PublicMethed.getTransactionInfoById(txid,blockingStubFull).get().getContractAddress().toByteArray();
    Assert.assertEquals(PublicMethed.queryAccount(contractAddress,blockingStubFull).getType(),AccountType.Contract);


    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress,
        freezeBandwidthBalance,0,frozenBandwidthKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenEnergyAddress,
        freezeEnergyBalance,1,frozenEnergyKey,blockingStubFull));
    Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenEnergyRevertAddress,
        freezeEnergyBalance,1,frozenEnergyRevertKey,blockingStubFull));
    if(PublicMethed.tronPowerProposalIsOpen(blockingStubFull)) {
      Assert.assertTrue(PublicMethed.freezeBalanceV2(frozenBandwidthAddress,
          freezeBandwidthBalance,2,frozenBandwidthKey,blockingStubFull));
    }
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "Bandwidth NetUsage change net_window_size")
  public void test01BandwidthNetUsageChangeNetWindowSIze() throws Exception{
    Account account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    AccountResourceMessage accountResourceMessage = PublicMethed.getAccountResource(frozenBandwidthAddress,blockingStubFull);
    Long beforeNetUsage = account.getNetUsage();
    Long beforeNetUsageFromAccountResource = accountResourceMessage.getNetUsed();
    Long beforeNetWindowSize = account.getNetWindowSize();
    Assert.assertTrue(beforeNetWindowSize == 28800);
    Assert.assertEquals(beforeNetUsage,beforeNetUsageFromAccountResource);
    Assert.assertTrue(beforeNetUsage == 0);
    String txid = PublicMethed.sendcoinGetTransactionId(foundationAddress,1L,
        frozenBandwidthAddress,frozenBandwidthKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    accountResourceMessage = PublicMethed.getAccountResource(frozenBandwidthAddress,blockingStubFull);
    Long afterNetUsage = account.getNetUsage();
    Long afterNetUsageFromAccountResource = accountResourceMessage.getNetUsed();
    Assert.assertEquals(afterNetUsage,afterNetUsageFromAccountResource);
    Long transactionNetUsage = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getNetUsage();
    Assert.assertTrue(afterNetUsage <= transactionNetUsage
        && afterNetUsage + 2 >= transactionNetUsage
        && transactionNetUsage > 200);

    Long beforeLatestConsumeTime = account.getLatestConsumeTime();

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    Long afterLatestConsumeTime = account.getLatestConsumeTime();
    //getLatestConsumeTime means this user latest use consume net Time,not current timestamp
    Assert.assertEquals(beforeLatestConsumeTime,afterLatestConsumeTime);
    beforeNetWindowSize = account.getNetWindowSize();
    logger.info("beforeNetWindowSize:" + beforeNetWindowSize);
    Assert.assertTrue(beforeNetWindowSize < 28800);
    beforeNetUsage = account.getNetUsage();

    Assert.assertTrue(beforeNetUsage > 200);
    txid = PublicMethed.sendcoinGetTransactionId(foundationAddress,1L,
        frozenBandwidthAddress,frozenBandwidthKey,blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    account = PublicMethed.queryAccount(frozenBandwidthAddress,blockingStubFull);
    Long afterNetWindowSize = account.getNetWindowSize();
    logger.info("afterNetWindowSize:" + afterNetWindowSize);
    Assert.assertTrue(
        afterNetWindowSize > beforeNetWindowSize
        && afterNetWindowSize <= 28795);
    afterNetUsage = account.getNetUsage();
    transactionNetUsage = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getNetUsage();
    Assert.assertTrue(afterNetUsage - beforeNetUsage <= transactionNetUsage
        && afterNetUsage - beforeNetUsage + 2 >= transactionNetUsage
        && transactionNetUsage > 200 && afterNetUsage > 400);




  }


  /**
   * constructor.
   */
  @Test(enabled = true, description = "EnergyUsage change energy_window_size when trigger is success")
  public void test02EnergyUsageChangeEnergyWindowSizeWithTriggerSuccess() throws Exception{
    Account account = PublicMethed.queryAccount(frozenEnergyAddress,blockingStubFull);
    AccountResourceMessage accountResourceMessage = PublicMethed.getAccountResource(frozenEnergyAddress,blockingStubFull);
    Long beforeEnergyUsage = account.getAccountResource().getEnergyUsage();
    Long beforeEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
    Long beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    Assert.assertTrue(beforeEnergyWindowSize == 28800);
    Assert.assertEquals(beforeEnergyUsage,beforeEnergyUsageFromAccountResource);
    Assert.assertTrue(beforeEnergyUsage == 0);


    Long cycleTime = 1L;
    String txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)", cycleTime.toString(), false,
        0, maxFeeLimit,frozenEnergyAddress, frozenEnergyKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenEnergyAddress,blockingStubFull);
    accountResourceMessage = PublicMethed.getAccountResource(frozenEnergyAddress,blockingStubFull);
    Long afterEnergyUsage = account.getAccountResource().getEnergyUsage();
    Long afterEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
    Assert.assertEquals(afterEnergyUsage,afterEnergyUsageFromAccountResource);
    Long transactionEnergyUsage = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertEquals(PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.SUCCESS);
    logger.info("transactionEnergyUsage:" + transactionEnergyUsage);
    Assert.assertTrue(afterEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage + 2 >= transactionEnergyUsage
        && transactionEnergyUsage > 600);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenEnergyAddress,blockingStubFull);
    beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyUsage:" + beforeEnergyUsage);
    Assert.assertTrue(beforeEnergyUsage < 28800);
    beforeEnergyUsage = account.getAccountResource().getEnergyUsage();

    Assert.assertTrue(beforeEnergyUsage > 600);
    txid = PublicMethed.triggerContract(contractAddress,
        "testUseCpu(uint256)",  cycleTime.toString(), false,
        0, 100000000L,frozenEnergyAddress, frozenEnergyKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.SUCCESS);
    account = PublicMethed.queryAccount(frozenEnergyAddress,blockingStubFull);
    Long afterEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyWindowSize:" + beforeEnergyWindowSize);
    logger.info("afterEnergyWindowSize:" + afterEnergyWindowSize);
    Assert.assertTrue(afterEnergyWindowSize <= 28795
        && afterEnergyWindowSize > beforeEnergyWindowSize);
    afterEnergyUsage = account.getAccountResource().getEnergyUsage();
    transactionEnergyUsage = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertTrue(afterEnergyUsage - beforeEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage - beforeEnergyUsage + 8 >= transactionEnergyUsage
        && transactionEnergyUsage > 600 && afterEnergyUsage > 600 * 2);




  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "EnergyUsage change energy_window_size when trigger is revert")
  public void test03EnergyUsageChangeEnergyWindowSizeWithTriggerRevert() throws Exception{
    Account account = PublicMethed.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
    AccountResourceMessage accountResourceMessage = PublicMethed.getAccountResource(frozenEnergyRevertAddress,blockingStubFull);
    Long beforeEnergyUsage = account.getAccountResource().getEnergyUsage();
    Long beforeEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
    Long beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    Assert.assertTrue(beforeEnergyWindowSize == 28800);
    Assert.assertEquals(beforeEnergyUsage,beforeEnergyUsageFromAccountResource);
    Assert.assertTrue(beforeEnergyUsage == 0);


    //Wrong method cause result is revert
    String txid = PublicMethed.triggerContract(contractAddress,
        "getRevertTransaction()", "", false,
        0, maxFeeLimit,frozenEnergyRevertAddress, frozenEnergyRevertKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


    account = PublicMethed.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
    accountResourceMessage = PublicMethed.getAccountResource(frozenEnergyRevertAddress,blockingStubFull);
    Long afterEnergyUsage = account.getAccountResource().getEnergyUsage();
    Long afterEnergyUsageFromAccountResource = accountResourceMessage.getEnergyUsed();
    Assert.assertEquals(afterEnergyUsage,afterEnergyUsageFromAccountResource);
    Long transactionEnergyUsage = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertEquals(PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.REVERT);
    logger.info("transactionEnergyUsage:" + transactionEnergyUsage);
    Assert.assertTrue(afterEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage + 2 >= transactionEnergyUsage
        && transactionEnergyUsage > 150);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    account = PublicMethed.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
    beforeEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyUsage:" + beforeEnergyUsage);
    Assert.assertTrue(beforeEnergyUsage < 28800);
    beforeEnergyUsage = account.getAccountResource().getEnergyUsage();

    Assert.assertTrue(beforeEnergyUsage > 50);
    txid = PublicMethed.triggerContract(contractAddress,
        "getRevertTransaction()",  "", false,
        0, maxFeeLimit,frozenEnergyRevertAddress, frozenEnergyRevertKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertEquals(PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getResult(), contractResult.REVERT);
    account = PublicMethed.queryAccount(frozenEnergyRevertAddress,blockingStubFull);
    Long afterEnergyWindowSize = account.getAccountResource().getEnergyWindowSize();
    logger.info("beforeEnergyWindowSize:" + beforeEnergyWindowSize);
    logger.info("afterEnergyWindowSize:" + afterEnergyWindowSize);
    Assert.assertTrue(afterEnergyWindowSize <= 28795
        && afterEnergyWindowSize > beforeEnergyWindowSize);
    afterEnergyUsage = account.getAccountResource().getEnergyUsage();
    transactionEnergyUsage = PublicMethed.getTransactionInfoById(txid,blockingStubFull)
        .get().getReceipt().getEnergyUsage();
    Assert.assertTrue(afterEnergyUsage - beforeEnergyUsage <= transactionEnergyUsage
        && afterEnergyUsage - beforeEnergyUsage + 8 >= transactionEnergyUsage
        && transactionEnergyUsage > 150 && afterEnergyUsage > 150 * 2);




  }

  /**
   * constructor.
   */
  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(frozenBandwidthAddress, frozenBandwidthKey, foundationAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


