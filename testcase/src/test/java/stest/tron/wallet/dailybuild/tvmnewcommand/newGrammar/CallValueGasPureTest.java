package stest.tron.wallet.dailybuild.tvmnewcommand.newGrammar;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.GrpcAPI.TransactionExtention;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class CallValueGasPureTest extends TronBaseTest {

  private final String foundationKey001 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] foundationAddress001 = PublicMethod.getFinalAddress(foundationKey001);
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] testAddress001 = ecKey1.getAddress();
  private String testKey001 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(testKey001);
  }

  @Test(enabled = true, description = "call.value.gas be pure", groups = {"contract", "daily"})
  public void test01DeployContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(testAddress001, 1000_000_000L, foundationAddress001, foundationKey001,
            blockingStubFull));
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress001, 100_000_000L, 0, 0,
            ByteString.copyFrom(testAddress001), foundationKey001, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(testAddress001, blockingStubFull);
    Protocol.Account info = PublicMethod.queryAccount(testKey001, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = accountResource.getEnergyUsed();
  Long beforeNetUsed = accountResource.getNetUsed();
  Long beforeFreeNetUsed = accountResource.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String filePath = "./src/test/resources/soliditycode/callValueGasPure.sol";
  String contractName = "C";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit, 0L, 0,
            10000, "0", 0, null, testKey001, testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = null;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    if (infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());

    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod.getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
  String param = "\"" + Base58.encode58Check(testAddress001) + "\"";
    TransactionExtention extention = PublicMethod
        .triggerConstantContractForExtention(contractAddress, "check(address)",
            param, false, 0, 1000000000L, "0", 0, testAddress001,
            testKey001, blockingStubFull);

    Assert.assertNotNull(extention);
    Assert.assertTrue(extention.hasResult());
    Assert.assertTrue(extention.getResult().getResult());

  }

  @AfterClass
  public void shutdown() throws InterruptedException {
    long balance = PublicMethod.queryAccount(testKey001, blockingStubFull).getBalance();
    PublicMethod
        .sendcoin(foundationAddress001, balance, testAddress001, testKey001, blockingStubFull);  }
}


