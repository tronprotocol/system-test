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
import org.tron.protos.Protocol;
import org.tron.protos.contract.SmartContractOuterClass;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NewFeatureForSolc0817 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] mapKeyContract = null;
  byte[] useForContract = null;
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
        .sendcoin(contractExcAddress, 300100_000_000L,
            testNetAccountAddress, testNetAccountKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath = "src/test/resources/soliditycode/NewFeature0817.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    mapKeyContract = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "test equality-comparison operators for external function types")
  public void test001EqualComparisonForFunction() {
    String txid = PublicMethed.triggerContract(mapKeyContract, "comparison_operators_for_local_external_function_pointers()", "#",
        false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    Assert.assertEquals(1, ByteArray.toInt(info.getContractResult(0).toByteArray()));

  }


  @Test(enabled = true, description = "test ContractName.functionName for abi.encodeCall,"
      + " in addition to external function pointers")
  public void test002SupportUserDefinedTypeOfData() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "test_function_name_for_abi_encodeCall()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(11116, result);
  }


  @Test(enabled = true, description = "using M for Type; is allowed at file level free function")
  public void test003UseForFreeFunction() {

    String filePath = "src/test/resources/soliditycode/NewFeature0817_1.sol";
    String contractName = "Lib";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    byte[] libAdd = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey, contractExcAddress,
         blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String filePath1 = "src/test/resources/soliditycode/NewFeature0817_2.sol";
    String contractName1 = "C";
    HashMap retMap1 = PublicMethed.getBycodeAbiForLibrary(filePath1, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();
    String library = retMap1.get("library").toString();
    String libraryAddress = library + Base58.encode58Check(libAdd);
    String compilerVersion = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.solidityCompilerVersion");
    useForContract = PublicMethed
        .deployContractForLibrary(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, libraryAddress, contractExcKey, contractExcAddress,
             compilerVersion, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(useForContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(useForContract,
            "g(uint256)", "3", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result1 = ByteArray.toInt(transactionExtention
        .getConstantResult(0).substring(0, 32).toByteArray());
    int result2 = ByteArray.toInt(transactionExtention
        .getConstantResult(0).substring(32, 64).toByteArray());
    int result3 = ByteArray.toInt(transactionExtention
        .getConstantResult(0).substring(64, 96).toByteArray());
    Assert.assertEquals(0, result1);
    Assert.assertEquals(3, result2);
    Assert.assertEquals(4, result3);
  }

  @Test(enabled = true, description = "using M for Type; is allowed at file level lib function")
  public void test004UseForLibFunction() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(useForContract,
            "f(uint256)", "3", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result1 = ByteArray.toInt(transactionExtention
        .getConstantResult(0).toByteArray());
    Assert.assertEquals(3, result1);
  }


  @Test(enabled = true, description = "using M for Type; is allowed at file level with global")
  public void test005UseForGlobal() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(useForContract,
            "glob()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result1 = ByteArray.toInt(transactionExtention
        .getConstantResult(0).substring(0, 32).toByteArray());
    int result2 = ByteArray.toInt(transactionExtention
        .getConstantResult(0).substring(32, 64).toByteArray());
    System.out.println(result1 + "  : " + result2);

    Assert.assertEquals(2, result1);
    Assert.assertEquals(1, result2);
  }

  @Test(enabled = true, description = "test event.selector")
  public void test006EventSelector() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(mapKeyContract,
            "eventSelector()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    String result1 = ByteArray.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
    Assert.assertEquals("92bbf6e823a631f3c8e09b1c8df90f378fb56f7fbc9701827e1ff8aad7f6a028", result1);

  }

  @Test(enabled = true, description = "Support using library constants in initializers of other constants")
  public void test007InitConstantsUseLibConstants() {

    String filePath = "src/test/resources/soliditycode/NewFeature0817_3.sol";
    String contractName = "C1";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] consCon = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(mapKeyContract,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(consCon,
            "LIMIT()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result1 = ByteArray.toInt(transactionExtention
        .getConstantResult(0).toByteArray());
    System.out.println(result1);
    Assert.assertEquals(100, result1);
  }

  @Test(enabled = true, description = "Correctly encode literals used in ``abi.encodeCall`` in place of fixed bytes arguments")
  public void test008BugFixAbiEncodeCall() {

    String filePath = "src/test/resources/soliditycode/NewFeature0817_4bugFix.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] consCon = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(consCon,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(consCon,
            "f()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    String  result1 = ByteArray.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000060"
        + "1234000000000000000000000000000000000000000000000000000000000000"
        + "6162000000000000000000000000000000000000000000000000000000000000"
        + "1234000000000000000000000000000000000000000000000000000000000000", result1);

    transactionExtention = PublicMethed
        .triggerConstantContractForExtention(consCon,
            "f2()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

      result1 = ByteArray.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000040"
        + "0000000000000000000000000000000000000000000000000000000000001234"
        + "0000000000000000000000000000000000000000000000000000000000001234", result1);
  }


  @Test(enabled = true, description = "wallet-cli cannot process fixed lend array, so this case is disable")
  public void test009BugFixStaticArray() {

    String filePath = "src/test/resources/soliditycode/NewFeature0817_4bugFix.sol";
    String contractName = "D";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] consCon = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(consCon,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(consCon,
            "g(uint256[],uint256[1])", "[65535],[255]", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    String  result1 = ByteArray.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
    System.out.println(result1);
//    Assert.assertEquals("", result1);
  }

  @Test(enabled = true, description = "")
  public void test010BugDirtyBytes() {

    String filePath = "src/test/resources/soliditycode/NewFeature0817_4bugFix.sol";
    String contractName = "E";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] consCon = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(consCon,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(consCon,
            "h()", "#", true,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    String  result1 = ByteArray.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
//    System.out.println(result1);
    Assert.assertEquals("000000000000000000000000000000000000000"
        + "00000000000000000000000200000000000000000000000000000000000000000"
        + "00000000000000000000004000000000000000000000000000000000000000000"
        + "0000000000000000000000000000000000000000000000000000000000000000"
        + "00000000000000000000000", result1);
  }

  @Test(enabled = true, description = "")
  public void test011BugOverrideMemory2calldata() {

    String filePath = "src/test/resources/soliditycode/NewFeature0817_4bugFix.sol";
    String contractName = "M";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    byte[] consCon = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(consCon,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(consCon,
            "g(uint256[])", "[9,8]", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    String  result1 = ByteArray.toHexString(transactionExtention
        .getConstantResult(0).toByteArray());
    System.out.println(result1);
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
        + "0000000000000000000000000000000000000000000000000000000000000002"
        + "0000000000000000000000000000000000000000000000000000000000000009"
        + "0000000000000000000000000000000000000000000000000000000000000008", result1);
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

