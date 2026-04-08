package stest.tron.wallet.dailybuild.trctoken;

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
public class ContractTrcToken034 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  byte[] transferTokenContractAddress;  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 4048000000L,
        foundationAddress, testKey002, blockingStubFull));
    logger.info(
        "dev001Address:" + Base58.encode58Check(dev001Address));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 4048000000L,
        foundationAddress, testKey002, blockingStubFull));
    logger.info(
        "user001Address:" + Base58.encode58Check(user001Address));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }


  @Test(enabled = true, description = "Deploy after transfertoken execute require contract", groups = {"contract", "daily"})
  public void deploy01TransferTokenContract() {

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        100, start, end, 1, description, url, 10000L,
        10000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
  // deploy transferTokenContract
    int originEnergyLimit = 50000;
  String filePath = "src/test/resources/soliditycode/contractTrcToken034.sol";
  String contractName = "token";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    transferTokenContractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            100000000L, 0, originEnergyLimit, assetAccountId.toStringUtf8(),
            50, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true, description = "Trigger after transfertoken execute require contract", groups = {"contract", "daily"})
  public void deploy02TransferTokenContract() {
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long beforeAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long beforeAssetIssueUserAddress = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId, blockingStubFull);
  Long beforeAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
  String param =
        "\"" + Base58.encode58Check(user001Address) + "\",1,\"" + assetAccountId
            .toStringUtf8()
            + "\"";
  final String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "failTransferTokenRevert(address,uint256,trcToken)",
        param, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
            .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);

    Account infoafter = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long afterAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueUserAddress = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId, blockingStubFull);

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);

    Assert.assertEquals(beforeAssetIssueDevAddress, afterAssetIssueDevAddress);
    Assert.assertEquals(beforeAssetIssueUserAddress, afterAssetIssueUserAddress);
    Assert.assertEquals(beforeAssetIssueContractAddress, afterAssetIssueContractAddress);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(user001Address, user001Key, foundationAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(dev001Address, dev001Key, 1, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(user001Address, user001Key, 1, user001Address, blockingStubFull);  }


}


