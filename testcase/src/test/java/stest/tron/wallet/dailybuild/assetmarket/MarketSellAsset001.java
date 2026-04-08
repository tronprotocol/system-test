package stest.tron.wallet.dailybuild.assetmarket;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.Return;
import org.tron.api.GrpcAPI.Return.response_code;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.MarketOrder;
import org.tron.protos.Protocol.MarketOrderList;
import org.tron.protos.Protocol.Transaction;
import org.tron.protos.Protocol.Transaction.Result.code;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MarketSellAsset001 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final String name = "testAssetIssue003_" + Long.toString(now);
  private static final String shortname = "a";
  private final String foundationKey002 =
      Configuration.getByPath("testng.conf").getString("foundationAccount.key2");
  private final byte[] foundationAddress002 = PublicMethod.getFinalAddress(foundationKey002);
  String description =
      Configuration.getByPath("testng.conf").getString("defaultParameter.assetDescription");
  String url = Configuration.getByPath("testng.conf").getString("defaultParameter.assetUrl");
  ECKey ecKey001 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey001.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey001.getPrivKeyBytes());
  byte[] assetAccountId001;
  ECKey ecKey002 = new ECKey(Utils.getRandom());
  byte[] testAddress002 = ecKey002.getAddress();
  String testKey002 = ByteArray.toHexString(ecKey002.getPrivKeyBytes());
  byte[] assetAccountId002;

  long sellTokenQuantity = 100;
  long buyTokenQuantity = 50;
  public String solidityNode =
      Configuration.getByPath("testng.conf").getStringList("solidityNode.ip.list").get(0);

  /** constructor. */
  @BeforeClass
  public void beforeClass() {
    initSolidityChannel();    channelSolidity = ManagedChannelBuilder.forTarget(solidityNode).usePlaintext().build();
    PublicMethod.printAddress(testKey001);
    PublicMethod.printAddress(testKey002);

    Assert.assertTrue(
        PublicMethod.sendcoin(
            testAddress001,
            20000_000000L,
            foundationAddress,
            foundationKey,
            blockingStubFull));
    Assert.assertTrue(
        PublicMethod.sendcoin(
            testAddress002,
            20000_000000L,
            foundationAddress,
            foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 5000;
  Long end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(
        PublicMethod.createAssetIssue(
            testAddress001,
            name,
            10000_000000L,
            1,
            1,
            start,
            end,
            1,
            description,
            url,
            10000L,
            10000L,
            1L,
            1L,
            testKey001,
            blockingStubFull));

    start = System.currentTimeMillis() + 5000;
    end = System.currentTimeMillis() + 1000000000;
    Assert.assertTrue(
        PublicMethod.createAssetIssue(
            testAddress002,
            name,
            10000_000000L,
            1,
            1,
            start,
            end,
            1,
            description,
            url,
            10000L,
            10000L,
            1L,
            1L,
            testKey002,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    assetAccountId001 =
        PublicMethod.queryAccount(testAddress001, blockingStubFull)
            .getAssetIssuedID()
            .toByteArray();

    assetAccountId002 =
        PublicMethod.queryAccount(testAddress002, blockingStubFull)
            .getAssetIssuedID()
            .toByteArray();
  }

  @Test(enabled = false, description = "create sellOrder", groups = {"daily"})
  void marketSellAssetTest001() {

    String txid =
        PublicMethod.marketSellAsset(
            testAddress001,
            testKey001,
            assetAccountId001,
            sellTokenQuantity,
            assetAccountId002,
            buyTokenQuantity,
            blockingStubFull);

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Assert.assertNotNull(txid);

    Optional<Transaction> transaction = PublicMethod.getTransactionById(txid, blockingStubFull);

    Assert.assertEquals(transaction.get().getRet(0).getRet(), code.SUCESS);

    Optional<MarketOrderList> orderList =
        PublicMethod.getMarketOrderByAccount(testAddress001, blockingStubFull);
    Assert.assertTrue(orderList.get().getOrdersCount() > 0);
  byte[] orderId = orderList.get().getOrders(0).getOrderId().toByteArray();

    MarketOrder order = PublicMethod.getMarketOrderById(orderId, blockingStubFull).get();

    Assert.assertEquals(order.getOrderId().toByteArray(), orderId);
    Assert.assertEquals(order.getOwnerAddress().toByteArray(), testAddress001);
    Assert.assertEquals(order.getSellTokenId().toByteArray(), assetAccountId001);
    Assert.assertEquals(order.getSellTokenQuantity(), sellTokenQuantity);
    Assert.assertEquals(order.getBuyTokenId().toByteArray(), assetAccountId002);
    Assert.assertEquals(order.getBuyTokenQuantity(), buyTokenQuantity);

    PublicMethod.waitSolidityNodeSynFullNodeData(blockingStubFull, blockingStubSolidity);
    Optional<Transaction> transactionFromSolidity =
        PublicMethod.getTransactionByIdSolidity(txid, blockingStubSolidity);
    Assert.assertEquals(transaction, transactionFromSolidity);
  }

  @Test(enabled = false, description = "create sellOrder with value excption", groups = {"daily"})
  void marketSellAssetTest002() {

    ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] testAddress = ecKey.getAddress();
  String testKey = ByteArray.toHexString(ecKey.getPrivKeyBytes());

    long sendCoinValue = 10000_000000L;
    Assert.assertTrue(
        PublicMethod.sendcoin(
            testAddress, sendCoinValue, foundationAddress, foundationKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long sellTokenQuantity = 100;
    long buyTokenQuantity = 50;

    Return resposne =
        PublicMethod.marketSellAssetGetResposne(
            testAddress,
            testKey,
            assetAccountId001,
            sellTokenQuantity,
            assetAccountId002,
            buyTokenQuantity,
            blockingStubFull);
    Assert.assertEquals(
        ByteArray.toStr(resposne.getMessage().toByteArray()),
        "Contract validate error : SellToken balance is not enough !");
    Assert.assertEquals(resposne.getCode(), response_code.CONTRACT_VALIDATE_ERROR);

    resposne =
        PublicMethod.marketSellAssetGetResposne(
            testAddress,
            testKey,
            assetAccountId001,
            0,
            assetAccountId002,
            buyTokenQuantity,
            blockingStubFull);
    Assert.assertEquals(
        ByteArray.toStr(resposne.getMessage().toByteArray()),
        "Contract validate error : token quantity must greater than zero");
    Assert.assertEquals(resposne.getCode(), response_code.CONTRACT_VALIDATE_ERROR);

    Account account = PublicMethod.queryAccount(testAddress, blockingStubFull);
    Assert.assertEquals(account.getBalance(), sendCoinValue);
  }

  @Test(enabled = false, description = "create sellOrder with tokenId excption", groups = {"daily"})
  void marketSellAssetTest003() {

    long beforeBalance = PublicMethod.queryAccount(testAddress001, blockingStubFull).getBalance();
    logger.info("BeforeBalance: " + beforeBalance);

    Return resposne =
        PublicMethod.marketSellAssetGetResposne(
            testAddress001,
            testKey001,
            "xxxx".getBytes(),
            sellTokenQuantity,
            assetAccountId002,
            buyTokenQuantity,
            blockingStubFull);
    Assert.assertEquals(
        ByteArray.toStr(resposne.getMessage().toByteArray()),
        "Contract validate error : sellTokenId is not a valid number");
    Assert.assertEquals(resposne.getCode(), response_code.CONTRACT_VALIDATE_ERROR);

    resposne =
        PublicMethod.marketSellAssetGetResposne(
            testAddress001,
            testKey001,
            assetAccountId001,
            sellTokenQuantity,
            "xxx".getBytes(),
            buyTokenQuantity,
            blockingStubFull);
    Assert.assertEquals(
        ByteArray.toStr(resposne.getMessage().toByteArray()),
        "Contract validate error : buyTokenId is not a valid number");
    Assert.assertEquals(resposne.getCode(), response_code.CONTRACT_VALIDATE_ERROR);

    resposne =
        PublicMethod.marketSellAssetGetResposne(
            testAddress001,
            testKey001,
            "10001039999".getBytes(),
            sellTokenQuantity,
            assetAccountId002,
            buyTokenQuantity,
            blockingStubFull);
    Assert.assertEquals(
        ByteArray.toStr(resposne.getMessage().toByteArray()),
        "Contract validate error : No sellTokenId !");
    Assert.assertEquals(resposne.getCode(), response_code.CONTRACT_VALIDATE_ERROR);

    resposne =
        PublicMethod.marketSellAssetGetResposne(
            testAddress001,
            testKey001,
            assetAccountId001,
            sellTokenQuantity,
            "10001039999".getBytes(),
            buyTokenQuantity,
            blockingStubFull);
    Assert.assertEquals(
        ByteArray.toStr(resposne.getMessage().toByteArray()),
        "Contract validate error : No buyTokenId !");
    Assert.assertEquals(resposne.getCode(), response_code.CONTRACT_VALIDATE_ERROR);

    resposne =
        PublicMethod.marketSellAssetGetResposne(
            testAddress001,
            testKey001,
            assetAccountId001,
            sellTokenQuantity,
            assetAccountId001,
            buyTokenQuantity,
            blockingStubFull);
    Assert.assertEquals(
        ByteArray.toStr(resposne.getMessage().toByteArray()),
        "Contract validate error : cannot exchange same tokens");
    Assert.assertEquals(resposne.getCode(), response_code.CONTRACT_VALIDATE_ERROR);

    long afterBalance = PublicMethod.queryAccount(testAddress002, blockingStubFull).getBalance();
    logger.info("AfterBalance: " + afterBalance);

    Assert.assertEquals(beforeBalance, afterBalance);
  }
}
