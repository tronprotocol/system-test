package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
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
public class fallbackReceive extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddressCaller = null;
  byte[] contractAddressTest0 = null;
  byte[] contractAddressTest1 = null;
  byte[] contractAddressTest2 = null;
  byte[] contractAddressTestPayable = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    PublicMethod
        .sendcoin(contractExcAddress, 1000_000_000_000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/fallbackUpgrade.sol";
  String contractName = "Caller";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddressCaller = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100,
            null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "Test0";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTest0 = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "Test1";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTest1 = PublicMethod
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "Test2";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTest2 = PublicMethod
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    contractName = "TestPayable";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    code = retMap.get("byteCode").toString();
    abi = retMap.get("abI").toString();
    contractAddressTestPayable = PublicMethod
        .deployContractFallback(contractName, abi, code, "", maxFeeLimit, 0L,
            100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "contract test0 has no fallback method", groups = {"contract", "daily"})
  public void test001NoFallback() {
    String txid = "";
  String method = "hello()";
    txid = PublicMethod.triggerContract(contractAddressTest0,
        method, "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
  }

  @Test(enabled = true, description = "contract test0 has no fallback method", groups = {"contract", "daily"})
  public void test002NoFallback() {
    String txid = "";
  String method = "callTest0(address)";
  String para = "\"" + Base58.encode58Check(contractAddressTest0) + "\"";
    txid = PublicMethod.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("FAILED", infoById.get().getResult().toString());
  }

  @Test(enabled = true, description = "contract test01 has fallback method", groups = {"contract", "daily"})
  public void test011Fallback() {
    String txid = "";
  String method = "callTest1(address)";
  String para = "\"" + Base58.encode58Check(contractAddressTest1) + "\"";
    txid = PublicMethod.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    List<Protocol.TransactionInfo.Log> logList = infoById.get().getLogList();
    if (!Objects.isNull(logList)) {
      for (Protocol.TransactionInfo.Log log : logList) {
        //logger.info("LOG data info:" + tmp);
        Assert.assertEquals("fallback",
            PublicMethod.getContractStringMsg(log.getData().toByteArray()));
      }
    }
  }

  @Test(enabled = true, description = "contract test01 has fallback method", groups = {"contract", "daily"})
  public void test012Fallback() {
    String txid = "";
  String method = "callTest2(address)";
  String para = "\"" + Base58.encode58Check(contractAddressTest1) + "\"";
    txid = PublicMethod.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("REVERT", infoById.get().getReceipt().getResult().toString());
  }

  @Test(enabled = true, description = "contract test01 has fallback method", groups = {"contract", "daily"})
  public void test013Fallback() {
    String txid = "";
    txid = PublicMethod.triggerContract(contractAddressTest1,
        "hello()", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());

    txid = PublicMethod.triggerContract(contractAddressTest1,
        "hello2()", "#", false,
        100000, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result:" + infoById.get().getReceipt().getResult());
    Assert.assertEquals("REVERT", infoById.get().getReceipt().getResult().toString());
  }

  @Test(enabled = true, description = "contract test02 has fallback payable method", groups = {"contract", "daily"})
  public void test021FallbackPayable() {
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
    logger.info("beforeBalance:" + beforeBalance);
  String txid = "";
    long value = 10000;
    txid = PublicMethod.triggerContract(contractAddressTest2, "hello()", "#", false, value,
        maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("result:" + infoById.get().getReceipt().getResult());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
  Long fee = infoById.get().getFee();
    logger.info("fee:" + fee);
    Protocol.Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
    logger.info("afterBalance:" + afterBalance);
    Assert.assertTrue(afterBalance + fee + value == beforeBalance);
  String method = "callTest2(address)";
  String para = "\"" + Base58.encode58Check(contractAddressTest2) + "\"";
    txid = PublicMethod.triggerContract(contractAddressCaller, method, para, false, value,
        maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("callTest2 result:" + infoById.get().getReceipt().getResult());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    fee = infoById.get().getFee();
    logger.info("callTest2 fee:" + fee);
    infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull1);
  Long afterBalance2 = infoafter.getBalance();
    logger.info("callTest2 afterBalance:" + afterBalance);
    Assert.assertTrue(afterBalance2 + fee + value == afterBalance);
  }

  @Test(enabled = true, description = "contract TestPayable has fallback and receive", groups = {"contract", "daily"})
  public void test041FallbackReceive() {
    String txid = "";
  String method = "callTestPayable1(address)";
  String para = "\"" + Base58.encode58Check(contractAddressTestPayable) + "\"";
    txid = PublicMethod.triggerContract(contractAddressCaller,
        method, para, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info("getResult: " + infoById.get().getResultValue());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals("fallback",
        PublicMethod.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));
    Assert.assertEquals("receive",
        PublicMethod.getContractStringMsg(infoById.get().getLog(1).getData().toByteArray()));
  }

  @Test(enabled = true, description = "contract TestPayable has fallback and receive", groups = {"contract", "daily"})
  public void test042FallbackReceive() {
    Protocol.Account info;
    GrpcAPI.AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  String txid = "";
  Long beforeBalance = info.getBalance();
    logger.info("beforeBalance:" + beforeBalance);
  String method = "callTest2(address)";
    long value = 10000;
  String para = "\"" + Base58.encode58Check(contractAddressTestPayable) + "\"";
    txid = PublicMethod.triggerContract(contractAddressCaller,
        method, para, false,
        value, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals("fallback",
        PublicMethod.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));
  Long fee = infoById.get().getFee();
    logger.info("fee:" + fee);
    Protocol.Account infoafter = PublicMethod.queryAccount(contractExcKey, blockingStubFull1);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractExcAddress, blockingStubFull1);
  Long afterBalance = infoafter.getBalance();
    logger.info("afterBalance:" + afterBalance);
    Assert.assertTrue(afterBalance + fee + value == beforeBalance);
  }

  @Test(enabled = true, description = "contract TestPayable has fallback and receive", groups = {"contract", "daily"})
  public void test05FallbackReceive() {
    String txid = "";
    long value = 10000;
    txid = PublicMethod.triggerContract(contractAddressTestPayable,
        "method()", "#", false,
        value, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals("fallback",
        PublicMethod.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));

    Protocol.Account infoafter = PublicMethod
        .queryAccount(contractAddressTestPayable, blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(contractAddressTestPayable,
            blockingStubFull);
  Long afterBalance = infoafter.getBalance();
    logger.info("contract balance:" + afterBalance.longValue());
    Assert.assertEquals(11000, afterBalance.longValue());

    txid = PublicMethod.triggerContract(contractAddressTestPayable,
        "#", "#", false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.get().getResult().toString());
    Assert.assertEquals("SUCESS", infoById.get().getResult().toString());
    Assert.assertEquals("receive",
        PublicMethod.getContractStringMsg(infoById.get().getLog(0).getData().toByteArray()));

  }

  //@AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(contractAddressTest0, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethod
        .freeResource(contractAddressTest1, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethod
        .freeResource(contractAddressTest2, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethod
        .freeResource(contractAddressTestPayable, contractExcKey, testNetAccountAddress,
            blockingStubFull);
    PublicMethod
        .freeResource(contractAddressCaller, contractExcKey, testNetAccountAddress,
            blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }
}