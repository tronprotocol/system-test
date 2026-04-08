package stest.tron.wallet.onlinestress;

import com.google.protobuf.ByteString;
import java.util.Optional;
import java.util.Random;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Exchange;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class TestExchangeTransaction extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  Optional<ExchangeList> listExchange;
  Optional<Exchange> exchangeIdInfo;
  Integer exchangeId = 0;
  Integer exchangeRate = 10;
  Long firstTokenInitialBalance = 10000L;
  Long secondTokenInitialBalance = firstTokenInitialBalance * exchangeRate;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {  }

  @Test(enabled = true, groups = {"stress"})
  public void testCreateShieldToken() {
    //just test key
    String tokenOwnerKey = "2925e186bb1e88988855f11ebf20ea3a6e19ed92328b0ffb576122e769d45b68";
  byte[] tokenOwnerAddress = PublicMethod.getFinalAddress(tokenOwnerKey);
    PublicMethod.printAddress(tokenOwnerKey);
    Assert.assertTrue(PublicMethod.sendcoin(tokenOwnerAddress, 20480000000L, foundationAddress,
        foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  String name = "shieldToken";
  Long start = System.currentTimeMillis() + 20000;
  Long end = System.currentTimeMillis() + 10000000000L;
  Long totalSupply = 1500000000000001L;
  String description = "This asset issue is use for exchange transaction stress";
  String url = "This asset issue is use for exchange transaction stress";
    Assert.assertTrue(PublicMethod.createAssetIssue(tokenOwnerAddress, name, totalSupply, 1, 1,
        start, end, 1, description, url, 1000L, 1000L,
        1L, 1L, tokenOwnerKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Account getAssetIdFromThisAccount =
        PublicMethod.queryAccount(tokenOwnerAddress, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
    logger.info("AssetId:" + assetAccountId.toString());


  }

  @Test(enabled = true, threadPoolSize = 20, invocationCount = 20, groups = {"stress"})
  public void testExchangeTransaction() {
    ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] exchangeAddress = ecKey1.getAddress();
  String exchangeKey = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  final byte[] transactionAddress = ecKey2.getAddress();
  String transactionKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethod.printAddress(exchangeKey);
    PublicMethod.printAddress(transactionKey);

    Assert.assertTrue(PublicMethod.sendcoin(exchangeAddress, 1500000000000000L, foundationAddress,
        foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(transactionAddress, 1500000000000000L, foundationAddress,
        foundationKey, blockingStubFull));
  Long totalSupply = 1500000000000000L;
    Random rand = new Random();
    Integer randNum = rand.nextInt(900000000) + 1;
  String name = "exchange_" + Long.toString(randNum);
  Long start = System.currentTimeMillis() + 20000;
  Long end = System.currentTimeMillis() + 10000000000L;
  String description = "This asset issue is use for exchange transaction stress";
  String url = "This asset issue is use for exchange transaction stress";
    Assert.assertTrue(PublicMethod.createAssetIssue(exchangeAddress, name, totalSupply, 1, 1,
        start, end, 1, description, url, 100000000L, 10000000000L,
        10L, 10L, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    Assert.assertTrue(PublicMethod.transferAsset(transactionAddress, name.getBytes(),
        1500000000L, exchangeAddress, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(30000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //500000000000000L  //5000000L
    Assert.assertTrue(PublicMethod.exchangeCreate(name.getBytes(), 500000000000000L,
        "_".getBytes(), 500000000000000L, exchangeAddress, exchangeKey, blockingStubFull));
    try {
      Thread.sleep(300000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    listExchange = PublicMethod.getExchangeList(blockingStubFull);
    exchangeId = listExchange.get().getExchangesCount();

    Integer i = 0;
    while (i++ < 10000) {
      PublicMethod.exchangeTransaction(exchangeId, "_".getBytes(), 100000, 99,
          transactionAddress, transactionKey, blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      PublicMethod.exchangeTransaction(exchangeId, name.getBytes(), 100000, 1,
          transactionAddress, transactionKey, blockingStubFull);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


