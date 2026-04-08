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
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.contractResult;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class ContractGrammar001 extends TronBaseTest {


  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] grammarAddress = ecKey1.getAddress();
  String testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String compilerVersion = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.solidityCompilerVersion");

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(testKeyForGrammarAddress);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }

  @Test(enabled = true, description = "Support function type", groups = {"contract", "daily"})
  public void test1Grammar001() {
    ecKey1 = new ECKey(Utils.getRandom());
    grammarAddress = ecKey1.getAddress();
    testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/contractGrammar001test1Grammar001.sol";
  String contractName = "FunctionSelector";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = "";
  String num = "true" + "," + "10";
    txid = PublicMethod.triggerContract(contractAddress,
        "select(bool,uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
  Long returnnumber = ByteArray.toLong(ByteArray.fromHexString(ByteArray.toHexString(
        infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber == 20);
  String num2 = "false" + "," + "10";
    txid = PublicMethod.triggerContract(contractAddress,
        "select(bool,uint256)", num2, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("infoById:" + infoById);
    Optional<Transaction> ById = PublicMethod.getTransactionById(txid, blockingStubFull);
    logger.info("getRet:" + ById.get().getRet(0));
    logger.info("getNumber:" + ById.get().getRet(0).getContractRet().getNumber());
    logger.info("getContractRetValue:" + ById.get().getRet(0).getContractRetValue());
    logger.info("getContractRet:" + ById.get().getRet(0).getContractRet());

    Assert.assertEquals(ById.get().getRet(0).getContractRet().getNumber(),
        contractResult.SUCCESS_VALUE);
    Assert.assertEquals(ById.get().getRet(0).getContractRetValue(), 1);
    Assert.assertEquals(ById.get().getRet(0).getContractRet(), contractResult.SUCCESS);

    Assert
        .assertEquals(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray()),
            "0000000000000000000000000000000000000000000000000000000000000064");
    Assert.assertEquals(contractResult.SUCCESS, infoById.get().getReceipt().getResult());

    logger.info("ById:" + ById);
    Assert.assertEquals(ById.get().getRet(0).getRet().getNumber(), 0);
    Assert.assertEquals(ById.get().getRet(0).getRetValue(), 0);
  Long returnnumber2 = ByteArray.toLong(ByteArray.fromHexString(
        ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber2 == 100);
  }

  @Test(enabled = true, description = "Ordinary library contract", groups = {"contract", "daily"})
  public void test2Grammar002() {
    String filePath = "src/test/resources/soliditycode/contractGrammar001test2Grammar002.sol";
  String contractName = "Set";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = "";
  String num = "1";
  byte[] contractAddress1 = null;
  String filePath1 = "src/test/resources/soliditycode/contractGrammar001test2Grammar002.sol";
  String contractName1 = "C";
    HashMap retMap1 = PublicMethod.getBycodeAbiForLibrary(filePath1, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
  String library = retMap1.get("library").toString();
  String libraryAddress = library + Base58.encode58Check(contractAddress);
    contractAddress1 = PublicMethod
        .deployContractForLibrary(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, libraryAddress, testKeyForGrammarAddress,
            grammarAddress, compilerVersion, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    txid = PublicMethod.triggerContract(contractAddress1,
        "register(uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull1);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  @Test(enabled = true, description = "Library contract", groups = {"contract", "daily"})
  public void test3Grammar003() {
    String filePath = "src/test/resources/soliditycode/contractGrammar001test3Grammar003.sol";
  String contractName = "Set";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = "";
  String num = "1";
  byte[] contractAddress1 = null;
  String contractName1 = "C";
    HashMap retMap1 = PublicMethod.getBycodeAbiForLibrary(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
  String library = retMap1.get("library").toString();
  String libraryAddress = library
        + Base58.encode58Check(contractAddress);
    contractAddress1 = PublicMethod
        .deployContractForLibrary(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, libraryAddress, testKeyForGrammarAddress,
            grammarAddress, compilerVersion, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    txid = PublicMethod.triggerContract(contractAddress1,
        "register(uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull1);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull1);

    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }


  @Test(enabled = true, description = "Extended type", groups = {"contract", "daily"})
  public void test4Grammar004() {
    ecKey1 = new ECKey(Utils.getRandom());
    grammarAddress = ecKey1.getAddress();
    testKeyForGrammarAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(PublicMethod
        .sendcoin(grammarAddress, 100000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/contractGrammar001test4Grammar004.sol";
  String contractName = "Search";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  byte[] contractAddress1 = null;
  String contractName1 = "C";
    HashMap retMap1 = PublicMethod.getBycodeAbiForLibrary(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
  String library = retMap1.get("library").toString();
  String libraryAddress = null;
    libraryAddress = library
        + Base58.encode58Check(contractAddress);
    contractAddress1 = PublicMethod
        .deployContractForLibrary(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 100, libraryAddress, testKeyForGrammarAddress,
            grammarAddress, compilerVersion, blockingStubFull);
  String txid = "";
  String num = "1";
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    txid = PublicMethod.triggerContract(contractAddress1,
        "append(uint256)", num, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String num1 = "0";
  String txid1 = PublicMethod.triggerContract(contractAddress1,
        "getData(uint256)", num1, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
  Long returnnumber = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber == 1);
  String num2 = "1" + "," + "2";
  String txid2 = PublicMethod.triggerContract(contractAddress1,
        "replace(uint256,uint256)", num2, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = null;
    infoById2 = PublicMethod.getTransactionInfoById(txid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
  String txid3 = PublicMethod.triggerContract(contractAddress1,
        "getData(uint256)", num1, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethod.getTransactionInfoById(txid3, blockingStubFull);
  Long returnnumber1 = ByteArray.toLong(ByteArray
        .fromHexString(ByteArray.toHexString(infoById1.get().getContractResult(0).toByteArray())));

    Assert.assertTrue(returnnumber1 == 2);

  }

  @Test(enabled = true, description = "Solidity assembly", groups = {"contract", "daily"})
  public void test5Grammar006() {
    String filePath = "src/test/resources/soliditycode/contractGrammar001test5Grammar006.sol";
  String contractName = "InfoFeed";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, testKeyForGrammarAddress,
        grammarAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = "";
  String number = "1";
  final String txid1 = PublicMethod.triggerContract(contractAddress,
        "f(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
  final String txid2 = PublicMethod.triggerContract(contractAddress,
        "d(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
  final String txid3 = PublicMethod.triggerContract(contractAddress,
        "d1(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
  final String txid4 = PublicMethod.triggerContract(contractAddress,
        "d2(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
  final String txid5 = PublicMethod.triggerContract(contractAddress,
        "d5(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
  final String txid6 = PublicMethod.triggerContract(contractAddress,
        "d4(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);
  final String txid8 = PublicMethod.triggerContract(contractAddress,
        "d6(uint256)", number, false,
        0, maxFeeLimit, grammarAddress, testKeyForGrammarAddress, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull1);

    Optional<TransactionInfo> infoById1 = PublicMethod
        .getTransactionInfoById(txid1, blockingStubFull1);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById2 = PublicMethod
        .getTransactionInfoById(txid2, blockingStubFull1);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    Assert.assertEquals(133,ByteArray.toInt(infoById2.get().getContractResult(0).toByteArray()));

    Optional<TransactionInfo> infoById3 = PublicMethod
        .getTransactionInfoById(txid3, blockingStubFull1);
    Assert.assertTrue(infoById3.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById4 = PublicMethod
        .getTransactionInfoById(txid4, blockingStubFull1);
    Assert.assertTrue(infoById4.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById5 = PublicMethod
        .getTransactionInfoById(txid5, blockingStubFull1);
    Assert.assertTrue(infoById5.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById6 = PublicMethod
        .getTransactionInfoById(txid6, blockingStubFull1);
    Assert.assertTrue(infoById6.get().getResultValue() == 0);

    Optional<TransactionInfo> infoById8 = PublicMethod
        .getTransactionInfoById(txid8, blockingStubFull1);
    Assert.assertTrue(infoById8.get().getResultValue() == 0);


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(grammarAddress, testKeyForGrammarAddress, testNetAccountAddress,
        blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

}
