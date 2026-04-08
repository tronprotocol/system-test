package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import static org.hamcrest.core.StringContains.containsString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class ConstantCallStorage001 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
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

  @BeforeClass(enabled = false)
  public void beforeClass() {
    initSolidityChannel();
    PublicMethod.printAddress(contractExcKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    }

  @Test(enabled = false, description = "TriggerconstantContract trigger modidy storage date", groups = {"contract", "daily"})
  public void testConstantCallStorage001() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/constantCallStorage001.sol";
  String contractName = "NotView";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, "[]", code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
  //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    Account info;

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(contractExcAddress,
        blockingStubFull);
    info = PublicMethod.queryAccount(contractExcKey, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "setnum()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(138,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    logger.info("transactionExtention: " + transactionExtention);

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "num()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(123,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = false, description = "TriggerconstantContract storage date by another contract ", groups = {"contract", "daily"})
  public void testConstantCallStorage002() {

    String filePath = "src/test/resources/soliditycode/constantCallStorage001.sol";
  String contractName = "UseNotView";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress002 = PublicMethod
        .deployContract(contractName, "[]", code, "", maxFeeLimit,
            0L, 100, null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress002, blockingStubFull);
  //Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress002,
            "setnumuseproxy(address)", "\"" + WalletClient.encode58Check(contractAddress) + "\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertEquals(138,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "num()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(123,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

  }


  @Test(enabled = false, description = "TriggerconstantContract storage date by another contract "
      + "view function, use 0.5.* version solidity complier", groups = {"contract", "daily"})
  public void testConstantCallStorage003() {
    String filePath = "src/test/resources/soliditycode/constantCallStorage002.sol";
  String contractName = "UseNotView";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress002 = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress002, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress002,
            "setnumuseproxy(address)", "\"" + WalletClient.encode58Check(contractAddress) + "\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertFalse(transactionExtention.getResult().getResult());
    Assert.assertThat(ByteArray.toStr(transactionExtention.getResult().getMessage().toByteArray()),
        containsString("Not enough energy"));
  }


  @Test(enabled = false, description = "TriggerconstantContract storage date by another contract "
      + "view function, use 0.4.* version solidity complier", groups = {"contract", "daily"})
  public void testConstantCallStorage004() {
    String filePath = "src/test/resources/soliditycode/constantCallStorage002.sol";
  String contractName = "UseNotView";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] contractAddress002 = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey, contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress002, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress002,
            "setnumuseproxy(address)", "\"" + WalletClient.encode58Check(contractAddress) + "\"",
            false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    logger.info("transactionExtention: " + transactionExtention);
    Assert.assertEquals(138,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress,
            "num()", "#", false,
            0, 0, "0", 0, contractExcAddress, contractExcKey, blockingStubFull);
    Assert.assertTrue(transactionExtention.getResult().getResult());
    Assert.assertEquals(123,
        ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  }

  /**
   * constructor.
   */


}
