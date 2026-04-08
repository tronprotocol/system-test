package stest.tron.wallet.contract.scenario;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray; import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;

import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class ContractScenario001 extends TronBaseTest {  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contract001Address = ecKey1.getAddress();
  String contract001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());  private ManagedChannel channelFull1 = null;  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true)
  public void deployAddressDemo() {
    ecKey1 = new ECKey(Utils.getRandom());
    contract001Address = ecKey1.getAddress();
    contract001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(contract001Key);

    Assert.assertTrue(PublicMethod.sendcoin(contract001Address, 20000000L, toAddress,
        testKey003, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(contract001Address, 15000000L,
        3, 1, contract001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contract001Address,
        blockingStubFull);
    Long energyLimit = accountResource.getEnergyLimit();
    Long energyUsage = accountResource.getEnergyUsed();
    Long balanceBefore = PublicMethod.queryAccount(contract001Key, blockingStubFull).getBalance();

    logger.info("before energy limit is " + Long.toString(energyLimit));
    logger.info("before energy usage is " + Long.toString(energyUsage));
    logger.info("before balance is " + Long.toString(balanceBefore));

    String filePath = "./src/test/resources/soliditycode/contractScenario001.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contract001Key, contract001Address, blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi() != null);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    accountResource = PublicMethod.getAccountResource(contract001Address, blockingStubFull1);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    Long balanceAfter = PublicMethod.queryAccount(contract001Key, blockingStubFull1).getBalance();

    logger.info("after energy limit is " + Long.toString(energyLimit));
    logger.info("after energy usage is " + Long.toString(energyUsage));
    logger.info("after balance is " + Long.toString(balanceAfter));

    Assert.assertTrue(energyLimit > 0);
    Assert.assertTrue(energyUsage > 0);
    Assert.assertEquals(balanceBefore, balanceAfter);
  }
}

