package stest.tron.wallet.dailybuild.eventquery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.junit.Assert;
import org.testng.annotations.*;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.*;

@Slf4j
public class MongoEventQuery005 extends MongoBase {

  public static String httpFullNode =
      Configuration.getByPath("testng.conf").getStringList("httpnode.ip.list").get(0);


  @Test(enabled = true, description =
      "MongoDB Event query for new Field in FreezeBalanceV2Contract", groups = {"daily"})
  public void test01EventQueryForTransactionFreezeBalanceV2() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethod.sendcoin(
            freezeAccount, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    String txid = PublicMethod.freezeBalanceV2AndGetTxId(freezeAccount,
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
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals("FreezeBalanceV2Contract", jsonObject.getString("contractType"));
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit, jsonObject.getLongValue("assetAmount"));
  }


  @Test(enabled = true,
      description = "MongoDB Event query for new Field in UnFreezeBalanceV2Contract", groups = {"daily"})
  public void test02EventQueryForTransactionUnFreezeBalanceV2() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethod.sendcoin(
            freezeAccount, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethod.unFreezeBalanceV2AndGetTxId(freezeAccount,
        freezeAccountKey, maxFeeLimit, 0, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
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
    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals("UnfreezeBalanceV2Contract", jsonObject.getString("contractType"));

    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit, jsonObject.getLongValue("assetAmount"));
  }

  @Test(enabled = true,
      description = "MongoDB Event query for new Field in CancelAllUnFreezeV2Contract", groups = {"daily"})
  public void test03EventQueryForTransactionCancelAllUnFreezeV2() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());
    Long freezeAmount = maxFeeLimit * 20;
    Assert.assertTrue(
        PublicMethod.sendcoin(
            freezeAccount, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethod.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.unFreezeBalanceV2AndGetTxId(freezeAccount,
        freezeAccountKey, maxFeeLimit, 0, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethod.cancelAllUnFreezeBalanceV2AndGetTxid(freezeAccount, freezeAccountKey,
        blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

        BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
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
    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals("CancelAllUnfreezeV2Contract", jsonObject.getString("contractType"));
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit,
        jsonObject.getJSONObject("extMap").getLongValue("BANDWIDTH"));
  }

  @Test(enabled = true,
      description = "MongoDB Event query for new Field in DelegateResourceContract", groups = {"daily"})
  public void test04EventQueryForTransactionDelegateResource() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    final String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey2.getAddress();
    String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethod.printAddress(receiverKey);

    final Long freezeAmount = maxFeeLimit * 20;
    final Long delegateAmount = 10000000L;

    Assert.assertTrue(
        PublicMethod.sendcoin(
            freezeAccount, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    Assert.assertTrue(
        PublicMethod.sendcoin(
            receiverAddress, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethod.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethod.delegateResourceV2AndGetTxId(freezeAccount,
        delegateAmount, 0, receiverAddress, freezeAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    BasicDBObject query = new BasicDBObject();
    query.put("transactionId", txid);
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

    JSONObject jsonObject = JSON.parseObject(document.toJson());
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals("DelegateResourceContract", jsonObject.getString("contractType"));
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals(Base58.encode58Check(receiverAddress), jsonObject.getString("toAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(delegateAmount.longValue(), jsonObject.getLongValue("assetAmount"));
  }

  @Test(enabled = true,
      description = "MongoDB Event query for new Field in UnDelegateResourceContract", groups = {"daily"})
  public void test05EventQueryForTransactionUnDelegateResource() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    final String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    ECKey ecKey2 = new ECKey(Utils.getRandom());
    byte[] receiverAddress = ecKey2.getAddress();
    String receiverKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethod.printAddress(receiverKey);

    final Long freezeAmount = maxFeeLimit * 20;
    final Long delegateAmount = 10000000L;

    Assert.assertTrue(
        PublicMethod.sendcoin(
            freezeAccount, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    Assert.assertTrue(
        PublicMethod.sendcoin(
            receiverAddress, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethod.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.delegateResourceV2AndGetTxId(freezeAccount,
        delegateAmount, 0, receiverAddress, freezeAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    String txid = PublicMethod.unDelegateResourceV2AndGetTxId(freezeAccount,
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
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals("UnDelegateResourceContract", jsonObject.getString("contractType"));
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals(Base58.encode58Check(receiverAddress), jsonObject.getString("toAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(delegateAmount.longValue(), jsonObject.getLongValue("assetAmount"));
  }

  @Test(enabled = true,
      description = "MongoDB Event query for new Field in WithdrawExpireUnfreezeContract", groups = {"daily"})
  public void test06EventQueryForTransactionWithdrawExpireUnfreeze() throws InterruptedException {
    ECKey ecKey = new ECKey(Utils.getRandom());
    byte[] freezeAccount = ecKey.getAddress();
    String freezeAccountKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    Long freezeAmount = maxFeeLimit * 20;
    Long delegateAmount = 10000000L;

    Assert.assertTrue(
        PublicMethod.sendcoin(
            freezeAccount, freezeAmount, foundationAddress2, foundationKey2, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.freezeBalanceV2AndGetTxId(freezeAccount,
        maxFeeLimit, 0, freezeAccountKey, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    PublicMethod.unFreezeBalanceV2AndGetTxId(freezeAccount,
        freezeAccountKey, maxFeeLimit, 0, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Thread.sleep(60000);
    String txid = PublicMethod.withdrawExpireUnfreezeAndGetTxId(freezeAccount,
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
    logger.info(jsonObject.toJSONString());
    Assert.assertEquals("WithdrawExpireUnfreezeContract", jsonObject.getString("contractType"));
    Assert.assertEquals(Base58.encode58Check(freezeAccount), jsonObject.getString("fromAddress"));
    Assert.assertEquals("trx", jsonObject.getString("assetName"));
    Assert.assertEquals(maxFeeLimit, jsonObject.getLongValue("assetAmount"));
  }


}
