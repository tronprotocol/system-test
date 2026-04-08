package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;


@Slf4j
@MultiNode
public class MultiValiSignPerformanceTest extends TronBaseTest {

  private final String fromKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] fromAddress = PublicMethod.getFinalAddress(fromKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractDepAddress = ecKey1.getAddress();
  String contractDepKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] nonexistentAddress = ecKey2.getAddress();
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] ecrecoverContractAddress = null;
  private byte[] multiValiSignContractAddress = null;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractDepKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }


  @Test(enabled = true, description = "deploy ecrecover contract", groups = {"stress"})
  public void test01DeployEcrecoverContract() {
    Assert.assertTrue(PublicMethod.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
        PublicMethod.getFreezeBalanceCount(contractDepAddress, contractDepKey, 170000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(contractDepAddress), fromKey, blockingStubFull));
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contractDepAddress,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(contractDepKey, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
  String filePath = "src/test/resources/soliditycode/multiValiSignPerformance01.sol";
  String contractName = "ecrecoverValidateSign";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, contractDepKey,
            contractDepAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod.getAccountResource(contractDepAddress, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(contractDepKey, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    ecrecoverContractAddress = infoById.get().getContractAddress().toByteArray();
    logger.info("ecrecoverContractAddress:" + infoById.get().getContractAddress());
    SmartContract smartContract = PublicMethod.getContract(ecrecoverContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "deploy multvalisign contract", groups = {"stress"})
  public void test02DeployMultvalisignContract() {
    Assert.assertTrue(PublicMethod.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
        PublicMethod.getFreezeBalanceCount(contractDepAddress, contractDepKey, 170000L,
            blockingStubFull), 0, 1,
        ByteString.copyFrom(contractDepAddress), fromKey, blockingStubFull));
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(contractDepAddress,
        blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(contractDepKey, blockingStubFull).getBalance();
    logger.info("before energyLimit is " + Long.toString(energyLimit));
    logger.info("before energyUsage is " + Long.toString(energyUsage));
    logger.info("before balanceBefore is " + Long.toString(balanceBefore));
  String filePath = "src/test/resources/soliditycode/multiValiSignPerformance02.sol";
  String contractName = "multiValidateSignContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final String transferTokenTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, contractDepKey,
            contractDepAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    accountResource = PublicMethod.getAccountResource(contractDepAddress, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(contractDepKey, blockingStubFull).getBalance();

    logger.info("after energyLimit is " + Long.toString(energyLimit));
    logger.info("after energyUsage is " + Long.toString(energyUsage));
    logger.info("after balanceAfter is " + Long.toString(balanceAfter));

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(transferTokenTxid, blockingStubFull);

    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    multiValiSignContractAddress = infoById.get().getContractAddress().toByteArray();
    logger.info("multiValiSignContractAddress:" + infoById.get().getContractAddress());
    SmartContract smartContract = PublicMethod.getContract(multiValiSignContractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  }

  @Test(enabled = true, description = "trigger ecrecover contract test", groups = {"stress"})
  public void test03triggerEcrecoverContract() {
    /*Assert.assertTrue(PublicMethod.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    try {
      Thread.sleep(new Long(30000));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }*/
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
  //test key
    byte[] hash = ByteArray
        .fromHexString("7d889f42b4a56ebe78264631a3b4daf21019e1170cce71929fb396761cdf532e");
    logger.info("hash:" + Hex.toHexString(hash));
  int cnt = 15;
    for (int i = 0; i < cnt; i++) {
      ECKey key = new ECKey();
  byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(WalletClient.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String[] inputArr = new String[parameters.size()];
  int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');
  String txid = "";
    long start = System.currentTimeMillis();
    txid = PublicMethod
        .triggerContract(PublicMethod.decode58Check("TDgdUs1gmn1JoeGMqQGkkxE1pcMNSo8kFj"),
            "validateSign(bytes32,bytes[],address[])", input,
            false, 0, maxFeeLimit, contractDepAddress, contractDepKey, blockingStubFull);
    long timeCosts = System.currentTimeMillis() - start;
    logger.info(
        "Ecrecover--cnt:" + cnt + ",timeCost:" + timeCosts + ",ms:" + (timeCosts * 1.0 / cnt));
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }


  @Test(enabled = true, description = "trigger mulivalisign contract test", groups = {"stress"})
  public void test04triggerMuliValiSignContract() {
    /*Assert.assertTrue(PublicMethod.sendcoin(contractDepAddress, 1000_000_000L, fromAddress,
        fromKey, blockingStubFull));
    try {
      Thread.sleep(new Long(30000));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }*/
    List<Object> signatures = new ArrayList<>();
    List<Object> addresses = new ArrayList<>();
  //just test key
    byte[] hash = ByteArray
        .fromHexString("7d889f42b4a56ebe78264631a3b4daf21019e1170cce71929fb396761cdf532e");
    logger.info("hash:" + Hex.toHexString(hash));
  int cnt = 15;
    for (int i = 0; i < cnt; i++) {
      ECKey key = new ECKey();
  byte[] sign = key.sign(hash).toByteArray();
      signatures.add(Hex.toHexString(sign));
      addresses.add(WalletClient.encode58Check(key.getAddress()));
    }
    List<Object> parameters = Arrays.asList("0x" + Hex.toHexString(hash), signatures, addresses);
    String[] inputArr = new String[parameters.size()];
  int i = 0;
    for (Object parameter : parameters) {
      if (parameter instanceof List) {
        StringBuilder sb = new StringBuilder();
        for (Object item : (List) parameter) {
          if (sb.length() != 0) {
            sb.append(",");
          }
          sb.append("\"").append(item).append("\"");
        }
        inputArr[i++] = "[" + sb.toString() + "]";
      } else {
        inputArr[i++] =
            (parameter instanceof String) ? ("\"" + parameter + "\"") : ("" + parameter);
      }
    }
    String input = StringUtils.join(inputArr, ',');
  String txid = "";
    long start = System.currentTimeMillis();
    txid = PublicMethod
        .triggerContract(PublicMethod.decode58Check("TVpTLZbBbP82aufo7p3qmb4ELiowH3mjQW"),
            "testArray(bytes32,bytes[],address[])", input, false,
            0, maxFeeLimit, contractDepAddress, contractDepKey, blockingStubFull);
    long timeCosts = System.currentTimeMillis() - start;
    logger.info(
        "MuliValiSign--cnt:" + cnt + ",timeCost:" + timeCosts + ",ms:" + (timeCosts * 1.0 / cnt));
    Optional<Protocol.TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
