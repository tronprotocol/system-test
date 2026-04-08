package stest.tron.wallet.contract.scenario;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class ContractScenario013 extends TronBaseTest {  byte[] contractAddress = null;
  String txid = "";
  Optional<TransactionInfo> infoById = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract013Address = ecKey1.getAddress();
  String contract013Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contract013Key);  }

  @Test(enabled = true, groups = {"contract", "smoke"})
  public void deployTronTrxAndSunContract() {
    Assert.assertTrue(PublicMethod.sendcoin(contract013Address, 20000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract013Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
  String filePath = "./src/test/resources/soliditycode/contractScenario013.sol";
  String contractName = "timetest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    txid = PublicMethod.deployContractAndGetTransactionInfoById(contractName, abi, code, "",
        maxFeeLimit, 0L, 100, null, contract013Key, contract013Address, blockingStubFull);
    logger.info(txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 0);
    Assert.assertFalse(infoById.get().getContractAddress().isEmpty());
  }

  @Test(enabled = true, groups = {"contract", "smoke"})
  public void triggerTronTrxAndSunContract() {
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract013Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
  String filePath = "./src/test/resources/soliditycode/contractScenario013.sol";
  String contractName = "timetest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contract013Key, contract013Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    logger.info("energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    contractAddress = infoById.get().getContractAddress().toByteArray();

    txid = PublicMethod.triggerContract(contractAddress,
        "time()", "#", false,
        0, 100000000L, contract013Address, contract013Key, blockingStubFull);
    logger.info(txid);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
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


