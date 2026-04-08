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
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class ContractScenario004 extends TronBaseTest {  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract004Address = ecKey1.getAddress();
  String contract004Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contract004Key);  }

  @Test(enabled = true, groups = {"contract", "smoke"})
  public void deployErc20TronTokenWithoutData() {
    Assert.assertTrue(PublicMethod.sendcoin(contract004Address, 200000000L, foundationAddress,
        foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract004Address, 100000000L,
        3, 1, contract004Key, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract004Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
  String filePath = "./src/test/resources/soliditycode//contractScenario004.sol";
  String contractName = "TronToken";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contract004Key, contract004Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info);
    Assert.assertTrue(info.get().getResultValue() == 1);
  }

  @Test(enabled = true, groups = {"contract", "smoke"})
  public void deployErc20TronTokenWithData() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contract004Address, 200000000L, foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract004Address, 100000000L,
        3, 1, contract004Key, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract004Address,
        blockingStubFull);
  Long energyLimit = accountResource.getEnergyLimit();
  Long energyUsage = accountResource.getEnergyUsed();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
  String filePath = "./src/test/resources/soliditycode//contractScenario004.sol";
  String contractName = "TronToken";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String constructorStr = "constructor(address)";
  String data = "\"" + Base58.encode58Check(contract004Address) + "\"";
  String txid = PublicMethod
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, contract004Key, contract004Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    System.out.println(info);
    Assert.assertTrue(info.get().getResultValue() == 0);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


