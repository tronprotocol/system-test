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
public class ContractTrcToken026 extends TronBaseTest {


  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  byte[] btestAddress;
  byte[] ctestAddress;
  byte[] transferTokenContractAddress;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] dev001Address = ecKey1.getAddress();
  String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] user001Address = ecKey2.getAddress();
  String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private String fullnodeLocal = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }


  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "Deploy transferToken contract", groups = {"contract", "daily"})
  public void deploy01TransferTokenContract001() {
    PublicMethod.printAddress(dev001Key);
    PublicMethod.printAddress(user001Key);

    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 4048000000L,
            foundationAddress, foundationKey, blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // freeze balance
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(dev001Address, 204800000,
        0, 1, dev001Key, blockingStubFull));
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
  String filePath = "src/test/resources/soliditycode/contractTrcToken026.sol";
  String contractName = "B";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    btestAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            0L, 0, originEnergyLimit, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address, blockingStubFull);
  String contractName1 = "C";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
  String code1 = retMap1.get("byteCode").toString();
  String abi1 = retMap1.get("abI").toString();
    ctestAddress = PublicMethod
        .deployContract(contractName1, abi1, code1, "", maxFeeLimit,
            0L, 0, originEnergyLimit, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address,
            blockingStubFull);
  String contractName2 = "token";
    HashMap retMap2 = PublicMethod.getBycodeAbi(filePath, contractName2);
  String code2 = retMap2.get("byteCode").toString();
  String abi2 = retMap2.get("abI").toString();
    transferTokenContractAddress = PublicMethod
        .deployContract(contractName2, abi2, code2, "", maxFeeLimit,
            1000000000L, 0, originEnergyLimit, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = true, description = "Multistage call transferToken use right tokenID", groups = {"contract", "daily"})
  public void deploy02TransferTokenContract002() {
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
  Long beforeAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long beforeAssetIssueBAddress = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  Long beforeAssetIssueCAddress = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long beforeBalanceContractAddress = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueBAddress:" + beforeAssetIssueBAddress);
    logger.info("beforeAssetIssueCAddress:" + beforeAssetIssueCAddress);

    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeBalanceContractAddress:" + beforeBalanceContractAddress);
  // 1.user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(btestAddress) + "\",\"" + Base58.encode58Check(ctestAddress)
            + "\",\"" + Base58.encode58Check(transferTokenContractAddress)
            + "\",1,\"" + assetAccountId.toStringUtf8() + "\"";
  final String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "testInCall(address,address,address,uint256,trcToken)",
        param, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key, blockingStubFull);
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
  Long afterAssetIssueCAddress = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueBAddress:" + afterAssetIssueBAddress);
    logger.info("afterAssetIssueCAddress:" + afterAssetIssueCAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
    Assert.assertEquals(afterBalanceContractAddress, beforeBalanceContractAddress);
    Assert.assertTrue(afterAssetIssueContractAddress == beforeAssetIssueContractAddress + 1);
    Assert.assertTrue(afterAssetIssueBAddress == beforeAssetIssueBAddress);
    Assert.assertTrue(afterAssetIssueCAddress == beforeAssetIssueCAddress - 1);
  }

  @Test(enabled = true, description = "Multistage call transferToken use fake tokenID", groups = {"contract", "daily"})
  public void deploy03TransferTokenContract003() {
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
  Long afterAssetIssueCAddress = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterAssetIssueBAddress:" + afterAssetIssueBAddress);
    logger.info("afterAssetIssueCAddress:" + afterAssetIssueCAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);
  //3. user trigger A to transfer token to B
    int i = randomInt(6666666, 9999999);

    ByteString tokenId1 = ByteString.copyFromUtf8(String.valueOf(i));
  String param1 =
        "\"" + Base58.encode58Check(btestAddress) + "\",\"" + Base58.encode58Check(ctestAddress)
            + "\",\"" + Base58.encode58Check(transferTokenContractAddress)
            + "\",1,\"" + tokenId1
            .toStringUtf8()
            + "\"";
  final String triggerTxid1 = PublicMethod.triggerContract(transferTokenContractAddress,
        "testInCall(address,address,address,uint256,trcToken)",
        param1, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account infoafter1 = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter1 = PublicMethod.getAccountResource(dev001Address,
        blockingStubFull);
  Long afterBalance1 = infoafter1.getBalance();
  Long afterEnergyUsed1 = resourceInfoafter1.getEnergyUsed();
  Long afterAssetIssueDevAddress1 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long afterNetUsed1 = resourceInfoafter1.getNetUsed();
  Long afterFreeNetUsed1 = resourceInfoafter1.getFreeNetUsed();
  final Long afterAssetIssueContractAddress1 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueBAddress1 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueCAddress1 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress1 = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("afterBalance1:" + afterBalance1);
    logger.info("afterEnergyUsed1:" + afterEnergyUsed1);
    logger.info("afterNetUsed1:" + afterNetUsed1);
    logger.info("afterFreeNetUsed1:" + afterFreeNetUsed1);
    logger.info("afterAssetIssueCount1:" + afterAssetIssueDevAddress1);
    logger.info("afterAssetIssueDevAddress1:" + afterAssetIssueContractAddress1);
    logger.info("afterAssetIssueBAddress1:" + afterAssetIssueBAddress1);
    logger.info("afterAssetIssueCAddress1:" + afterAssetIssueCAddress1);
    logger.info("afterBalanceContractAddress1:" + afterBalanceContractAddress1);

    Optional<TransactionInfo> infoById1 = PublicMethod
        .getTransactionInfoById(triggerTxid1, blockingStubFull);
    Assert.assertTrue(infoById1.get().getResultValue() == 0);
    Assert.assertEquals(afterBalanceContractAddress, afterBalanceContractAddress1);
    Assert.assertTrue(afterAssetIssueContractAddress == afterAssetIssueContractAddress1);
    Assert.assertTrue(afterAssetIssueBAddress == afterAssetIssueBAddress1);
    Assert.assertTrue(afterAssetIssueCAddress == afterAssetIssueCAddress1);
  }

  @Test(enabled = true, description = "Multistage call transferToken token value not enough", groups = {"contract", "daily"})
  public void deploy04TransferTokenContract004() {

    final Long afterAssetIssueContractAddress1 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueBAddress1 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueCAddress1 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  final Long afterBalanceContractAddress1 =
        PublicMethod.queryAccount(transferTokenContractAddress, blockingStubFull).getBalance();
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress1);
    logger.info("afterAssetIssueBAddress:" + afterAssetIssueBAddress1);
    logger.info("afterAssetIssueCAddress:" + afterAssetIssueCAddress1);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress1);
  //4. user trigger A to transfer token to B
    String param2 =
        "\"" + Base58.encode58Check(btestAddress) + "\",\"" + Base58.encode58Check(ctestAddress)
            + "\",\"" + Base58.encode58Check(transferTokenContractAddress)
            + "\",10000000,\"" + assetAccountId
            .toStringUtf8()
            + "\"";
  final String triggerTxid2 = PublicMethod.triggerContract(transferTokenContractAddress,
        "testInCall(address,address,address,uint256,trcToken)",
        param2, false, 0, 1000000000L, "0",
        0, dev001Address, dev001Key,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById2 = PublicMethod
            .getTransactionInfoById(triggerTxid2, blockingStubFull);
    Assert.assertTrue(infoById2.get().getResultValue() == 0);
  Long afterAssetIssueContractAddress2 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueBAddress2 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueCAddress2 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress2 = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("afterAssetIssueDevAddress2:" + afterAssetIssueContractAddress2);
    logger.info("afterAssetIssueBAddress2:" + afterAssetIssueBAddress2);
    logger.info("afterAssetIssueCAddress2:" + afterAssetIssueCAddress2);
    logger.info("afterBalanceContractAddress2:" + afterBalanceContractAddress2);

    Assert.assertEquals(afterBalanceContractAddress1, afterBalanceContractAddress2);
    Assert.assertTrue(afterAssetIssueContractAddress1 == afterAssetIssueContractAddress2);
    Assert.assertTrue(afterAssetIssueBAddress1 == afterAssetIssueBAddress2);
    Assert.assertTrue(afterAssetIssueCAddress1 == afterAssetIssueCAddress2);
  }

  @Test(enabled = true, description = "Multistage call transferToken calltoken ID not exist", groups = {"contract", "daily"})
  public void deploy05TransferTokenContract005() {

    final Long afterAssetIssueContractAddress2 = PublicMethod.getAssetIssueValue(
            transferTokenContractAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueBAddress2 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueCAddress2 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  final Long afterBalanceContractAddress2 = PublicMethod
            .queryAccount(transferTokenContractAddress, blockingStubFull).getBalance();
  //5. user trigger A to transfer token to B
    String param3 =
        "\"" + Base58.encode58Check(btestAddress) + "\",\"" + Base58.encode58Check(ctestAddress)
            + "\",\"" + Base58.encode58Check(transferTokenContractAddress)
            + "\",1,\"" + assetAccountId
            .toStringUtf8()
            + "\"";
  int i = randomInt(6666666, 9999999);

    ByteString tokenId1 = ByteString.copyFromUtf8(String.valueOf(i));
  final String triggerTxid3 = PublicMethod.triggerContract(transferTokenContractAddress,
        "testInCall(address,address,address,uint256,trcToken)",
        param3, false, 0, 1000000000L, tokenId1
            .toStringUtf8(), 1, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(triggerTxid3 == null);
  Long afterAssetIssueDevAddress3 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long afterAssetIssueContractAddress3 = PublicMethod.getAssetIssueValue(
            transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueBAddress3 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueCAddress3 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress3 = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("afterAssetIssueCount3:" + afterAssetIssueDevAddress3);
    logger.info("afterAssetIssueDevAddress3:" + afterAssetIssueContractAddress3);
    logger.info("afterAssetIssueBAddress3:" + afterAssetIssueBAddress3);
    logger.info("afterAssetIssueCAddress3:" + afterAssetIssueCAddress3);
    logger.info("afterBalanceContractAddress3:" + afterBalanceContractAddress3);

    Assert.assertEquals(afterBalanceContractAddress2, afterBalanceContractAddress3);
    Assert.assertTrue(afterAssetIssueContractAddress2 == afterAssetIssueContractAddress3);
    Assert.assertTrue(afterAssetIssueBAddress2 == afterAssetIssueBAddress3);
    Assert.assertTrue(afterAssetIssueCAddress2 == afterAssetIssueCAddress3);
  }

  @Test(enabled = true, description = "Multistage call transferToken calltoken value not enough", groups = {"contract", "daily"})
  public void deploy06TransferTokenContract006() {

    final Long afterAssetIssueContractAddress3 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueBAddress3 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueCAddress3 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  final Long afterBalanceContractAddress3 =
        PublicMethod.queryAccount(transferTokenContractAddress, blockingStubFull).getBalance();
  //6. user trigger A to transfer token to B
    String param4 =
        "\"" + Base58.encode58Check(btestAddress) + "\",\"" + Base58.encode58Check(ctestAddress)
            + "\",\"" + Base58.encode58Check(transferTokenContractAddress)
            + "\",1,\"" + assetAccountId.toStringUtf8() + "\"";
  final String triggerTxid4 = PublicMethod.triggerContract(transferTokenContractAddress,
        "testInCall(address,address,address,uint256,trcToken)",
        param4, false, 0, 1000000000L, assetAccountId
            .toStringUtf8(), 100000000, dev001Address, dev001Key, blockingStubFull);
    Assert.assertTrue(triggerTxid4 == null);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterAssetIssueContractAddress4 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueBAddress4 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueCAddress4 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress4 = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("afterAssetIssueDevAddress4:" + afterAssetIssueContractAddress4);
    logger.info("afterAssetIssueBAddress4:" + afterAssetIssueBAddress4);
    logger.info("afterAssetIssueCAddress4:" + afterAssetIssueCAddress4);
    logger.info("afterBalanceContractAddress4:" + afterBalanceContractAddress4);

    Assert.assertEquals(afterBalanceContractAddress3, afterBalanceContractAddress4);
    Assert.assertTrue(afterAssetIssueContractAddress3 == afterAssetIssueContractAddress4);
    Assert.assertTrue(afterAssetIssueBAddress3 == afterAssetIssueBAddress4);
    Assert.assertTrue(afterAssetIssueCAddress3 == afterAssetIssueCAddress4);
  }

  @Test(enabled = true, description = "Multistage call transferToken use right tokenID,tokenvalue", groups = {"contract", "daily"})
  public void deploy07TransferTokenContract007() {
    final Long afterAssetIssueDevAddress4 = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  final Long afterAssetIssueContractAddress4 = PublicMethod.getAssetIssueValue(
            transferTokenContractAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueBAddress4 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  final Long afterAssetIssueCAddress4 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  final Long afterBalanceContractAddress4 =
        PublicMethod.queryAccount(transferTokenContractAddress, blockingStubFull).getBalance();
  //2. user trigger A to transfer token to B
    String param5 =
        "\"" + Base58.encode58Check(btestAddress) + "\",\"" + Base58.encode58Check(ctestAddress)
            + "\",\"" + Base58.encode58Check(transferTokenContractAddress)
            + "\",1,\"" + assetAccountId.toStringUtf8() + "\"";
  final String triggerTxid5 = PublicMethod.triggerContract(transferTokenContractAddress,
        "testInCall(address,address,address,uint256,trcToken)",
        param5, false, 0, 1000000000L, assetAccountId
            .toStringUtf8(), 1, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long afterAssetIssueContractAddress5 = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueBAddress5 = PublicMethod
        .getAssetIssueValue(btestAddress, assetAccountId, blockingStubFull);
  Long afterAssetIssueCAddress5 = PublicMethod
        .getAssetIssueValue(ctestAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress5 = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("afterAssetIssueDevAddress5:" + afterAssetIssueContractAddress5);
    logger.info("afterAssetIssueBAddress5:" + afterAssetIssueBAddress5);
    logger.info("afterAssetIssueCAddress5:" + afterAssetIssueCAddress5);
    logger.info("afterBalanceContractAddress5:" + afterBalanceContractAddress5);

    Optional<TransactionInfo> infoById5 = PublicMethod
        .getTransactionInfoById(triggerTxid5, blockingStubFull);
    Assert.assertTrue(infoById5.get().getResultValue() == 0);
    Assert.assertEquals(afterBalanceContractAddress4, afterBalanceContractAddress5);
    Assert.assertTrue(afterAssetIssueContractAddress4 + 2 == afterAssetIssueContractAddress5);
    Assert.assertTrue(afterAssetIssueBAddress4 == afterAssetIssueBAddress5);
    Assert.assertTrue(afterAssetIssueCAddress4 - 1 == afterAssetIssueCAddress5);

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


