package stest.tron.wallet.dailybuild.tvmnewcommand.transferfailed;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class TransferFailed007 extends TronBaseTest {

  byte[] contractAddress = null;
  byte[] contractAddress1 = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] accountExcAddress = ecKey1.getAddress();
  String accountExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(accountExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

  }

  @Test(enabled = false, description = "Deploy contract for trigger", groups = {"contract", "daily"})
  public void deployContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(accountExcAddress, 10000000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TransferFailed007.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid1 = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid1, blockingStubFull);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = false, description = "TransferFailed for create2", groups = {"contract", "daily"})
  public void triggerContract() {
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(accountExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(accountExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    Assert.assertTrue(PublicMethod
        .sendcoin(contractAddress, 15L, accountExcAddress, accountExcKey, blockingStubFull));
    logger.info(
        "contractAddress balance before: " + PublicMethod
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
  String filePath = "./src/test/resources/soliditycode/TransferFailed007.sol";
  String contractName = "Caller";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String testContractCode = retMap.get("byteCode").toString();
  Long salt = 1L;
  String param = "\"" + testContractCode + "\"," + salt;
  String triggerTxid = PublicMethod.triggerContract(contractAddress,
        "deploy(bytes,uint256)", param, false, 0L,
        maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
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

    long afterBalance = 0L;
    afterBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    logger.info(
        "contractAddress balance after : " + PublicMethod
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals(5L, afterBalance);
    Assert.assertFalse(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    triggerTxid = PublicMethod.triggerContract(contractAddress,
        "deploy(bytes,uint256)", param, false, 0L,
        maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);

    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    fee = infoById.get().getFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    energyUsed = infoById.get().getReceipt().getEnergyUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);

    afterBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
    logger.info(
        "contractAddress balance after : " + PublicMethod
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals(5L, afterBalance);
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

  }

  /**
   * constructor.
   */
  @AfterClass

  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(accountExcAddress, accountExcKey, foundationAddress, blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }
}
