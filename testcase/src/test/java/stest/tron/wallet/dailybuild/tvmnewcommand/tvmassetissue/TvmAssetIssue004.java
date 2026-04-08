package stest.tron.wallet.dailybuild.tvmnewcommand.tvmassetissue;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
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
public class TvmAssetIssue004 extends TronBaseTest {

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
  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  private byte[] dev003Address = ecKey3.getAddress();
  private String dev003Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    PublicMethod.printAddress(dev002Key);
    PublicMethod.printAddress(dev003Key);
  }

  @Test(enabled = false, description = "tokenIssue and transfer to account", groups = {"contract", "daily"})
  public void tokenIssueAndTransferToAccount() {
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

    long contractAddressBalance2 = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance - 1024000000L, contractAddressBalance2);
  // transfer token to create exist account
    Assert.assertTrue(PublicMethod
        .sendcoin(dev003Address, 10_000_000L, dev001Address, dev001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long dev001AddressBalanceBefore = PublicMethod.queryAccount(dev001Address, blockingStubFull)
        .getBalance();
    logger.info("dev001AddressBalanceBefore: " + dev001AddressBalanceBefore);
    param = "\"" + Base58.encode58Check(dev003Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
  String methodTransferToken = "transferToken(address,uint256,trcToken)";
    txid = PublicMethod.triggerContract(contractAddress, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long dev001AddressBalanceAfter = PublicMethod.queryAccount(dev001Address, blockingStubFull)
        .getBalance();
    logger.info("dev001AddressBalanceAfter: " + dev001AddressBalanceAfter);
    long assetIssueValueAfter = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long dev003AssetValue = PublicMethod
        .getAssetIssueValue(dev003Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(100L, dev003AssetValue);
  // transfer token to create new account
    long dev001AddressBalanceBefore1 = PublicMethod.queryAccount(dev001Address, blockingStubFull)
        .getBalance();
    logger.info("dev001AddressBalanceBefore1: " + dev001AddressBalanceBefore1);
    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
    txid = PublicMethod.triggerContract(contractAddress, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertTrue(infoById.get().getReceipt().getEnergyUsageTotal() > 30000);
    long dev001AddressBalanceAfter2 = PublicMethod.queryAccount(dev001Address, blockingStubFull)
        .getBalance();
    logger.info("dev001AddressBalanceAfter2: " + dev001AddressBalanceAfter2);
    long assetIssueValueAfter1 = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long dev002AssetValue = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValueAfter - 100L, assetIssueValueAfter1);
    Assert.assertEquals(100L, dev002AssetValue);
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);  }
}
