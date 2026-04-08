package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.protos.Protocol.MarketOrderList;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;


@Slf4j
public class MultiSignMarketAssetTest extends TronBaseTest {  ECKey ecKey0 = new ECKey(Utils.getRandom());
  byte[] testAddress001 = ecKey0.getAddress();
  String testKey001 = ByteArray.toHexString(ecKey0.getPrivKeyBytes());

  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[2];
  String accountPermissionJson = "";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey1.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey2.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  private ManagedChannel channelSolidity = null;

  /**
   * constructor.
   */

  @BeforeClass
  public void beforeClass() {
    initSolidityChannel();    Assert.assertTrue(PublicMethod
        .sendcoin(testAddress001, 20000_000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    long start = System.currentTimeMillis() + 2000;
    long end = System.currentTimeMillis() + 1000000000;

    Assert.assertTrue(PublicMethod.createAssetIssue(testAddress001,
        "MarketAsset" + start,
        100_000000L,
        1,1,
        start, end,1,"MarketAsset","MarketAsset.com",10000L,10000L,1L, 1L,testKey001,
        blockingStubFull));
  Long balanceBefore = PublicMethod.queryAccount(testAddress001, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = testKey001;
    ownerKeyString[1] = foundationKey;
  // operation include MarketSellAssetContract(52)
    Integer[] ints = {0, 1, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 30, 31,
        32, 33, 41, 42, 43, 44, 45, 48, 49, 52, 53};
  String operations = PublicMethodForMultiSign.getOperations(ints);

    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":2,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(testKey001) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(foundationKey)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethodForMultiSign
        .accountPermissionUpdate(accountPermissionJson, testAddress001, testKey001,
            blockingStubFull, ownerKeyString);

    PublicMethod.waitProduceNextBlock(blockingStubFull);

  }

  @Test(enabled = false, description = "MultiSignForMarketSellAsset with active_permissions", groups = {"contract", "daily"})
  public void testMultiSignForMarketSellAsset001() {
    //  MarketSellAsset
    ByteString assetAccountId = PublicMethod
        .queryAccount(testAddress001, blockingStubFull).getAssetIssuedID();
  int marketOrderCountBefore = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();

    Assert.assertTrue(PublicMethodForMultiSign
        .marketSellAsset(testAddress001,assetAccountId.toByteArray(),10,"_".getBytes(),10,2,
            permissionKeyString,blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
//    PublicMethod.waitProduceNextBlock(blockingStubFull);

    MarketOrderList marketOrder = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get();
    Assert.assertEquals(marketOrderCountBefore + 1, marketOrder.getOrdersCount());
  }

  @Test(enabled = false, description = "MultiSignForMarketSellAsset with owner_permission", groups = {"contract", "daily"})
  public void testMultiSignForMarketSellAsset002() {
    //  MarketSellAsset
    ByteString assetAccountId = PublicMethod
        .queryAccount(testAddress001, blockingStubFull).getAssetIssuedID();
  int marketOrderCountBefore = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();


    Assert.assertTrue(PublicMethodForMultiSign
        .marketSellAsset(testAddress001,assetAccountId.toByteArray(),10,"_".getBytes(),10,0,
            ownerKeyString,blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    MarketOrderList marketOrder = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get();
    Assert.assertEquals(marketOrderCountBefore + 1, marketOrder.getOrdersCount());
  }


  @Test(enabled = false, dependsOnMethods = "testMultiSignForMarketSellAsset001",
      description = "MultiSignForMarketOrderCancel with active_permissions", groups = {"contract", "daily"})
  public void testMultiSignForMarketOrderCancel001() {
    // MarketOrderCancel

    ByteString orderId = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrders(0).getOrderId();
  int marketOrderCountBefore = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();


    Assert.assertTrue(PublicMethodForMultiSign.marketCancelOrder(testAddress001,
        orderId.toByteArray(),2,permissionKeyString,blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(marketOrderCountBefore - 1, PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount());

  }

  @Test(enabled = false, dependsOnMethods = "testMultiSignForMarketSellAsset002",
      description = "MultiSignForMarketOrderCancel with owner_permission", groups = {"contract", "daily"})
  public void testMultiSignForMarketOrderCancel002() {
    // MarketOrderCancel

    ByteString orderId = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrders(0).getOrderId();
  int marketOrderCountBefore = PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount();


    Assert.assertTrue(PublicMethodForMultiSign.marketCancelOrder(testAddress001,
        orderId.toByteArray(),0,ownerKeyString,blockingStubFull));

    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Assert.assertEquals(marketOrderCountBefore - 1, PublicMethod
        .getMarketOrderByAccount(testAddress001, blockingStubFull).get().getOrdersCount());

  }


  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


