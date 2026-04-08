package stest.tron.wallet.dailybuild.tvmnewcommand.isContract;

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
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
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
public class isContractCommand001 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  byte[] contractA = null;
  byte[] contractC = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1).usePlaintext().build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    PublicMethod
        .sendcoin(contractExcAddress, 1000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TvmIsContract001.sol";
  String contractName = "testIsContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);


    contractName = "C";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractC = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }


  @Test(enabled = true, description = "Correct contract address test", groups = {"contract", "daily"})
  public void test01CorrectContractAddress() {
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
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
  String num = "\"" + Base58.encode58Check(contractAddress) + "\"";
    txid = PublicMethod
        .triggerContract(contractAddress, "testIsContractCommand(address)", num, false, 0,
            maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
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
    Protocol.Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "testIsContractView(address)", num,
            false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Account address test", groups = {"contract", "daily"})
  public void test02AccountAddress() {
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
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
  String num = "\"" + Base58.encode58Check(contractExcAddress) + "\"";
    txid = PublicMethod
        .triggerContract(contractAddress, "testIsContractCommand(address)", num, false, 0,
            maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
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

    Protocol.Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "testIsContractView(address)", num,
            false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Nonexistent account address test", groups = {"contract", "daily"})
  public void test03NonexistentAddress() {
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
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
  String num = "\"" + Base58.encode58Check(nonexistentAddress) + "\"";
    txid = PublicMethod
        .triggerContract(contractAddress, "testIsContractCommand(address)", num, false, 0,
            maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(0, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
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
    Protocol.Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "testIsContractView(address)", num,
            false, 0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Constructor return test", groups = {"contract", "daily"})
  public void test04ConstructorReturn() {
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
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
    txid = PublicMethod
        .triggerContract(contractAddress, "testConstructor()", "", false, 0, maxFeeLimit,
            contractExcAddress, contractExcKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(1, ByteArray.toInt(infoById.get().getContractResult(0).toByteArray()));
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
    Protocol.Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "testConstructorView()", "", false, 0,
            0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }


  @Test(enabled = true, description = "active contract address before deploy contract,then deploy will fail", groups = {"contract", "daily"})
  public void test05ActiveContractBeforeDeploy() {
    String filePath = "src/test/resources/soliditycode/TvmIsContract001.sol";
  String contractName = "testIsContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    Protocol.Transaction transaction = PublicMethod
        .deployContractWithoutBroadcast(contractName, abi, code, "", maxFeeLimit, 0L,
            100, 1000L, "0", 0L,
            null, contractExcKey, contractExcAddress, blockingStubFull);
  byte[] contractAdd = PublicMethod.generateContractAddress(transaction, contractExcAddress);
    Assert.assertTrue(PublicMethod.sendcoin(contractAdd, 1L, testNetAccountAddress,
        testNetAccountKey, blockingStubFull ));
  //Code = CONTRACT_VALIDATE_ERROR
    //Message = Contract validate error : Trying to create a contract with existing contract address
    GrpcAPI.Return response = PublicMethod.broadcastTransaction(transaction, blockingStubFull);
    Assert.assertEquals("CONTRACT_VALIDATE_ERROR", response.getCode().name());
    Assert.assertFalse(response.getResult());
  }

  @Test(enabled = true, description = "deploy A in create2, A trigger B's iscontract in it's constructor", groups = {"contract", "daily"})
  public void test06TriggerInCreate2Constructor() {
    String contractName = "A";
  String filePath = "src/test/resources/soliditycode/TvmIsContract001.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String hexB = ByteArray.toHexString(contractAddress);
  String testContractCode = code;
    testContractCode += "000000000000000000000000" + hexB;
  Long salt = 7L;
  String param = "\"" + testContractCode + "\"," + salt;
  String triggerTxid = PublicMethod.triggerContract(contractC,
        "create2(bytes,uint256)", param, false, 0L,
        1000000000L, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);
  String hexA = "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(24);
    logger.info("hexA: " + hexA);
    contractA = ByteArray.fromHexString(hexA);
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractA, "testConstructorView()", "", false, 0,
            0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("trigger A testConstructorView : " + transactionExtention.toString());
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "deploy A in create, A trigger B's iscontract in it's constructor", groups = {"contract", "daily"})
  public void test07TriggerInCreateConstructor() {
    String contractName = "A";
  String filePath = "src/test/resources/soliditycode/TvmIsContract001.sol";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String hexB = ByteArray.toHexString(contractAddress);
  String testContractCode = code;
    testContractCode += "000000000000000000000000" + hexB;
  String param = "\"" + testContractCode + "\"";
  String triggerTxid = PublicMethod.triggerContract(contractC,
        "create(bytes)", param, false, 0L,
        1000000000L, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);
  String hexA = "41" + ByteArray.toHexString(info.get().getContractResult(0).toByteArray()).substring(24);
    logger.info("hexA: " + hexA);
    contractA = ByteArray.fromHexString(hexA);
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractA, "testConstructorView()", "", false, 0,
            0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("trigger A testConstructorView : " + transactionExtention.toString());
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }


  @Test(enabled = true, description = "A call B's iscontract in it's constructor", groups = {"contract", "daily"})
  public void test08ConstructorCall() {
    String filePath = "src/test/resources/soliditycode/TvmIsContract001.sol";
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String param = "\"" + Base58.encode58Check(contractAddress) + "\"";
  String txid = PublicMethod
        .deployContractWithConstantParame(contractName, abi, code, "constructor(address)",param,null,
            maxFeeLimit, 0L, 100,
            null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(info.get().getResultValue() == 0);
  byte[] aContract = info.get().getContractAddress().toByteArray();

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(aContract, "testConstructorView()", "", false, 0,
            0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("test08 A call B in constructor :" + transactionExtention.toString());
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethod.queryAccount(contractExcKey, blockingStubFull).getBalance();
    PublicMethod.sendcoin(testNetAccountAddress, balance, contractExcAddress, contractExcKey,
        blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
