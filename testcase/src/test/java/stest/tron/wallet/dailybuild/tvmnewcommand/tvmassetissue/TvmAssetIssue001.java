package stest.tron.wallet.dailybuild.tvmnewcommand.tvmassetissue;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class TvmAssetIssue001 extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = 10000000000L;
  private static String name = "testAssetIssue_" + Long.toString(now);
  private static String abbr = "testAsset_" + Long.toString(now);
  private static String description = "desc_" + Long.toString(now);
  private static String url = "url_" + Long.toString(now);
  private static String assetIssueId = null;
  private byte[] contractAddress = null;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] dev002Address = ecKey2.getAddress();
  private String dev002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    PublicMethod.printAddress(dev002Key);
  }

  @Test(enabled = false, description = "tokenIssue normal", groups = {"contract", "daily"})
  public void tokenIssueNormal() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/tvmAssetIssue001.sol";
  String contractName = "tvmAssetIssue001";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1050000000L;
  final String deployTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod
        .getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    long contractAddressBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);

    AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(dev001Address, blockingStubFull);
    Account info = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);

    /*String param = "0000000000000000000000000000000000007465737441737365744973737565"
        + "0000000000000000000074657374417373657431353938333439363637393631"
        + "0000000000000000000000000000000000000000000000000000000000989680"
        + "0000000000000000000000000000000000000000000000000000000000000001";*/
    String tokenName = PublicMethod.stringToHexString(name);
  String tokenAbbr = PublicMethod.stringToHexString(abbr);
  String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
  String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
  String txid = PublicMethod.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    logger.info("getAssetV2Map(): " + PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map());
    long assetIssueValue = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(dev001Address, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
    long contractAddressBalance2 = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance - 1024000000L, contractAddressBalance2);

    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
  String methodTransferToken = "transferToken(address,uint256,trcToken)";
    txid = PublicMethod.triggerContract(contractAddress, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    long assetIssueValueAfter = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long dev002AssetValue = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(100L, dev002AssetValue);
  }

  @Test(enabled = false, description = "updateAsset normal", groups = {"contract", "daily"})
  public void updateAssetNormal() {
    AccountResourceMessage resourceInfo = PublicMethod
        .getAccountResource(dev001Address, blockingStubFull);
    Account info = PublicMethod.queryAccount(dev001Address, blockingStubFull);
  Long beforeBalance = info.getBalance();
  Long beforeEnergyUsed = resourceInfo.getEnergyUsed();
  Long beforeNetUsed = resourceInfo.getNetUsed();
  Long beforeFreeNetUsed = resourceInfo.getFreeNetUsed();
    logger.info("beforeBalance:" + beforeBalance);
    logger.info("beforeEnergyUsed:" + beforeEnergyUsed);
    logger.info("beforeNetUsed:" + beforeNetUsed);
    logger.info("beforeFreeNetUsed:" + beforeFreeNetUsed);
  String param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
  String methodUpdateAsset = "updateAsset(trcToken,string,string)";
  String txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    AssetIssueContract assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  Long fee = infoById.get().getFee();
  Long netUsed = infoById.get().getReceipt().getNetUsage();
  Long energyUsed = infoById.get().getReceipt().getEnergyUsage();
  Long netFee = infoById.get().getReceipt().getNetFee();
    long energyUsageTotal = infoById.get().getReceipt().getEnergyUsageTotal();
    logger.info("fee:" + fee);
    logger.info("netUsed:" + netUsed);
    logger.info("energyUsed:" + energyUsed);
    logger.info("netFee:" + netFee);
    logger.info("energyUsageTotal:" + energyUsageTotal);
    Protocol.Account infoafter = PublicMethod.queryAccount(dev001Address, blockingStubFull);
    GrpcAPI.AccountResourceMessage resourceInfoafter = PublicMethod
        .getAccountResource(dev001Address, blockingStubFull);
  Long afterBalance = infoafter.getBalance();
  Long afterEnergyUsed = resourceInfoafter.getEnergyUsed();
  Long afterNetUsed = resourceInfoafter.getNetUsed();
  Long afterFreeNetUsed = resourceInfoafter.getFreeNetUsed();
    logger.info("afterBalance:" + afterBalance);
    logger.info("afterEnergyUsed:" + afterEnergyUsed);
    logger.info("afterNetUsed:" + afterNetUsed);
    logger.info("afterFreeNetUsed:" + afterFreeNetUsed);
    Assert.assertTrue(afterBalance + fee == beforeBalance);
    Assert.assertTrue(beforeEnergyUsed + energyUsed >= afterEnergyUsed);
    Assert.assertTrue(beforeFreeNetUsed + netUsed >= afterFreeNetUsed);
    Assert.assertTrue(beforeNetUsed + netUsed >= afterNetUsed);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);  }
}
