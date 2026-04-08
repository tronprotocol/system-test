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
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class ContractTrcToken023 extends TronBaseTest {


  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountId = null;
  byte[] transferTokenContractAddress;
  byte[] btestAddress;
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
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    PublicMethod.printAddress(user001Key);
  }

  @Test(enabled = true, description = "deploy contract for transfer token", groups = {"contract", "daily"})
  public void deploy01TransferTokenContract() {

    Assert.assertTrue(PublicMethod
            .sendcoin(dev001Address, 4048000000L, foundationAddress, foundationKey, blockingStubFull));
  // freeze balance
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(dev001Address, 204800000,
        0, 1, dev001Key, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod.createAssetIssue(dev001Address, tokenName, TotalSupply, 1,
        10000, start, end, 1, description, url, 100000L,
        100000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  int i = randomInt(6666666, 9999999);
    ByteString tokenId1 = ByteString.copyFromUtf8(String.valueOf(i));
    assetAccountId = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
  // deploy transferTokenContract
    int originEnergyLimit = 50000;
  String filePath2 = "src/test/resources/soliditycode/contractTrcToken023.sol";
  String contractName2 = "tokenTest";
    HashMap retMap2 = PublicMethod.getBycodeAbi(filePath2, contractName2);
  String code2 = retMap2.get("byteCode").toString();
  String abi2 = retMap2.get("abI").toString();
    transferTokenContractAddress = PublicMethod
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000000L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
  String contractName = "B";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath2, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    btestAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            1000000000L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    /*Assert
        .assertFalse(PublicMethod.sendcoin(transferTokenContractAddress, 1000000000L, foundationAddress,
            foundationKey, blockingStubFull));
    Assert
        .assertFalse(PublicMethod.sendcoin(btestAddress, 1000000000L, foundationAddress,
            foundationKey, blockingStubFull));*/

    // devAddress transfer token to userAddress
    PublicMethod.transferAsset(transferTokenContractAddress, assetAccountId.toByteArray(),
            100, dev001Address, dev001Key, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "transfer token which from address does not have", groups = {"contract", "daily"})
  public void deploy02TransferTokenContract() {
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long beforeAssetIssueDevAddress = PublicMethod.getAssetIssueValue(dev001Address,
            assetAccountId,blockingStubFull);
  Long beforeAssetIssueContractAddress = PublicMethod.getAssetIssueValue(
            transferTokenContractAddress, assetAccountId,blockingStubFull);
  Long beforeAssetIssueBAddress = PublicMethod.getAssetIssueValue(btestAddress,
            assetAccountId, blockingStubFull);
  Long beforeBalanceContractAddress = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueBAddress:" + beforeAssetIssueBAddress);

    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeBalanceContractAddress:" + beforeBalanceContractAddress);
  String param = "\"" + Base58.encode58Check(btestAddress) +  "\",\""
            + assetAccountId.toStringUtf8() + "\",\"1\"";
  final String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "TransferTokenTo(address,trcToken,uint256)", param, false, 0,
            1000000000L, "0", 0, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

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
  Long afterAssetIssueBAddress = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();


    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueBAddress:" + afterAssetIssueBAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);


    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 1);

    Assert.assertEquals(afterBalanceContractAddress, beforeBalanceContractAddress);
    Assert.assertTrue(afterAssetIssueContractAddress == beforeAssetIssueContractAddress);
    Assert.assertTrue(afterAssetIssueBAddress == beforeAssetIssueBAddress);

  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, foundationAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, foundationKey, 0, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, foundationKey, 1, dev001Address, blockingStubFull);  }


}


