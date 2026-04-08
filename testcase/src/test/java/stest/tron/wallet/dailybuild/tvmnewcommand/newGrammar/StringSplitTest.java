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
public class StringSplitTest extends TronBaseTest {

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
  String filePath = "./src/test/resources/soliditycode/stringSplit.sol";
  String contractName = "testStringSplit";
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

  @Test(enabled = true, description = "get s1 n1", groups = {"contract", "daily"})
  public void test01GetS1N1() {
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS1()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s12,./",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS1N1()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s12,./",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

  @Test(enabled = true, description = "get s2 n2", groups = {"contract", "daily"})
  public void test01GetS2N2() {
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS2()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s123?\\'.",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS2N2()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("s123?\'.",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

  @Test(enabled = true, description = "get s3 n3", groups = {"contract", "daily"})
  public void test01GetS3N3() {
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS3()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS3N3()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

  @Test(enabled = true, description = "get s4 n4", groups = {"contract", "daily"})
  public void test01GetS4N4() {
    TransactionExtention transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS4()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));

    transactionExtention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "getS4N4()", "#", false, 0,
            0, "0", 0, dev001Address, dev001Key, blockingStubFull);
    Assert.assertThat(transactionExtention.getResult().getCode().toString(),
        containsString("SUCCESS"));
    Assert.assertEquals("AB",
        PublicMethod.hexStringToString(PublicMethod.removeAll0sAtTheEndOfHexStr(
            ByteArray.toHexString(transactionExtention.getConstantResult(0).toByteArray()))
            .substring(128)));
  }

}
