package stest.tron.wallet.dailybuild.trctoken;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.TronBaseTest;
import stest.tron.wallet.common.client.utils.MultiNode;
@Slf4j
@MultiNode
public class ContractTrcToken031 extends TronBaseTest {


  private static final long now = System.currentTimeMillis();
  private static final long TotalSupply = 10000000L;
  private static ByteString assetAccountId = null;
  private static String tokenName = "testAssetIssue_" + Long.toString(now);
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
  byte[] transferTokenContractAddress;
  private String fullnode1 = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {  }

  @Test(enabled = true, description = "Deploy suicide contract", groups = {"contract", "daily"})
  public void deploy01TransferTokenContract() {
    PublicMethod.printAddress(dev001Key);

    Assert.assertTrue(PublicMethod.sendcoin(dev001Address, 4048000000L, foundationAddress,
            foundationKey, blockingStubFull));
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
    logger.info("assetId: " + assetAccountId);
  // deploy transferTokenContract
    int originEnergyLimit = 50000;
  String filePath = "src/test/resources/soliditycode/contractTrcToken031.sol";
  String contractName = "token";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    transferTokenContractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit,
            1000000000L, 0, originEnergyLimit, assetAccountId.toStringUtf8(),
            100, null, dev001Key, dev001Address, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(transferTokenContractAddress);

    /*Assert.assertFalse(PublicMethod.sendcoin(transferTokenContractAddress,
            1000000000L, foundationAddress, foundationKey, blockingStubFull));*/
  }

  @Test(enabled = true, description = "Trigger suicide contract,toaddress is myself", groups = {"contract", "daily"})
  public void deploy02TransferTokenContract() {
    Long beforeAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long beforeAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long beforeBalanceContractAddress = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("beforeAssetIssueCount:" + beforeAssetIssueContractAddress);
    logger.info("beforeAssetIssueDevAddress:" + beforeAssetIssueDevAddress);
    logger.info("beforeBalanceContractAddress:" + beforeBalanceContractAddress);
  String param =
        "\"" + Base58.encode58Check(transferTokenContractAddress)
            + "\"";
  final String triggerTxid = PublicMethod.triggerContract(transferTokenContractAddress,
        "kill(address)", param, false, 0, 1000000000L,
            "0", 0, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
            .getTransactionInfoById(triggerTxid, blockingStubFull);
    Assert.assertTrue(infoById.get().getResultValue() == 0);
  Long afterAssetIssueDevAddress = PublicMethod
        .getAssetIssueValue(dev001Address, assetAccountId, blockingStubFull);
  Long afterAssetIssueContractAddress = PublicMethod
        .getAssetIssueValue(transferTokenContractAddress, assetAccountId, blockingStubFull);
  Long afterBalanceContractAddress = PublicMethod.queryAccount(transferTokenContractAddress,
        blockingStubFull).getBalance();

    logger.info("afterAssetIssueCount:" + afterAssetIssueDevAddress);
    logger.info("afterAssetIssueDevAddress:" + afterAssetIssueContractAddress);
    logger.info("afterBalanceContractAddress:" + afterBalanceContractAddress);


    if(PublicMethod.allowTvmSelfdestructRestrictionIsActive(blockingStubFull)) {
      Assert.assertEquals(beforeBalanceContractAddress, afterBalanceContractAddress );
      Assert.assertEquals(beforeAssetIssueContractAddress, afterAssetIssueContractAddress);
    }else {
      Assert.assertTrue(afterBalanceContractAddress == 0);
      Assert.assertTrue(afterAssetIssueContractAddress == 0);
    }
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.unFreezeBalance(dev001Address, dev001Key, 1, dev001Address, blockingStubFull);
    PublicMethod.freeResource(dev001Address, dev001Key, foundationAddress, blockingStubFull);  }

}


