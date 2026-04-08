package stest.tron.wallet.contract.scenario;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class ContractScenario008 extends TronBaseTest {

  {
    fullnode = config.getStringList("fullnode.ip.list").get(1);
  }

  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract008Address = ecKey1.getAddress();
  String contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
  }

  @Test(enabled = true)
  public void deployErc721CryptoKitties() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract008Address = ecKey1.getAddress();
    contract008Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(contract008Key);
    Assert.assertTrue(PublicMethod.sendcoin(contract008Address, 5000000000L, fromAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract008Address, 1000000L,
        3, 1, contract008Key, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract008Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Account account = PublicMethod.queryAccount(contract008Key, blockingStubFull);
    logger.info("before balance is " + Long.toString(account.getBalance()));
    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    Long shortFeeLimit = 900L;

    String filePath = "./src/test/resources/soliditycode/contractScenario008.sol";
    String contractName = "KittyCore";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", shortFeeLimit,
        0L, 100, null, contract008Key, contract008Address, blockingStubFull);

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contract008Key, contract008Address, blockingStubFull);

    final SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    accountResource = PublicMethod.getAccountResource(contract008Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    account = PublicMethod.queryAccount(contract008Key, blockingStubFull);
    logger.info("after balance is " + Long.toString(account.getBalance()));
    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }
}
