package stest.tron.wallet.contract.scenario;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class ContractScenario012 extends TronBaseTest {  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  byte[] contractAddress = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  String receiveAddressParam;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract012Address = ecKey1.getAddress();
  String contract012Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] receiverAddress = ecKey2.getAddress();
  String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contract012Key);
    PublicMethod.printAddress(receiverKey);  }

  @Test(enabled = true, groups = {"contract", "smoke"})
  public void test1DeployTransactionCoin() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract012Address = ecKey1.getAddress();
    contract012Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    Assert.assertTrue(PublicMethod.sendcoin(contract012Address, 2000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract012Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
  String filePath = "./src/test/resources/soliditycode/contractScenario012.sol";
  String contractName = "PayTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 100,
            null, contract012Key, contract012Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);
  }


  @Test(enabled = true, groups = {"contract", "smoke"})
  public void test2TriggerTransactionCoin() {
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    receiveAddressParam = "\"" + Base58.encode58Check(foundationAddress)
        + "\"";
  //When the contract has no money,transaction coin failed.
    txid = PublicMethod.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddressParam, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    Assert.assertTrue(infoById.get().getResultValue() == 1);
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());
  }


  @Test(enabled = true, groups = {"contract", "smoke"})
  public void test3TriggerTransactionCanNotCreateAccount() {
    ecKey2 = new ECKey(Utils.getRandom());
    receiverAddress = ecKey2.getAddress();
    receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  //Send some trx to the contract account.
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
    receiveAddressParam = "\"" + Base58.encode58Check(receiverAddress)
        + "\"";
  //In smart contract, you can create account
    txid = PublicMethod.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddressParam, false,
        1000000000L, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    logger.info("result is " + infoById.get().getResultValue());
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

    Account account2 = PublicMethod.queryAccount(receiverAddress, blockingStubFull);
    Assert.assertEquals(5L, account2.getBalance());

  }


  @Test(enabled = true, groups = {"contract", "smoke"})
  public void test4TriggerTransactionCoin() {
    receiveAddressParam = "\"" + Base58.encode58Check(receiverAddress)
        + "\"";
    Account account = PublicMethod.queryAccount(contractAddress, blockingStubFull);
    logger.info("contract Balance : -- " + account.getBalance());
  //This time, trigger the methed sendToAddress2 is OK.
    Assert.assertTrue(PublicMethod.sendcoin(receiverAddress, 10000000L, toAddress,
        testKey003, blockingStubFull));
    txid = PublicMethod.triggerContract(contractAddress,
        "sendToAddress2(address)", receiveAddressParam, false,
        0, 100000000L, contract012Address, contract012Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info(txid);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infobyid : --- " + infoById);
    logger.info("result is " + infoById.get().getResultValue());
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertTrue(infoById.get().getFee() == infoById.get().getReceipt().getEnergyFee());
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());

  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


