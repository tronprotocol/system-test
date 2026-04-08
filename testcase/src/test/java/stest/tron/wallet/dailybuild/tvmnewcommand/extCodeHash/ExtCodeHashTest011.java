package stest.tron.wallet.dailybuild.tvmnewcommand.extCodeHash;

import static org.hamcrest.core.StringContains.containsString;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Hash;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class ExtCodeHashTest011 extends TronBaseTest {

  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] extCodeHashContractAddress = null;
  private byte[] testContractAddress = null;
  private String expectedCodeHash = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    PublicMethod.printAddress(user001Key);
  }

  @Test(enabled = true, description = "Deploy extcodehash contract", groups = {"contract", "daily"})
  public void test01DeployExtCodeHashContract() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
  String filePath = "./src/test/resources/soliditycode/extCodeHash11.sol";
  String contractName = "Counter";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    expectedCodeHash = ByteArray.toHexString(Hash.sha3(Hex.decode(code)));
    logger.info("expectedCodeHash: " + expectedCodeHash);
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    extCodeHashContractAddress = infoById.get().getContractAddress().toByteArray();

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    transferTokenTxid = PublicMethod.triggerContract(extCodeHashContractAddress,
        "getCodeHashByAddr()", "#", false, 0,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    Assert.assertTrue(infoById.get().getResultValue() != 0);
    Assert
        .assertThat(ByteArray
                .toStr(infoById.get().getResMessage().toByteArray()),
            containsString("REVERT opcode executed"));
  }


  @Test(enabled = true, description = "Deploy extcodehash contract", groups = {"contract", "daily"})
  public void test01DeployExtCodeHashContract1() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
  String filePath = "./src/test/resources/soliditycode/extCodeHash11.sol";
  String contractName = "Counter1";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    expectedCodeHash = ByteArray.toHexString(Hash.sha3(Hex.decode(code)));
    logger.info("expectedCodeHash: " + expectedCodeHash);
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    extCodeHashContractAddress = infoById.get().getContractAddress().toByteArray();

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
    transferTokenTxid = PublicMethod.triggerContract(extCodeHashContractAddress,
        "getCodeHashByAddr()", "#", false, 0,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    List<String> retList = PublicMethod
        .getStrings(transactionInfo.getContractResult(0).toByteArray());

    logger.info(
        "the value: " + retList);

    Assert.assertEquals(retList.get(1),
        retList.get(0));
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    extCodeHashContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(extCodeHashContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "Deploy extcodehash contract", groups = {"contract", "daily"})
  public void test01DeployExtCodeHashContract2() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
  String filePath = "./src/test/resources/soliditycode/extCodeHash11.sol";
  String contractName = "Counter2";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    expectedCodeHash = ByteArray.toHexString(Hash.sha3(Hex.decode(code)));
    logger.info("expectedCodeHash: " + expectedCodeHash);
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    extCodeHashContractAddress = infoById.get().getContractAddress().toByteArray();

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
  String num = "\"" + Base58.encode58Check(dev001Address) + "\"";

    transferTokenTxid = PublicMethod.triggerContract(extCodeHashContractAddress,
        "getCodeHashByAddr(address)", num, false, 0,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    List<String> retList = PublicMethod
        .getStrings(transactionInfo.getContractResult(0).toByteArray());

    logger.info(
        "the value: " + retList);
    Assert.assertEquals(retList.get(1),
        retList.get(0));
  }


  @Test(enabled = true, description = "Deploy extcodehash contract", groups = {"contract", "daily"})
  public void test01DeployExtCodeHashContract3() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
  String filePath = "./src/test/resources/soliditycode/extCodeHash11.sol";
  String contractName = "Counter2";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    expectedCodeHash = ByteArray.toHexString(Hash.sha3(Hex.decode(code)));
    logger.info("expectedCodeHash: " + expectedCodeHash);
  String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    extCodeHashContractAddress = infoById.get().getContractAddress().toByteArray();

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));
  String num = "\"" + Base58.encode58Check(dev001Address) + "\"";

    transferTokenTxid = PublicMethod.triggerContract(extCodeHashContractAddress,
        "getCodeHashByAddr(address)", num, false, 0,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    List<String> retList = PublicMethod
        .getStrings(transactionInfo.getContractResult(0).toByteArray());

    logger.info(
        "the value: " + retList);
    Assert.assertEquals(retList.get(1),
        retList.get(0));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 0, user001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 0, dev001Address, blockingStubFull);  }
}


