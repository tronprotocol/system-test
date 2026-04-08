package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;

@Slf4j
@MultiNode
public class AbstractTest extends TronBaseTest {

  private final byte[] fromAddress = PublicMethod.getFinalAddress(testKey002);
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "compile abstract contract 001", groups = {"contract", "daily"})
  public void test01CompileAbstractContract001() {
    String filePath = "./src/test/resources/soliditycode/abstract001.sol";
  String contractName = "abstract001";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    Assert.assertTrue(abi.length() > 0);
    Assert.assertTrue(code.length() == 0);
  }

  @Test(enabled = true, description = "compile abstract contract 002", groups = {"contract", "daily"})
  public void test02CompileAbstractContract002() {
    String filePath = "./src/test/resources/soliditycode/abstract002.sol";
  String contractName = "abstract002";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    Assert.assertTrue(abi.length() > 0);
    Assert.assertTrue(code.length() == 0);
  }

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


