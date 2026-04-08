package stest.tron.wallet.dailybuild.originenergylimit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class ContractOriginEnergyLimit001 extends TronBaseTest {
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress3 = ecKey1.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
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
    PublicMethod.printAddress(testKeyForGrammarAddress3);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }


  //Origin_energy_limit001,028,029
  @Test(enabled = true, description = "Boundary value and update test", groups = {"daily"})
  public void testOrigin_energy_limit001() {
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress3, 100000000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/contractOriginEnergyLimit001.sol";
  String contractName = "findArgsContractTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String contractAddress = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, -1, "0", 0,
            null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(contractAddress == null);
  String contractAddress1 = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 0, "0", 0,
            null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    Assert.assertTrue(contractAddress1 == null);
  byte[] contractAddress2 = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 9223372036854775807L, "0",
            0, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertFalse(PublicMethod.updateEnergyLimit(contractAddress2, -1L,
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract = PublicMethod.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertFalse(PublicMethod.updateEnergyLimit(contractAddress2, 0L,
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract1 = PublicMethod.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract1.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertTrue(PublicMethod.updateEnergyLimit(contractAddress2,
        9223372036854775807L, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull));
    SmartContract smartContract2 = PublicMethod.getContract(contractAddress2, blockingStubFull);
    Assert.assertTrue(smartContract2.getOriginEnergyLimit() == 9223372036854775807L);

    Assert.assertTrue(PublicMethod.updateEnergyLimit(contractAddress2, 'c',
        testKeyForGrammarAddress3, grammarAddress3, blockingStubFull));
    SmartContract smartContract3 = PublicMethod.getContract(contractAddress2, blockingStubFull);
    Assert.assertEquals(smartContract3.getOriginEnergyLimit(), 99);

    Assert.assertFalse(PublicMethod.updateEnergyLimit(contractAddress2, 1L,
        foundationKey, foundationAddress, blockingStubFull));
    SmartContract smartContract4 = PublicMethod.getContract(contractAddress2, blockingStubFull);
    Assert.assertEquals(smartContract4.getOriginEnergyLimit(), 99);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(grammarAddress3, testKeyForGrammarAddress3, foundationAddress,
        blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
