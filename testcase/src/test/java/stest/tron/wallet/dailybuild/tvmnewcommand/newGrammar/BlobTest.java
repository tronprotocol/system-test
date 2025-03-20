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
public class BlobTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethed.getFinalAddress(testNetAccountKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  byte[] contractC,contractD;
  String base58contractC;
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

    String filePath = "src/test/resources/soliditycode/blob.sol";
    String contractName = "C";
    String compileParam = "--via-ir";
    HashMap retMap = PublicMethed.getBycodeAbiWithParam(filePath, contractName, compileParam);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractC = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    base58contractC = Base58.encode58Check(contractC);

    contractName = "D";
    retMap = PublicMethed.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractD = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);

    PublicMethed.waitProduceNextBlock(blockingStubFull);
    SmartContractOuterClass.SmartContract smartContract = PublicMethed.getContract(contractC,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi().toString());
    smartContract = PublicMethed.getContract(contractD, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi().toString());
  }

  @Test(enabled = true, description = "test blobhash in assembly")
  public void test01AssemblyBlobhash() {
    String txid = PublicMethed.triggerContract(contractC,
            "assemblyBlobhash()", "#",
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",result);
  }

  @Test(enabled = true, description = "test blobbasefee in assembly")
  public void test02assemblyBlobbasefee() {
    String txid = PublicMethed.triggerContract(contractC,
            "assemblyBlobbasefee()", "#",
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",result);
  }

  @Test(enabled = true, description = "test blobhash(index)")
  public void test03GlobalBlobHash() {
    String args = "1";
    String txid = PublicMethed.triggerContract(contractC,
            "globalBlobHash(uint256)", args,
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",result);
  }

  @Test(enabled = true, description = "test block.blobbasefee")
  public void test04GlobalBlobbasefee() {
    String txid = PublicMethed.triggerContract(contractC,
            "globalBlobbasefee()", "#",
            false, 0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000",result);
  }

  @Test(enabled = true, description = "test blob KZGPointEvaluation")
  public void test05KZGPointEvaluation(){
    String  versionedHash ="015a4cab4911426699ed34483de6640cf55a568afc5c5edffdcbd8bcd4452f68";
    String z = "0000000000000000000000000000000000000000000000000000000000000065";
    String y = "60f557194475973322b33dc989896381844508234bfa6fbeefe5fa165ae15a0a";
    String commitment = "a70477b56251e8770969c83eaed665d3ab99b96b72270a4"
     + "1009f2752b5c06a06bd089ad48952c12b1dbf83dccd9d373f";
    String proof = "879f9a41956deae578bc65e7133f164394b8677bc2e7b1356be61"
            + "d47720ed2a3326bfddebc67cd37ee9e7537d7814afe";
String args = "\""+ versionedHash+"\",\"" + commitment +"\",\""+z+"\",\""+y+"\",\""+proof+"\"";
    GrpcAPI.TransactionExtention transactionExtention = PublicMethed.triggerConstantContractForExtention(contractC,
            "verifyKZG(bytes,bytes,bytes,bytes,bytes)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    System.out.println(transactionExtention.toString());
    String result = ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray());
    Assert.assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000001" +
            "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000001000" +
                    "73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", result);
  }

  @Test(enabled = true, description = "test blob KZGPointEvaluation")
  public void test06KZGPointEvaluation(){
    String  versionedHash ="015a4cab4911426699ed34483de6640cf55a568afc5c5edffdcbd8bcd4452f68";
    String z = "0000000000000000000000000000000000000000000000000000000000000065";
    String y = "60f557194475973322b33dc989896381844508234bfa6fbeefe5fa165ae15a0a";
    String commitment = "a70477b56251e8770969c83eaed665d3ab99b96b72270a4"
            + "1009f2752b5c06a06bd089ad48952c12b1dbf83dccd9d373f";
    String proof = "879f9a41956deae578bc65e7133f164394b8677bc2e7b1356be61"
            + "d47720ed2a3326bfddebc67cd37ee9e7537d7814afe";
    String args = "\""+ versionedHash+"\",\"" + commitment +"\",\""+z+"\",\""+y+"\",\""+proof+"\"";
    String txid = PublicMethed.triggerContract(contractC,
            "verifyKZG1(bytes,bytes,bytes,bytes,bytes)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000001000" +
                    "73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", result);
  }


  @Test(enabled = true, description = "test blob KZGPointEvaluation")
  public void test07callKZGPointEvaluation(){
    String  versionedHash ="01a327088bb2b13151449d8313c281d0006d12e8453e863637b746898b6ad5a6";
    String z = "0000000000000000000000000000000000000000000000000000000000000000";
    String y = "0000000010000000000000000000000000000000000000000000000000000000";
    String commitment = "8f26f349339c68b33ce856aa2c05b8f89e7c23db0c00817550679998efcbd8f2464f9e1ea6c3172b0b750603d1e4ea38";
    String proof = "97d8c90897645ac9e31e8017981de0f9d0d5de4cec12899680ee4e810f4f7f56ac765e46a801f2f1046f8f305d33e27c";
    String args = "\""+ versionedHash+"\",\"" + commitment +"\",\""+z+"\",\""+y+"\",\""+proof+"\"";
    String txid = PublicMethed.triggerContract(contractC,
            "verifyKZGCall(bytes,bytes,bytes,bytes,bytes)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000001000" +
                    "73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", result);
  }

  @Test(enabled = true, description = "test staticcallBlobhash")
  public void test08StaticcallBlobhashn(){
    String args = "\"" + base58contractC + "\"";
    String txid = PublicMethed.triggerContract(contractD,
            "staticcallBlobhash(address)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test StaticcallBlobBaseFee")
  public void test09StaticcallBlobBaseFee(){
    String args = "\"" + base58contractC + "\"";
    String txid = PublicMethed.triggerContract(contractD,
            "staticcallBlobBaseFee(address)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test blob KZGPointEvaluation")
  public void test10callBlobhash(){
    String args = "\"" + base58contractC + "\"";
    String txid = PublicMethed.triggerContract(contractD,
            "callBlobhash(address)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test StaticcallBlobBaseFee")
  public void test11CallBlobBaseFee(){
    String args = "\"" + base58contractC + "\"";
    String txid = PublicMethed.triggerContract(contractD,
            "callBlobBaseFee(address)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test delegatecallBlobhash")
  public void test12delegatecallBlobhash(){
    String args = "\"" + base58contractC + "\"";
    String txid = PublicMethed.triggerContract(contractD,
            "delegatecallBlobhash(address)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000", result);
  }

  @Test(enabled = true, description = "test delegatecallBlobBaseFee")
  public void test13delegatecallBlobBaseFee(){
    String args = "\"" + base58contractC + "\"";
    String txid = PublicMethed.triggerContract(contractD,
            "delegatecallBlobBaseFee(address)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals("0000000000000000000000000000000000000000000000000000000000000000", result);
  }

  @Test(enabled = false, description = "test blob KZGPointEvaluation")
  public void test14delegatecallKZGPointEvaluation(){
    String  versionedHash ="0000000000000000000000000000000000000000000000000000000000000000";
    String z = "0000000000000000000000000000000000000000000000000000000000000000";
    String y = "0000000000000000000000000000000000000000000000000000000000000000";
    String commitment = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    String proof = "000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000";
    String args = "\""+ versionedHash+"\",\"" + commitment +"\",\""+z+"\",\""+y+"\",\""+proof+"\"";
    String txid = PublicMethed.triggerContract(contractC,
            "verifyKZG1(bytes,bytes,bytes,bytes,bytes)", args,
            false, 0, maxFeeLimit,"0",0, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Protocol.TransactionInfo info = PublicMethed.getTransactionInfoById(txid, blockingStubFull).get();
    logger.info(info.toString());
    Assert.assertEquals(Protocol.TransactionInfo.code.SUCESS, info.getResult());
    Assert.assertEquals(Protocol.Transaction.Result.contractResult.SUCCESS, info.getReceipt().getResult());
    String result = ByteArray.toHexString(info.getContractResult(0).toByteArray());
    Assert.assertEquals(
            "0000000000000000000000000000000000000000000000000000000000000001" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000000040" +
                    "0000000000000000000000000000000000000000000000000000000000001000" +
                    "73eda753299d7d483339d80809a1d80553bda402fffe5bfeffffffff00000001", result);
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

