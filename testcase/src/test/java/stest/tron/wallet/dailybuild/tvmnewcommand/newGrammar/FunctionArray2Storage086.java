package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class FunctionArray2Storage086 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] mapKeyContract = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/function_type_array_to_storage.sol";
  String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }


  @Test(enabled = true, description = "function array test view to default", groups = {"contract", "daily"})
  public void test01View2Default() {
    String triggerTxid =
        PublicMethod.triggerContract(mapKeyContract, "testViewToDefault()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals(12,
        ByteArray.toInt(transactionInfo.get().getContractResult(0).substring(0, 32).toByteArray()));
    Assert.assertEquals(22,
        ByteArray.toInt(transactionInfo.get().getContractResult(0)
            .substring(32, 64).toByteArray()));
  }

  @Test(enabled = true, description = "function array pure to default", groups = {"contract", "daily"})
  public void test02Pure2Default() {
    String triggerTxid =
        PublicMethod.triggerContract(mapKeyContract, "testPureToDefault()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals(13,
        ByteArray.toInt(transactionInfo.get().getContractResult(0).substring(0, 32).toByteArray()));
    Assert.assertEquals(23,
        ByteArray.toInt(transactionInfo.get().getContractResult(0)
            .substring(32, 64).toByteArray()));

  }

  @Test(enabled = true, description = "function array pure to view ", groups = {"contract", "daily"})
  public void test03Pure2View() {
    String triggerTxid =
        PublicMethod.triggerContract(mapKeyContract, "testPureToView()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals(13,
        ByteArray.toInt(transactionInfo.get().getContractResult(0).substring(0, 32).toByteArray()));
    Assert.assertEquals(23,
        ByteArray.toInt(transactionInfo.get().getContractResult(0)
            .substring(32, 64).toByteArray()));
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

