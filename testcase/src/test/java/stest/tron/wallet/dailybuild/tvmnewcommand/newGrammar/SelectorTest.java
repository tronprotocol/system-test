package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import static org.hamcrest.core.StringContains.containsString;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.TransactionExtention;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class SelectorTest extends TronBaseTest {

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
  String filePath = "./src/test/resources/soliditycode/selector.sol";
  String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
  final String aContractAddress = ByteArray.toHexString(contractAddress);

    contractName = "testSelector";
    retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    abi = retMap.get("abI").toString();
    code = retMap.get("byteCode").toString();
    code = PublicMethod.replaceCode(code, aContractAddress.substring(1));
    logger.info("code:" + code);

    contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "",
            maxFeeLimit, 0L, 0, 10000,
            "0", 0, null, dev001Key,
            dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "Get the selector of public or external library functions "
      + "through member variables", groups = {"contract", "daily"})
  public void test01GetSelector() {
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getselector2()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    logger.info("result: " + ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()));
    Assert.assertEquals("f8b2cb4f", PublicMethod.removeAll0sAtTheEndOfHexStr(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(0, 64)));
    Assert.assertEquals("b4cef28d", PublicMethod.removeAll0sAtTheEndOfHexStr(ByteArray
        .toHexString(transactionExtention.getConstantResult(0).toByteArray()).substring(64)));
  }

}
