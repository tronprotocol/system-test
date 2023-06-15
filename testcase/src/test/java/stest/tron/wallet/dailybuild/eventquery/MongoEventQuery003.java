package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bson.Document;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.WalletClient;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.MongoBase;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;

@Slf4j
public class MongoEventQuery003 extends MongoBase {

  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  public static String httpFullNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  public static String httpsolidityNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(3);
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String eventnode =
      Configuration.getByPath("testng.conf").getStringList("eventnode.ip.list").get(0);
  private Long maxFeeLimit =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.maxFeeLimit");
  byte[] contractAddress;
  String txid;

  private String soliditynode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] event001Address = ecKey1.getAddress();
  String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private JSONObject responseContent;
  private HttpResponse response;
  String param;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode).usePlaintext().build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    ecKey1 = new ECKey(Utils.getRandom());
    event001Address = ecKey1.getAddress();
    event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);

    Assert.assertTrue(
        PublicMethed.sendcoin(
            event001Address, maxFeeLimit, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String filePath = "./src/test/resources/soliditycode/contractTestMongoDbEvent.sol";
    String contractName = "SimpleStorage";
    HashMap retMap = PublicMethed.getBycodeAbi(filePath, contractName);

    String code = retMap.get("byteCode").toString();
    String abi = retMap.get("abI").toString();
    contractAddress =
        PublicMethed.deployContract(
            contractName,
            abi,
            code,
            "",
            maxFeeLimit,
            0L,
            50,
            null,
            event001Key,
            event001Address,
            blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    logger.info("contractAddress:" + contractAddress);
  }

  @Test(enabled = true, priority = 4, description = "MongoDB Event query for contract event")
  public void test01MongoDbEventQueryForContractEvent() {
    logger.info("event001Key:" + event001Key);
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] event001Address = ecKey1.getAddress();
    String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    logger.info("event001Key-001:" + event001Key);
    Assert.assertTrue(
        PublicMethed.sendcoin(
            event001Address, maxFeeLimit, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    param = "700";
    txid =
        PublicMethed.triggerContract(
            contractAddress,
            "store(uint256)",
            param,
            false,
            0,
            maxFeeLimit,
            event001Address,
            event001Key,
            blockingStubFull);
    logger.info("txid:" + txid);
    BasicDBObject query = new BasicDBObject();
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    query.put("uniqueId", txid + "_1");
    FindIterable<Document> findIterable = mongoDatabase.getCollection("contractevent").find(query);
    FindIterable<Document> findIterableLog = mongoDatabase.getCollection("contractlog").find(query);

    MongoCursor<Document> mongoCursor = findIterable.iterator();
    MongoCursor<Document> mongoCursorLog = findIterableLog.iterator();

    Document document = null;
    Document documentLog = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("contractevent").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObject = JSON.parseObject(document.toJson());
    Assert.assertEquals(txid, jsonObject.getString("transactionId"));

    //query contractlog to prove redundancy=true is valid
    retryTimes = 5;
    while (retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursorLog.hasNext()) {
        mongoCursorLog = mongoDatabase.getCollection("contractlog").find(query).iterator();
      } else {
        documentLog = mongoCursorLog.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObjectLog = JSON.parseObject(documentLog.toJson());
    Assert.assertEquals(txid, jsonObjectLog.getString("transactionId"));


    Assert.assertEquals("storedNumber", jsonObject.getString("eventName"));
    Assert.assertEquals(
        "storedNumber(uint256,uint256,uint256,address)", jsonObject.getString("eventSignature"));
    Assert.assertEquals(
        "storedNumber(uint256 oldNumber,uint256 newNumber,uint256 addedNumber,address sender)",
        jsonObject.getString("eventSignatureFull"));
    Assert.assertEquals("contractEventTrigger", jsonObject.getString("triggerName"));
    Assert.assertEquals("", jsonObject.getString("callerAddress"));
    Assert.assertNull(jsonObject.getString("logInfo"));
    Assert.assertNull("", jsonObject.getString("abi"));
    Assert.assertFalse(jsonObject.getBoolean("removed"));
    Assert.assertEquals("0", jsonObject.getJSONObject("topicMap").getString("0"));
    Assert.assertEquals(param, jsonObject.getJSONObject("topicMap").getString("1"));
    Assert.assertEquals("0", jsonObject.getJSONObject("topicMap").getString("oldNumber"));
    Assert.assertEquals(param, jsonObject.getJSONObject("topicMap").getString("newNumber"));
    Assert.assertEquals(param, jsonObject.getJSONObject("dataMap").getString("2"));
    Assert.assertEquals(param, jsonObject.getJSONObject("dataMap").getString("addedNumber"));

    expectInformationFromGetTransactionInfoById(jsonObject, txid);
    expectInformationFromGetTransactionById(jsonObject, txid);

    expectInformationFromGetBlockByNum(jsonObject, txid);
    testLatestSolidifiedBlockNumber(jsonObject);
  }

  @Test(enabled = true, priority = 4, description
      = "MongoDb Event query for solidity contract event")
  public void test02MongoDbEventQueryForContractSolidityEvent() {
    logger.info("event001Key:" + event001Key);
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    byte[] event001Address = ecKey1.getAddress();
    String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    logger.info("event001Key-001:" + event001Key);
    Assert.assertTrue(
        PublicMethed.sendcoin(
            event001Address, maxFeeLimit, fromAddress, testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String param1 = "800";
    txid =
        PublicMethed.triggerContract(
            contractAddress,
            "store(uint256)",
            param1,
            false,
            0,
            maxFeeLimit,
            event001Address,
            event001Key,
            blockingStubFull);
    logger.info("txid:" + txid);
    BasicDBObject query = new BasicDBObject();
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    query.put("uniqueId", txid + "_1");
    FindIterable<Document> findIterable = mongoDatabase.getCollection("solidityevent").find(query);
    FindIterable<Document> findIterableSolidityLog
        = mongoDatabase.getCollection("soliditylog").find(query);

    MongoCursor<Document> mongoCursor = findIterable.iterator();
    MongoCursor<Document> mongoCursorSolidityLog = findIterableSolidityLog.iterator();

    Document document = null;
    Document documentSolidityLog = null;

    int retryTimes = 20;
    while (retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("solidityevent").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObject = JSON.parseObject(document.toJson());
    Assert.assertEquals(txid, jsonObject.getString("transactionId"));


    //query soliditylog to prove redundancy=true is valid
    retryTimes = 5;
    while (retryTimes-- > 0) {
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursorSolidityLog.hasNext()) {
        mongoCursorSolidityLog = mongoDatabase.getCollection("soliditylog").find(query).iterator();
      } else {
        documentSolidityLog = mongoCursorSolidityLog.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);
    JSONObject jsonObjectSolidityLog = JSON.parseObject(documentSolidityLog.toJson());
    Assert.assertEquals(txid, jsonObjectSolidityLog.getString("transactionId"));

    Assert.assertEquals("storedNumber", jsonObject.getString("eventName"));
    Assert.assertEquals(
        "storedNumber(uint256,uint256,uint256,address)", jsonObject.getString("eventSignature"));
    Assert.assertEquals(
        "storedNumber(uint256 oldNumber,uint256 newNumber,uint256 addedNumber,address sender)",
        jsonObject.getString("eventSignatureFull"));
    Assert.assertEquals("solidityEventTrigger", jsonObject.getString("triggerName"));
    Assert.assertEquals("", jsonObject.getString("callerAddress"));
    Assert.assertNull(jsonObject.getString("logInfo"));
    Assert.assertNull("", jsonObject.getString("abi"));
    Assert.assertFalse(jsonObject.getBoolean("removed"));
    Assert.assertEquals(param, jsonObject.getJSONObject("topicMap").getString("0"));
    Assert.assertEquals(param1, jsonObject.getJSONObject("topicMap").getString("1"));
    Assert.assertEquals(param, jsonObject.getJSONObject("topicMap").getString("oldNumber"));
    Assert.assertEquals(param1, jsonObject.getJSONObject("topicMap").getString("newNumber"));
    Assert.assertEquals(
        String.valueOf(Integer.parseInt(param) + Integer.parseInt(param1)),
        jsonObject.getJSONObject("dataMap").getString("2"));
    Assert.assertEquals(
        String.valueOf(Integer.parseInt(param) + Integer.parseInt(param1)),
        jsonObject.getJSONObject("dataMap").getString("addedNumber"));

    expectInformationFromGetTransactionInfoById(jsonObject, txid);
    expectInformationFromGetTransactionById(jsonObject, txid);
    expectInformationFromGetBlockByNum(jsonObject, txid);
    testLatestSolidifiedBlockNumber(jsonObject);
  }


  private void testLatestSolidifiedBlockNumber(JSONObject jsonObject) {
    HttpMethed.printJsonContent(jsonObject);
    response = HttpMethed.getNowBlockFromSolidity(httpsolidityNode);
    responseContent = HttpMethed.parseResponseContent(response);
    HttpMethed.printJsonContent(responseContent);
    Long latestSolidifiedBlockNumber =
        responseContent.getJSONObject("block_header").getJSONObject("raw_data").getLong("number");
    Assert.assertTrue(
        jsonObject.getLong("latestSolidifiedBlockNumber") < latestSolidifiedBlockNumber);

    logger.info("latestSolidifiedBlockNumber:" + latestSolidifiedBlockNumber);
    logger.info("jsonObject.getLong(\"latestSolidifiedBlockNumber\"):"
        + jsonObject.getLong("latestSolidifiedBlockNumber"));
    Assert.assertTrue(
        (latestSolidifiedBlockNumber - jsonObject.getLong("latestSolidifiedBlockNumber")) < 10);
  }

  private void expectInformationFromGetTransactionInfoById(JSONObject jsonObject, String txid) {
    response = HttpMethed.getTransactionInfoById(httpFullNode, txid, false);
    responseContent = HttpMethed.parseResponseContent(response);
    logger.info("timestamp:" + responseContent.getString("blockTimeStamp"));
    logger.info("timestamp:" + jsonObject.getString("timeStamp"));
    Assert.assertTrue(
        jsonObject.getString("timeStamp").contains(responseContent.getString("blockTimeStamp")));
    Assert.assertTrue(
        jsonObject.getString("blockNumber").contains(responseContent.getString("blockNumber")));
    Assert.assertEquals(
        responseContent.getJSONArray("log").getJSONObject(0).getString("address"),
        jsonObject.getJSONObject("rawData").getString("address"));

    Assert.assertEquals(
        responseContent.getJSONArray("log").getJSONObject(0).getJSONArray("topics").getString(0),
        jsonObject.getJSONObject("rawData").getJSONArray("topics").getString(0));
    Assert.assertEquals(
        responseContent.getJSONArray("log").getJSONObject(0).getString("data"),
        jsonObject.getJSONObject("rawData").getString("data"));

    logger.info("blockTimeStampFromHttp:" + responseContent.getString("blockTimeStamp"));
    logger.info("timeStampFromMongoDB:" + jsonObject.getString("timeStamp"));
  }

  private void expectInformationFromGetTransactionById(JSONObject jsonObject, String txId) {
    response = HttpMethed.getTransactionById(httpFullNode, txid);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(txId, jsonObject.getString("transactionId"));
    String contractAddress =
        WalletClient.encode58Check(
            ByteArray.fromHexString(
                responseContent
                    .getJSONObject("raw_data")
                    .getJSONArray("contract")
                    .getJSONObject(0)
                    .getJSONObject("parameter")
                    .getJSONObject("value")
                    .getString("contract_address")));
    Assert.assertEquals(contractAddress, jsonObject.getString("contractAddress"));

    String ownerAddress =
        WalletClient.encode58Check(
            ByteArray.fromHexString(
                responseContent
                    .getJSONObject("raw_data")
                    .getJSONArray("contract")
                    .getJSONObject(0)
                    .getJSONObject("parameter")
                    .getJSONObject("value")
                    .getString("owner_address")));

    Assert.assertEquals(ownerAddress, jsonObject.getString("originAddress"));

    Assert.assertEquals(ownerAddress, jsonObject.getJSONObject("dataMap").getString("3"));
    Assert.assertEquals(ownerAddress, jsonObject.getJSONObject("dataMap").getString("sender"));
    Assert.assertEquals(
        WalletClient.encode58Check(event001Address), jsonObject.getString("creatorAddress"));
  }

  private void expectInformationFromGetBlockByNum(JSONObject jsonObject, String txId) {

    response = HttpMethed.getTransactionInfoById(httpFullNode, txId);
    responseContent = HttpMethed.parseResponseContent(response);
    long blockNumber = responseContent.getInteger("blockNumber");
    response = HttpMethed.getBlockByNum(httpFullNode, blockNumber);
    responseContent = HttpMethed.parseResponseContent(response);
    Assert.assertEquals(responseContent.getString("blockID"), jsonObject.getString("blockHash"));
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
