package stest.tron.wallet.dailybuild.eventquery.mongoevent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bson.Document;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethed;
import stest.tron.wallet.common.client.utils.MongoBase;
import stest.tron.wallet.common.client.utils.PublicMethed;

@Slf4j
public class MongoEventQuery001 extends MongoBase {

  private final String testKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String mongoNode =
      Configuration.getByPath("testng.conf").getStringList("mongonode.ip.list").get(0);
  private Long maxFeeLimit =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.maxFeeLimit");
  public static String httpFullNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);

  public static String httpsolidityNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(3);
  private JSONObject responseContent;
  private HttpResponse response;
  List<String> transactionIdList = null;

  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext(true).build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }

  @Test(enabled = true, description = "Event query for block on mongoDB")
  public void test01MongoDbEventQueryForBlock() {
    FindIterable<Document> findIterable = mongoDatabase.getCollection("block").find();
    MongoCursor<Document> mongoCursor = findIterable.iterator();
    Integer blockNumber = 0;
    Boolean hasTransactions = false;
    while (mongoCursor.hasNext()) {
      Document document = mongoCursor.next();
      Assert.assertTrue(Integer.parseInt(document.get("blockNumber").toString()) > 0);
      if (Integer.parseInt(document.get("transactionSize").toString()) > 0) {
        hasTransactions = true;
        blockNumber = Integer.parseInt(document.get("blockNumber").toString());
        logger.info("blockNumber:" + blockNumber);
        response = HttpMethed.getBlockByNum(httpFullNode, blockNumber);
        responseContent = HttpMethed.parseResponseContent(response);
        String blockIdFromHttp = responseContent.getString("blockID");
        Assert.assertEquals(blockIdFromHttp, document.get("blockHash").toString());
        logger.info("blockIdFromHttp:" + blockIdFromHttp);

        transactionIdList = new ArrayList<>();
        if (responseContent.getJSONArray("transactions").size() > 0) {
          for (int i = 0; i < responseContent.getJSONArray("transactions").size(); i++) {
            transactionIdList.add(
                responseContent.getJSONArray("transactions").getJSONObject(i).getString("txID"));
          }
        }
        Assert.assertEquals(transactionIdList, document.getList("transactionList", String.class));
        response = HttpMethed.getTransactionCountByBlocknum(httpFullNode, blockNumber);
        responseContent = HttpMethed.parseResponseContent(response);
        Assert.assertEquals(
            responseContent.getString("count"), document.get("transactionSize").toString());
        break;
      }
    }
    Assert.assertTrue(hasTransactions);
  }

  @Test(enabled = true, description = "Event query for solidity on mongoDB")
  public void test02MongoDbEventQueryForSolidity() {

    response = HttpMethed.getNowBlockFromSolidity(httpsolidityNode);
    responseContent = HttpMethed.parseResponseContent(response);
    Integer blockNumber =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getInteger("number");

    BasicDBObject query = new BasicDBObject();
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    query.put("latestSolidifiedBlockNumber", blockNumber);
    FindIterable<Document> findIterable = mongoDatabase.getCollection("solidity").find(query);
    MongoCursor<Document> mongoCursor = findIterable.iterator();

    Document document = mongoCursor.next();

    Assert.assertEquals(
        String.valueOf(blockNumber), document.get("latestSolidifiedBlockNumber").toString());

    response = HttpMethed.getBlockByNumFromSolidity(httpsolidityNode, blockNumber);
    responseContent = HttpMethed.parseResponseContent(response);
    String timeStampFromHttp =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getString("timestamp");
    logger.info("timeStampFromHttp:" + timeStampFromHttp);
    Assert.assertEquals(timeStampFromHttp, document.get("timeStamp").toString());
  }

  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
