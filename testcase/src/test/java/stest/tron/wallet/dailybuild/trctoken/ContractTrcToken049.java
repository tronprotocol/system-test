package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletGrpc;
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
public class ContractTrcToken049 extends TronBaseTest {


  private static final long TotalSupply = 10000000L;
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
  private ManagedChannel channelFull1 = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull1 = null;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);
  private static int randomInt(int minInt, int maxInt) {
    return (int) Math.round(Math.random() * (maxInt - minInt) + minInt);
  }


  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  /**
   * constructor.
   */
  public ByteString createAssetissue(byte[] devAddress, String devKey, String tokenName) {

    Long start = System.currentTimeMillis() + 2000;
  Long end = System.currentTimeMillis() + 1000000000;

    logger.info("The token name: " + tokenName);
  //Create a new AssetIssue success.
    Assert.assertTrue(PublicMethod.createAssetIssue(devAddress, tokenName, TotalSupply, 1,
        100, start, end, 1, description, url, 10000L, 10000L,
        1L, 1L, devKey, blockingStubFull));

    ByteString assetAccountId = PublicMethod.queryAccount(devAddress, blockingStubFull)
            .getAssetIssuedID();
    logger.info("The tokenID: " + assetAccountId);

    return assetAccountId;
  }

  @Test(enabled = true, description = "TransferToken to myself", groups = {"contract", "daily"})
  public void deployTransferTokenContract() {

    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 2048000000,
            foundationAddress, testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(user001Address, 4048000000L,
            foundationAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // freeze balance
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(dev001Address, 204800000,
        0, 1, dev001Key, blockingStubFull));

    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(user001Address, 2048000000,
        0, 1, user001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String tokenName = "testAI_" + randomInt(10000, 90000);
    ByteString tokenId = createAssetissue(user001Address, user001Key, tokenName);

    PublicMethod.transferAsset(dev001Address, tokenId.toByteArray(), 101, user001Address,
        user001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // deploy transferTokenContract
    String filePath = "./src/test/resources/soliditycode/contractTrcToken049.sol";
  String contractName = "tokenTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  String txid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0L, 100, 10000, tokenId.toStringUtf8(),
            0, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
  byte[] transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();

    AccountResourceMessage resourceInfo = PublicMethod.getAccountResource(user001Address,
        blockingStubFull);
  Long beforeBalance = PublicMethod
            .queryAccount(user001Address, blockingStubFull).getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
  Long beforeAssetIssueCount =
        PublicMethod.getAssetIssueValue(user001Address, tokenId, blockingStubFull);
  Long beforeAssetIssueContractAddress = PublicMethod
            .getAssetIssueValue(transferTokenContractAddress, tokenId, blockingStubFull);
  final Long beforeAssetIssueDev = PublicMethod.getAssetIssueValue(dev001Address, tokenId,
        blockingStubFull);
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
    logger.info("beforeAssetIssueCount:" + beforeAssetIssueCount);
    logger.info("beforeAssetIssueContractAddress:" + beforeAssetIssueContractAddress);
  // user trigger A to transfer token to B
    String param =
        "\"" + Base58.encode58Check(dev001Address) + "\",\"" + tokenId
            .toStringUtf8()
            + "\",\"1\"";
  final String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "TransferTokenTo(address,trcToken,uint256)",
        param, false, 0, 100000000L, "0",
        0, user001Address, user001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Account infoafter = PublicMethod.queryAccount(user001Address, blockingStubFull);
    AccountResourceMessage resourceInfoafter = PublicMethod.getAccountResource(user001Address,
        blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterAssetIssueCount =
        PublicMethod.getAssetIssueValue(user001Address, tokenId, blockingStubFull);
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
  Long afterAssetIssueContractAddress = PublicMethod
            .getAssetIssueValue(transferTokenContractAddress, tokenId, blockingStubFull);
  final Long afterAssetIssueDev = PublicMethod.getAssetIssueValue(dev001Address, tokenId,
        blockingStubFull);
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    logger.info("afterAssetIssueCount:" + afterAssetIssueCount);
    logger.info("afterAssetIssueContractAddress:" + afterAssetIssueContractAddress);

    infoById = PublicMethod
        .getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    Assert.assertTrue(infoById.get().getResultValue() == 1);
    Assert.assertEquals(beforeBalance, afterBalance);
    Assert.assertEquals(beforeAssetIssueCount, afterAssetIssueCount);
    Assert.assertTrue(beforeAssetIssueContractAddress == afterAssetIssueContractAddress);

    Assert.assertTrue(beforeAssetIssueDev == afterAssetIssueDev);
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
    PublicMethod.freeResource(user001Address, user001Key, foundationAddress, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, testKey002, 0, dev001Address, blockingStubFull);
    PublicMethod.unFreezeBalance(foundationAddress, testKey002, 0, user001Address, blockingStubFull);  }

}


