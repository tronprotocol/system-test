package stest.tron.wallet.dailybuild.tvmnewcommand.create2;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class Create2Test005 extends TronBaseTest {

  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] factoryContractAddress = null;
  private byte[] testContractAddress = null;
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

  @Test(enabled = false, description = "Deploy factory contract", groups = {"contract", "daily"})
  public void test01DeployFactoryContract() {
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
        PublicMethod.getFreezeBalanceCount(dev001Address, dev001Key, 170000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress, 10_000_000L + PublicMethod.randomFreezeAmount.getAndAdd(1),
        0, 0, ByteString.copyFrom(dev001Address), testKey002, blockingStubFull));

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
  String filePath = "./src/test/resources/soliditycode/create2contract.sol";
  String contractName = "Factory";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    factoryContractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(factoryContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = false, description = "Trigger create2 command with 0 extended bytecode", groups = {"contract", "daily"})
  public void test02TriggerCreate2ToDeployTestContract() {
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
        PublicMethod.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethod.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devEnergyLimitBefore is " + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, devEnergyUsageBefore is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethod.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userEnergyLimitBefore is " + Long.toString(userEnergyLimitBefore));
    logger.info("before trigger, userEnergyUsageBefore is " + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));
  Long callValue = Long.valueOf(0);
  String testContractCode = "608060405234801561001057600080fd5b50d3801561001d57600080fd5b50d2801"
        + "561002a57600080fd5b5060c9806100396000396000f3fe6080604052348015600f57600080fd5b50d38015"
        + "601b57600080fd5b50d28015602757600080fd5b50600436106066577c01000000000000000000000000000"
        + "00000000000000000000000000000600035046368e5c0668114606b578063e5aa3d58146083575b600080fd"
        + "5b60716089565b60408051918252519081900360200190f35b60716097565b6000805460010190819055905"
        + "65b6000548156fe0000000000000000000000000000000000000000000000000000000000000000000000";
  Long salt = 1L;
  String param = "\"" + testContractCode + "\"," + salt;
  final String triggerTxid = PublicMethod.triggerContract(factoryContractAddress,
        "deploy(bytes,uint256)", param, false, callValue,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethod.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethod.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("after trigger, userEnergyLimitAfter is " + Long.toString(userEnergyLimitAfter));
    logger.info("after trigger, userEnergyUsageAfter is " + Long.toString(userEnergyUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    if (infoById.get().getResultValue() != 0) {
      Assert.fail(
          "transaction failed with message: " + infoById.get().getResMessage().toStringUtf8());
    }

    logger.info(
        "the value: " + PublicMethod
            .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray()));

    List<String> retList = PublicMethod
        .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray());
  Long actualSalt = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));

    logger.info("actualSalt: " + actualSalt);
  byte[] tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(0)), 12, tmpAddress, 0, 20);
  String addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
  String addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);

    testContractAddress = WalletClient.decodeFromBase58Check(addressFinal);

    SmartContract smartContract = PublicMethod.getContract(testContractAddress, blockingStubFull);
  // contract created by create2, doesn't have ABI
    Assert.assertEquals(0, smartContract.getAbi().getEntrysCount());
  // the contract owner of contract created by create2 is the factory contract
    Assert.assertEquals(Base58.encode58Check(factoryContractAddress),
        Base58.encode58Check(smartContract.getOriginAddress().toByteArray()));
  // the contract address in transaction info,
    // contract address of create2 contract is factory contract
    Assert.assertEquals(Base58.encode58Check(factoryContractAddress),
        Base58.encode58Check(infoById.get().getContractAddress().toByteArray()));
  }

  @Test(enabled = false, description = "Trigger test contract", groups = {"contract", "daily"})
  public void test03TriggerTestContract() {

    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
        PublicMethod.getFreezeBalanceCount(user001Address, user001Key, 50000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(user001Address), testKey002, blockingStubFull));

    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    long devEnergyLimitBefore = accountResource.getEnergyLimit();
    long devEnergyUsageBefore = accountResource.getEnergyUsed();
    long devBalanceBefore = PublicMethod.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("before trigger, devEnergyLimitBefore is " + Long.toString(devEnergyLimitBefore));
    logger.info("before trigger, devEnergyUsageBefore is " + Long.toString(devEnergyUsageBefore));
    logger.info("before trigger, devBalanceBefore is " + Long.toString(devBalanceBefore));

    accountResource = PublicMethod.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitBefore = accountResource.getEnergyLimit();
    long userEnergyUsageBefore = accountResource.getEnergyUsed();
    long userBalanceBefore = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("before trigger, userEnergyLimitBefore is " + Long.toString(userEnergyLimitBefore));
    logger.info("before trigger, userEnergyUsageBefore is " + Long.toString(userEnergyUsageBefore));
    logger.info("before trigger, userBalanceBefore is " + Long.toString(userBalanceBefore));
  Long callValue = Long.valueOf(0);
  final String triggerTxid = PublicMethod.triggerContract(testContractAddress,
        "plusOne()", "#", false, callValue,
        1000000000L, "0", 0, user001Address, user001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    long devEnergyLimitAfter = accountResource.getEnergyLimit();
    long devEnergyUsageAfter = accountResource.getEnergyUsed();
    long devBalanceAfter = PublicMethod.queryAccount(dev001Address, blockingStubFull).getBalance();

    logger.info("after trigger, devEnergyLimitAfter is " + Long.toString(devEnergyLimitAfter));
    logger.info("after trigger, devEnergyUsageAfter is " + Long.toString(devEnergyUsageAfter));
    logger.info("after trigger, devBalanceAfter is " + Long.toString(devBalanceAfter));

    accountResource = PublicMethod.getAccountResource(user001Address, blockingStubFull);
    long userEnergyLimitAfter = accountResource.getEnergyLimit();
    long userEnergyUsageAfter = accountResource.getEnergyUsed();
    long userBalanceAfter = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("after trigger, userEnergyLimitAfter is " + Long.toString(userEnergyLimitAfter));
    logger.info("after trigger, userEnergyUsageAfter is " + Long.toString(userEnergyUsageAfter));
    logger.info("after trigger, userBalanceAfter is " + Long.toString(userBalanceAfter));

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    logger.info(
        "the value: " + PublicMethod
            .getStrings(transactionInfo.getContractResult(0).toByteArray()));

    List<String> retList = PublicMethod
        .getStrings(transactionInfo.getContractResult(0).toByteArray());
  Long ret = ByteArray.toLong(ByteArray.fromHexString(retList.get(0)));

    logger.info("ret: " + ret);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    SmartContract smartContract = PublicMethod.getContract(infoById.get().getContractAddress()
        .toByteArray(), blockingStubFull);

    long consumeUserPercent = smartContract.getConsumeUserResourcePercent();
    logger.info("ConsumeURPercent: " + consumeUserPercent);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 1,
        dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 0,
        dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 1,
        user001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, testKey002, 0,
        user001Address, blockingStubFull);
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


