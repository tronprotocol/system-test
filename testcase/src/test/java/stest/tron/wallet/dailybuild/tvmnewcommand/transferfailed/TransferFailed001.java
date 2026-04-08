package stest.tron.wallet.dailybuild.tvmnewcommand.transferfailed;

import static org.tron.protos.Protocol.TransactionInfo.code.FAILED;

import com.google.protobuf.ByteString;
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
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.EnergyCost;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class TransferFailed001 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    }

  @Test(enabled = true, description = "Transfer trx insufficient balance", groups = {"contract", "daily"})
  public void test001TransferTrxInsufficientBalance() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        2000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";
  String num = "2000001";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxInsufficientBalance(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("infoById:" + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);


  }


  @Test(enabled = true, description = "Transfer balance enough", groups = {"contract", "daily"})
  public void test002TransferEnough() {

    //Assert.assertTrue(PublicMethod
    //    .sendcoin(contractAddress, 3000000L, testNetAccountAddress, testNetAccountKey,
    //        blockingStubFull));
  //PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";
  String num = "1";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxInsufficientBalance(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    logger.info("infoById" + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee - 1 == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);


  }


  @Test(enabled = true, description = "Transfer trx nonexistent target", groups = {"contract", "daily"})
  public void test003TransferTrxNonexistentTarget() {

    //Assert.assertTrue(PublicMethod
    //    .sendcoin(contractAddress, 1000000L, testNetAccountAddress, testNetAccountKey,
    //        blockingStubFull));

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  String txid = "";
  String num = "1" + ",\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxNonexistentTarget(uint256,address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Account nonexistentAddressAccount = PublicMethod
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(1, nonexistentAddressAccount.getBalance());
    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxNonexistentTarget(uint256,address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);

    fee = infoById.get().getFee();
    netUsed = infoById.get().getReceipt().getNetUsage();
    energyUsed = infoById.get().getReceipt().getEnergyUsage();
    netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal2 = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal2);

    infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
    afterBalance = infoafter.getBalance();
    afterEnergyUsed = resourceInfoafter.getEnergyUsed();
    afterNetUsed = resourceInfoafter.getNetUsed();
    afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    nonexistentAddressAccount = PublicMethod
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(2, nonexistentAddressAccount.getBalance());
    Assert.assertEquals(0, infoById.get().getResultValue());

    Assert.assertEquals(energyUsageTotal2 + EnergyCost.getNewAcctCall(),
        energyUsageTotal);

  }


  @Test(enabled = true, description = "Transfer trx to myself", groups = {"contract", "daily"})
  public void test004TransferTrxSelf() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";
  String num = "1";

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxSelf(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.TRANSFER_FAILED, infoById.get().getReceipt().getResult());
    Assert.assertEquals("transfer trx failed: Cannot transfer TRX to yourself.",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);


  }


  @Test(enabled = true, description = "Transfer trx nonexistent target and insufficient balance", groups = {"contract", "daily"})
  public void test005TransferTrxNonexistentTarget() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  String txid = "";
  String num = "10000000" + ",\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxNonexistentTarget(uint256,address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);


  }


  @Test(enabled = true, description = "Transfer trx to myself and insufficient balance", groups = {"contract", "daily"})
  public void test006TransferTrxSelf() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  String txid = "";
  String num = "1000000000";

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxSelf(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(contractResult.REVERT, infoById.get().getReceipt().getResult());
    Assert.assertEquals(
        "REVERT opcode executed",
        ByteArray.toStr(infoById.get().getResMessage().toByteArray()));

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertNotEquals(10000000, energyUsageTotal);


  }

  @Test(enabled = true, description = "PreCompiled transfertoken with value,"
      + " long.max < value or long.min > value", groups = {"contract", "daily"})
  public void test007TransferTrckenPreCompiled() {

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    Account info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String txid = "";
  String num = "1";

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenCompiledLongMax1()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
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

    resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    beforeBalance = info.getBalance();
    beforeEnergyUsed = resourceInfo.getEnergyUsed();
    beforeNetUsed = resourceInfo.getNetUsed();
    beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertTrue(energyUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals("REVERT opcode executed", infoById.get().getResMessage().toStringUtf8());

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenCompiledLongMin1()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
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

    resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
    beforeBalance = info.getBalance();
    beforeEnergyUsed = resourceInfo.getEnergyUsed();
    beforeNetUsed = resourceInfo.getNetUsed();
    beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertTrue(energyUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals("REVERT opcode executed", infoById.get().getResMessage().toStringUtf8());

  }

  @Test(enabled = false, description = "PreCompiled tokenbalance", groups = {"contract", "daily"})
  public void test008TransferTrctoken() {

    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  String description = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetUrl");

    ByteString assetAccountId = null;
  final long TotalSupply = 10000000L;
    long now = System.currentTimeMillis();
  String tokenName = "testAssetIssue_" + Long.toString(now);
  //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethod.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethod.queryAccount(contractExcAddress, blockingStubFull)
        .getAssetIssuedID();
  String filePath = "src/test/resources/soliditycode/TransferFailed001.sol";
  String contractName = "EnergyOfTransferFailedTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod.transferAsset(contractAddress,
        assetAccountId.toByteArray(), 100L, contractExcAddress, contractExcKey,
        blockingStubFull));
  Long returnAddressBytesAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("returnAddressBytesAccountCountBefore : " + returnAddressBytesAccountCountBefore);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  String num =
        "\"" + Base58.encode58Check(contractAddress) + "\"," + "\"" + assetAccountId.toStringUtf8()
            + "\"";
  //String num = "\""+Base58.encode58Check(contractAddress) +"\","+ "\"" + -1 + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenTest(address,uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(energyUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals(100, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));


  }

  @Test(enabled = true, description = "PreCompiled address(0x1) query tokenbalance", groups = {"contract", "daily"})
  public void test009TransferTrctoken() {
    //address: 410000000000000000000000000000000000000001
    String addressx = "T9yD14Nj9j7xAB4dbGeiX9h8unkKLxmGkn";
  byte[] addressxx = WalletClient.decodeFromBase58Check(addressx);

    Assert.assertTrue(PublicMethod
        .sendcoin(addressxx, 1000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  String description = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.assetUrl");

    ByteString assetAccountId = null;
  final long TotalSupply = 10000000L;
    long now = System.currentTimeMillis();
  String tokenName = "testAssetIssue_" + Long.toString(now);
  //Create a new AssetIssue success.
    Assert
        .assertTrue(PublicMethod.createAssetIssue(contractExcAddress, tokenName, TotalSupply, 1,
            10000, start, end, 1, description, url, 100000L,
            100000L, 1L, 1L, contractExcKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethod.queryAccount(contractExcAddress, blockingStubFull)
        .getAssetIssuedID();

    Assert.assertTrue(PublicMethod.transferAsset(addressxx,
        assetAccountId.toByteArray(), 100L, contractExcAddress, contractExcKey,
        blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long returnAddressBytesAccountCountBefore = PublicMethod
        .getAssetIssueValue(addressxx, assetAccountId, blockingStubFull);
    logger.info("returnAddressBytesAccountCountBefore : " + returnAddressBytesAccountCountBefore);
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long testNetAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountBefore = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("testNetAccountCountBefore:" + testNetAccountCountBefore);
    logger.info("contractAccountCountBefore:" + contractAccountCountBefore);
  String txid = "";
  //String num = "\""+Base58.encode58Check(contractAddress)
    // +"\","+ "\"" + assetAccountId.toStringUtf8() + "\"";
  String num = "\"" + assetAccountId.toStringUtf8() + "\"";
  //String num = "\""+Base58.encode58Check(contractAddress) +"\","+ "\"" + -1 + "\"";
    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTokenCompiledTokenId(uint256)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long testNetAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractExcAddress, assetAccountId, blockingStubFull);
  Long contractAccountCountAfter = PublicMethod
        .getAssetIssueValue(contractAddress, assetAccountId, blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("testNetAccountCountAfter:" + testNetAccountCountAfter);
    logger.info("contractAccountCountAfter:" + contractAccountCountAfter);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(energyUsageTotal < maxFeeLimit / 10);
    Assert.assertEquals(100, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "transferTrx to nonexistent target ,but revert", groups = {"contract", "daily"})
  public void test010TransferRevert() {

    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  String txid = "";
  String num = "1" + ",\"" + Base58.encode58Check(nonexistentAddress) + "\"";

    txid = PublicMethod.triggerContract(contractAddress,
        "testTransferTrxrevert(uint256,address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
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

    Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);

    Account nonexistentAddressAccount = PublicMethod
        .queryAccount(nonexistentAddress, blockingStubFull1);
    Assert.assertEquals(0, nonexistentAddressAccount.getBalance());
    Assert.assertEquals(1, infoById.get().getResultValue());

    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    Assert.assertTrue(energyUsageTotal > EnergyCost.getNewAcctCall());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }


}
