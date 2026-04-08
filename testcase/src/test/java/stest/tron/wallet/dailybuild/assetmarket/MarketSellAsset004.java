package stest.tron.wallet.dailybuild.assetmarket;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.MarketOrderList;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j

public class MarketSellAsset004 extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue003_" + Long.toString(now);
  private static final String shortname = "a";
  private final String foundationKey002 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] foundationAddress002 = PublicMethod.getFinalAddress(foundationKey002);
  String description = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.assetUrl");

  byte [] trx = ByteArray.fromString("_");
  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey001.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  byte[] assetAccountId001;
  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey002.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  byte[] assetAccountId002;  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(testKey001);
    PublicMethod.printAddress(testKey002);

    Assert.assertTrue(PublicMethod.sendcoin(testAddress001,20000_000000L,foundationAddress,
        foundationKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(testAddress002,20000_000000L,foundationAddress,
        foundationKey,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 5000;
  Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod.createAssetIssue(testAddress001,name,10000_000000L,1,1,start,
        end,1,description,url,10000L,10000L,1L, 1L,testKey001,blockingStubFull));

    start = System.currentTimeMillis() + 5000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod.createAssetIssue(testAddress002,name,10000_000000L,1,1,start,
        end,1,description,url,10000L,10000L,1L, 1L,testKey002,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetAccountId001 = PublicMethod.queryAccount(testAddress001, blockingStubFull)
        .getAssetIssuedID().toByteArray();

    assetAccountId002 = PublicMethod.queryAccount(testAddress002, blockingStubFull)
        .getAssetIssuedID().toByteArray();
  }


  @Test(enabled = false,description = "The order amount exceeds the balance", groups = {"daily"})
  void marketCancelAssetTest002() {

    String txid = PublicMethod.marketSellAsset(testAddress001,testKey001,assetAccountId001,100,
        trx,50,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Transaction> transaction = PublicMethod
        .getTransactionById(txid, blockingStubFull);
    logger.info("transaction: " + transaction);
    Assert.assertEquals(transaction.get().getRet(0).getRet().toString(), "SUCESS");

    Optional<MarketOrderList> orderList = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);
  byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();
    txid = PublicMethod.marketCancelOrder(testAddress001,testKey001,orderId,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);


  }

}
