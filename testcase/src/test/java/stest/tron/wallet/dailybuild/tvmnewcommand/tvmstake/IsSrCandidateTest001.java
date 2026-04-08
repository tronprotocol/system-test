package stest.tron.wallet.dailybuild.tvmnewcommand.tvmstake;

import java.util.HashMap;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
  public class IsSrCandidateTest001 extends TronBaseTest {
  private String testFoundationKey = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private byte[] testFoundationAddress = PublicMethod.getFinalAddress(testFoundationKey);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey1.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private byte[] contractAddress;

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethod.printAddress(testKey001);    PublicMethod
        .sendcoin(testAddress001, 1000_000_000L, testFoundationAddress, testFoundationKey,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "src/test/resources/soliditycode/isSRCandidate.sol";
  String contractName = "TestIsSRCandidate";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, 100, null, testKey001,
            testAddress001, blockingStubFull);
  }

  @Test(enabled = false, description = "Witness Address should be true", groups = {"contract", "daily"})
  void tvmStakeTest001() {
    String methodStr = "isSRCandidateTest(address)";
  String argsStr = "\"" + PublicMethod.getAddressString(witnessKey) + "\"";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,1);
  }

  @Test(enabled = false, description = "Account Address should be false", groups = {"contract", "daily"})
  void tvmStakeTest002() {
    String methodStr = "isSRCandidateTest(address)";
  String argsStr = "\"" + Base58.encode58Check(testAddress001) + "\"";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,0);
  }

  @Test(enabled = false, description = "zero Address(0x00) should be false", groups = {"contract", "daily"})
  void tvmStakeTest003() {
    String methodStr = "zeroAddressTest()";
  String argsStr = "";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,0);
  }

  @Test(enabled = false, description = "Contract Address should be false", groups = {"contract", "daily"})
  void tvmStakeTest004() {
    String methodStr = "localContractAddrTest()";
  String argsStr = "";
    TransactionExtention returns = PublicMethod
        .triggerConstantContractForExtention(contractAddress, methodStr, argsStr,
            false, 0, maxFeeLimit, "", 0, testAddress001, testKey001, blockingStubFull);
  int isSR = ByteArray.toInt(returns.getConstantResult(0).toByteArray());

    Assert.assertEquals(isSR,0);
  }

}
