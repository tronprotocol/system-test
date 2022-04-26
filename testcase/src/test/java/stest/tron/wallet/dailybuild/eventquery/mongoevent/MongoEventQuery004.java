package stest.tron.wallet.dailybuild.eventquery.mongoevent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.WalletGrpc;
import org.tron.api.WalletSolidityGrpc;
import org.zeromq.ZMQ;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.MongoBase;
import stest.tron.wallet.common.client.utils.PublicMethed;
import stest.tron.wallet.common.client.utils.Utils;
import zmq.ZMQ.Event;

@Slf4j
public class MongoEventQuery004 extends MongoBase {

  private final String testKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key1");
  private final byte[] fromAddress = PublicMethed.getFinalAddress(testKey002);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethed.getFinalAddress(testKey003);
  byte[] contractAddress;
  String txid;
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] event001Address = ecKey1.getAddress();
  String event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode = Configuration.getByPath("testng.conf")
      .getStringList("fullnode.ip.list").get(0);
  private String eventnode = Configuration.getByPath("testng.conf")
      .getStringList("eventnode.ip.list").get(0);
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private ManagedChannel channelSolidity = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSolidity = null;
  private Long maxFeeLimit = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.maxFeeLimit");



  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode)
        .usePlaintext(true)
        .build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);

    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext(true)
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    ecKey1 = new ECKey(Utils.getRandom());
    event001Address = ecKey1.getAddress();
    event001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethed.printAddress(event001Key);

    Assert.assertTrue(PublicMethed.sendcoin(event001Address, maxFeeLimit * 30, fromAddress,
        testKey002, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String contractName = "addressDemo";
    String code = Configuration.getByPath("testng.conf")
        .getString("code.code_ContractEventAndLog1");
    String abi = Configuration.getByPath("testng.conf")
        .getString("abi.abi_ContractEventAndLog1");
    contractAddress = PublicMethed.deployContract(contractName, abi, code, "", maxFeeLimit,
        0L, 50, null, event001Key, event001Address, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);


  }

  @Test(enabled = true, description = "MongoDb event query for contract log")
  public void test01EventQueryForContractLog() {
    txid = PublicMethed.triggerContract(contractAddress,
        "depositForLog()", "#", false,
        1L, 100000000L, event001Address, event001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    BasicDBObject query = new BasicDBObject();
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    query.put("transactionId", txid);
    FindIterable<Document> findIterable = mongoDatabase.getCollection("contractlog").find(query);
    MongoCursor<Document> mongoCursor = findIterable.iterator();

    Document document = mongoCursor.next();
    JSONObject jsonObject = JSON.parseObject(document.toJson());

    Assert.assertEquals(txid, jsonObject.getString("transactionId"));
    Assert.assertEquals(3, jsonObject.getJSONArray("topicList").size());
  }


  @Test(enabled = true, description = "Event query for solidity contract log")
  public void test02EventQueryForContractSolidityLog() {
    txid = PublicMethed.triggerContract(contractAddress,
        "depositForLog()", "#", false,
        1L, 100000000L, event001Address, event001Key, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    BasicDBObject query = new BasicDBObject();
    PublicMethed.waitSolidityNodeSynFullNodeData(blockingStubFull,blockingStubSolidity);
    query.put("transactionId", txid);
    FindIterable<Document> findIterable = mongoDatabase.getCollection("soliditylog").find(query);
    MongoCursor<Document> mongoCursor = findIterable.iterator();

    Document document = mongoCursor.next();
    JSONObject jsonObject = JSON.parseObject(document.toJson());

    Assert.assertEquals(txid, jsonObject.getString("transactionId"));
    Assert.assertEquals(3, jsonObject.getJSONArray("topicList").size());
  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {
    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}


