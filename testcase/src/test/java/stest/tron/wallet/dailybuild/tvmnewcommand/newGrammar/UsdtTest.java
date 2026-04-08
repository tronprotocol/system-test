package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
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
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class UsdtTest extends TronBaseTest {  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  private byte[] usdtAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  String dev58 = Base58.encode58Check(dev001Address);
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
    PublicMethod.freezeBalanceV2(testNetAccountAddress, 1000000000L, 0, testNetAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceForReceiver(testNetAccountAddress, 2000000000L,
        0, 1, ByteString.copyFrom(dev001Address), testNetAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String contractName = "Usdt-test";
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", 3000000000L,
            0, 70, 10000, "0", 0, null, dev001Key, dev001Address,
            blockingStubFull);
    logger.info("txid: " + txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);

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

  @Test(enabled = true, description = "Test 4.7.1.1 hot fix of trigger "
      + "has no balance and no energyLimit when transfer USDT", groups = {"contract", "daily"})
  public void test01() {

    Assert.assertTrue(PublicMethod
        .sendcoin(callerddress, 10000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod
        .sendcoin(testNetAccountAddress, 10000000L, callerddress, callerKey, blockingStubFull));
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + callerAddress58 + "\",100";
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    long origin_window_size_before = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
    argsStr = "\"" + dev58 + "\",1";
    txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("11111 infoById: " + infoById);
    logger.info("origin_window_size_before: " + origin_window_size_before);
    logger.info("origin_window_size_after: " + origin_window_size_after);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getNetUsage() > 0);


  }

  /**
   * constructor.
   */

  @Test(enabled = true, description = "Test 4.7.1.1 hot fix of trigger "
      + "has balance, no energyLimit and feeLimit is 0 when transfer USDT", groups = {"contract", "daily"})
  public void test02() {

    Assert.assertTrue(PublicMethod
        .sendcoin(callerddress, 10000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + dev58 + "\",1";
    long origin_window_size_before = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("22222 infoById: " + infoById);
    logger.info("origin_window_size_before: " + origin_window_size_before);
    logger.info("origin_window_size_after: " + origin_window_size_after);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getNetUsage() > 0);


  }

  @Test(enabled = true, description = "Test 4.7.1.1 hot fix of trigger has "
      + "no balance,no energyLimit,feeLimit is 0 when transfer USDT", groups = {"contract", "daily"})
  public void test03() {
    long balance = PublicMethod.queryAccount(callerddress, blockingStubFull).getBalance();
    PublicMethod.delegateResourceV2(testNetAccountAddress, 300000000L, 0, callerddress, testNetAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod
        .sendcoin(testNetAccountAddress, balance, callerddress, callerKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + dev58 + "\",1";
    long origin_window_size_before = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("22222 infoById: " + infoById);
    logger.info("origin_window_size_before: " + origin_window_size_before);
    logger.info("origin_window_size_after: " + origin_window_size_after);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getNetUsage() > 0);

  }

  @Test(enabled = true, description = "Test 4.7.1.1 hot fix of trigger has "
      + "no balance, has energyLimit,feeLimit is 0 when transfer USDT", groups = {"contract", "daily"})
  public void test04() {
    // no balance, has energyLimit,feeLimit is 0
    long balance = PublicMethod.queryAccount(callerddress, blockingStubFull).getBalance();
    PublicMethod.delegateResourceV2(testNetAccountAddress, 600000000L, 1, callerddress, testNetAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    if (balance != 0) {
      Assert.assertTrue(PublicMethod
          .sendcoin(testNetAccountAddress, balance, callerddress, callerKey, blockingStubFull));
    }
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + dev58 + "\",1";
    long origin_window_size_before = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("22222 infoById: " + infoById);
    logger.info("origin_window_size_before: " + origin_window_size_before);
    logger.info("origin_window_size_after: " + origin_window_size_after);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getNetUsage() > 0);

  }

  @Test(enabled = true, description = "Test 4.7.1.1 hot fix of trigger has "
      + "has balance and energyLimit, feeLimit is 0 when transfer USDT", groups = {"contract", "daily"})
  public void test05() {
    // has balance, has energyLimit,feeLimit is 0
    Assert.assertTrue(PublicMethod
        .sendcoin(callerddress,  1000000000L, testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String methedStr = "transfer(address,uint256)";
  String argsStr = "\"" + dev58 + "\",1";
    long origin_window_size_before = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
  String txid = PublicMethod.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerddress, callerKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethod.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("22222 infoById: " + infoById);
    logger.info("origin_window_size_before: " + origin_window_size_before);
    logger.info("origin_window_size_after: " + origin_window_size_after);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getNetUsage() > 0);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, testNetAccountAddress, blockingStubFull);
    PublicMethod.freeResource(callerddress, callerKey, testNetAccountAddress, blockingStubFull);
    PublicMethod.unDelegateResourceV2(testNetAccountAddress, 1000000000L, 1, dev001Address, testNetAccountKey, blockingStubFull);
    PublicMethod.unDelegateResourceV2(testNetAccountAddress, 300000000L, 0, callerddress, testNetAccountKey, blockingStubFull);  }
}


