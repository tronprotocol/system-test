package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NewFeatureForSolc0822 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contractD = null;
  byte[] contractM = null;
  byte[] contractA = null;
  byte[] contractB = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethed.printAddress(contractExcKey);
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext()
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    Assert.assertTrue(PublicMethed
        .sendcoin(contractExcAddress, 50100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/NewFeature0822_1.sol";
    String contractName = "D";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractD = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);

    contractName = "M";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractM = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    contractName = "A";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractA = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    contractName = "B";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractB = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contractD,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethed.getContract(contractM, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethed.getContract(contractA, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    smartContract = PublicMethed.getContract(contractB, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "event at file level")
  public void test001FileLevelEvent() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
            .triggerConstantContractForExtention(contractD, "f()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    System.out.println(transactionExtention);
    Assert.assertEquals(ByteArray.toHexString("abc".getBytes()),
            ByteArray.toHexString(transactionExtention.getLogs(0).getData().substring(64).toByteArray()).substring(0,6));
    Assert.assertEquals(8,
            ByteArray.toInt(transactionExtention.getLogs(1).getData().substring(0, 32).toByteArray()));
    Assert.assertEquals(2, transactionExtention.getLogs(1).getTopicsCount());
    Assert.assertEquals(6,
            ByteArray.toInt(transactionExtention.getLogs(2).getData().toByteArray()));
  }

  @Test(enabled = true, description = "Fix internal error when requesting userdoc or devdoc for a contract " +
          "that emits an event defined in a foreign contract or interface")
  public void test002ImmutableInitInCondition() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
            .triggerConstantContractForExtention(contractM, "emitEvent(uint256)", "8", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    System.out.println(transactionExtention);
    Assert.assertEquals(2, transactionExtention.getLogs(0).getTopicsCount());
    Assert.assertEquals(ByteArray.toHexString(contractExcAddress).substring(2),
            ByteArray.toHexString(transactionExtention.getLogs(0).getTopics(1).substring(12).toByteArray()));
    Assert.assertEquals(8,
            ByteArray.toInt(transactionExtention.getLogs(0).getData().toByteArray()));
  }

  @Test(enabled = true, description = "Remove redundant overflow checks of certain ``for`` loops when the counter variable cannot overflow.")
  public void test003UncheckForLoopsOverflow() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
            .triggerConstantContractForExtention(contractA, "inc()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    long incEnergy = transactionExtention.getEnergyUsed();
    transactionExtention = PublicMethed
            .triggerConstantContractForExtention(contractB, "inc()", "#", false,
                    0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
            transactionExtention.getTransaction().getRet(0).getRet().toString());
    long incUncheckEnergy = transactionExtention.getEnergyUsed();
    logger.info("incEnergy: " + incEnergy + "  incUncheckEnergy: " + incUncheckEnergy);
    Assert.assertEquals(incEnergy, incUncheckEnergy);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethed.freedResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}

