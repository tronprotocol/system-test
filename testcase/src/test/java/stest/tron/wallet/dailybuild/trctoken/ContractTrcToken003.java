package stest.tron.wallet.dailybuild.trctoken;

import static org.tron.api.GrpcAPI.Return.response_code.CONTRACT_VALIDATE_ERROR;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class ContractTrcToken003 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 1000L;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
  private static ByteString assetAccountDev = null;
  private static ByteString assetAccountUser = null;
  private String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  private String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] user001Address = ecKey2.getAddress();
  private String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);

  }

  @Test(enabled = true, description = "DeployContract with exception condition", groups = {"contract", "daily"})
  public void deployTransferTokenContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 1100_000_000L, fromAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod
        .sendcoin(user001Address, 1100_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress,
//        PublicMethod.getFreezeBalanceCount(dev001Address, dev001Key, 50000L, blockingStubFull), 0,
//        1, ByteString.copyFrom(dev001Address), foundationKey, blockingStubFull));
//    Assert.assertTrue(PublicMethod.freezeBalanceForReceiver(fromAddress, 10_000_000L + PublicMethod.randomFreezeAmount.getAndAdd(1), 0, 0,
//        ByteString.copyFrom(dev001Address), foundationKey, blockingStubFull));
//    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;
  //dev Create a new AssetIssue
    Assert.assertTrue(PublicMethod
        .createAssetIssue(dev001Address, tokenName, TotalSupply, 1, 10000, start, end, 1,
            description, url, 100000L, 100000L, 1L, 1L, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetAccountDev = PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The assetAccountDev token name: " + tokenName);
    logger.info("The assetAccountDev token ID: " + assetAccountDev.toStringUtf8());

    start = System.currentTimeMillis() + 2000;
    end = System.currentTimeMillis() + 1000000000;
  //user Create a new AssetIssue
    Assert.assertTrue(PublicMethod
        .createAssetIssue(user001Address, tokenName, TotalSupply, 1, 10000, start, end, 1,
            description, url, 100000L, 100000L, 1L, 1L, user001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountUser = PublicMethod.queryAccount(user001Address, blockingStubFull)
        .getAssetIssuedID();
    logger.info("The assetAccountUser token name: " + tokenName);
    logger.info("The assetAccountUser token ID: " + assetAccountUser.toStringUtf8());
  //before deploy, check account resource
    AccountResourceMessage accountResource = PublicMethod
        .getAccountResource(dev001Address, blockingStubFull);
    long energyLimit = accountResource.getEnergyLimit();
    long energyUsage = accountResource.getEnergyUsed();
    long balanceBefore = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountBefore = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountDev, blockingStubFull);
  Long userAssetCountBefore = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountUser, blockingStubFull);

    logger.info("before energyLimit is " + energyLimit);
    logger.info("before energyUsage is " + energyUsage);
    logger.info("before balanceBefore is " + balanceBefore);
    logger.info(
        "before dev has AssetId: " + assetAccountDev.toStringUtf8() + ", devAssetCountBefore: "
            + devAssetCountBefore);
    logger.info(
        "before dev has AssetId: " + assetAccountUser.toStringUtf8() + ", userAssetCountBefore: "
            + userAssetCountBefore);
  String filePath = "./src/test/resources/soliditycode/contractTrcToken003.sol";
  String contractName = "tokenTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  // the tokenId is not exist
    String fakeTokenId = Long.toString(Long.valueOf(assetAccountDev.toStringUtf8()) + 100);
  Long fakeTokenValue = 100L;

    GrpcAPI.Return response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert
        .assertEquals("contract validate error : No asset !".toLowerCase(),
            response.getMessage().toStringUtf8().toLowerCase());
  // deployer didn't have any such token
    fakeTokenId = assetAccountUser.toStringUtf8();
    fakeTokenValue = 100L;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : assetBalance must greater than 0.".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());
  // deployer didn't have any Long.MAX_VALUE
    fakeTokenId = Long.toString(Long.MAX_VALUE);
    fakeTokenValue = 100L;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : No asset !".toLowerCase(),
            response.getMessage().toStringUtf8().toLowerCase());
  // the tokenValue is not enough
    fakeTokenId = assetAccountDev.toStringUtf8();
    fakeTokenValue = devAssetCountBefore + 100;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, fakeTokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : assetBalance is not sufficient.".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());
  // tokenid is -1
    fakeTokenId = Long.toString(-1);
    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // tokenid is 100_0000L
    fakeTokenId = Long.toString(100_0000L);
    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());
  // tokenid is Long.MIN_VALUE
    fakeTokenId = Long.toString(Long.MIN_VALUE);
    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // tokenid is 0
    fakeTokenId = Long.toString(0);

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            fakeTokenId, 100, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals(
        ("contract validate error : invalid arguments "
            + "with tokenValue = 100, tokenId = 0").toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());
  // tokenvalue is less than 0
    fakeTokenValue = -1L;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            assetAccountDev.toStringUtf8(), fakeTokenValue, null, dev001Key, dev001Address,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenValue must be >= 0".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());

    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // tokenvalue is long.min
    fakeTokenValue = Long.MIN_VALUE;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, 0L, 0, 10000,
            assetAccountDev.toStringUtf8(), fakeTokenValue, null, dev001Key, dev001Address,
            blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenValue must be >= 0".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());
  String tokenId = Long.toString(-1);
    long tokenValue = 0;
    long callValue = 10;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    tokenId = Long.toString(Long.MIN_VALUE);
    tokenValue = 0;
    callValue = 10;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    tokenId = Long.toString(1000000);
    tokenValue = 0;
    callValue = 10;

    response = PublicMethod
        .deployContractAndGetResponse(contractName, abi, code, "", maxFeeLimit, callValue, 0, 10000,
            tokenId, tokenValue, null, dev001Key, dev001Address, blockingStubFull);

    Assert.assertFalse(response.getResult());
    Assert.assertEquals(CONTRACT_VALIDATE_ERROR, response.getCode());
    Assert.assertEquals("contract validate error : tokenId must be > 1000000".toLowerCase(),
        response.getMessage().toStringUtf8().toLowerCase());

    accountResource = PublicMethod.getAccountResource(dev001Address, blockingStubFull);
    energyLimit = accountResource.getEnergyLimit();
    energyUsage = accountResource.getEnergyUsed();
    long balanceAfter = PublicMethod.queryAccount(dev001Key, blockingStubFull).getBalance();
  Long devAssetCountAfter = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountDev, blockingStubFull);
  Long userAssetCountAfter = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountUser, blockingStubFull);

    logger.info("after energyLimit is " + energyLimit);
    logger.info("after energyUsage is " + energyUsage);
    logger.info("after balanceAfter is " + balanceAfter);
    logger.info(
        "after dev has AssetId: " + assetAccountDev.toStringUtf8() + ", devAssetCountAfter: "
            + devAssetCountAfter);
    logger.info(
        "after user has AssetId: " + assetAccountDev.toStringUtf8() + ", userAssetCountAfter: "
            + userAssetCountAfter);

    Assert.assertEquals(devAssetCountBefore, devAssetCountAfter);
    Assert.assertEquals(userAssetCountBefore, userAssetCountAfter);

  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);
    PublicMethod.freeResource(user001Address, user001Key, fromAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, foundationKey, 1, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(fromAddress, foundationKey, 0, dev001Address, blockingStubFull);  }
}


