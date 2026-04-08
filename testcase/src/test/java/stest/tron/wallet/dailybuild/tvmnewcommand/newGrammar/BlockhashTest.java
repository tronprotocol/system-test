package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class BlockhashTest extends TronBaseTest {
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

    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 100_000_000L, fromAddress,
        testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/BlockHash.sol";
  String contractName = "TestBlockHash";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();

    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "BlockHash should not be change after command OR", groups = {"contract", "daily"})
  public void test01BlockHashWithOR() {
    String methodStr = "testOR1(bytes32)";
  String argStr = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
  String txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
  String ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
  // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
  // blockHash before OR should equals to blockHash after OR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));

    methodStr = "testOR2(bytes32)";
    txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
  // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
  // blockHash before OR should equals to blockHash after OR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));
  }

  @Test(enabled = true, description = "BlockHash should not be change after command AND", groups = {"contract", "daily"})
  public void test02BlockHashWithAND() {
    String methodStr = "testAND1(bytes32)";
  String argStr = "0000000000000000000000000000000000000000000000000000000000000000";
  String txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
  String ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
  // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
  // blockHash before AND should equals to blockHash after AND
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));

    methodStr = "testAND2(bytes32)";
    txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
  // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
  // blockHash before AND should equals to blockHash after AND
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));
  }

  @Test(enabled = true, description = "BlockHash should not be change after command XOR", groups = {"contract", "daily"})
  public void test03BlockHashWithXOR() {
    String methodStr = "testXOR1(bytes32)";
  String argStr = "00000000000000000000000000000000ffffffffffffffffffffffffffffffff";
  String txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
  String ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
  // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
  // blockHash before XOR should equals to blockHash after XOR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));

    methodStr = "testXOR2(bytes32)";
    txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, true,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
    ContractResult = ByteArray.toHexString(infoById.get()
        .getContractResult(0).toByteArray());
  // 3 bytes32
    Assert.assertEquals(192, ContractResult.length());
  // blockHash before XOR should equals to blockHash after XOR
    Assert.assertEquals(ContractResult.substring(0,64),ContractResult.substring(128));
  }

}
