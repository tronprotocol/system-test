package stest.tron.wallet.dailybuild.assetmarket;

import com.google.protobuf.ByteString;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.Transaction;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j

public class MarketSellAsset005 extends TronBaseTest {

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
  long sellTokenQuantity = 100;
  long buyTokenQuantity = 50;
  byte [] trx = ByteArray.fromString("_");
  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey001.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  byte[] assetAccountId001;
  ByteString assetAccountId;
  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey002.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  byte[] assetAccountId002;  @BeforeClass(enabled = true)
  public void beforeClass() {    PublicMethod.printAddress(testKey001);
    PublicMethod.printAddress(testKey002);

    Assert.assertTrue(PublicMethod.sendcoin(testAddress001,2024_000000L,foundationAddress,
        foundationKey,blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(testAddress002,2024_000000L,foundationAddress,
        foundationKey,blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 5000;
  Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(PublicMethod.createAssetIssue(testAddress001,name,10000_000000L,1,1,start,
        end,1,description,url,10000L,10000L,1L, 1L,testKey001,blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    assetAccountId001 = PublicMethod.queryAccount(testAddress001, blockingStubFull)
        .getAssetIssuedID().toByteArray();

    assetAccountId = PublicMethod.queryAccount(testAddress001, blockingStubFull).getAssetIssuedID();


  }


  @Test(enabled = false,description = "Create an order to sell Trx and buy Trc10", groups = {"daily"})
  void test01SellTrxBuyTrc10() {
    long balanceAfter = PublicMethod.queryAccount(testKey001, blockingStubFull).getBalance();
    PublicMethod.transferAsset(testAddress002, assetAccountId001, 10000, testAddress001,
        testKey001, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  final Map<String, Long> beforeAsset001 = PublicMethod.queryAccount(testAddress001,
        blockingStubFull).getAssetV2Map();
  String txid = PublicMethod.marketSellAsset(testAddress002,testKey002,trx,
            sellTokenQuantity,assetAccountId001,
            buyTokenQuantity,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Transaction> transaction = PublicMethod
        .getTransactionById(txid, blockingStubFull);
    logger.info("transaction: " + transaction);
    Assert.assertEquals(transaction.get().getRet(0).getRet().toString(), "SUCESS");

    logger.info("beforeAsset001: " + beforeAsset001);

    txid = PublicMethod.marketSellAsset(testAddress001, testKey001, assetAccountId001,
            sellTokenQuantity * 2,
            trx, buyTokenQuantity * 2, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);


    Map<String, Long> afterAsset001 = PublicMethod.queryAccount(testAddress001, blockingStubFull)
            .getAssetV2Map();

    logger.info("afterAsset001: " + afterAsset001);
  String assetId001 = ByteArray.toStr(assetAccountId001);
    Assert.assertEquals((beforeAsset001.get(assetId001) - sellTokenQuantity * 2),
            afterAsset001.get(assetId001).longValue());

  }

  @Test(enabled = false,description = "Create an order to sell Trc10 and buy Trx", groups = {"daily"})
  void test02SellTrc10BuyTrx() {
    long balanceAfter = PublicMethod.queryAccount(testKey001, blockingStubFull).getBalance();
  final Map<String, Long> beforeAsset001 = PublicMethod.queryAccount(testAddress001,
        blockingStubFull).getAssetV2Map();
  String txid = PublicMethod.marketSellAsset(testAddress002,testKey002,assetAccountId001,
            sellTokenQuantity,trx,
            buyTokenQuantity,blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Optional<Transaction> transaction = PublicMethod
            .getTransactionById(txid, blockingStubFull);
    logger.info("transaction: " + transaction);
    Assert.assertEquals(transaction.get().getRet(0).getRet().toString(), "SUCESS");

    logger.info("beforeAsset001: " + beforeAsset001);

    txid = PublicMethod.marketSellAsset(testAddress001, testKey001, trx,
            sellTokenQuantity * 2,
            assetAccountId001, buyTokenQuantity * 2, blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);


  }


}
