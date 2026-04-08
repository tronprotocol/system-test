package stest.tron.wallet.dailybuild.tvmnewcommand.tvmassetissue;

import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.TransactionInfo;
import org.tron.protos.contract.AssetIssueContractOuterClass.AssetIssueContract;
import org.tron.protos.contract.SmartContractOuterClass.SmartContract;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class TvmAssetIssue005 extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = 10000000000L;
  private static String name = "testAssetIssue_" + Long.toString(now);
  private static String abbr = "testAsset_" + Long.toString(now);
  private static String description = "desc_" + Long.toString(now);
  private static String url = "url_" + Long.toString(now);
  private byte[] contractAddress = null;
  private long contractAddressBalance;
  private ECKey ecKey1 = new ECKey(Utils.getRandom());
  private byte[] dev001Address = ecKey1.getAddress();
  private String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ECKey ecKey2 = new ECKey(Utils.getRandom());
  private byte[] dev002Address = ecKey2.getAddress();
  private String dev002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ECKey ecKey3 = new ECKey(Utils.getRandom());
  private byte[] dev003Address = ecKey3.getAddress();
  private String dev003Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  private ECKey ecKey4 = new ECKey(Utils.getRandom());
  private byte[] dev004Address = ecKey4.getAddress();
  private String dev004Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());

  /**
   * constructor.
   */
  @BeforeClass(enabled = false)
  public void beforeClass() {    PublicMethod.printAddress(dev001Key);
    PublicMethod.printAddress(dev002Key);
    PublicMethod.printAddress(dev003Key);
    PublicMethod.printAddress(dev004Key);
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 7000_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "tokenIssue and updateAsset with suicide to account", groups = {"contract", "daily"})
  public void tokenIssue001AndSuicideToAccount() {
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue005.sol";
  String contractName = "tvmAssetIssue005";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1050000000L;
  // deploy
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
    contractAddressBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);
  // tokenIssue
    name = "testAssetIssu1_" + Long.toString(now);
    abbr = "testAsse1_" + Long.toString(now);
  String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
  String tokenName = PublicMethod.stringToHexString(name);
  String tokenAbbr = PublicMethod.stringToHexString(abbr);
  String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
  String txid = PublicMethod.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
  String assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
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
    AssetIssueContract assetIssueByName = PublicMethod.getAssetIssueByName(name, blockingStubFull);
    AssetIssueContract assetIssueByAccount = PublicMethod
        .getAssetIssueByAccount(contractAddress, blockingStubFull).get().getAssetIssue(0);
    AssetIssueContract assetIssueListByName = PublicMethod
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());
    long contractAddressBalance2 = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance - 1024000000L, contractAddressBalance2);
  // transferToken
    String methodTransferToken = "transferToken(address,uint256,trcToken)";
    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
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
  // updateAsset
    String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    long returnId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnId);
    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  // selfdestruct
    String methodSuicide = "SelfdestructTest(address)";
    param = "\"" + Base58.encode58Check(dev003Address) + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodSuicide, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID().size());
    Assert.assertEquals(0,
        PublicMethod.getAssetIssueByAccount(dev003Address, blockingStubFull).get()
            .getAssetIssueCount());
    Assert.assertEquals(0,
        PublicMethod.queryAccount(dev003Address, blockingStubFull).getAssetIssuedID().size());
    long contractAssetCountDev003 = PublicMethod
        .getAssetIssueValue(dev003Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValueAfter, contractAssetCountDev003);
    assetIssueValue = PublicMethod.queryAccount(dev003Address, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(assetIssueValueAfter, assetIssueValue);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
    assetIssueByName = PublicMethod.getAssetIssueByName(name, blockingStubFull);
    assetIssueByAccount = PublicMethod
        .getAssetIssueByAccount(contractAddress, blockingStubFull).get().getAssetIssue(0);
    assetIssueListByName = PublicMethod
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());
    dev002AssetValue = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(100L, dev002AssetValue);

    Assert.assertTrue(PublicMethod
        .sendcoin(dev002Address, 100_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  // transferAsset,success
    Assert.assertTrue(PublicMethod.transferAsset(dev002Address, assetIssueId.getBytes(), 100L,
        dev003Address, dev003Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long assetIssueValueDev002 = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    long assetIssueValueDev003 = PublicMethod
        .getAssetIssueValue(dev003Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(200L, assetIssueValueDev002);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueDev003);

    Assert.assertTrue(PublicMethod.transferAsset(dev004Address, assetIssueId.getBytes(), 102L,
        dev002Address, dev002Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long assetIssueValueDev002After = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    long assetIssueValueDev004 = PublicMethod
        .getAssetIssueValue(dev004Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(102L, assetIssueValueDev004);
    Assert.assertEquals(assetIssueValueDev002 - 102L, assetIssueValueDev002After);
  // updateAsset,will fail
    Assert.assertFalse(PublicMethod
        .updateAsset(dev003Address, "updateDesc1".getBytes(), "updateURL1".getBytes(), 1L, 2L,
            dev003Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertFalse(PublicMethod
        .updateAsset(contractAddress, "updateDesc2".getBytes(), "updateURL2".getBytes(), 3L, 4L,
            dev003Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  }

  @Test(enabled = false, description = "tokenIssue and updateAsset with suicide to contract", groups = {"contract", "daily"})
  public void tokenIssue002AndSuicideToContract() {
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue005.sol";
  String contractName = "tvmAssetIssue005";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1050000000L;
  // deploy
    String deployTxid = PublicMethod
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
    byte[] contractAddress2 = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod
        .getContract(contractAddress2, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    long contractAddressBalance2 = PublicMethod.queryAccount(contractAddress2, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance2);

    deployTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    smartContract = PublicMethod
        .getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());
    contractAddressBalance = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getBalance();
    Assert.assertEquals(callvalue, contractAddressBalance);
  // tokenIssue
    name = "testAssetIssu2_" + Long.toString(now);
    abbr = "testAsse2_" + Long.toString(now);
  String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
  String tokenName = PublicMethod.stringToHexString(name);
  String tokenAbbr = PublicMethod.stringToHexString(abbr);
  String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
  String txid = PublicMethod.triggerContract(contractAddress2, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
  String assetIssueId = PublicMethod.queryAccount(contractAddress2, blockingStubFull)
        .getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    logger.info("getAssetV2Map(): " + PublicMethod.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map());
    long assetIssueValue = PublicMethod.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress2),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
    AssetIssueContract assetIssueByName = PublicMethod.getAssetIssueByName(name, blockingStubFull);
    AssetIssueContract assetIssueByAccount = PublicMethod
        .getAssetIssueByAccount(contractAddress2, blockingStubFull).get().getAssetIssue(0);
    AssetIssueContract assetIssueListByName = PublicMethod
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());
    long contractAddressBalanceAfter2 = PublicMethod
        .queryAccount(contractAddress2, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance2 - 1024000000L, contractAddressBalanceAfter2);
  // transferToken
    String methodTransferToken = "transferToken(address,uint256,trcToken)";
    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
    txid = PublicMethod.triggerContract(contractAddress2, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long assetIssueValueAfter = PublicMethod.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long dev002AssetValue = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(100L, dev002AssetValue);

    param =
        "\"" + Base58.encode58Check(contractAddress) + "\"," + 50 + ",\"" + assetIssueId + "\"";
    txid = PublicMethod.triggerContract(contractAddress2, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long assetIssueValueAfter2 = PublicMethod.queryAccount(contractAddress2, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    long contractAssetValue = PublicMethod
        .getAssetIssueValue(contractAddress, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValueAfter - 50L, assetIssueValueAfter2);
    Assert.assertEquals(50L, contractAssetValue);
  // selfdestruct
    String methodSuicide = "SelfdestructTest(address)";
    param = "\"" + Base58.encode58Check(contractAddress) + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress2, methodSuicide, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    Assert.assertEquals(0,
        PublicMethod.queryAccount(contractAddress2, blockingStubFull).getAssetIssuedID().size());
    Assert.assertEquals(0,
        PublicMethod.getAssetIssueByAccount(contractAddress, blockingStubFull).get()
            .getAssetIssueCount());
    Assert.assertEquals(0,
        PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID().size());
    assetIssueValue = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(assetIssueValueAfter2 + 50L, assetIssueValue);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress2),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
    assetIssueByName = PublicMethod.getAssetIssueByName(name, blockingStubFull);
    assetIssueByAccount = PublicMethod
        .getAssetIssueByAccount(contractAddress2, blockingStubFull).get().getAssetIssue(0);
    assetIssueListByName = PublicMethod
        .getAssetIssueListByName(name, blockingStubFull)
        .get().getAssetIssue(0);
    Assert.assertEquals(assetIssueId, assetIssueByName.getId());
    Assert.assertEquals(name, ByteArray.toStr(assetIssueByAccount.getName().toByteArray()));
    Assert.assertEquals(assetIssueId, assetIssueListByName.getId());
  // transferToken,success
    methodTransferToken = "transferToken(address,uint256,trcToken)";
    param = "\"" + Base58.encode58Check(dev002Address) + "\"," + 100 + ",\"" + assetIssueId + "\"";
    txid = PublicMethod.triggerContract(contractAddress, methodTransferToken, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    assetIssueValueAfter = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    dev002AssetValue = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(assetIssueValue - 100L, assetIssueValueAfter);
    Assert.assertEquals(200L, dev002AssetValue);

    Assert.assertTrue(PublicMethod.transferAsset(dev004Address, assetIssueId.getBytes(), 12L,
        dev002Address, dev002Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    long assetIssueValueDev002After = PublicMethod
        .getAssetIssueValue(dev002Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    long assetIssueValueDev004 = PublicMethod
        .getAssetIssueValue(dev004Address, ByteString.copyFrom(assetIssueId.getBytes()),
            blockingStubFull);
    Assert.assertEquals(12L, assetIssueValueDev004);
    Assert.assertEquals(dev002AssetValue - 12L, assetIssueValueDev002After);
  // updateAsset,will fail
    String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    param = "\"" + assetIssueId + "\",\"updateUrl\",\"updateDesc\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long returnId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(0, assetIssueById.getDescription().size());
    Assert.assertEquals(0, assetIssueById.getUrl().size());
    Assert.assertEquals(Base58.encode58Check(contractAddress2),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  }

  @Test(enabled = false, description = "tokenIssue and updateAsset suicide with create2", groups = {"contract", "daily"})
  public void tokenIssue003AndSuicideWithCreate2() {
    String filePath = "./src/test/resources/soliditycode/tvmAssetIssue005.sol";
  String contractName = "B";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
  final String deployTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            0, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
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
  String methodTokenIssue = "deploy(uint256)";
  String param = "" + 6;
    logger.info("param: " + param);
  String txid = PublicMethod.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    TransactionInfo transactionInfo = infoById.get();
    logger.info("EnergyUsageTotal: " + transactionInfo.getReceipt().getEnergyUsageTotal());
    logger.info("NetUsage: " + transactionInfo.getReceipt().getNetUsage());
    logger.info(
        "the value: " + PublicMethod
            .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray()));
    List<String> retList = PublicMethod
        .getStrings(transactionInfo.getLogList().get(0).getData().toByteArray());
  Long actualSalt = ByteArray.toLong(ByteArray.fromHexString(retList.get(1)));
    logger.info("actualSalt: " + actualSalt);
  byte[] tmpAddress = new byte[20];
    System.arraycopy(ByteArray.fromHexString(retList.get(0)),
        12, tmpAddress, 0, 20);
  String addressHex = "41" + ByteArray.toHexString(tmpAddress);
    logger.info("address_hex: " + addressHex);
  String addressFinal = Base58.encode58Check(ByteArray.fromHexString(addressHex));
    logger.info("address_final: " + addressFinal);
  byte[] callContractAddress = WalletClient.decodeFromBase58Check(addressFinal);

    Assert.assertTrue(PublicMethod
        .sendcoin(callContractAddress, 1500_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    name = "testAssetIssu3_" + Long.toString(now);
    abbr = "testAsse3_" + Long.toString(now);
    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
  String tokenName = PublicMethod.stringToHexString(name);
  String tokenAbbr = PublicMethod.stringToHexString(abbr);
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(callContractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
  String assetIssueId = PublicMethod.queryAccount(callContractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
  String methodSuicide = "SelfdestructTest(address)";
    param = "\"" + Base58.encode58Check(dev003Address) + "\"," + 10000000;
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(callContractAddress, methodSuicide, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    methodTokenIssue = "deploy(uint256)";
    param = "" + 6;
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(PublicMethod
        .sendcoin(callContractAddress, 1500_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    tokenName = PublicMethod.stringToHexString("testAssetIssue_11111");
    tokenAbbr = PublicMethod.stringToHexString("testAssetIssue_22222");
    param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 6;
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(callContractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
  String assetIssueId2 = PublicMethod.queryAccount(callContractAddress, blockingStubFull)
        .getAssetIssuedID().toStringUtf8();
    logger.info("assetIssueId2: " + assetIssueId2);
    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId2));
    Assert.assertEquals(Long.parseLong(assetIssueId) + 1, Long.parseLong(assetIssueId2));
    Assert.assertEquals(2,
        PublicMethod.getAssetIssueByAccount(callContractAddress, blockingStubFull).get()
            .getAssetIssueCount());
  // updateAsset
    String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    param = "\"123\",\"updateURLURL\",\"updateDESCDESC\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(callContractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
    long returnId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnId);
  String newAssetIssueId = PublicMethod.queryAccount(callContractAddress, blockingStubFull)
        .getAssetIssuedID()
        .toStringUtf8();
    logger.info("newAssetIssueId: " + newAssetIssueId);
    AssetIssueContract newAssetIssueById = PublicMethod
        .getAssetIssueById(newAssetIssueId, blockingStubFull);
    Assert.assertEquals("testAssetIssue_11111",
        ByteArray.toStr(newAssetIssueById.getName().toByteArray()));
    Assert.assertEquals("testAssetIssue_22222",
        ByteArray.toStr(newAssetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals("updateDESCDESC",
            ByteArray.toStr(newAssetIssueById.getDescription().toByteArray()));
    Assert.assertEquals("updateURLURL", ByteArray.toStr(newAssetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, newAssetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(callContractAddress),
        Base58.encode58Check(newAssetIssueById.getOwnerAddress().toByteArray()));

    AssetIssueContract oldAssetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(oldAssetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(oldAssetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(0, oldAssetIssueById.getDescription().size());
    Assert.assertEquals(0, oldAssetIssueById.getUrl().size());
    Assert.assertEquals(6, oldAssetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(callContractAddress),
        Base58.encode58Check(oldAssetIssueById.getOwnerAddress().toByteArray()));
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);  }
}
