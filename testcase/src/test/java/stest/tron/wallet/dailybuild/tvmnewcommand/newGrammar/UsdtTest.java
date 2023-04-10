package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class UsdtTest {

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
    .getStringList("fullnode.ip.list").get(0);
  private long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
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
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(dev001Address, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.freezeBalanceV2(testNetAccountAddress, 4000000000L, 1, testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceForReceiver(testNetAccountAddress, 2000000000L,
        0, 1, ByteString.copyFrom(dev001Address), testNetAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String contractName = "Usdt-test";
    String txid = PublicMethed
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", 3000000000L,
            0, 70, 10000, "0", 0, null, dev001Key, dev001Address,
            blockingStubFull);
    logger.info("txid: " + txid);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed
        .getTransactionInfoById(txid, blockingStubFull);

    if (txid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }

    usdtAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethed
        .getContract(usdtAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    logger.info(Base58.encode58Check(usdtAddress));
  }

  /**
   * constructor.
   */

  @Test(enabled = true, description = "Test 4.7.1.1 hot fix of trigger " +
      "has no balance and no energyLimit when transfer USDT")
  public void test01() {

    Assert.assertTrue(PublicMethed
        .sendcoin(callerddress, 10000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethed
        .sendcoin(testNetAccountAddress, 10000000L, callerddress, callerKey, blockingStubFull));
    String methedStr = "transfer(address,uint256)";
    String argsStr = "\"" + callerAddress58 + "\",100";
    String txid = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    long origin_window_size_before = PublicMethed.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
    argsStr = "\"" + dev58 + "\",1";
    txid = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, maxFeeLimit, callerddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethed.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
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

  @Test(enabled = true, description = "Test 4.7.1.1 hot fix of trigger " +
      "has no no energyLimit and feeLimit is 0 when transfer USDT")
  public void test02() {

    Assert.assertTrue(PublicMethed
        .sendcoin(callerddress, 10000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String methedStr = "transfer(address,uint256)";
    String argsStr = "\"" + dev58 + "\",1";
    long origin_window_size_before = PublicMethed.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
    String txid = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethed.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("22222 infoById: " + infoById);
    logger.info("origin_window_size_before: " + origin_window_size_before);
    logger.info("origin_window_size_after: " + origin_window_size_after);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getNetUsage() > 0);


  }

  @Test(enabled = false, description = "orgin_energy_used>0, caller.balance > 0, caller.energyLimit=0,feelimit=0")
  public void test03() {

    Assert.assertTrue(PublicMethed
        .sendcoin(callerddress, 10000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String methedStr = "transfer(address,uint256)";
    String argsStr = "\"" + dev58 + "\",1";
    long origin_window_size_before = PublicMethed.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();
    String txid = PublicMethed.triggerContract(usdtAddress, methedStr, argsStr,
        false, 0, 0, callerddress, callerKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    long origin_window_size_after = PublicMethed.queryAccount(dev001Address,blockingStubFull)
        .getAccountResource().getEnergyWindowSize();

    Optional<TransactionInfo> infoById = PublicMethed.getTransactionInfoById(txid, blockingStubFull);
    logger.info("22222 infoById: " + infoById);
    logger.info("origin_window_size_before: " + origin_window_size_before);
    logger.info("origin_window_size_after: " + origin_window_size_after);
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.OUT_OF_ENERGY, infoById.get().getReceipt().getResult());
//    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() == 0);
//    Assert.assertTrue(infoById.get().getReceipt().getOriginEnergyUsage() == 0);
//    Assert.assertTrue(infoById.get().getReceipt().getNetUsage() > 0);


  }
  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(dev001Address, dev001Key, testNetAccountAddress, blockingStubFull);
    PublicMethed.freedResource(callerddress, callerKey, testNetAccountAddress, blockingStubFull);
    PublicMethed.unDelegateResourceV2(testNetAccountAddress, 1000000000L, 1, dev001Address, testNetAccountKey, blockingStubFull);

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

  }
}


