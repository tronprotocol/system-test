package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class CanCunTest extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  byte[] contractA,contractB;
  String base58ContractA;
  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 50100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/CanCun.sol";
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractA = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        10000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    base58ContractA = Base58.encode58Check(contractA);
    contractName = "B";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractB = PublicMethod.deployContractFallback(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(contractA,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi().toString());
    smartContract = PublicMethod.getContract(contractB, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi().toString());
  }

  @Test(enabled = true, description = "tload and tstore", groups = {"contract", "daily"})
  public void test01TloadTstore() {
    String txid = PublicMethod.triggerContract(contractA,
            "tstoreAndTload()", "#",
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "nonreentrant for entrant 1 time", groups = {"contract", "daily"})
  public void test02TloadTstore() {
    String txid = PublicMethod.triggerContract(contractA,
            "claimGift()", "#",
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    long callValue = info.getInternalTransactions(0).getCallValueInfo(0).getCallValue();
    Assert.assertEquals(1000000, callValue);
  }

  @Test(enabled = true, description = "call nonreentrant once", groups = {"contract", "daily"})
  public void test03TloadTstore() {
    String args = "\"" + base58ContractA + "\"";
  String txid = PublicMethod.triggerContract(contractB,
            "claimGiftOnce(address)", args,
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    long callValue = info.getInternalTransactions(1).getCallValueInfo(0).getCallValue();
    Assert.assertEquals(1000000, callValue);
    long balance = PublicMethod.queryAccount(contractB,blockingStubFull).getBalance();
    Assert.assertEquals(callValue, balance);
  }

  @Test(enabled = true, description = "call nonreentrant for 2 times ", groups = {"contract", "daily"})
  public void test04TloadTstore() {
    String args = "\"" + base58ContractA + "\"";
  String txid = PublicMethod.triggerContract(contractB,
            "claimGiftTwice(address)", args,
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.FAILED, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT, info.getReceipt().getResult());
  }

  @Test(enabled = true, description = "", groups = {"contract", "daily"})
  public void test05MemoryCopy() {
    String txid = PublicMethod.triggerContract(contractA,
            "memoryCopy()", "#",
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000050",ByteArray.toHexString(info.getContractResult(0).toByteArray()));
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
  }
  @Test(enabled = true, description = "TSTORE will also REVERT if called during a STATICCALL", groups = {"contract", "daily"})
  public void test06StaticcallTstore() {
    String args = "\"" + base58ContractA + "\"";
  String txid = PublicMethod.triggerContract(contractB,
            "staticcallTstore(address)", args,
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethod.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(contractExcAddress, contractExcKey,
        testNetAccountAddress, blockingStubFull);  }


}

