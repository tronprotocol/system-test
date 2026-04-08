package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.protobuf.ByteString;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import java.util.*;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bson.Document;
import org.junit.Assert;
import org.testng.annotations.*;
import org.tron.api.GrpcAPI;
import org.tron.protos.Protocol;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.Base58;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.MongoBase;
import stest.tron.wallet.common.client.utils.ProposalEnum;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Retry;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class MongoEventQuery002 extends MongoBase {
  private final String testNetAccountKey =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] testNetAccountAddress = PublicMethod.getFinalAddress(testNetAccountKey);

  private String eventnode =
      Configuration.getByPath("testng.conf").getStringList("eventnode.ip.list").get(0);
  byte[] contractAddress;
  String txId;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] internalTxsAddress = ecKey1.getAddress();
  String testKeyForInternalTxsAddress = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  public static String httpFullNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  public static String httpsolidityNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(3);
  private JSONObject responseContent;
  private HttpResponse response;
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] event002Address = ecKey2.getAddress();
  String event002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] event003Address = ecKey3.getAddress();
  String event003Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  String deployContractTxId = null;
  List<String> transactionIdList = null;
  long runTimes = 1000;
  int useUpFreeBandwidth = 0;
  String txIdForInternalTransaction;
  long blockNumber = 0L;
  String txIdIndex0 = null;
  String txIdIndex2 = null;
  String energyPrice = null;
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  private static final long now = System.currentTimeMillis();
  private static String name = "testAssetIssue002_" + now;
  int index = 0;
  String description =
      Configuration.getByPath("testng.conf").getString("defaultParameter.assetDescription");
  Long amount = 1000000L;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    Assert.assertTrue(
        PublicMethod.sendcoin(
            internalTxsAddress,
            100000000000L,
            testNetAccountAddress,
            testNetAccountKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String filePath =
        "src/test/resources/soliditycode/"
            + "contractInternalTransaction001testInternalTransaction001.sol";
    String contractName = "A";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress =
        PublicMethod.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            1000000L,
            50,
            null,
            testKeyForInternalTxsAddress,
            internalTxsAddress,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String contractName1 = "C";
    HashMap retMap1 = PublicMethod.getBycodeAbi(filePath, contractName1);
    String code1 = retMap1.get("byteCode").toString();
    String abi1 = retMap1.get("abI").toString();

    byte[] contractAddress1 =
        PublicMethod.deployContract(
            contractName1,
            abi1,
            code1,
            "",
            maxFeeLimit,
            1000000L,
            100,
            null,
            testKeyForInternalTxsAddress,
            internalTxsAddress,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String initParmes = "\"" + Base58.encode58Check(contractAddress1) + "\"";
    txId =
        PublicMethod.triggerContract(
            contractAddress,
            "test1(address)",
            initParmes,
            false,
            0,
            maxFeeLimit,
            internalTxsAddress,
            testKeyForInternalTxsAddress,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("txId:" + txId);
    String initParmes2 = "\"" + Base58.encode58Check(contractAddress1) + "\",\"1\"";
    txIdForInternalTransaction =
        PublicMethod.triggerContract(
            contractAddress,
            "test2(address,uint256)",
            initParmes2,
            false,
            0,
            maxFeeLimit,
            internalTxsAddress,
            testKeyForInternalTxsAddress,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("txIdForInternalTransaction:" + txIdForInternalTransaction);

    ecKey2 = new ECKey(Utils.getRandom());
    event002Address = ecKey2.getAddress();
    event002Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethod.printAddress(event002Key);
    logger.info("event002Key:" + event002Key);
    GrpcAPI.AccountResourceMessage accountResource =
        PublicMethod.getAccountResource(internalTxsAddress, blockingStubFull);
    long energyLimit = accountResource.getTotalEnergyLimit();
    long energyWeight = accountResource.getTotalEnergyWeight();
    energyWeight = energyWeight == 0 ? 1 : energyWeight;
    logger.info("energyWeight:" + energyWeight);
    long freezeAmount = 1000000000;
    if (energyLimit / energyWeight > 5000) {
      freezeAmount = 900000000000000L;
    }
    logger.info("freezeAmount:" + freezeAmount);
    Assert.assertTrue(
        PublicMethod.sendcoin(
            event002Address, freezeAmount, fromAddress, testKey002, blockingStubFull));

    Assert.assertTrue(
        PublicMethod.sendcoin(
            event002Address, maxFeeLimit * 30, fromAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertTrue(
        PublicMethod.sendcoin(
            event003Address, 1000000000L, fromAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    contractName = "addressDemo";
    code = Configuration.getByPath("testng.conf").getString("code.code_ContractEventAndLog1");
    abi = Configuration.getByPath("testng.conf").getString("abi.abi_ContractEventAndLog1");
    deployContractTxId =
        PublicMethod.deployContractAndGetTransactionInfoById(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            50,
            null,
            event002Key,
            event002Address,
            blockingStubFull);
    logger.info("deployContractTxId:" + deployContractTxId);
    Assert.assertTrue(
        PublicMethod.freezeBalanceGetEnergy(
            event002Address, freezeAmount, 0, 1, event002Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(
        PublicMethod.freezeBalanceGetEnergy(
            testNetAccountAddress, freezeAmount, 0, 1, testNetAccountKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    freezeAmount =
        PublicMethod.getFreezeBalanceCount(event003Address, event003Key, 5000L, blockingStubFull);
    logger.info("freezeAmount:" + freezeAmount);
    Assert.assertTrue(
        PublicMethod.freezeBalanceGetEnergy(
            event003Address, freezeAmount, 0, 1, event003Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = true, description = "MongoDB Event query for transaction", groups = {"daily"})
  public void test01EventQueryForTransaction() throws InterruptedException {
    BasicDBObject query = new BasicDBObject();
    logger.info("deployContractTxId:" + deployContractTxId);
    query.put("transactionId", deployContractTxId);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;
    int retryTimes = 10;
    while (retryTimes-- > 0) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObject = JSON.parseObject(document.toJson());

    response = HttpMethod.getTransactionInfoById(httpFullNode, deployContractTxId);
    responseContent = HttpMethod.parseResponseContent(response);

    String contractAddressStartWith41 = responseContent.getString("contract_address");
    String contractAddressFromHttp =
        WalletClient.encode58Check(ByteArray.fromHexString(contractAddressStartWith41));
    Assert.assertEquals(contractAddressFromHttp, jsonObject.getString("contractAddress"));

    contractAddress = ByteArray.fromHexString(contractAddressStartWith41);
    runTimes = 10;
    txIdIndex0 =
        PublicMethod.triggerContract(
            contractAddress,
            "depositForLogCycle(uint256)",
            String.valueOf(runTimes),
            false,
            0,
            maxFeeLimit,
            event003Address,
            event003Key,
            blockingStubFull);

    PublicMethod.triggerContract(
        contractAddress,
        "depositForLogCycle(uint256)",
        String.valueOf(runTimes),
        false,
        0,
        maxFeeLimit,
        event003Address,
        event003Key,
        blockingStubFull);

    txIdIndex2 =
        PublicMethod.triggerContract(
            contractAddress,
            "depositForLogCycle(uint256)",
            String.valueOf(runTimes),
            false,
            0,
            maxFeeLimit,
            event003Address,
            event003Key,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("txIdIndex0:" + txIdIndex0);
    logger.info("txIdIndex2:" + txIdIndex2);
    response = HttpMethod.getTransactionInfoById(httpFullNode, txId);
    responseContent = HttpMethod.parseResponseContent(response);
    blockNumber = responseContent.getInteger("blockNumber");
    logger.info("blockNumber:" + blockNumber);
    response = HttpMethod.getBlockByNum(httpFullNode, blockNumber);
    responseContent = HttpMethod.parseResponseContent(response);

/*
    index = 2;
    transactionIdList = new ArrayList<>();
    for (int i = 0; i < responseContent.getJSONArray("transactions").size(); i++) {
      transactionIdList.add(
          i, responseContent.getJSONArray("transactions").getJSONObject(i).getString("txID"));
    }
    txIdIndex0 = transactionIdList.get(0);
    logger.info("txIDIndex0:" + txIdIndex0);
    txIdIndex2 = transactionIdList.get(2);
    logger.info("txIDIndex2:" + txIdIndex2);
*/

    query = new BasicDBObject();
    //PublicMethod.waitProduceNextBlock(blockingStubFull);
    //txIdIndex0 = txId;
    //txIdIndex2 = txId;
    query.put("transactionId", txIdIndex2);
    findIterable = mongoDatabase.getCollection("transaction").find(query);
    mongoCursor = findIterable.iterator();
    retryTimes = 10;
    document = null;
    while (retryTimes-- > 0) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObjectTxIdIndex2 = JSON.parseObject(document.toJson());
    expectInformationFromEnergyPrice(jsonObjectTxIdIndex2);
    expectInformationFromGetTransactionById(
        contractAddressFromHttp, jsonObjectTxIdIndex2, txIdIndex2);

    query = new BasicDBObject();
    //PublicMethod.waitProduceNextBlock(blockingStubFull);
    query.put("transactionId", txIdIndex0);
    findIterable = mongoDatabase.getCollection("transaction").find(query);
    mongoCursor = findIterable.iterator();
    retryTimes = 10;
    document = null;

    while (retryTimes-- > 0) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();

        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObjectTxIdIndex0 = JSON.parseObject(document.toJson());
    //expectInformationFromGetTransactionInfoById(
    //    jsonObjectTxIdIndex0, jsonObjectTxIdIndex2, txIdIndex0);

    testNetFee();
  }

  @Test(
      enabled = true,
      description = "MongoDB Event query for transaction of internalTransactionList", groups = {"daily"})
  public void test02EventQueryForTransaction() throws InterruptedException {
    response = HttpMethod.getTransactionInfoById(httpFullNode, txIdForInternalTransaction);
    responseContent = HttpMethod.parseResponseContent(response);
    int size = responseContent.getJSONArray("internal_transactions").size();
    BasicDBObject query = new BasicDBObject();
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    query.put("transactionId", txIdForInternalTransaction);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = mongoCursor.next();

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    try {
      JSONArray internalTransactionList = jsonObject.getJSONArray("internalTransactionList");
    } catch (Exception e) {
      Assert.assertTrue(e instanceof NullPointerException);
    }
//    Assert.assertEquals(
//        responseContent.getJSONArray("internal_transactions").size(),
//        jsonObject.getJSONArray("internalTransactionList").size());
//
//    Optional<Protocol.TransactionInfo> infoById = null;
//    infoById = PublicMethod.getTransactionInfoById(txIdForInternalTransaction, blockingStubFull);
//    for (int i = 0; i < size; i++) {
//      logger.info("i:" + i);
//      JSONObject jsonObjectFromHttp =
//          responseContent.getJSONArray("internal_transactions").getJSONObject(i);
//      JSONObject jsonObjectFromMongoDb =
//          jsonObject.getJSONArray("internalTransactionList").getJSONObject(i);
//      Assert.assertEquals(
//          jsonObjectFromHttp.getString("hash"), jsonObjectFromMongoDb.getString("hash"));
//      if (jsonObjectFromHttp.getJSONArray("callValueInfo").getJSONObject(0).getString("callValue")
//          == null) {
//        Assert.assertEquals("0", jsonObjectFromMongoDb.getString("callValue"));
//      } else {
//        Assert.assertEquals(
//            jsonObjectFromHttp
//                .getJSONArray("callValueInfo")
//                .getJSONObject(0)
//                .getString("callValue"),
//            jsonObjectFromMongoDb.getString("callValue"));
//      }
//
//      Assert.assertEquals("{}", jsonObjectFromMongoDb.getString("tokenInfo"));
//      Assert.assertEquals(
//          jsonObjectFromHttp.getString("transferTo_address"),
//          jsonObjectFromMongoDb.getString("transferTo_address"));
//      Assert.assertEquals(
//          jsonObjectFromHttp.getString("caller_address"),
//          jsonObjectFromMongoDb.getString("caller_address"));
//      Assert.assertEquals("", jsonObjectFromMongoDb.getString("extra"));
//      Assert.assertEquals(false, jsonObjectFromMongoDb.getBoolean("rejected"));
//
//      Assert.assertEquals(
//          ByteArray.toStr(infoById.get().getInternalTransactions(i).getNote().toByteArray()),
//          jsonObjectFromMongoDb.getString("note"));
//    }
  }

  @Test(enabled = true, description = "MongoDB Event query for transaction of transfer TRX.", groups = {"daily"})
  public void test03EventQueryForTransaction() throws InterruptedException {

    txId =
        HttpMethod.sendCoinGetTxid(httpFullNode, fromAddress, event002Address, amount, testKey002);
    logger.info("transfer trx Id：" + txId);
    //HttpMethod.waitToProduceOneBlock(httpFullNode);
    BasicDBObject query = new BasicDBObject();
    //PublicMethod.waitProduceNextBlock(blockingStubFull);
    query.put("transactionId", txId);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    int retryTimes = 10;

    Document document = null;
    while (retryTimes-- > 0) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();

        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObject = JSON.parseObject(document.toJson());

    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(String.valueOf(amount), jsonObject.getString("assetAmount"));
    response = HttpMethod.getNowBlockFromSolidity(httpsolidityNode);
    responseContent = HttpMethod.parseResponseContent(response);
    Long latestSolidifiedBlockNumber =
        responseContent.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    logger.info("mongo latestSolidifiedBlockNumber = {}, block chain solid num = {}", jsonObject.getLong("latestSolidifiedBlockNumber"), latestSolidifiedBlockNumber);
    Assert.assertTrue(
        jsonObject.getLong("latestSolidifiedBlockNumber") <= latestSolidifiedBlockNumber);

    Assert.assertTrue(
        (latestSolidifiedBlockNumber - jsonObject.getLong("latestSolidifiedBlockNumber")) < 7);
  }

  @Test(enabled = true, description = "MongoDB Event query for transaction of  contractCallValue.", groups = {"daily"})
  public void test04EventQueryForTransaction() throws InterruptedException {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] dev001Address = ecKey1.getAddress();
    String dev001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    logger.info("dev001Key:" + dev001Key);
    Assert.assertTrue(
        PublicMethod.sendcoin(
            dev001Address, 3100_000_000L, fromAddress, testKey002, blockingStubFull));
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] user001Address = ecKey2.getAddress();
    String user001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    logger.info("user001Key:" + user001Key);
    Assert.assertTrue(
        PublicMethod.sendcoin(
            user001Address, 300_000_000L, fromAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    // Create a new AssetIssue success.
    long now = System.currentTimeMillis();
    final long TotalSupply = 1000L;
    String tokenName = "testAssetIssue_" + now;

    Assert.assertTrue(
        PublicMethod.createAssetIssue(
            dev001Address,
            tokenName,
            TotalSupply,
            1,
            10000,
            System.currentTimeMillis() + 2000,
            System.currentTimeMillis() + 1000000000,
            1,
            description,
            url,
            100000L,
            100000L,
            1L,
            1L,
            dev001Key,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ByteString assetAccountId =
        PublicMethod.queryAccount(dev001Address, blockingStubFull).getAssetIssuedID();
    logger.info("The token name: " + tokenName);
    logger.info("The token ID: " + assetAccountId.toStringUtf8());

    // before deploy, check account resource

    String filePath = "./src/test/resources/soliditycode/contractTrcToken002.sol";
    String contractName = "tokenTest";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();

    String tokenId = assetAccountId.toStringUtf8();
    long tokenValue = 200;
    long callValue = 0;

    String transferTokenTxid =
        PublicMethod.deployContractAndGetTransactionInfoById(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            callValue,
            0,
            10000,
            tokenId,
            tokenValue,
            null,
            dev001Key,
            dev001Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Optional<Protocol.TransactionInfo> infoById =
        PublicMethod.getTransactionInfoById(transferTokenTxid, blockingStubFull);
    logger.info("Deploy energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());

    if (transferTokenTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("deploy transaction failed with message: " + infoById.get().getResMessage());
    }

    Assert.assertTrue(PublicMethod.sendcoin(
        user001Address, 2000000000, fromAddress, testKey002, blockingStubFull));

    Assert.assertTrue(
        PublicMethod.transferAsset(
            user001Address,
            assetAccountId.toByteArray(),
            10L,
            dev001Address,
            dev001Key,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    byte[] transferTokenContractAddress = infoById.get().getContractAddress().toByteArray();
    //Assert.assertTrue(PublicMethod.sendcoin(
    //    transferTokenContractAddress, 5000000, fromAddress, testKey002, blockingStubFull));
    //PublicMethod.waitProduceNextBlock(blockingStubFull);

    tokenId = assetAccountId.toStringUtf8();
    tokenValue = 10;
    callValue = 10;

    String triggerTxid =
        PublicMethod.triggerContract(
            transferTokenContractAddress,
            "msgTokenValueAndTokenIdTest()",
            "#",
            false,
            callValue,
            1000000000L,
            tokenId,
            tokenValue,
            user001Address,
            user001Key,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    infoById = PublicMethod.getTransactionInfoById(triggerTxid, blockingStubFull);
    logger.info("Trigger energytotal is " + infoById.get().getReceipt().getEnergyUsageTotal());
    logger.info("triggerTxid:" + triggerTxid);
    if (triggerTxid == null || infoById.get().getResultValue() != 0) {
      Assert.fail("transaction failed with message: " + infoById.get().getResMessage());
    }

    PublicMethod.waitProduceNextBlock(blockingStubFull);

    BasicDBObject query = new BasicDBObject();
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    query.put("transactionId", triggerTxid);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();

    int retryTimes = 40;
    Document document = null;
    while (retryTimes-- > 0) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();

        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObject = JSON.parseObject(document.toJson());
    response = HttpMethod.getTransactionById(httpFullNode, triggerTxid);
    responseContent = HttpMethod.parseResponseContent(response);
    Assert.assertEquals(
        responseContent
            .getJSONObject("raw_data")
            .getJSONArray("contract")
            .getJSONObject(0)
            .getJSONObject("parameter")
            .getJSONObject("value")
            .getString("call_value"),
        jsonObject.getString("contractCallValue"));
  }

  @Test(
      enabled = true,
      description = "MongoDB Event query for transaction of  result and contractResult.", groups = {"daily"})
  public void test05EventQueryForTransaction() throws InterruptedException {
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] event003Address = ecKey3.getAddress();
    String event003Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    logger.info("event003Key:" + event003Key);
    Assert.assertTrue(
        PublicMethod.sendcoin(
            event003Address, 100000000000L, fromAddress, testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    String filePath = "src/test/resources/soliditycode/assertExceptiontest1DivideInt.sol";
    String contractName = "divideIHaveArgsReturnStorage";
    HashMap retMap = PublicMethod.getBycodeAbi(filePath, contractName);
    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress =
        PublicMethod.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            100,
            null,
            event003Key,
            event003Address,
            blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    String num = "4" + "," + "0";
    txId =
        PublicMethod.triggerContract(
            contractAddress,
            "divideIHaveArgsReturn(int256,int256)",
            num,
            false,
            0,
            maxFeeLimit,
            event003Address,
            event003Key,
            blockingStubFull);

    logger.info("transactionId:" + txId);
    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txId);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;
    int retryTimes = 40;
    while (retryTimes-- > 0) {
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();

        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObject = JSON.parseObject(document.toJson());
    response = HttpMethod.getTransactionInfoById(httpFullNode, txId);
    responseContent = HttpMethod.parseResponseContent(response);
    Assert.assertEquals(
        responseContent.getJSONArray("contractResult").getString(0),
        jsonObject.getString("contractResult"));
    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("result"),
        jsonObject.getString("result"));
  }

  @Test(enabled = true, description = "MongoDB Event query for transaction of  data.", groups = {"daily"})
  public void test06EventQueryForTransaction() throws InterruptedException {
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    byte[] event003Address = ecKey3.getAddress();
    // String event003Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
    String transactionIdForData =
        PublicMethod.sendcoinGetTransactionIdForConstructData(
            event003Address, 2000000, fromAddress, testKey002, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    logger.info("transactionIdForData:" + transactionIdForData);

    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", transactionIdForData);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    response = HttpMethod.getTransactionById(httpFullNode, transactionIdForData);
    responseContent = HttpMethod.parseResponseContent(response);
    JSONObject jsonObject = JSON.parseObject(document.toJson());
    Assert.assertEquals(
        responseContent.getJSONObject("raw_data").getString("data"), jsonObject.getString("data"));
  }

  private void testNetFee() {

    useUpFreeBandwidth = 20;
    while (useUpFreeBandwidth-- > 0) {
          PublicMethod.sendcoin(
              fromAddress, 1000000L + PublicMethod.randomFreezeAmount.addAndGet(1), event002Address, event002Key, blockingStubFull);
      //PublicMethod.waitProduceNextBlock(blockingStubFull);
    }
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    txId =
        PublicMethod.triggerContract(
            contractAddress,
            "triggerUintEvent()",
            "#",
            false,
            0,
            maxFeeLimit,
            event002Address,
            event002Key,
            blockingStubFull);
    BasicDBObject query = new BasicDBObject();
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    query.put("transactionId", txId);
    logger.info("txId:" + txId);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);

    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();

    Document document = null;
    int retryTimes = 15;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethod.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObject = JSON.parseObject(document.toJson());

    response = HttpMethod.getTransactionInfoById(httpFullNode, txId);
    responseContent = HttpMethod.parseResponseContent(response);
    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("net_fee"),
        jsonObject.getString("netFee"));
  }

  private void expectInformationFromEnergyPrice(JSONObject jsonObject) {
    response = HttpMethod.getEnergyPric(httpFullNode);
    responseContent = HttpMethod.parseResponseContent(response);
    String prices = responseContent.getString("prices");
    logger.info("prices:" + prices);
    String[] strArray = prices.split(":");
    String energyUnitPrice = strArray[strArray.length - 1];
    energyPrice = energyUnitPrice;
    logger.info("energyUnitPrice:" + energyUnitPrice);
    Assert.assertEquals(energyUnitPrice, jsonObject.getString("energyUnitPrice"));
  }

  private void expectInformationFromGetTransactionInfoById(
      JSONObject jsonObjectTxIdIndex0, JSONObject jsonObjectTxIdIndex3, String txIdIndex) {
    response = HttpMethod.getTransactionInfoById(httpFullNode, txIdIndex);
    responseContent = HttpMethod.parseResponseContent(response);

    logger.info("expectInformationFromGetTransactionInfoById jsonObjectTxIdIndex0: " + jsonObjectTxIdIndex0.toJSONString());
    //logger.info("expectInformationFromGetTransactionInfoById jsonObjectTxIdIndex2: " + jsonObjectTxIdIndex2.toJSONString());
    logger.info("expectInformationFromGetTransactionInfoById responseContent: " + responseContent.toJSONString());
    logger.info("timestamp:" + responseContent.getString("blockTimeStamp"));
    logger.info("timestamp:" + jsonObjectTxIdIndex0.getString("timeStamp"));
    logger.info("contractRetFromHttp:" + responseContent.getJSONArray("contractResult").size());
    if (responseContent.getJSONArray("contractResult").size() == 0) {
      Assert.assertNull(jsonObjectTxIdIndex0.getString("contractResult"));
    }

    Assert.assertTrue(
        jsonObjectTxIdIndex0
            .getString("timeStamp")
            .contains(responseContent.getString("blockTimeStamp")));
    Assert.assertEquals(
        responseContent.getLong("blockNumber"), jsonObjectTxIdIndex0.getLong("blockNumber"));
    Long blockNumber = responseContent.getLong("blockNumber");
    logger.info("blockNumber:" + blockNumber);

    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("energy_usage"),
        jsonObjectTxIdIndex0.getString("energyUsage"));
    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("energy_fee"),
        jsonObjectTxIdIndex0.getString("energyFee"));
    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("energy_usage_total"),
        jsonObjectTxIdIndex0.getString("energyUsageTotal"));

/*    Assert.assertEquals(
        String.valueOf(
            responseContent.getJSONObject("receipt").getLong("energy_usage_total") * (index + 1)),
        jsonObjectTxIdIndex2.getString("cumulativeEnergyUsed"));*/

    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("net_usage"),
        jsonObjectTxIdIndex0.getString("netUsage"));

    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("result"),
        jsonObjectTxIdIndex0.getString("result"));

    Assert.assertEquals(
        responseContent.getJSONObject("receipt").getString("origin_energy_usage"),
        jsonObjectTxIdIndex0.getString("originEnergyUsage"));

    Assert.assertEquals(
        responseContent.getJSONArray("log").getJSONObject(0).getString("address"),
        jsonObjectTxIdIndex0.getJSONArray("logList").getJSONObject(0).getString("address"));

    Assert.assertEquals(
        responseContent.getJSONArray("log").getJSONObject(0).getString("data"),
        jsonObjectTxIdIndex0.getJSONArray("logList").getJSONObject(0).getString("data"));

    Assert.assertEquals(
        txIdIndex0,
        jsonObjectTxIdIndex0.getJSONArray("logList").getJSONObject(0).getString("transactionHash"));

    Assert.assertEquals(
        blockNumber,
        jsonObjectTxIdIndex0.getJSONArray("logList").getJSONObject(0).getLong("blockNumber"));

    for (int i = 0;
        i < responseContent.getJSONArray("log").getJSONObject(0).getJSONArray("topics").size();
        i++) {
      Assert.assertEquals(
          responseContent.getJSONArray("log").getJSONObject(0).getJSONArray("topics").getString(i),
          jsonObjectTxIdIndex0
              .getJSONArray("logList")
              .getJSONObject(0)
              .getJSONArray("topicList")
              .getString(i));
    }

    response = HttpMethod.getBlockByNum(httpFullNode, blockNumber);
    responseContent = HttpMethod.parseResponseContent(response);

/*
    Assert.assertEquals(
        String.valueOf(index),
        jsonObjectTxIdIndex2
            .getJSONArray("logList")
            .getJSONObject(0)
            .getString("transactionIndex"));

    Assert.assertEquals(String.valueOf(index), jsonObjectTxIdIndex2.getString("transactionIndex"));

    Assert.assertEquals(
        String.valueOf(index * runTimes), jsonObjectTxIdIndex2.getString("preCumulativeLogCount"));
    Assert.assertEquals(
        String.valueOf(index * runTimes),
        jsonObjectTxIdIndex2.getJSONArray("logList").getJSONObject(0).getString("logIndex"));
*/

    Assert.assertEquals(
        responseContent.getString("blockID"), jsonObjectTxIdIndex0.getString("blockHash"));
    Assert.assertEquals(
        responseContent.getString("blockID"),
        jsonObjectTxIdIndex0.getJSONArray("logList").getJSONObject(0).getString("blockHash"));

    Assert.assertEquals(
        txIdIndex0,
        jsonObjectTxIdIndex0.getJSONArray("logList").getJSONObject(0).getString("transactionHash"));
  }

  private void expectInformationFromGetTransactionById(
      String contractAddressFromHttp, JSONObject jsonObject, String txIdIndex) {
    response = HttpMethod.getTransactionById(httpFullNode, txIdIndex);
    responseContent = HttpMethod.parseResponseContent(response);

    Assert.assertEquals(txIdIndex, jsonObject.getString("transactionId"));
    Assert.assertEquals("transactionTrigger", jsonObject.getString("triggerName"));
    Assert.assertNull(jsonObject.getString("contractAddress"));
    String ownerAddressStartWith41 =
        responseContent
            .getJSONObject("raw_data")
            .getJSONArray("contract")
            .getJSONObject(0)
            .getJSONObject("parameter")
            .getJSONObject("value")
            .getString("owner_address");
    String ownerAddressFromHttp =
        WalletClient.encode58Check(ByteArray.fromHexString(ownerAddressStartWith41));
    logger.info("ownerAddressFromHttp:" + ownerAddressFromHttp);

    Assert.assertEquals(ownerAddressFromHttp, jsonObject.getString("fromAddress"));

    Assert.assertEquals(contractAddressFromHttp, jsonObject.getString("toAddress"));

    Assert.assertEquals(
        responseContent.getJSONObject("raw_data").getString("fee_limit"),
        jsonObject.getString("feeLimit"));

    Assert.assertEquals(responseContent.getString("txID"), jsonObject.getString("transactionId"));
    Assert.assertEquals(
        responseContent
            .getJSONObject("raw_data")
            .getJSONArray("contract")
            .getJSONObject(0)
            .getString("type"),
        jsonObject.getString("contractType"));
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(event002Address, event002Key, fromAddress, blockingStubFull);
  }
}
