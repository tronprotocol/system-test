package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.bson.Document;
import org.junit.Assert;
import org.testng.annotations.Test;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.HttpMethod;
import stest.tron.wallet.common.client.utils.MongoBase;
import stest.tron.wallet.common.client.utils.PublicMethod;

@Slf4j
public class MongoEventQuery001 extends MongoBase {

  public static String httpFullNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);

  public static String httpsolidityNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(3);
  private JSONObject responseContent;
  private HttpResponse response;
  List<String> transactionIdList = null;

  @Test(enabled = true, description = "Event query for block on mongoDB", groups = {"daily"})
  public void test01MongoDbEventQueryForBlock() {
    FindIterable<Document> findIterable = mongoDatabase.getCollection("block").find();
    MongoCursor<Document> mongoCursor = findIterable.iterator();
    Long blockNumber = 0L;
    Boolean hasTransactions = false;
    while (mongoCursor.hasNext()) {
      Document document = mongoCursor.next();
      Assert.assertTrue(Integer.parseInt(document.get("blockNumber").toString()) > 0);
      if (Integer.parseInt(document.get("transactionSize").toString()) > 0) {
        hasTransactions = true;
        blockNumber = Long.parseLong(document.get("blockNumber").toString());
        logger.info("blockNumber:" + blockNumber);
        response = HttpMethod.getBlockByNum(httpFullNode, blockNumber);
        responseContent = HttpMethod.parseResponseContent(response);
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
        response = HttpMethod.getTransactionCountByBlocknum(httpFullNode, blockNumber);
        responseContent = HttpMethod.parseResponseContent(response);
        Assert.assertEquals(
            responseContent.getString("count"), document.get("transactionSize").toString());
        break;
      }
    }
    Assert.assertTrue(hasTransactions);
  }

  @Test(enabled = true, description = "Event query for solidity on mongoDB", groups = {"daily"})
  public void test02MongoDbEventQueryForSolidity() {

    response = HttpMethod.getNowBlockFromSolidity(httpsolidityNode);
    responseContent = HttpMethod.parseResponseContent(response);
    Long blockNumber =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getLong("number");

    BasicDBObject query = new BasicDBObject();
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    query.put("latestSolidifiedBlockNumber", blockNumber);
    FindIterable<Document> findIterable = mongoDatabase.getCollection("solidity").find(query);
    MongoCursor<Document> mongoCursor = findIterable.iterator();

    Document document = mongoCursor.next();

    Assert.assertEquals(
        String.valueOf(blockNumber), document.get("latestSolidifiedBlockNumber").toString());

    response = HttpMethod.getBlockByNumFromSolidity(httpsolidityNode, blockNumber);
    responseContent = HttpMethod.parseResponseContent(response);
    String timeStampFromHttp =
        responseContent
            .getJSONObject("block_header")
            .getJSONObject("raw_data")
            .getString("timestamp");
    logger.info("timeStampFromHttp:" + timeStampFromHttp);
    Assert.assertEquals(timeStampFromHttp, document.get("timeStamp").toString());
  }

}
