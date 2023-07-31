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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class NewFeatureForSolc0820 {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  byte[] contract1 = null;
  byte[] contract2 = null;
  byte[] contract3 = null;
  byte[] lib = null;
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

    String filePath = "src/test/resources/soliditycode/NewFeature0820_1.sol";
    String contractName = "C";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contract1 = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
//    PublicMethed.waitProduceNextBlock(blockingStubFull);

    filePath = "src/test/resources/soliditycode/NewFeature0820_2.sol";
    contractName = "D";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contract2 = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);

    filePath = "src/test/resources/soliditycode/NewFeature0820_3.sol";
    contractName = "L";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    lib = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    filePath = "src/test/resources/soliditycode/NewFeature0820_3.sol";
    contractName = "C";
    retMap = PublicMethed.getBycodeAbiForLibrary(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    String library = retMap.get("library").toString();
    String libraryAddress = library + Base58.encode58Check(lib);
    String compilerVersion = Configuration.getByPath("testng.conf")
        .getString("defaultParameter.solidityCompilerVersion");
    contract3 = PublicMethed
        .deployContractForLibrary(contractName, abi, code, "", maxFeeLimit,
            0L, 100, libraryAddress, contractExcKey, contractExcAddress,
            compilerVersion, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contract1,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    smartContract = PublicMethed.getContract(contract2, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    smartContract = PublicMethed.getContract(contract3, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "test using for T global inside sol")
  public void test001UsingForInsideSol() {
    String args = "1,2";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contract1,"pick(uint8,uint8)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1, result);
  }

  @Test(enabled = true, description = "test using for T global beside sol")
  public void test002UsingForBesideSol() {
    String args = "3,2";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contract2,"getFirstParam(uint8,uint8)", args, false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(3, result);
  }

  @Test(enabled = true, description = "test external library functions in ``using for``")
  public void test003ExternalLibFunctionInUsingFor() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contract3,"f()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(1, result);
  }

  @Test(enabled = true, description = "test public library functions in ``using for`` ")
  public void test004PublicLibFunctionInUsingFor() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contract3,"g()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(2, result);
  }

  @Test(enabled = true, description = "test internal library functions in ``using for`` ")
  public void test005ExternalLibFunctionInUsingFor() {
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed
        .triggerConstantContractForExtention(contract3,"h()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);

    int result = ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(true, transactionExtention.getResult().getResult());
    Assert.assertEquals("SUCESS",
        transactionExtention.getTransaction().getRet(0).getRet().toString());
    Assert.assertEquals(3, result);
  }

  @Test(enabled = true, description = "emit event by a contract but defined outside of it.")
  public void test006EventDefinedOutsideOfContract() throws Exception{
    String txid = PublicMethed
        .triggerContract(contract3,"i()", "#", false,
            0, maxFeeLimit, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    String topic1 = ByteArray.toHexString(info.getLog(0).getTopics(0).toByteArray());
    String topic2 = ByteArray.toHexString(info.getLog(1).getTopics(0).toByteArray());
    Assert.assertEquals(topic1, "92bbf6e823a631f3c8e09b1c8df90f378fb56f7fbc9701827e1ff8aad7f6a028"); //E() Keccak256
    Assert.assertEquals(topic2, "70a5d861ef9816388422765f41d618eb3abdf490acb37354b539729e37b09f0e"); //H() Keccak256

    //ABI: Include events in the ABI that are emitted by a contract but defined outside of it.
    String filePath = "src/test/resources/soliditycode/output/NewFeature0820_3/C.abi";
    StringBuilder builder = readFile(filePath);
    Assert.assertTrue(builder.toString().contains("\"name\":\"E\""));
  }

  public StringBuilder  readFile(String fileName) throws Exception{
    BufferedReader br = new BufferedReader(new FileReader(fileName));
    StringBuilder builder = new StringBuilder();
    String str;
    while((str = br.readLine())!=null){
      builder.append(str);
    }
    br.close();
    return builder;
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

