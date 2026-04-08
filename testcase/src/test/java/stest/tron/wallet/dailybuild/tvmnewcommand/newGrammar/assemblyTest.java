package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
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
public class assemblyTest extends TronBaseTest {
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
  String filePath = "./src/test/resources/soliditycode/assemblyTest.sol";
  String contractName = "assemblyTest";
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

  @Test(enabled = true, description = "get assembly references fuction number, type: uint", groups = {"contract", "daily"})
  public void test01AssemblyReferencesUint() {
    String methodStr = "getZuint()";
  String argStr = "";
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit,"0",0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals(1,ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    methodStr = "getZuint2()";
  String txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
  int ContractResult = ByteArray.toInt(infoById.get()
        .getContractResult(0).toByteArray());
    Assert.assertEquals(1,ContractResult);


  }

  @Test(enabled = true, description = "get assembly references fuction number, type: boolen", groups = {"contract", "daily"})
  public void test02AssemblyReferencesBoolen() {
    String methodStr = "getZbool()";
  String argStr = "";

    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argStr, false,
            0, maxFeeLimit,"0",0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertEquals(1,ByteArray.toInt(transactionExtention.getConstantResult(0).toByteArray()));

    methodStr = "getZbool2()";
  String txid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertEquals(0,infoById.get().getResultValue());
  int ContractResult = ByteArray.toInt(infoById.get()
        .getContractResult(0).toByteArray());
    Assert.assertEquals(1,ContractResult);
  }
}
