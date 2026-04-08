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
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class enumAndStructTest extends TronBaseTest {
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
    PublicMethod.printAddress(testKey001);
  String filePath = "src/test/resources/soliditycode/enumAndStruct.sol";
  String contractName = "enumAndStructTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0, 100, null,
            testFoundationKey, testFoundationAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "get Enum and Struct", groups = {"contract", "daily"})
  public void EnumAndStructTest001() {


    String methodStr = "getvalue()";
  String argStr = "";
  String TriggerTxid = PublicMethod.triggerContract(contractAddress, methodStr, argStr, false,
        0, maxFeeLimit, testFoundationAddress, testFoundationKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> transactionInfo = PublicMethod
        .getTransactionInfoById(TriggerTxid, blockingStubFull);

    logger.info("transactionInfo: " + transactionInfo.get());
    Assert.assertEquals(0,transactionInfo.get().getResultValue());
    Assert.assertTrue(transactionInfo.get().getFee() < maxFeeLimit);
    Assert.assertEquals(1,
        ByteArray.toInt(transactionInfo.get().getContractResult(0).toByteArray()));
  }


}
