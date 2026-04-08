package stest.tron.wallet.dailybuild.tvmnewcommand.tvmassetissue;

import java.util.HashMap;
import java.util.Map;
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
public class TvmAssetIssue003 extends TronBaseTest {

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

  @Test(enabled = false, description = "updateAsset illegal parameter verification", groups = {"contract", "daily"})
  public void updateAsset001IllegalParameterVerification() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 1100_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/tvmAssetIssue001.sol";
  String contractName = "tvmAssetIssue001";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1024000000L;
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
  // desc and url is trx, will success
    url = "trx";
    description = "trx";
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
  String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
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
  // desc.length is 201, will fail
    String descriptions =
        "desc_1234567890desc_1234567890desc_1234567890desc_1234567890desc_1234567890"
            + "desc_1234567890desc_1234567890desc_1234567890desc_1234567890desc_1234567890desc"
            + "_1234567890"
            + "desc_1234567890desc_1234567890desc_1";
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + descriptions + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
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
  // desc.length is "", will success
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(0, assetIssueById.getDescription().size());
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  // desc.length is chinese, will success
    description = "token说明";
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
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
  // url.length is 257, will fail
    String urls =
        "url_12345678901url_12345678901url_12345678901url_12345678901url_12345678901url_12345678901"
            + "url_12345678901url_12345678901url_12345678901url_12345678901url_12345678901url"
            + "_12345678901"
            + "url_12345678901url_12345678901url_12345678901url_12345678901url_12345678901ur";
    param = "\"" + assetIssueId + "\",\"" + urls + "\",\"" + description + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
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
  // url.length is "", will fail
    param = "\"" + assetIssueId + "\",\"\",\"" + description + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
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
  // url.length is chinese, will success
    url = "官网";
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
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
  }

  @Test(enabled = false, description = "updateAsset called multiple times in one contract", groups = {"contract", "daily"})
  public void updateAsset002CalledMultipleTimesInOneContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 1100_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/tvmAssetIssue002.sol";
  String contractName = "tvmAssetIssue002";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1024000000L;
  final String deployTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("infoById: " + infoById.get().getReceipt().getEnergyUsageTotal());
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
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);

    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
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
  // updateAsset
    description = "desc1_" + Long.toString(now);
    url = "url1_" + Long.toString(now);
  String description2 = "desc2_" + Long.toString(now);
  String url2 = "url2_" + Long.toString(now);
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\",\"" + url2 + "\",\""
        + description2 + "\"";
    logger.info("param: " + param);
  String methodUpdateAsset = "updateAsset(trcToken,string,string,string,string)";
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals(description2, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url2, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  }

  @Test(enabled = false, description = "updateAsset revert", groups = {"contract", "daily"})
  public void updateAsset003Revert() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 1500_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/tvmAssetIssue003.sol";
  String contractName = "tvmAssetIssue003";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1225000000L;
  final String deployTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(deployTxid, blockingStubFull);
    logger.info("infoById: " + infoById.get().getReceipt().getEnergyUsageTotal());
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
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
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
  // updateAsset
    String description1 =
        "desc_1234567890desc_1234567890desc_1234567890desc_1234567890desc_1234567890"
            + "desc_1234567890desc_1234567890desc_1234567890desc_1234567890desc_1234567890desc"
            + "_1234567890"
            + "desc_1234567890desc_1234567890desc_1";
  String url1 = "url1_" + Long.toString(now);
    param = "\"" + assetIssueId + "\",\"" + url1 + "\",\"" + description1 + "\",\"" + Base58
        .encode58Check(dev002Address) + "\"";
    logger.info("param: " + param);
  String methodUpdateAsset = "updateAssetAndTransfer(trcToken,string,string,address)";
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(0, returnAssetId);
    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    logger.info("assetIssueById: " + assetIssueById);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(0, assetIssueById.getDescription().size());
    Assert.assertEquals(0, assetIssueById.getUrl().size());
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));

    long balance = PublicMethod.queryAccount(dev002Address, blockingStubFull).getBalance();
    Assert.assertEquals(200000000L, balance);
  }

  @Test(enabled = false, description = "updateAsset call another contract in one contract", groups = {"contract", "daily"})
  public void updateAsset004CallAnotherInOneContract() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 3100_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/tvmAssetIssue004.sol";
  String contractName = "tvmAssetIssue004";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1030000000L;
  String deployTxid = PublicMethod
        .deployContractAndGetTransactionInfoById(contractName, abi, code, "", maxFeeLimit,
            callvalue, 0, 10000, "0", 0L, null, dev001Key, dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<TransactionInfo> infoById = PublicMethod
        .getTransactionInfoById(deployTxid, blockingStubFull);
    if (deployTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage()
          .toStringUtf8());
    }
    contractAddress = infoById.get().getContractAddress().toByteArray();
    SmartContract smartContract = PublicMethod
        .getContract(contractAddress, blockingStubFull);
    Assert.assertNotNull(smartContract.getAbi());

    callvalue = 1024000000L;
  String txid = PublicMethod.triggerContract(contractAddress, "getContractAddress()", "#", false,
        callvalue, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());
  String addressHex =
        "41" + ByteArray.toHexString(infoById.get().getContractResult(0).toByteArray())
            .substring(24);
    logger.info("address_hex: " + addressHex);
  byte[] contractAddressA = ByteArray.fromHexString(addressHex);
    logger.info("contractAddressA: " + Base58.encode58Check(contractAddressA));
    long contractAddressBalance = PublicMethod.queryAccount(contractAddressA, blockingStubFull)
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
  String tokenName = PublicMethod.stringToHexString(name);
  String tokenAbbr = PublicMethod.stringToHexString(abbr);
  String param =
        "\"" + tokenName + "\",\"" + tokenAbbr + "\"," + totalSupply + "," + 2;
    logger.info("param: " + param);
  String methodTokenIssue = "tokenIssue(bytes32,bytes32,uint64,uint8)";
    txid = PublicMethod.triggerContract(contractAddress, methodTokenIssue, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    assetIssueId = PublicMethod.queryAccount(contractAddressA, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    long returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    logger.info("returnAssetId: " + returnAssetId);
    Assert.assertEquals(returnAssetId, Long.parseLong(assetIssueId));
    Map<String, Long> assetV2Map = PublicMethod.queryAccount(contractAddress, blockingStubFull)
        .getAssetV2Map();
    Assert.assertEquals(0, assetV2Map.size());
    long assetIssueValue = PublicMethod.queryAccount(contractAddressA, blockingStubFull)
        .getAssetV2Map().get(assetIssueId);
    Assert.assertEquals(totalSupply, assetIssueValue);
    AssetIssueContract assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert.assertEquals(totalSupply, assetIssueById.getTotalSupply());
    Assert.assertEquals(2, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddressA),
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
    long contractAddressBalance2 = PublicMethod.queryAccount(contractAddressA, blockingStubFull)
        .getBalance();
    Assert.assertEquals(contractAddressBalance - 1024000000L, contractAddressBalance2);
  // updateAsset
    param = "\"" + assetIssueId + "\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
  String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod.getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
    assetIssueId = PublicMethod.queryAccount(contractAddressA, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(2, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddressA),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  }

  @Test(enabled = false, description = "updateAsset verify token", groups = {"contract", "daily"})
  public void updateAsset005VerifyTokenId() {
    Assert.assertTrue(PublicMethod
        .sendcoin(dev001Address, 1100_000_000L, fromAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod
        .sendcoin(dev002Address, 50_000_000L, fromAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String filePath = "./src/test/resources/soliditycode/tvmAssetIssue001.sol";
  String contractName = "tvmAssetIssue001";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
  String code = retMap.get("byteCode").toString();
  String abi = retMap.get("abI").toString();
    long callvalue = 1024000000L;
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
  // token id does not exist, will update myself
    url = "trx";
    description = "trx";
    param = "\"1119125\",\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
  String methodUpdateAsset = "updateAsset(trcToken,string,string)";
    txid = PublicMethod.triggerContract(contractAddress, methodUpdateAsset, param, false,
        0, maxFeeLimit, dev001Address, dev001Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    logger.info("assetIssueById: " + assetIssueById);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));
  // not owner's asset, will update myself
    AssetIssueContract assetIssueByIdBefore = PublicMethod
        .getAssetIssueById("1000004", blockingStubFull);
  final String nameBefore = ByteArray.toStr(assetIssueByIdBefore.getName().toByteArray());
  final String abbrBefore = ByteArray.toStr(assetIssueByIdBefore.getAbbr().toByteArray());
  final String descBefore = assetIssueByIdBefore.getDescription().size() == 0 ? ""
        : ByteArray.toStr(assetIssueByIdBefore.getDescription().toByteArray());
  final String urlBefore = assetIssueByIdBefore.getUrl().size() == 0 ? ""
        : ByteArray.toStr(assetIssueByIdBefore.getUrl().toByteArray());
  final long precisionBefore = assetIssueByIdBefore.getPrecision();
    url = url + "123456";
    description = description + "123";
    param = "\"" + url + "\",\"" + description + "\"";
    logger.info("param: " + param);
    txid = PublicMethod
        .triggerContract(contractAddress, "updateOtherAccountAsset(string,string)", param, false,
            0, maxFeeLimit, dev002Address, dev002Key, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    infoById = PublicMethod
        .getTransactionInfoById(txid, blockingStubFull);
    logger.info(infoById.toString());
    Assert.assertEquals(0, infoById.get().getResultValue());

    returnAssetId = ByteArray.toLong((infoById.get().getContractResult(0).toByteArray()));
    Assert.assertEquals(1, returnAssetId);
    assetIssueId = PublicMethod.queryAccount(contractAddress, blockingStubFull).getAssetIssuedID()
        .toStringUtf8();
    logger.info("assetIssueId: " + assetIssueId);
    assetIssueById = PublicMethod
        .getAssetIssueById(assetIssueId, blockingStubFull);
    logger.info("assetIssueById: " + assetIssueById);
    Assert.assertEquals(name, ByteArray.toStr(assetIssueById.getName().toByteArray()));
    Assert.assertEquals(abbr, ByteArray.toStr(assetIssueById.getAbbr().toByteArray()));
    Assert
        .assertEquals(description, ByteArray.toStr(assetIssueById.getDescription().toByteArray()));
    Assert.assertEquals(url, ByteArray.toStr(assetIssueById.getUrl().toByteArray()));
    Assert.assertEquals(6, assetIssueById.getPrecision());
    Assert.assertEquals(Base58.encode58Check(contractAddress),
        Base58.encode58Check(assetIssueById.getOwnerAddress().toByteArray()));

    AssetIssueContract assetIssueByIdAfter = PublicMethod
        .getAssetIssueById("1000004", blockingStubFull);
  String descAfter = assetIssueByIdBefore.getDescription().size() == 0 ? ""
        : ByteArray.toStr(assetIssueByIdAfter.getDescription().toByteArray());
  String urlAfter = assetIssueByIdBefore.getUrl().size() == 0 ? ""
        : ByteArray.toStr(assetIssueByIdAfter.getUrl().toByteArray());
    Assert.assertEquals(nameBefore, ByteArray.toStr(assetIssueByIdAfter.getName().toByteArray()));
    Assert.assertEquals(abbrBefore, ByteArray.toStr(assetIssueByIdAfter.getAbbr().toByteArray()));
    Assert.assertEquals(descBefore, descAfter);
    Assert.assertEquals(urlBefore, urlAfter);
    Assert.assertEquals(precisionBefore, assetIssueByIdAfter.getPrecision());
  }

  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(dev001Address, dev001Key, fromAddress, blockingStubFull);  }
}
