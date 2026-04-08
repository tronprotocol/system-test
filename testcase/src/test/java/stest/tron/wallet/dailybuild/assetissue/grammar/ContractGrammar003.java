package stest.tron.wallet.dailybuild.assetissue.grammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.Hash;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class ContractGrammar003 extends TronBaseTest {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress3 = ecKey1.getAddress();
  String testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);

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


  @Test(enabled = true, description = "Complex structure", groups = {"contract", "daily"})
  public void test1Grammar014() {
    ecKey1 = new ECKey(Utils.getRandom());
    grammarAddress3 = ecKey1.getAddress();
    testKeyForGrammarAddress3 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress3, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/contractGrammar003test1Grammar014.sol";
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String contractName1 = "B";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
  byte[] contractAddress1 = PublicMethod
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = PublicMethod.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid1 = PublicMethod.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
  Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 0);
  Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber1 == 0);
    Optional<TransactionInfo> infoById4 = null;
  String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
  String txid4 = PublicMethod.triggerContract(contractAddress,
        "callTest(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById4 = PublicMethod.getTransactionInfoById(txid4, blockingStubFull);

    Assert.assertTrue(infoById4.get().getResultValue() == 0);
  String txid5 = PublicMethod.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById5 = null;
    infoById5 = PublicMethod.getTransactionInfoById(txid5, blockingStubFull);
  Long returnnumber5 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById5.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber5 == 0);
  String txid6 = PublicMethod.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById6 = null;
    infoById6 = PublicMethod.getTransactionInfoById(txid6, blockingStubFull);
  Long returnnumber6 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById6.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber6 == 1);
  String txid7 = PublicMethod.triggerContract(contractAddress,
        "callcodeTest(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById7 = null;
    infoById7 = PublicMethod.getTransactionInfoById(txid7, blockingStubFull);

    Assert.assertTrue(infoById7.get().getResultValue() == 0);
  String txid8 = PublicMethod.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById8 = null;
    infoById8 = PublicMethod.getTransactionInfoById(txid8, blockingStubFull);
  Long returnnumber8 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById8.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber8 == 1);
  String txid9 = PublicMethod.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById9 = null;
    infoById9 = PublicMethod.getTransactionInfoById(txid9, blockingStubFull);
  Long returnnumber9 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById9.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber9 == 1);
  String txid10 = PublicMethod.triggerContract(contractAddress,
        "delegatecallTest(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById10 = null;
    infoById10 = PublicMethod.getTransactionInfoById(txid10, blockingStubFull);

    Assert.assertTrue(infoById10.get().getResultValue() == 0);
  String txid11 = PublicMethod.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById11 = null;
    infoById11 = PublicMethod.getTransactionInfoById(txid11, blockingStubFull);
  Long returnnumber11 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById11.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber11 == 1);
  String txid12 = PublicMethod.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById12 = null;
    infoById12 = PublicMethod.getTransactionInfoById(txid12, blockingStubFull);
  Long returnnumber12 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById12.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber12 == 1);
  String initParmes1 = "\"" + Base58.encode58Check(contractAddress1) + "\"";
  String txid13 = PublicMethod.triggerContract(contractAddress,
        "callAddTest(address)", initParmes1, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById13 = null;
    infoById13 = PublicMethod.getTransactionInfoById(txid13, blockingStubFull);

    Assert.assertTrue(infoById13.get().getResultValue() == 0);
  String txid14 = PublicMethod.triggerContract(contractAddress,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById14 = null;
    infoById14 = PublicMethod.getTransactionInfoById(txid14, blockingStubFull);
  Long returnnumber14 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById14.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber14 == 1);
  String txid15 = PublicMethod.triggerContract(contractAddress1,
        "getnumberForB()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById15 = null;
    infoById15 = PublicMethod.getTransactionInfoById(txid15, blockingStubFull);
  Long returnnumber15 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById15.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber15 == 3);
  }


  @Test(enabled = true, description = "Fallback function ", groups = {"contract", "daily"})
  public void test2Grammar015() {
    String filePath = "src/test/resources/soliditycode/contractGrammar003test2Grammar015.sol";
  String contractName = "ExecuteFallback";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    Optional<TransactionInfo> infoById = null;
  String txid = PublicMethod.triggerContract(contractAddress,
        "callExistFunc()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);

    Optional<TransactionInfo> infoById1 = null;
  String txid1 = PublicMethod.triggerContract(contractAddress,
        "callNonExistFunc()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);


    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  String i = ByteArray.toHexString(Hash.sha3("ExistFuncCalled(bytes,uint256)".getBytes()));
  String resultvalue = ByteArray
        .toHexString(infoById.get().getLogList().get(0).getTopicsList().get(0).toByteArray());
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(i, resultvalue);


    infoById1 = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
  String value = ByteArray.toHexString(Hash.sha3("FallbackCalled(bytes)".getBytes()));
  String resultvalue1 = ByteArray
        .toHexString(infoById1.get().getLogList().get(0).getTopicsList().get(0).toByteArray());
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertEquals(value, resultvalue1);

  }

  @Test(enabled = true, description = "Permission control ", groups = {"contract", "daily"})
  public void test3Grammar016() {
    String filePath = "src/test/resources/soliditycode/contractGrammar003test3Grammar016.sol";
  String contractName = "D";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);

  String contractName1 = "E";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
  byte[] contractAddress1 = PublicMethod
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, null, testKeyForGrammarAddress3,
            grammarAddress3, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
  String txid = PublicMethod.triggerContract(contractAddress,
        "readData()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
  String txid1 = PublicMethod.triggerContract(contractAddress1,
        "g()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
  String num = "3";
  String txid2 = PublicMethod.triggerContract(contractAddress1,
        "setData(uint256)", num, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);

  String txid3 = PublicMethod.triggerContract(contractAddress1,
        "getData()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);


    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);


    infoById2 = PublicMethod.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethod.getTransactionInfoById(txid3, blockingStubFull);
  Long returnnumber3 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById3.get().getContractResult(0).toByteArray())));
    logger.info("test3Grammar016 returnnumber3 : " + returnnumber3);
    Assert.assertTrue(returnnumber3 == 3);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);

  }

  @Test(enabled = true, description = "Structure", groups = {"contract", "daily"})
  public void test4Grammar017() {
    String filePath = "src/test/resources/soliditycode/contractGrammar003test4Grammar017.sol";
  String contractName = "CrowdFunding";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress1 = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String initParmes = "\"" + Base58.encode58Check(grammarAddress3) + "\",\"1\"";
    Optional<TransactionInfo> infoById = null;
  String txid = PublicMethod.triggerContract(contractAddress1,
        "candidate(address,uint256)", initParmes, false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
  String txid1 = PublicMethod.triggerContract(contractAddress1,
        "check(uint256)", "1", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
  String txid2 = PublicMethod.triggerContract(contractAddress1,
        "vote(uint256)", "1", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(returnnumber1 == 1);
    Optional<TransactionInfo> infoById1 = PublicMethod
        .getTransactionInfoById(txid1, blockingStubFull1);
  Long returnnumber2 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(returnnumber2 == 1);
    Optional<TransactionInfo> infoById2 = PublicMethod
        .getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);

  }

  @Test(enabled = true, description = "Built-in function", groups = {"contract", "daily"})
  public void test5Grammar018() {
    String filePath = "src/test/resources/soliditycode/contractGrammar003test5Grammar018.sol";
  String contractName = "Grammar18";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
  String txid = PublicMethod.triggerContract(contractAddress,
        "testAddmod()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
  String txid1 = PublicMethod.triggerContract(contractAddress,
        "testMulmod()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
  String txid2 = PublicMethod.triggerContract(contractAddress,
        "testKeccak256()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
  String txid3 = PublicMethod.triggerContract(contractAddress,
        "testSha256()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
  String txid4 = PublicMethod.triggerContract(contractAddress,
        "testSha3()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber == 1);

    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
  Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertTrue(returnnumber1 == 2);

    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethod.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById3 = null;
    infoById3 = PublicMethod.getTransactionInfoById(txid3, blockingStubFull);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById4 = null;
    infoById4 = PublicMethod.getTransactionInfoById(txid4, blockingStubFull);
    Assert.assertTrue(infoById4.get().getResultValue() == 0);
  }


  @Test(enabled = true, description = "Time unit", groups = {"contract", "daily"})
  public void test6Grammar019() {

    String filePath = "src/test/resources/soliditycode/contractGrammar003test6Grammar019.sol";
  String contractName = "timetest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = PublicMethod.triggerContract(contractAddress,
        "timetest()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);

  }


  @Test(enabled = true, description = "Trx and sun unit conversion.", groups = {"contract", "daily"})
  public void test7Grammar020() {
    String filePath = "src/test/resources/soliditycode/contractGrammar003test7Grammar020.sol";
  String contractName = "trxtest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress3,
        grammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
  String txid = PublicMethod.triggerContract(contractAddress,
        "test()", "#", false,
        0, maxFeeLimit, grammarAddress3, testKeyForGrammarAddress3, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(grammarAddress3, testKeyForGrammarAddress3, testNetAccountAddress,
        blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
