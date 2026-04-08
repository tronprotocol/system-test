package stest.tron.wallet.dailybuild.tvmnewcommand.istanbul;

import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
public class Create2IstanbulTest001 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/create2Istanbul.sol";
  String contractName = "create2Istanbul";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
      .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, testKey001,
        testAddress001, blockingStubFull);
  }

  /**
   * Create2 Algorithm Changed
   * Before: according to msg.sender`s Address, salt, bytecode to get create2 Address
   * After : according to contract`s Address, salt, bytecode to get create2 Address
   * The calculated Create2 address should be same as get(bytes1,bytes,uint256)
   */

  @Test(enabled = true, description = "create2 Algorithm Change", groups = {"contract", "daily"})
  public void create2IstanbulTest001() {
    String filePath = "src/test/resources/soliditycode/create2Istanbul.sol";
  String contractName = "B";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String methodStr = "deploy(bytes,uint256)";
  String argStr = "\"" + code + "\"," + "1";
  String txid = PublicMethod
        .triggerContract(contractAddress, methodStr, argStr, false, 0, maxFeeLimit,
        testAddress001, testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    TransactionInfo option = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull).get();
  String returnHex = ByteArray.toHexString(option.getContractResult(0).toByteArray());

    Assert.assertEquals(0,option.getResultValue());
  String methodStr2 = "get(bytes1,bytes,uint256)";
  String argStr2 = "\"41\",\"" + code + "\"," + 1;
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr2, argStr2,
        false, 0,
        maxFeeLimit, "0", 0, testAddress001, testKey001, blockingStubFull);
  String getHex = ByteArray.toHexString(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(returnHex,getHex);

  }
}
