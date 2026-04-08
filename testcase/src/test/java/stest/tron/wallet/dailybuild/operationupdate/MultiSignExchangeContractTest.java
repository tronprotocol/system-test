package stest.tron.wallet.dailybuild.operationupdate;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.ExchangeList;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol.Account;
import org.tron.protos.Protocol.Exchange;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.PublicMethodForMultiSign;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class MultiSignExchangeContractTest extends TronBaseTest {
  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = 1000000001L;
  private static String name1 = "exchange001_1_" + Long.toString(now);
  private static String name2 = "exchange001_2_" + Long.toString(now);
  private final String operations = Configuration.getByPath("testng.conf")
      .getString("defaultParameter.operations");
  String description = "just-test";
  String url = "https://github.com/tronprotocol/wallet-cli/";
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] exchange001Address = ecKey1.getAddress();
  String exchange001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] secondExchange001Address = ecKey2.getAddress();
  String secondExchange001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  Long secondTransferAssetToFirstAccountNum = 100000000L;
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] manager1Address = ecKey3.getAddress();
  String manager1Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  ECKey ecKey4 = new ECKey(Utils.getRandom());
  byte[] manager2Address = ecKey4.getAddress();
  String manager2Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  String[] permissionKeyString = new String[2];
  String[] ownerKeyString = new String[3];
  String accountPermissionJson = "";
  Account firstAccount;
  ByteString assetAccountId1;
  ByteString assetAccountId2;

  Optional<Exchange> exchangeIdInfo;
  Integer exchangeId = 0;
  Integer exchangeRate = 10;
  Long firstTokenInitialBalance = 10000L;
  Long secondTokenInitialBalance = firstTokenInitialBalance * exchangeRate;
  private long multiSignFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.multiSignFee");
  private long updateAccountPermissionFee = Configuration.getByPath("testng.conf")
      .getLong("defaultParameter.updateAccountPermissionFee");
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);
  }

  @Test(enabled = false, description = "MultiSign for create token", groups = {"contract", "daily"})
  public void test1CreateUsedAsset() {
    ecKey1 = new ECKey(Utils.getRandom());
    exchange001Address = ecKey1.getAddress();
    exchange001Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());

    ecKey2 = new ECKey(Utils.getRandom());
    secondExchange001Address = ecKey2.getAddress();
    secondExchange001Key = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

    PublicMethod.printAddress(exchange001Key);
    PublicMethod.printAddress(secondExchange001Key);

    Assert.assertTrue(PublicMethod.sendcoin(exchange001Address, 10240000000L, foundationAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(secondExchange001Address, 10240000000L, foundationAddress,
        testKey002, blockingStubFull));
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(foundationAddress, 100000000000L + PublicMethod.randomFreezeAmount.addAndGet(1), 0, 0,
            ByteString.copyFrom(exchange001Address),
            testKey002, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  Long start = System.currentTimeMillis() + 5000L;
  Long end = System.currentTimeMillis() + 5000000L;
    Assert.assertTrue(PublicMethod.createAssetIssue(exchange001Address, name1, totalSupply, 1,
        1, start, end, 1, description, url, 10000L, 10000L,
        1L, 1L, exchange001Key, blockingStubFull));
    Assert.assertTrue(PublicMethod.createAssetIssue(secondExchange001Address, name2, totalSupply, 1,
        1, start, end, 1, description, url, 10000L, 10000L,
        1L, 1L, secondExchange001Key, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  }

  @Test(enabled = false, description = "MultiSign for create exchange", groups = {"contract", "daily"})
  public void test2CreateExchange() {
    ecKey3 = new ECKey(Utils.getRandom());
    manager1Address = ecKey3.getAddress();
    manager1Key = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

    ecKey4 = new ECKey(Utils.getRandom());
    manager2Address = ecKey4.getAddress();
    manager2Key = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  final long needCoin = updateAccountPermissionFee + multiSignFee;
  Long balanceBefore = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);

    permissionKeyString[0] = manager1Key;
    permissionKeyString[1] = manager2Key;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    ownerKeyString[0] = exchange001Key;
    ownerKeyString[1] = manager1Key;
    ownerKeyString[2] = manager2Key;
    accountPermissionJson =
        "{\"owner_permission\":{\"type\":0,\"permission_name\":\"owner\",\"threshold\":3,\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(exchange001Key)
            + "\",\"weight\":1}]},"
            + "\"active_permissions\":[{\"type\":2,\"permission_name\":\"active0\",\"threshold\":2,"
            + "\"operations\":\"" + operations + "\","
            + "\"keys\":["
            + "{\"address\":\"" + PublicMethod.getAddressString(manager1Key) + "\",\"weight\":1},"
            + "{\"address\":\"" + PublicMethod.getAddressString(manager2Key) + "\",\"weight\":1}"
            + "]}]}";
    logger.info(accountPermissionJson);
    PublicMethodForMultiSign.accountPermissionUpdate(
        accountPermissionJson, exchange001Address, exchange001Key,
        blockingStubFull, ownerKeyString);


    Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
    assetAccountId1 = getAssetIdFromThisAccount.getAssetIssuedID();

    getAssetIdFromThisAccount = PublicMethod
        .queryAccount(secondExchange001Address, blockingStubFull);
    assetAccountId2 = getAssetIdFromThisAccount.getAssetIssuedID();

    firstAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
  Long token1BeforeBalance = 0L;
    for (String name : firstAccount.getAssetMap().keySet()) {
      token1BeforeBalance = firstAccount.getAssetMap().get(name);
    }
    Assert.assertTrue(PublicMethod.transferAsset(exchange001Address, assetAccountId2.toByteArray(),
        secondTransferAssetToFirstAccountNum, secondExchange001Address,
        secondExchange001Key, blockingStubFull));
  Long token2BeforeBalance = secondTransferAssetToFirstAccountNum;
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //logger.info("name1 is " + name1);
  //logger.info("name2 is " + name2);
  //logger.info("first balance is " + Long.toString(token1BeforeBalance));
  //logger.info("second balance is " + token2BeforeBalance.toString());
  //CreateExchange
    Assert.assertTrue(
        PublicMethodForMultiSign.exchangeCreate1(
            assetAccountId1.toByteArray(), firstTokenInitialBalance,
            assetAccountId2.toByteArray(), secondTokenInitialBalance, exchange001Address,
            exchange001Key, blockingStubFull, 0, ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    exchangeId = PublicMethod.getExchangeIdByCreatorAddress(exchange001Address, blockingStubFull).intValue();
  Long balanceAfter = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin + 1024_000_000L);

  }


  @Test(enabled = false, description = "Multisign for inject exchange", groups = {"contract", "daily"})
  public void test4InjectExchange() {
    exchangeIdInfo = PublicMethod.getExchange(exchangeId.toString(), blockingStubFull);
  final Long beforeExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
  final Long beforeExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
  final long needCoin = multiSignFee;
  Long balanceBefore = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    firstAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
  Long beforeToken1Balance = 0L;
  Long beforeToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("before token 1 balance is " + Long.toString(beforeToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(beforeToken2Balance));
    Integer injectBalance = 100;
    Assert.assertTrue(
        PublicMethodForMultiSign.injectExchange1(
            exchangeId, assetAccountId1.toByteArray(), injectBalance,
            exchange001Address, exchange001Key, blockingStubFull, 0, ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    firstAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
  Long afterToken1Balance = 0L;
  Long afterToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("before token 1 balance is " + Long.toString(afterToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(afterToken2Balance));

    Assert.assertTrue(beforeToken1Balance - afterToken1Balance == injectBalance);
    Assert.assertTrue(beforeToken2Balance - afterToken2Balance == injectBalance
        * exchangeRate);

    exchangeIdInfo = PublicMethod.getExchange(exchangeId.toString(), blockingStubFull);
  Long afterExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
  Long afterExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    Assert.assertTrue(afterExchangeToken1Balance - beforeExchangeToken1Balance
        == injectBalance);
    Assert.assertTrue(afterExchangeToken2Balance - beforeExchangeToken2Balance
        == injectBalance * exchangeRate);
  Long balanceAfter = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  @Test(enabled = false, description = "MultiSign for withdraw exchange", groups = {"contract", "daily"})
  public void test5WithdrawExchange() {
    final long needCoin = multiSignFee;
  Long balanceBefore = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    exchangeIdInfo = PublicMethod.getExchange(exchangeId.toString(), blockingStubFull);
  final Long beforeExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
  final Long beforeExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();

    firstAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
  Long beforeToken1Balance = 0L;
  Long beforeToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }

    logger.info("before token 1 balance is " + Long.toString(beforeToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(beforeToken2Balance));
    Integer withdrawNum = 200;
    Assert.assertTrue(
        PublicMethodForMultiSign.exchangeWithdraw1(
            exchangeId, assetAccountId1.toByteArray(), withdrawNum,
            exchange001Address, exchange001Key, blockingStubFull, 0, ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    firstAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
  Long afterToken1Balance = 0L;
  Long afterToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }

    logger.info("before token 1 balance is " + Long.toString(afterToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(afterToken2Balance));

    Assert.assertTrue(afterToken1Balance - beforeToken1Balance == withdrawNum);
    Assert.assertTrue(afterToken2Balance - beforeToken2Balance == withdrawNum
        * exchangeRate);
    exchangeIdInfo = PublicMethod.getExchange(exchangeId.toString(), blockingStubFull);
  Long afterExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
  Long afterExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    Assert.assertTrue(afterExchangeToken1Balance - beforeExchangeToken1Balance
        == -withdrawNum);
    Assert.assertTrue(afterExchangeToken2Balance - beforeExchangeToken2Balance
        == -withdrawNum * exchangeRate);
  Long balanceAfter = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);

  }

  @Test(enabled = false, description = "MultiSign for transaction exchange", groups = {"contract", "daily"})
  public void test6TransactionExchange() {
    final long needCoin = multiSignFee;
  Long balanceBefore = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceBefore: " + balanceBefore);
    exchangeIdInfo = PublicMethod.getExchange(exchangeId.toString(), blockingStubFull);
  final Long beforeExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
  final Long beforeExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    logger.info("beforeExchangeToken1Balance" + beforeExchangeToken1Balance);
    logger.info("beforeExchangeToken2Balance" + beforeExchangeToken2Balance);

    firstAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
  Long beforeToken1Balance = 0L;
  Long beforeToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        beforeToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }

    logger.info("before token 1 balance is " + Long.toString(beforeToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(beforeToken2Balance));
    Integer transactionNum = 50;
    Assert.assertTrue(
        PublicMethodForMultiSign
            .exchangeTransaction1(exchangeId, assetAccountId1.toByteArray(), transactionNum, 1,
                exchange001Address, exchange001Key, blockingStubFull, 0, ownerKeyString));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    firstAccount = PublicMethod.queryAccount(exchange001Address, blockingStubFull);
  Long afterToken1Balance = 0L;
  Long afterToken2Balance = 0L;
    for (String id : firstAccount.getAssetV2Map().keySet()) {
      if (assetAccountId1.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken1Balance = firstAccount.getAssetV2Map().get(id);
      }
      if (assetAccountId2.toStringUtf8().equalsIgnoreCase(id)) {
        afterToken2Balance = firstAccount.getAssetV2Map().get(id);
      }
    }
    logger.info("before token 1 balance is " + Long.toString(afterToken1Balance));
    logger.info("before token 2 balance is " + Long.toString(afterToken2Balance));

    exchangeIdInfo = PublicMethod.getExchange(exchangeId.toString(), blockingStubFull);
  Long afterExchangeToken1Balance = exchangeIdInfo.get().getFirstTokenBalance();
  Long afterExchangeToken2Balance = exchangeIdInfo.get().getSecondTokenBalance();
    logger.info("afterExchangeToken1Balance" + afterExchangeToken1Balance);
    logger.info("afterExchangeToken2Balance" + afterExchangeToken2Balance);
    Assert.assertTrue(afterExchangeToken1Balance - beforeExchangeToken1Balance
        == beforeToken1Balance - afterToken1Balance);
    Assert.assertTrue(afterExchangeToken2Balance - beforeExchangeToken2Balance
        == beforeToken2Balance - afterToken2Balance);
  Long balanceAfter = PublicMethod.queryAccount(exchange001Address, blockingStubFull)
        .getBalance();
    logger.info("balanceAfter: " + balanceAfter);
    Assert.assertEquals(balanceBefore - balanceAfter, needCoin);
  }

  /**
   * constructor.
   */

  @AfterClass
  public void shutdown() throws InterruptedException {  }
}


