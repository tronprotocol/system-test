package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.*;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.Assert;
import org.testng.annotations.*;
import org.tron.api.WalletGrpc;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

@Slf4j
public class MongoEventQuery005 extends MongoBase {
  private final String foundationKey =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] foundationAddress = PublicMethed.getFinalAddress(foundationKey);

  private ManagedChannel channelFull = null;
  private WalletGrpc.WalletBlockingStub blockingStubFull = null;
  private String fullnode =
      Configuration.getByPath("testng.conf").getStringList("fullnode.ip.list").get(0);
  private String eventnode =
      Configuration.getByPath("testng.conf").getStringList("eventnode.ip.list").get(0);
  private Long maxFeeLimit =
      Configuration.getByPath("testng.conf").getLong("defaultParameter.maxFeeLimit");


  public static String httpFullNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);
  public static String httpsolidityNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(3);



  ECKey ecKey3 = new ECKey(Utils.getRandom());

  List<String> transactionIdList = null;



  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  Long amount = 1000000L;
  private String mongoNode =
      Configuration.getByPath("testng.conf").getStringList("mongonode.ip.list").get(0);
  private MongoClient mongoClient;


  /** constructor. */
  @BeforeClass(enabled = true)
  public void beforeClass() {
    channelFull = ManagedChannelBuilder.forTarget(fullnode).usePlaintext().build();
    blockingStubFull = WalletGrpc.newBlockingStub(channelFull);
  }


  @Test(enabled = true, description = "MongoDB Event query for new Field in FreezeBalanceV2Contract")
  public void test01EventQueryForTransactionFreezeBalanceV2() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    String txid = PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);

    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit.longValue(), jsonObject.getLongValue("assetAmount"));
  }


  @Test(enabled = true, description = "MongoDB Event query for new Field in UnFreezeBalanceV2Contract")
  public void test02EventQueryForTransactionUnFreezeBalanceV2() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.unFreezeBalanceV2AndGetTxId(freezeAccount,
        freezeAccountKey, maxFeeLimit, 0, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit.longValue(), jsonObject.getLongValue("assetAmount"));
  }

  @Test(enabled = true, description = "MongoDB Event query for new Field in CancelAllUnFreezeV2Contract")
  public void test03EventQueryForTransactionCancelAllUnFreezeV2() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.unFreezeBalanceV2AndGetTxId(freezeAccount,
        freezeAccountKey, maxFeeLimit, 0, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.cancelAllUnFreezeBalanceV2AndGetTxid(freezeAccount, freezeAccountKey,
        blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

        BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit.longValue(), jsonObject.getJSONObject("extMap").getLongValue("BANDWIDTH"));
  }

  @Test(enabled = true, description = "MongoDB Event query for new Field in DelegateResourceContract")
  public void test04EventQueryForTransactionDelegateResource() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey2.getAddress();
    String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(receiverKey);

    Long freezeAmount = maxFeeLimit * 20;
    Long delegateAmount = 10000000L;

    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.sendcoin(
            receiverAddress, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.delegateResourceV2AndGetTxId(freezeAccount,
        delegateAmount, 0, receiverAddress, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals(Base58.encode58Check(receiverAddress), jsonObject.getString("toAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(delegateAmount.longValue(), jsonObject.getLongValue("assetAmount"));
  }

  @Test(enabled = true, description = "MongoDB Event query for new Field in UnDelegateResourceContract")
  public void test05EventQueryForTransactionUnDelegateResource() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey2.getAddress();
    String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethed.printAddress(receiverKey);

    Long freezeAmount = maxFeeLimit * 20;
    Long delegateAmount = 10000000L;

    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(
        PublicMethed.sendcoin(
            receiverAddress, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.delegateResourceV2AndGetTxId(freezeAccount,
        delegateAmount, 0, receiverAddress, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethed.unDelegateResourceV2AndGetTxId(freezeAccount,
        delegateAmount, 0, receiverAddress, freezeAccountKey, blockingStubFull);

    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals(Base58.encode58Check(receiverAddress), jsonObject.getString("toAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(delegateAmount.longValue(), jsonObject.getLongValue("assetAmount"));
  }

  @Test(enabled = true, description = "MongoDB Event query for new Field in WithdrawExpireUnfreezeContract")
  public void test06EventQueryForTransactionWithdrawExpireUnfreeze() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    Long freezeAmount = maxFeeLimit * 20;
    Long delegateAmount = 10000000L;

    Assert.assertTrue(
        PublicMethed.sendcoin(
            freezeAccount, freezeAmount, foundationAddress, foundationKey, blockingStubFull));
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    PublicMethed.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);

    PublicMethed.unFreezeBalanceV2AndGetTxId(freezeAccount,
        freezeAccountKey, maxFeeLimit, 0, blockingStubFull);
    PublicMethed.waitProduceNextBlock(blockingStubFull);
    Thread.sleep(60000);
    String txid = PublicMethed.withdrawExpireUnfreezeAndGetTxId(freezeAccount,
        freezeAccountKey, blockingStubFull);
    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
    FindIterable<org.bson.Document> findIterable =
        mongoDatabase.getCollection("transaction").find(query);
    MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator();
    Document document = null;

    int retryTimes = 40;
    while (retryTimes-- > 0) {
      logger.info("retryTimes:" + retryTimes);
      PublicMethed.waitProduceNextBlock(blockingStubFull);
      if (!mongoCursor.hasNext()) {
        mongoCursor = mongoDatabase.getCollection("transaction").find(query).iterator();
      } else {
        document = mongoCursor.next();
        break;
      }
    }
    Assert.assertTrue(retryTimes > 0);

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit.longValue(), jsonObject.getLongValue("assetAmount"));
  }



  /** constructor. */
  @AfterClass
  public void shutdown() throws InterruptedException {

    if (channelFull != null) {
      channelFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}
