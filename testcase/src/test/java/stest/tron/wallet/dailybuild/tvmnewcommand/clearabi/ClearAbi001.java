package stest.tron.wallet.dailybuild.tvmnewcommand.clearabi;

import static org.hamcrest.core.StringContains.containsString;

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
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class ClearAbi001 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress1 = ecKey2.getAddress();
  String contractExcKey1 = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
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
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress1, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Clear a contract created by other account", groups = {"contract", "daily"})
  public void testClearAbi001() {
    String filePath = "src/test/resources/soliditycode/ClearAbi001.sol";
  String contractName = "testConstantContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
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
        .clearContractAbiForExtention(contractAddress, contractExcAddress1, contractExcKey1,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("is not the owner of the contract"));

    smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());


  }


  @Test(enabled = true, description = "Clear a contract with ABI created by itself", groups = {"contract", "daily"})
  public void testClearAbi002() {

    String contractName = "testConstantContract";
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertFalse(smartContract.getAbi().toString().isEmpty());
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
  String txid = PublicMethod
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  String txid1 = PublicMethod
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById1 = null;
    infoById1 = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);

    smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());
  }


  @Test(enabled = true, description = "Clear a contract without ABI", groups = {"contract", "daily"})
  public void testClearAbi003() {

    String contractName = "testConstantContract";
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
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
  String txid = PublicMethod
        .clearContractAbi(contractAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = null;
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);

    smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertTrue(smartContract.getAbi().toString().isEmpty());
    Assert.assertTrue(smartContract.getName().equalsIgnoreCase(contractName));
    Assert.assertFalse(smartContract.getBytecode().toString().isEmpty());


  }

  @Test(enabled = true, description = "Clear a account address", groups = {"contract", "daily"})
  public void testClearAbi004() {
    TransactionExtention transactionExtention = PublicMethod
        .clearContractAbiForExtention(contractExcAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("Contract validate error : Contract not exists"));
  }


  @Test(enabled = true, description = "Clear a uninitialized account", groups = {"contract", "daily"})
  public void testClearAbi005() {

    ECKey ecKeyN = new ECKey(Utils.getRandom());
  byte[] contractExcAddressN = ecKeyN.getAddress();
  String contractExcKeyN = ByteArray.toHexString(ecKeyN.getPrivKeyBytes());

    TransactionExtention transactionExtention = PublicMethod
        .clearContractAbiForExtention(contractExcAddressN, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("CONTRACT_VALIDATE_ERROR"));
    Assert.assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
        containsString("Contract validate error : Contract not exists"));

  }

  @Test(enabled = true, description = "Clear a not meet the rules address", groups = {"contract", "daily"})
  public void testClearAbi006() {
    byte[] fakeAddress = "412B5D".getBytes();
    TransactionExtention transactionExtention = PublicMethod
        .clearContractAbiForExtention(fakeAddress, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention.getResult().getMessage().toStringUtf8(),
            containsString("Contract validate error : Contract not exists"));
  byte[] fakeAddress1 = "412B5D3405B2D26767C9C09886D53DEAFF6EB718AC111".getBytes();

    TransactionExtention transactionExtention1 = PublicMethod
        .clearContractAbiForExtention(fakeAddress1, contractExcAddress, contractExcKey,
            blockingStubFull);
    Assert
        .assertThat(transactionExtention1.getResult().getCode().toString(),
            containsString("CONTRACT_VALIDATE_ERROR"));
    Assert
        .assertThat(transactionExtention1.getResult().getMessage().toStringUtf8(),
            containsString("Contract validate error : Contract not exists"));


  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod
        .freeResource(contractExcAddress, contractExcKey, testNetAccountAddress, blockingStubFull);
    PublicMethod.freeResource(contractExcAddress1, contractExcKey1, testNetAccountAddress,
        blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }  }


}
