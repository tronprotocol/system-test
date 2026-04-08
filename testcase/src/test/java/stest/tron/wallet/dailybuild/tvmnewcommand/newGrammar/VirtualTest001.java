package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
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
public class VirtualTest001 extends TronBaseTest {

  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 1000_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Deploy 0.5.15 about virtual", groups = {"contract", "daily"})
  public void test01OverrideContract515() {
    String contractName = "Z";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_virtual001");
  String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_virtual001");
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "5", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethod.triggerContract(contractAddress, "setBool(bool)", "true", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethod.triggerContract(contractAddress, "setString(string)", "\"1q2w\"", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(5, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "y()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "z()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "3171327700000000000000000000000000000000000000000000000000000000",
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @Test(enabled = true, description = "Deploy 0.6.0 about virtual", groups = {"contract", "daily"})
  public void test02OverrideContract060() {
    String filePath = "./src/test/resources/soliditycode/virtual001.sol";
  String contractName = "Z";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod.getTransactionInfoById(txid,
        blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress,
        blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    txid = PublicMethod.triggerContract(contractAddress, "setValue(uint256)", "5", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethod.triggerContract(contractAddress, "setBool(bool)", "true", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    txid = PublicMethod.triggerContract(contractAddress, "setString(string)", "\"1q2w\"", false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "x()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(5, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "y()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals(1, ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "z()", "#",
            false, 0, 0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals("SUCCESS", transactionExtention.getResult().getCode().toString());
    Assert
        .assertEquals("0000000000000000000000000000000000000000000000000000000000000020"
                + "0000000000000000000000000000000000000000000000000000000000000004"
                + "3171327700000000000000000000000000000000000000000000000000000000",
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()));
  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    PublicMethod.sendcoin(fromAddress, balance, dev001Address, dev001Key,
        blockingStubFull);  }
}


