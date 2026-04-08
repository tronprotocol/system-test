package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
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
import java.util.Optional;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class NewFeatureForSolc086WithVIAIR extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] mapKeyContract = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");
  private Long energyFee = 0L;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/NewFeature086.sol";
    String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbiWithParam(filePath, contractName,"--experimental-via-ir");

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        500000000L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethod.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    energyFee = PublicMethod.getChainParametersValue(
            ProposalEnum.GetEnergyFee.getProposalName(), blockingStubFull);

  }

  @Test(enabled = true, description = "catch assert fail", groups = {"contract", "daily"})
  public void test01TrtCatchAssertFail() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "catchAssertFail()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1, trueRes);

  }

  @Test(enabled = true, description = "catch under flow", groups = {"contract", "daily"})
  public void test02CatchUnderFlow() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "catchUnderFlow()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(17, trueRes);

  }

  @Test(enabled = true, description = "catch divide zero", groups = {"contract", "daily"})
  public void test03CatchDivideZero() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "catchDivideZero()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(18, trueRes);
  }

  @Test(enabled = true, description = "get address code length", groups = {"contract", "daily"})
  public void test04GetAddressCodeLength() {
    String triggerTxid = PublicMethod.triggerContract(mapKeyContract, "getAddressCodeLength()",
        "#", false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    //as compile contract with --experimental-via-ir, trigger costs more energy than no via-ir
    Assert.assertEquals(401, transactionInfo.get().getReceipt().getEnergyUsageTotal());
  }

  @Test(enabled = true, description = "fix kecca256 bug: differt length return same code", groups = {"contract", "daily"})
  public void test05Kecca256BugFix() {
    String args = "\"abcd123\"";
    String triggerTxid = PublicMethod.triggerContract(mapKeyContract, "keccak256Bug(string)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals(0,
        ByteArray.toInt(transactionInfo.get().getContractResult(0).toByteArray()));
    logger.info(transactionInfo.toString());
  }

  @Test(enabled = true, description = "revert error type with params", groups = {"contract", "daily"})
  public void test06RevertErrorType() {
    String args = "\"T9yD14Nj9j7xAB4dbGeiX9h8unkKHxuWwb\",1000000000";
    String triggerTxid = PublicMethod.triggerContract(mapKeyContract, "transfer(address,uint256)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(1, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("cf479181",
        ByteArray.toHexString(transactionInfo.get()
            .getContractResult(0).substring(0, 4).toByteArray()));
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0)
            .substring(4, 36).toByteArray()));
    Assert.assertEquals("000000000000000000000000000000000000000000000000000000003b9aca00",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0)
            .substring(36, 68).toByteArray()));

  }

  @Test(enabled = true, description = "revert error type no params", groups = {"contract", "daily"})
  public void test07RevertErrorType() {
    String triggerTxid = PublicMethod.triggerContract(mapKeyContract, "withdraw()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertEquals(1, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.REVERT,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("82b42900",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0)
            .substring(0, 4).toByteArray()));
  }

  @Test(enabled = true, description = "test bytes concat", groups = {"contract", "daily"})
  public void test08bytesConcat() {
    String args = "\"0x1234\",\"p2\",\"0x48e2f56f2c57e3532146eef2587a2a72\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "bytesConcat(bytes,string,bytes16)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    int trueRes = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(36, trueRes);
  }

  @Test(enabled = true, description = "test emit event", groups = {"contract", "daily"})
  public void test09EmitEvent() {
    String triggerTxid = PublicMethod.triggerContract(mapKeyContract, "testEmitEvent()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals(6,
        ByteArray.toInt(transactionInfo.get().getLog(0).getData().toByteArray()));
  }

  @Test(enabled = true, description = "test bytes convert to byteN overflow", groups = {"contract", "daily"})
  public void test10Bytes2ByteN() {
    String args = "\"0x12345678\"";
    String triggerTxid = PublicMethod.triggerContract(mapKeyContract, "bytes2BytesN(bytes)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("1234560000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "test bytes convert to byteN underflow", groups = {"contract", "daily"})
  public void test11Bytes2ByteN() {
    String args = "\"0x1234\"";
    String triggerTxid = PublicMethod.triggerContract(mapKeyContract, "bytes2BytesN(bytes)",
        args, false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info(transactionInfo.toString());
    Assert.assertEquals(0, transactionInfo.get().getResultValue());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS,
        transactionInfo.get().getReceipt().getResult());
    Assert.assertEquals("1234000000000000000000000000000000000000000000000000000000000000",
        ByteArray.toHexString(transactionInfo.get().getContractResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "get contract address by different function", groups = {"contract", "daily"})
  public void test12GetConcatAddress() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "getContractAddress()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    String res1 = ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .substring(0, 32).toByteArray());
    String res2 = ByteArray.toHexString(transactionExtention.getConstantResult(0)
        .substring(32, 64).toByteArray());
    Assert.assertEquals(res1, res2);
  }

  @Test(enabled = true, description = "test bytes concat with empty string", groups = {"contract", "daily"})
  public void test13bytesConcatWithEmptyStr() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(mapKeyContract,
            "bytesConcatWithEmptyStr()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
  }

}

