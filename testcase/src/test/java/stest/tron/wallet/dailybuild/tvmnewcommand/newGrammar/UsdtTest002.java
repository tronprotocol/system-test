package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.Optional;
import stest.tron.wallet.common.client.utils.Flaky;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@Flaky(reason = "Contract deployment timing, intermittent null results",
    since = "2026-04-03")
@MultiNode
public class UsdtTest002 extends TronBaseTest {  private String fullnodeLocal = Configuration.getByPath("testng.conf")
    .getStringList("fullnode.ip.list").get(1);
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  private byte[] usdtAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String dev58 = Base58.encode58Check(dev001Address);
  private Long energyFee = 0L;
  private Long rightFeeLimit = 0L;
  int consumeUserResourcePercent = 70;
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] callerddress = ecKey.getAddress();
  String callerAddress58 = Base58.encode58Check(callerddress);
  String callerKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  String abi = Configuration.getByPath("testng.conf")
      .getString("abi.abi_usdt");
  String code = Configuration.getByPath("testng.conf")
      .getString("code.code_usdt");


  @BeforeClass(enabled = true)
  public void beforeClass() {    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
  String contractName = "Usdt-test";
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", 1500000000L,
            0, consumeUserResourcePercent, 10000, "0", 0,
            null, dev001Key, dev001Address, blockingStubFull);
    logger.info("deploy usdt txid: " + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);

    if (txid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    usdtAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod
        .getContract(usdtAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    logger.info(Base58.encode58Check(usdtAddress));
  }

  /**
   * constructor.
   */

  @Test(enabled = true, description = "args feeLimit = need feeLimit - 1", groups = {"contract", "daily"})
  public void test01() {

    Assert.assertTrue(PublicMethod
        .sendcoin(callerddress, 1000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //it cost more energy when transfer to an address which does not has usdt
    String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + callerAddress58 + "\",100";
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
  //transfer to callerAddress58 the second time to get feeLimit
    argsStr = "\"" + callerAddress58 + "\",1";
    txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    logger.info("test01 estimate feeLimit infoById: " + infoById.toString());

    long energyTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    energyFee = PublicMethod
        .getChainParametersValue(ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);
    rightFeeLimit = energyTotal * energyFee;
    long feeLimit = rightFeeLimit - 1;
  //transfer to callerAddress58 the third time with feeLimit == needFeeLimit - 1
    argsStr = "\"" + callerAddress58 + "\",1";
    txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, feeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("need feeLimit - 1  infoById: " + infoById);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertEquals(0, infoById.get().getReceipt().getOriginEnergyUsage());
    Assert.assertEquals(0, infoById.get().getReceipt().getEnergyUsage());
    Assert.assertEquals(0, infoById.get().getReceipt().getNetFee());
    Assert.assertEquals(0, infoById.get().getReceipt().getEnergyPenaltyTotal());
    Assert.assertEquals(energyFee * infoById.get().getReceipt().getEnergyUsageTotal(),
        infoById.get().getReceipt().getEnergyFee());
    Assert.assertEquals(infoById.get().getFee(), infoById.get().getReceipt().getEnergyFee());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyFee() < rightFeeLimit - 1);
    Assert.assertTrue(infoById.get().getResMessage().toStringUtf8().contains("Not enough energy for"));

    Protocol.Transaction transaction = PublicMethod.getTransactionById(txid, blockingStubFull).get();
    Assert.assertEquals(feeLimit, transaction.getRawData().getFeeLimit());
    Assert.assertEquals("OUT_OF_ENERGY", transaction.getRet(0).getContractRet().toString());

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "transfer usdt, no energy, and trx balance = feeLimit", groups = {"contract", "daily"})
  public void test02() {
    Assert.assertTrue(PublicMethod.freezeBalanceV2(dev001Address, 100000000L, 0, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long balance = PublicMethod.queryAccount(dev001Address, blockingStubFull).getBalance();
    long needSend = balance - rightFeeLimit;
    Assert.assertTrue(PublicMethod.sendcoin(callerddress, needSend, dev001Address, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + callerAddress58 + "\",1";
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, rightFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionInfo infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.getReceipt().getResult());
    Assert.assertEquals(energyFee * infoById.getReceipt().getEnergyUsageTotal(), infoById.getReceipt().getEnergyFee());
    Assert.assertEquals(0, infoById.getReceipt().getOriginEnergyUsage());
    Assert.assertEquals(388, infoById.getReceipt().getNetUsage());
    Assert.assertEquals(0, infoById.getReceipt().getEnergyUsage());
    Assert.assertEquals(0, infoById.getReceipt().getNetFee());
    Assert.assertEquals(0, infoById.getReceipt().getEnergyPenaltyTotal());
    Protocol.Transaction transaction = PublicMethod.getTransactionById(txid, blockingStubFull).get();
    Assert.assertEquals(rightFeeLimit.longValue(), transaction.getRawData().getFeeLimit());
    Assert.assertEquals("SUCCESS", transaction.getRet(0).getContractRet().toString());

  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "transfer usdt, no energy, and trx balance = feeLimit - 1", groups = {"contract", "daily"})
  public void test03() {

    long balance = PublicMethod.queryAccount(dev001Address, blockingStubFull).getBalance();
    long needSend = rightFeeLimit - balance - 1;
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, needSend, testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + callerAddress58 + "\",1";
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, rightFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertEquals(0, infoById.get().getReceipt().getOriginEnergyUsage());
    Assert.assertEquals(0, infoById.get().getReceipt().getEnergyUsage());
    Assert.assertEquals(0, infoById.get().getReceipt().getNetFee());
    Assert.assertEquals(0, infoById.get().getReceipt().getEnergyPenaltyTotal());
    Assert.assertEquals(infoById.get().getFee(), infoById.get().getReceipt().getEnergyFee());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyFee() < rightFeeLimit);
    Assert.assertTrue(infoById.get().getResMessage().toStringUtf8().contains("Not enough energy for"));
    Protocol.Transaction transaction = PublicMethod.getTransactionById(txid, blockingStubFull).get();
    Assert.assertEquals(rightFeeLimit.longValue(), transaction.getRawData().getFeeLimit());
    Assert.assertEquals("OUT_OF_ENERGY", transaction.getRet(0).getContractRet().toString());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "originEnergy from 0 to enough", groups = {"contract", "daily"})
  public void test04() {
    //origin address has no frozen balance for energy
    String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + dev58 + "\",1";
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("test04 no originEnergy: " + infoById.get().toString());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    long originEnergyUsage = infoById.get().getReceipt().getOriginEnergyUsage();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    long netUsage = infoById.get().getReceipt().getNetUsage();
    long energyFeeCost = infoById.get().getReceipt().getEnergyFee();
    Assert.assertEquals(energyFee * energyUsageTotal, energyFeeCost);
    Assert.assertEquals(0, originEnergyUsage);
    Assert.assertEquals(389, netUsage);
    Assert.assertEquals(0, infoById.get().getReceipt().getEnergyUsage());
    Assert.assertEquals(0, infoById.get().getReceipt().getNetFee());
    Assert.assertEquals(0, infoById.get().getReceipt().getEnergyPenaltyTotal());
    Protocol.Transaction transaction = PublicMethod.getTransactionById(txid, blockingStubFull).get();
    Assert.assertEquals(maxFeeLimit, transaction.getRawData().getFeeLimit());
    Assert.assertEquals("SUCCESS", transaction.getRet(0).getContractRet().toString());
  //origin address freeze balance to get energy
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 300000000L, testNetAccountAddress, testNetAccountKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceV2(dev001Address, 250000000L, 1, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //origin address has enough frozen energy
    txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("test04 enough originEnergy: " + infoById.get().toString());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());
    originEnergyUsage = infoById.get().getReceipt().getOriginEnergyUsage();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    netUsage = infoById.get().getReceipt().getNetUsage();
    energyFeeCost = infoById.get().getReceipt().getEnergyFee();
    Assert.assertEquals(Math.round(energyUsageTotal * (100 - consumeUserResourcePercent) / 100), originEnergyUsage);
    Assert.assertEquals(389, netUsage);
    Assert.assertEquals(energyFee * (energyUsageTotal - originEnergyUsage), energyFeeCost);
    Assert.assertEquals(0, infoById.get().getReceipt().getNetFee());
    Assert.assertEquals(0, infoById.get().getReceipt().getEnergyPenaltyTotal());
    transaction = PublicMethod.getTransactionById(txid, blockingStubFull).get();
    Assert.assertEquals(maxFeeLimit, transaction.getRawData().getFeeLimit());
    Assert.assertEquals("SUCCESS", transaction.getRet(0).getContractRet().toString());
  }

  /**
   * constructor.
   */
  @Test(enabled = true, description = "change consumeUserResourcePercent from 70 to 80", groups = {"contract", "daily"})
  public void test05() {
    consumeUserResourcePercent = 80;
    Assert.assertTrue(PublicMethod.updateSetting(usdtAddress, consumeUserResourcePercent, dev001Key, dev001Address, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long temPercent = PublicMethod.getContract(usdtAddress, blockingStubFull).getConsumeUserResourcePercent();
    Assert.assertEquals(consumeUserResourcePercent, temPercent);
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + dev58 + "\",1";
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    TransactionInfo infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info("test05 consumer Percent is 80 : " + infoById.toString());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.getReceipt().getResult());
    long originEnergyUsage = infoById.getReceipt().getOriginEnergyUsage();
    long energyUsageTotal = infoById.getReceipt().getEnergyUsageTotal();
    long netUsage = infoById.getReceipt().getNetUsage();
    long energyFeeCost = infoById.getReceipt().getEnergyFee();
    Assert.assertEquals(Math.round(energyUsageTotal * (100 - consumeUserResourcePercent) / 100), originEnergyUsage);
    Assert.assertEquals(389, netUsage);
    Assert.assertEquals(energyFee * (energyUsageTotal - originEnergyUsage), energyFeeCost);
    Assert.assertEquals(0, infoById.getReceipt().getNetFee());
    Assert.assertEquals(0, infoById.getReceipt().getEnergyPenaltyTotal());
    Protocol.Transaction transaction = PublicMethod.getTransactionById(txid, blockingStubFull).get();
    Assert.assertEquals(maxFeeLimit, transaction.getRawData().getFeeLimit());
    Assert.assertEquals("SUCCESS", transaction.getRet(0).getContractRet().toString());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.unFreezeBalanceV2(dev001Address,dev001Key,50000000L,1, blockingStubFull);
    PublicMethod.unFreezeBalanceV2(dev001Address,dev001Key,100000000L,0, blockingStubFull);
    PublicMethod.freeResource(dev001Address, dev001Key, testNetAccountAddress, blockingStubFull);
    PublicMethod.freeResource(callerddress, callerKey, testNetAccountAddress, blockingStubFull);  }
}


