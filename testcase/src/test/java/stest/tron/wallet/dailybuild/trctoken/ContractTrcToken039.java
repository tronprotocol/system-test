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
public class ContractTrcToken039 extends TronBaseTest {

  private static final long TotalSupply = 10000000L;
  private static final long now = System.currentTimeMillis();
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
  byte[] proxyTestAddress;
  byte[] atestAddress;
  byte[] btestAddress;  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }


  @Test(enabled = true, description = "Deploy Proxy contract", groups = {"contract", "daily"})
  public void deploy01TransferTokenContract() {
    Assert
        .assertTrue(PublicMethod.sendcoin(dev001Address, 4048000000L, foundationAddress,
            testKey002, blockingStubFull));
    logger.info("dev001Address:" + Base58.encode58Check(dev001Address));
    Assert
        .assertTrue(PublicMethod.sendcoin(user001Address, 4048000000L, foundationAddress,
            testKey002, blockingStubFull));
    logger.info("user001Address:" + Base58.encode58Check(user001Address));
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
    int originEnergyLimit = 50000;
  String filePath = "src/test/resources/soliditycode/contractTrcToken039.sol";
  String contractName = "Proxy";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    proxyTestAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            1000L, 0, originEnergyLimit, assetAccountId.toStringUtf8(),
            1000, null, dev001Key, dev001Address,
            blockingStubFull);
  String contractName1 = "A";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
    atestAddress = PublicMethod
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String contractName2 = "B";
    HashMap retMap2 = PublicMethod.getBycodeAbi(filePath, contractName2);
  String code2 = retMap2.get("byteCode").toString();
  String abi2 = retMap2.get("abI").toString();
    btestAddress = PublicMethod
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            0L, 0, originEnergyLimit, "0",
            0, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true, dependsOnMethods = "deploy01TransferTokenContract",
      description = "Trigger Proxy contract use AddressA", groups = {"contract", "daily"})
  public void deploy02TransferTokenContract() {
    Account info;
    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long beforeAssetIssueUserAddress = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(proxyTestAddress, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueBAddress = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueAAddress = PublicMethod
        .getAssetIssueValue(atestAddress, assetAccountId,
            blockingStubFull);
  Long beforeBalanceContractAddress = PublicMethod.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
  Long beforeUserBalance = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();
    logger.info("beforeBalance:" + beforeBalance);

    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueBAddress:" + beforeAssetIssueBAddress);

    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeAssetIssueUserAddress:" + beforeAssetIssueUserAddress);
  String param =
        "\"" + Base58.encode58Check(atestAddress) + "\"";
  String param1 =
        "\"" + "1" + "\",\"" + Base58.encode58Check(user001Address) + "\",\"" + assetAccountId
            .toStringUtf8()
            + "\"";
  String triggerTxid = PublicMethod.triggerContract(proxyTestAddress,
        "upgradeTo(address)",
        param, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  final String triggerTxid1 = PublicMethod.triggerContract(proxyTestAddress,
        "trans(uint256,address,trcToken)",
        param1, false, 0, 1000000000L, assetAccountId
            .toStringUtf8(),
        1, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long afterAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(proxyTestAddress, assetAccountId,
            blockingStubFull);
  Long afterAssetIssueBAddress = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId,
            blockingStubFull);
  Long afterAssetIssueAAddress = PublicMethod
        .getAssetIssueValue(atestAddress, assetAccountId,
            blockingStubFull);
  Long afterAssetIssueUserAddress = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress = PublicMethod.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
  Long afterUserBalance = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueBAddress:" + afterAssetIssueBAddress);
    logger.info("afterAssetIssueUserAddress:" + afterAssetIssueUserAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);
    logger.info("afterUserBalance:" + afterUserBalance);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress == beforeAssetIssueUserAddress);
    Assert.assertTrue(afterBalanceContractAddress == beforeBalanceContractAddress - 1);
    Assert.assertTrue(afterAssetIssueContractAddress == beforeAssetIssueContractAddress + 1);
    Assert.assertTrue(afterAssetIssueDevAddress == beforeAssetIssueDevAddress - 1);
    Assert.assertTrue(afterUserBalance == beforeUserBalance + 1);
    Assert.assertTrue(afterAssetIssueUserAddress == afterAssetIssueUserAddress);
    Assert.assertTrue(afterAssetIssueBAddress == beforeAssetIssueBAddress);
  }

  @Test(enabled = true,dependsOnMethods = "deploy02TransferTokenContract",
      description = "Trigger Proxy contract use AddressB", groups = {"contract", "daily"})
  public void deploy03TransferTokenContract() {
    Account info1;
    AccountResourceMessage resourceInfo1 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
    info1 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance1 = info1.getBalance();
  Long beforeAssetIssueDevAddress1 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long beforeAssetIssueUserAddress1 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueContractAddress1 = PublicMethod
        .getAssetIssueValue(proxyTestAddress, assetAccountId,
            blockingStubFull);
  Long beforeAssetIssueBAddress1 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId,
            blockingStubFull);
  Long beforeBalanceContractAddress1 = PublicMethod.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
  Long beforeUserBalance1 = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();
    logger.info("beforeBalance1:" + beforeBalance1);
    logger.info("beforeAssetIssueContractAddress1:" + beforeAssetIssueContractAddress1);
    logger.info("beforeAssetIssueBAddress1:" + beforeAssetIssueBAddress1);

    logger.info("beforeAssetIssueDevAddress1:" + beforeAssetIssueDevAddress1);
    logger.info("beforeAssetIssueUserAddress1:" + beforeAssetIssueUserAddress1);
    logger.info("beforeBalanceContractAddress1:" + beforeBalanceContractAddress1);
    logger.info("beforeUserBalance1:" + beforeUserBalance1);
  String param3 =
        "\"" + Base58.encode58Check(btestAddress) + "\"";
  String param2 =
        "\"" + "1" + "\",\"" + Base58.encode58Check(user001Address) + "\",\"" + assetAccountId
            .toStringUtf8()
            + "\"";
  String triggerTxid2 = PublicMethod.triggerContract(proxyTestAddress,
        "upgradeTo(address)", param3, false, 0, 1000000000L,
            assetAccountId.toStringUtf8(), 1, dev001Address, dev001Key, blockingStubFull);
  String triggerTxid3 = PublicMethod.triggerContract(proxyTestAddress,
        "trans(uint256,address,trcToken)",
        param2, false, 0, 1000000000L, assetAccountId
            .toStringUtf8(),
        1, dev001Address, dev001Key,
        blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account infoafter1 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  Long afterBalance1 = infoafter1.getBalance();
  Long afterAssetIssueDevAddress1 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long afterAssetIssueContractAddress1 = PublicMethod
        .getAssetIssueValue(proxyTestAddress, assetAccountId,
            blockingStubFull);
  Long afterAssetIssueBAddress1 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId,
            blockingStubFull);
  Long afterAssetIssueUserAddress1 = PublicMethod
        .getAssetIssueValue(user001Address, assetAccountId,
            blockingStubFull);
  Long afterBalanceContractAddress1 = PublicMethod.queryAccount(proxyTestAddress,
        blockingStubFull).getBalance();
  Long afterUserBalance1 = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getBalance();

    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterAssetIssueCount1:" + afterAssetIssueDevAddress1);
    logger.info("afterAssetIssueDevAddress1:" + afterAssetIssueContractAddress1);
    logger.info("afterAssetIssueBAddress1:" + afterAssetIssueBAddress1);
    logger.info("afterAssetIssueUserAddress1:" + afterAssetIssueUserAddress1);
    logger.info("afterBalanceContractAddress1:" + afterBalanceContractAddress1);
    logger.info("afterUserBalance1:" + afterUserBalance1);

    Optional<TransactionInfo> infoById2 = PublicMethod
        .getTransactionInfoById(triggerTxid3, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
    Assert.assertTrue(afterAssetIssueUserAddress1 == beforeAssetIssueUserAddress1);
    Assert.assertTrue(afterBalanceContractAddress1 == beforeBalanceContractAddress1 - 1);
    Assert.assertTrue(afterAssetIssueContractAddress1 == beforeAssetIssueContractAddress1 + 1);
    Assert.assertTrue(afterAssetIssueDevAddress1 == beforeAssetIssueDevAddress1 - 1);
    Assert.assertTrue(afterUserBalance1 == beforeUserBalance1 + 1);
    Assert.assertTrue(afterAssetIssueUserAddress1 == afterAssetIssueUserAddress1);
    PublicMethod.unFreezeBalance(dev001Address, dev001Key, 1,
        dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(user001Address, user001Key, 1,
        user001Address, blockingStubFull);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, foundationAddress, blockingStubFull);
    PublicMethod.freeResource(user001Address, user001Key, foundationAddress, blockingStubFull);  }


}


