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
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.api.GrpcAPI.TransactionExtention;
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
public class TransferFailed005 extends TronBaseTest {

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
    PublicMethod.printAddress(accountExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext().build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    {
      Assert.assertTrue(PublicMethod
          .sendcoin(accountExcAddress, 10000_000_000L, foundationAddress, foundationKey,
              blockingStubFull));
      PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
  String contractName = "EnergyOfTransferFailedTest";
      HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

      contractAddress = PublicMethod
          .deployContract(contractName, abi, code, "", maxFeeLimit, 100L, 100L, null, accountExcKey,
              accountExcAddress, blockingStubFull);

      filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
      contractName = "Caller";
      retMap = PublicMethod.getBycodeAbi(filePath, contractName);
      code = retMap.get("byteCode").toString();
      abi = retMap.get("abI").toString();

      contractAddress1 = PublicMethod
          .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100L, null, accountExcKey,
              accountExcAddress, blockingStubFull);
    }
  }

  @Test(enabled = false, description = "Deploy contract for trigger", groups = {"contract", "daily"})
  public void deployContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(accountExcAddress, 10000_000_000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100L, null, accountExcKey,
            accountExcAddress, blockingStubFull);
  String txid1 = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid1, blockingStubFull);
    contractAddress = infoById.get().getContractAddress().toByteArray();
    Assert.assertEquals(0, infoById.get().getResultValue());

    filePath = "src/test/resources/soliditycode/TransferFailed005.sol";
    contractName = "Caller";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();

    contractAddress1 = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100L, null, accountExcKey,
            accountExcAddress, blockingStubFull);
    txid1 = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100L,
            null, accountExcKey, accountExcAddress, blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    contractAddress1 = infoById.get().getContractAddress().toByteArray();
    logger.info("caller address : " + Base58.encode58Check(contractAddress1));
    Assert.assertEquals(0, infoById.get().getResultValue());
  }

  @Test(enabled = true, description = "TransferFailed for function call_value ", groups = {"contract", "daily"})
  public void triggerContract01() {
    Account info = null;

    AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(accountExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(accountExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  //Assert.assertTrue(PublicMethod
    //    .sendcoin(contractAddress, 1000100L, accountExcAddress, accountExcKey, blockingStubFull));
  //Assert.assertTrue(PublicMethod
    //    .sendcoin(contractAddress1, 1, accountExcAddress, accountExcKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info("contractAddress balance before: " + PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethod
        .queryAccount(contractAddress1, blockingStubFull).getBalance());
    long paramValue = 1;
  // transfer trx to self`s account
    String param = "\"" + paramValue + "\",\"" + Base58.encode58Check(contractAddress) + "\"";
  String triggerTxid = PublicMethod
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    Assert.assertEquals(infoById.get().getResultValue(), 1);
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
    Assert.assertEquals("TRANSFER_FAILED", infoById.get().getReceipt().getResult().toString());
    Assert.assertEquals("transfer trx failed: Cannot transfer TRX to yourself.",
        infoById.get().getResMessage().toStringUtf8());
    Assert.assertEquals(100L,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(0L,
        PublicMethod.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);
  // transfer trx to unactivate account
    ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] accountExcAddress2 = ecKey2.getAddress();
    param = "\"" + paramValue + "\",\"" + Base58.encode58Check(accountExcAddress2) + "\"";
    triggerTxid = PublicMethod
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(triggerTxid, blockingStubFull);

    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());

    Assert.assertEquals(99L,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(0L,
        PublicMethod.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);
  // transfer trx to caller, value enough , function success contractResult(call_value) successed
    param = "\"" + paramValue + "\",\"" + Base58.encode58Check(contractAddress1) + "\"";
    triggerTxid = PublicMethod
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(triggerTxid, blockingStubFull);
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
  int contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(1, contractResult);

    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertEquals(infoById.get().getResult().toString(), "SUCESS");
    Assert.assertEquals(98L,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1L,
        PublicMethod.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);
  // transfer trx to caller, value not enough, function success
    // but contractResult(call_value) failed
    param = "\"" + 100 + "\",\"" + Base58.encode58Check(contractAddress1) + "\"";
    triggerTxid = PublicMethod
        .triggerContract(contractAddress, "testCallTrxInsufficientBalance(uint256,address)", param,
            false, 0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
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
  //contractResult`s first boolean value
    contractResult = ByteArray.toInt(infoById.get().getContractResult(0).toByteArray());
    Assert.assertEquals(0, contractResult);
    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertEquals(infoById.get().getResult().toString(), "SUCESS");
    Assert.assertEquals(98L,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1L,
        PublicMethod.queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);


  }

  @Test(enabled = true, description = "TransferFailed for create", groups = {"contract", "daily"})
  public void triggerContract02() {
    final Long contractBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Account info = null;

    AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(accountExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(accountExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  //Assert.assertTrue(PublicMethod
    //    .sendcoin(contractAddress, 1000100L, accountExcAddress, accountExcKey, blockingStubFull));
  //Assert.assertTrue(PublicMethod
    //    .sendcoin(contractAddress1, 1, accountExcAddress, accountExcKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    logger.info("contractAddress balance before: " + PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethod
        .queryAccount(contractAddress1, blockingStubFull).getBalance());
    long paramValue = 1;
  String param = "\"" + paramValue + "\"";
  String triggerTxid = PublicMethod
        .triggerContract(contractAddress, "testCreateTrxInsufficientBalance(uint256)", param, false,
            0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
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

    logger.info("contractAddress balance before: " + PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethod
        .queryAccount(contractAddress1, blockingStubFull).getBalance());
    Assert.assertEquals(infoById.get().getResultValue(), 0);
    Assert.assertFalse(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertEquals(contractBalance - 1,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    param = "\"" + (contractBalance + 1) + "\"";
    triggerTxid = PublicMethod
        .triggerContract(contractAddress, "testCreateTrxInsufficientBalance(uint256)", param, false,
            0L, maxFeeLimit, accountExcAddress, accountExcKey, blockingStubFull);
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

    logger.info("contractAddress balance before: " + PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    logger.info("callerAddress balance before: " + PublicMethod
        .queryAccount(contractAddress1, blockingStubFull).getBalance());

    Assert.assertEquals(infoById.get().getResultValue(), 1);
    Assert.assertEquals(infoById.get().getResMessage().toStringUtf8(), "REVERT opcode executed");
    Assert.assertEquals(contractBalance - 1,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);


  }

  @Test(enabled = true, description = "TransferFailed for create2", groups = {"contract", "daily"})
  public void triggerContract03() {
    final Long contractBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(accountExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(accountExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  //Assert.assertTrue(PublicMethod
    //    .sendcoin(contractAddress, 15L, accountExcAddress, accountExcKey, blockingStubFull));
    logger.info("contractAddress balance before: " + PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance());
  String filePath = "./src/test/resources/soliditycode/TransferFailed007.sol";
  String contractName = "Caller";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String testContractCode = retMap.get("byteCode").toString();
  Long salt = 1L;
  String param = "\"" + testContractCode + "\"," + salt;
  String triggerTxid = PublicMethod
        .triggerContract(contractAddress, "deploy(bytes,uint256)", param, false, 0L, maxFeeLimit,
            accountExcAddress, accountExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

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
  Long afterBalance = 0L;
    afterBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
    logger.info("contractAddress balance after : " + PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals(contractBalance - 10L, afterBalance.longValue());
    Assert.assertFalse(infoById.get().getInternalTransactions(0).getRejected());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

    triggerTxid = PublicMethod
        .triggerContract(contractAddress, "deploy2(bytes,uint256)", param, false, 0L, maxFeeLimit,
            accountExcAddress, accountExcKey, blockingStubFull);
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

    afterBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull).getBalance();
    logger.info("contractAddress balance after : " + PublicMethod
        .queryAccount(contractAddress, blockingStubFull).getBalance());
    Assert.assertEquals(1, infoById.get().getResultValue());
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
    Assert.assertEquals(contractBalance - 10L, afterBalance.longValue());
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() < 10000000);

  }

  @Test(enabled = true, description = "Triggerconstant a transfer function", groups = {"contract", "daily"})
  public void triggerContract04() {
    Account account = PublicMethod.queryAccount(accountExcAddress, blockingStubFull);
    Account contractAccount = PublicMethod.queryAccount(contractAddress, blockingStubFull);
  final Long AccountBeforeBalance = account.getBalance();
  final Long contractAccountBalance = contractAccount.getBalance();

    TransactionExtention return1 = PublicMethod.triggerConstantContractForExtention(contractAddress,
        "testTransferTrxInsufficientBalance(uint256)", "1", false, 0L, 1000000000, "0", 0L,
        accountExcAddress, accountExcKey, blockingStubFull);
    Assert.assertEquals(response_code.SUCCESS, return1.getResult().getCode());
    /*Assert.assertEquals(
        "class org.tron.core.vm.program.Program$StaticCallModificationException "
            + ": Attempt to call a state modifying opcode inside STATICCALL",
        return1.getResult().getMessage().toStringUtf8());*/

    logger.info("return1: " + return1);

    account = PublicMethod.queryAccount(accountExcAddress, blockingStubFull);
    contractAccount = PublicMethod.queryAccount(contractAddress, blockingStubFull);

    Assert.assertEquals(AccountBeforeBalance.longValue(), account.getBalance());
    Assert.assertEquals(contractAccountBalance.longValue(), contractAccount.getBalance());
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
