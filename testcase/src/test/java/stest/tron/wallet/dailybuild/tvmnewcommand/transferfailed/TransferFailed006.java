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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class TransferFailed006 extends TronBaseTest {

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
  String filePath = "src/test/resources/soliditycode/TransferFailed006.sol";
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

    filePath = "src/test/resources/soliditycode/TransferFailed006.sol";
    contractName = "Caller";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();

    txid1 = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid1, blockingStubFull);
    contractAddress1 = infoById.get().getContractAddress().toByteArray();
    logger.info("caller address : " + Base58.encode58Check(contractAddress1));
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = false, description = "TransferFailed for create", groups = {"contract", "daily"})
  public void triggerContract() {
    Account info = null;

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
        .sendcoin(contractAddress, 1000100L, accountExcAddress, accountExcKey, blockingStubFull));
    Assert.assertTrue(PublicMethod
        .sendcoin(contractAddress1, 1, accountExcAddress, accountExcKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info(
        "contractAddress balance before: " + PublicMethod
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    logger.info(
        "callerAddress balance before: " + PublicMethod
            .queryAccount(contractAddress1, blockingStubFull)
            .getBalance());
    long paramValue = 1000000;
  String param = "\"" + paramValue + "\"";
  String triggerTxid = PublicMethod.triggerContract(contractAddress,
        "testCreateTrxInsufficientBalance(uint256)", param, false, 0L,
        maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(infoById.get().getReceipt().getResult() + "");
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

    logger.info(
        "contractAddress balance before: " + PublicMethod
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    logger.info(
        "callerAddress balance before: " + PublicMethod
            .queryAccount(contractAddress1, blockingStubFull)
            .getBalance());
    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertFalse(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertEquals(100L,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    triggerTxid = PublicMethod.triggerContract(contractAddress,
        "testCreateTrxInsufficientBalance(uint256)", param, false, 0L,
        maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(triggerTxid, blockingStubFull);
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

    logger.info(
        "contractAddress balance before: " + PublicMethod
            .queryAccount(contractAddress, blockingStubFull)
            .getBalance());
    logger.info(
        "callerAddress balance before: " + PublicMethod
            .queryAccount(contractAddress1, blockingStubFull)
            .getBalance());

    Assert.assertEquals(infoById.get().getResultValue(), 1);
    Assert.assertEquals(infoById.get().getResMessage().toStringUtf8(), "REVERT opcode executed");
    Assert.assertEquals(100L,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1L,
        PublicMethod.queryAccount(contractAddress1, blockingStubFull).getBalance());
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
