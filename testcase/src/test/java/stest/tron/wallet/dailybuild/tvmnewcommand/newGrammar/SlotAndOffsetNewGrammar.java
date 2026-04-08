package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class SlotAndOffsetNewGrammar extends TronBaseTest {
  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  /**
   * trigger contract and assert.
   */
  public void assertResult(byte[] contractAddress) {
    String txid = "";
    txid = PublicMethod.triggerContract(contractAddress, "getA()", "", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
  String res = "0000000000000000000000000000000000000000000000000000000000000001"
        + "0000000000000000000000000000000000000000000000000000000000000000";
    System.out.println(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(res,
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()));

    txid = PublicMethod.triggerContract(contractAddress, "getE()", "", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    res = "0000000000000000000000000000000000000000000000000000000000000004"
        + "0000000000000000000000000000000000000000000000000000000000000000";
    System.out.println(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(res,
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()));

  }

  @Test(enabled = true, description = "Deploy 0.6.x new grammer", groups = {"contract", "daily"})
  public void testSlotAndOffsetOldVersion() {
    String contractName = "A";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_slotAndOffset_06x");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_slotAndOffset_06x");
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    assertResult(contractAddress);

  }

  @Test(enabled = true, description = "Deploy 0.7.0 new grammer", groups = {"contract", "daily"})
  public void testSlotAndOffsetNew() {
    String filePath = "./src/test/resources/soliditycode/slotAndOffsetNewGrammer.sol";
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0, infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    assertResult(contractAddress);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);  }
}
