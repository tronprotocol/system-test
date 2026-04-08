package stest.tron.wallet.dailybuild.manual;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestAccount010 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account010Address = ecKey1.getAddress();
  String account010Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] account010SecondAddress = ecKey2.getAddress();
  String account010SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] account010InvalidAddress = ecKey3.getAddress();
  String account010InvalidKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = false)
  public void beforeClass() {
    PublicMethod.printAddress(account010Key);
    PublicMethod.printAddress(account010SecondKey);  }

  @Test(enabled = false, groups = {"daily"})
  public void testGetStorage() {
    Assert.assertTrue(PublicMethod.sendcoin(account010Address, 100000000,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(account010SecondAddress, 100000000,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(account010InvalidAddress, 100000000,
        foundationAddress, foundationKey, blockingStubFull));
    Account account010Info = PublicMethod.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account010Info.getAccountResource().getStorageLimit() == 0);
    Assert.assertTrue(account010Info.getAccountResource().getLatestExchangeStorageTime() == 0);

    Assert.assertTrue(PublicMethod.buyStorage(100000000L, account010Address, account010Key,
        blockingStubFull));

    account010Info = PublicMethod.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account010Info.getAccountResource().getStorageLimit() > 0);
    Assert.assertTrue(account010Info.getAccountResource().getLatestExchangeStorageTime() > 0);

    AccountResourceMessage account010Resource = PublicMethod.getAccountResource(account010Address,
        blockingStubFull);
    Assert.assertTrue(account010Resource.getStorageLimit() > 0);
  }

  @Test(enabled = false, groups = {"daily"})
  public void testSellStorage() {
    AccountResourceMessage account010Resource = PublicMethod.getAccountResource(account010Address,
        blockingStubFull);
  Long storageLimit = account010Resource.getStorageLimit();
    Account account001Info = PublicMethod.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account001Info.getBalance() == 0);
  //When there is no enough storage,sell failed.
    Assert.assertFalse(PublicMethod.sellStorage(storageLimit + 1, account010Address, account010Key,
        blockingStubFull));
  //Can not sell 0 storage
    Assert.assertFalse(PublicMethod.sellStorage(0, account010Address, account010Key,
        blockingStubFull));
  //Sell all storage.
    Assert.assertTrue(PublicMethod.sellStorage(storageLimit, account010Address, account010Key,
        blockingStubFull));
    account010Resource = PublicMethod.getAccountResource(account010Address,
        blockingStubFull);
    storageLimit = account010Resource.getStorageLimit();
    Assert.assertTrue(storageLimit == 0);
    account001Info = PublicMethod.queryAccount(account010Key, blockingStubFull);
    Assert.assertTrue(account001Info.getBalance() > 0);


  }


  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


