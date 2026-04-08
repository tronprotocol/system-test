package stest.tron.wallet.account;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray; import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestAccount009 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long FREENETLIMIT = 5000L;
  private static final long BASELINE = 4800L;
  private static String name = "AssetIssue012_" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account009Address = ecKey1.getAddress();
  String account009Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] account009SecondAddress = ecKey2.getAddress();
  String account009SecondKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
  ECKey ecKey3 = new ECKey(Utils.getRandom());
  byte[] account009InvalidAddress = ecKey3.getAddress();
  String account009InvalidKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(account009Key);
    PublicMethod.printAddress(account009SecondKey);  }

  @Test(enabled = true, groups = {"smoke"})
  public void testGetEnergy() {
    Assert.assertTrue(PublicMethod.sendcoin(account009Address, 10000000,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(account009SecondAddress, 10000000,
        foundationAddress, foundationKey, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(account009InvalidAddress, 10000000,
        foundationAddress, foundationKey, blockingStubFull));

    Account account009Info = PublicMethod.queryAccount(account009Key, blockingStubFull);
    logger.info(Long.toString(
        account009Info.getAccountResource().getFrozenBalanceForEnergy().getExpireTime()));
    Assert.assertTrue(account009Info.getAccountResource().getEnergyUsage() == 0);
    Assert.assertTrue(account009Info.getAccountResource().getFrozenBalanceForEnergy()
        .getExpireTime() == 0);

    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(account009Address, 1000000L,
        3, 1, account009Key, blockingStubFull));
    account009Info = PublicMethod.queryAccount(account009Key, blockingStubFull);
    Assert.assertTrue(account009Info.getAccountResource().getEnergyUsage() == 0);
    Assert.assertTrue(account009Info.getAccountResource().getFrozenBalanceForEnergy()
        .getFrozenBalance() == 1000000L);

    AccountResourceMessage account009Resource = PublicMethod.getAccountResource(account009Address,
        blockingStubFull);
    Assert.assertTrue(account009Resource.getTotalEnergyLimit() >= 50000000000L);
    Assert.assertTrue(account009Resource.getEnergyLimit() > 0);
    Assert.assertTrue(account009Resource.getTotalEnergyWeight() >= 1);
  }

  @Test(enabled = true, groups = {"smoke"})
  public void testGetEnergyInvalid() {
    //The resourceCode can only be 0 or 1
    Assert.assertTrue(PublicMethod.freezeBalanceGetEnergy(account009InvalidAddress,
        1000000L, 3, 0, account009InvalidKey, blockingStubFull));
    Assert.assertFalse(PublicMethod.freezeBalanceGetEnergy(account009InvalidAddress, 1000000L,
        3, -1, account009InvalidKey, blockingStubFull));
    Assert.assertFalse(PublicMethod.freezeBalanceGetEnergy(account009InvalidAddress, 1000000L,
        3, 3, account009InvalidKey, blockingStubFull));

  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


