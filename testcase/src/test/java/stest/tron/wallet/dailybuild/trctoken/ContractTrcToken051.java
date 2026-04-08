package stest.tron.wallet.dailybuild.trctoken;

import static org.tron.protos.Protocol.TransactionInfo.code.FAILED;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.TronBaseTest;
@Slf4j
public class ContractTrcToken051 extends TronBaseTest {

  private static final long TotalSupply = 10000000L;
  private static final long now = System.currentTimeMillis();
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }


  @Test(enabled = true, description = "TransferToken to contract developer tokenID is 0", groups = {"contract", "daily"})
  public void deployTransferTokenContract() {

    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 2048000000,
            foundationAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 6048000000L,
            foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // freeze balance
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(dev001Address, 204800000,
        0, 1, dev001Key, blockingStubFull));

    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(user001Address, 2048000000,
        0, 1, user001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        100, start, end, 1, description, url, 10000L,
        10000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
  // deploy transferTokenContract
    String filePath = "./src/test/resources/soliditycode/contractTrcToken051.sol";
  String contractName = "tokenTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  byte[] transferTokenContractAddress;
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 10000, assetAccountId.toStringUtf8(),
            0, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(user001Address,
        blockingStubFull);
    info = PublicMethod.queryAccount(user001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long beforeAssetIssueCount = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId, blockingStubFull);
  Long beforeAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long beforeAssetIssueDev = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueCount);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDev:" + beforeAssetIssueDev);
  String fakeassetAccountId = Long.toString(0L);
  String param = "\"" + Base58.encode58Check(dev001Address)
        + "\"," + fakeassetAccountId + ",\"1\"";
  final String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "TransferTokenTo(address,trcToken,uint256)",
        param, false, 0, 100000000L, "0",
        0, user001Address, user001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
            .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertFalse(infoById.get().getResultValue() == 0);
    Assert.assertEquals(FAILED, infoById.get().getResult());
    Assert.assertEquals("REVERT opcode executed",
            infoById.get().getResMessage().toStringUtf8());
    Account infoafter = PublicMethod.queryAccount(user001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(user001Address,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterAssetIssueCount = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId, blockingStubFull);
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long afterAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueDev = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueCount);
    logger.info("afterAssetIssueContractAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueDev:" + afterAssetIssueDev);

    Assert.assertEquals(beforeAssetIssueCount, afterAssetIssueCount);
    Assert.assertEquals(beforeAssetIssueContractAddress, afterAssetIssueContractAddress);
    Assert.assertEquals(beforeAssetIssueDev, afterAssetIssueDev);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.unFreezeBalance(dev001Address, dev001Key, 1, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(user001Address, user001Key, 1, user001Address, blockingStubFull);
    PublicMethod.freeResource(dev001Address, dev001Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(user001Address, user001Key, foundationAddress, blockingStubFull);  }


}


