package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class constructorDefaultsTest extends TronBaseTest {  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(dev001Key);  }

  @Test(enabled = true, description = "Constructor default test", groups = {"contract", "daily"})
  public void Test01ConstructorDefault() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 200000000L, foundationAddress, foundationKey, blockingStubFull));
    AccountResourceMessage accountResource = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/ConstructorDefaults.sol";
  String contractName = "testIsContract";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String constructorStr = "constructor(bool)";
  String data = "0";
  String txid = PublicMethod
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  byte[] contractaddress = null;
    Optional<TransactionInfo> info = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(info.toString());
    Assert.assertTrue(info.get().getResultValue() == 0);
    data = "false";
    txid = PublicMethod
        .deployContractWithConstantParame(contractName, abi, code, constructorStr, data, "",
            maxFeeLimit, 0L, 100, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    info = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(info.toString());
    Assert.assertTrue(info.get().getResultValue() == 0);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
    PublicMethod.sendcoin(foundationAddress, balance, dev001Address, dev001Key,
        blockingStubFull);  }
}


