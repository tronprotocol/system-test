package stest.tron.wallet.dailybuild.tvmnewcommand.isContract;

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
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.api.WalletGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class isContractCommand002 extends TronBaseTest {

  private final String testNetAccountKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);
  byte[] contractAddress = null;
  byte[] selfdestructContractAddress = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] contractExcAddress = ecKey1.getAddress();
  String contractExcKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  byte[] selfdestructContractExcAddress = ecKey1.getAddress();
  String selfdestructContractKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(contractExcKey);
    PublicMethod.printAddress(selfdestructContractKey);    channelFull1 = ManagedChannelBuilder.forTarget(fullnode1)
        .usePlaintext()
        .build();
    blockingStubFull1 = WalletGrpc.newBlockingStub(channelFull1);
  }


  @Test(enabled = true, description = "Selfdestruct contract test isContract Command", groups = {"contract", "daily"})
  public void test01SelfdestructContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TvmIsContract001.sol";
  String contractName = "testIsContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 100, null, contractExcKey,
        contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String txid = "";
  String num = "\"" + Base58.encode58Check(contractAddress) + "\"";
    Assert.assertTrue(PublicMethod
        .sendcoin(selfdestructContractExcAddress, 10000000000L, testNetAccountAddress,
            testNetAccountKey,
            blockingStubFull));

    selfdestructContractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, selfdestructContractKey,
            selfdestructContractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    txid = PublicMethod.triggerContract(selfdestructContractAddress,
        "testIsContractCommand(address)", num, false,
        0, maxFeeLimit, selfdestructContractExcAddress, selfdestructContractKey, blockingStubFull);
    Optional<Protocol.TransactionInfo> infoById1 = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(1, ByteArray.toInt(infoById1.get().getContractResult(0).toByteArray()));
    logger.info(infoById1.toString());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(selfdestructContractAddress,
            "testIsContractView(address)", num, false,
            0, 0, "0", 0, selfdestructContractExcAddress, selfdestructContractKey,
            blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
  String txid1 = "";
    txid1 = PublicMethod.triggerContract(contractAddress,
        "selfdestructContract(address)", num, false,
        0, maxFeeLimit, contractExcAddress, contractExcKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    logger.info(infoById1.toString());

    txid1 = PublicMethod.triggerContract(selfdestructContractAddress,
        "testIsContractCommand(address)", num, false,
        0, maxFeeLimit, selfdestructContractExcAddress, selfdestructContractKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById1 = PublicMethod.getTransactionInfoById(txid1, blockingStubFull);
    logger.info(infoById1.toString());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(1, ByteArray.toInt(infoById1.get().getContractResult(0).toByteArray()));
    }else {
      Assert.assertEquals(0, ByteArray.toInt(infoById1.get().getContractResult(0).toByteArray()));
    }
    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(selfdestructContractAddress,
            "testIsContractView(address)", num, false,
            0, 0, "0", 0, selfdestructContractExcAddress, selfdestructContractKey,
            blockingStubFull);
    logger.info("transactionExtention:" + transactionExtention.toString());
    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert.assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    }else {
      Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
      Assert.assertEquals(0, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));
    }
  }

  @Test(enabled = true, description = "No constructor test isContract Command", groups = {"contract", "daily"})
  public void test02NoConstructorContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(contractExcAddress, 10000000000L, testNetAccountAddress, testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/TvmIsContract002.sol";
  String contractName = "testIsContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, null, contractExcKey,
            contractExcAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> info = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(info.get().toString());
    Assert.assertEquals(0, info.get().getResultValue());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethod.queryAccount(contractExcKey, blockingStubFull).getBalance();
    PublicMethod.sendcoin(testNetAccountAddress, balance, contractExcAddress, contractExcKey,
        blockingStubFull);    if (channelFull1 != null) {
      channelFull1.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }


}
