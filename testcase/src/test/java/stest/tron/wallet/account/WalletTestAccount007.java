package stest.tron.wallet.account;

import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI.AccountNetMessage;


import org.tron.protos.Protocol.Account;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.Parameter.CommonConstant;
import stest.tron.wallet.common.client.utils.ByteArray; import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestAccount007 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 10000000000L;
  private static final long FREENETLIMIT = 5000L;
  private static final long BASELINE = 4800L;
  private static String name = "AssetIssue012_" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  //owner account
  ECKey ecKey1 = new ECKey(Utils.getRandom());
  byte[] account007Address = ecKey1.getAddress();
  String account007Key = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
  //Wait to be create account
  ECKey ecKey2 = new ECKey(Utils.getRandom());
  byte[] newAccountAddress = ecKey2.getAddress();
  String newAccountKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    logger.info(account007Key);  }

  @Test(enabled = true, groups = {"smoke"})
  public void testCreateAccount() {
    Assert.assertTrue(PublicMethod.sendcoin(account007Address, 10000000,
        foundationAddress, foundationKey, blockingStubFull));
    Account accountInfo = PublicMethod.queryAccount(account007Key, blockingStubFull);
  final Long beforeBalance = accountInfo.getBalance();

    AccountNetMessage accountNetInfo = PublicMethod.getAccountNet(account007Address,
        blockingStubFull);
  final Long beforeFreeNet = accountNetInfo.getFreeNetUsed();

    Assert.assertTrue(PublicMethod.createAccount(account007Address, newAccountAddress,
        account007Key, blockingStubFull));

    accountInfo = PublicMethod.queryAccount(account007Key, blockingStubFull);
  Long afterBalance = accountInfo.getBalance();

    accountNetInfo = PublicMethod.getAccountNet(account007Address,
        blockingStubFull);
  Long afterFreeNet = accountNetInfo.getFreeNetUsed();

    logger.info(Long.toString(beforeBalance));
    logger.info(Long.toString(afterBalance));
  //When creator has no bandwidth, he can't use the free net.
    Assert.assertTrue(afterFreeNet == beforeFreeNet);
  //When the creator has no bandwidth, create a new account should spend 0.1TRX.
    Assert.assertTrue(beforeBalance - afterBalance == 100000);
  }

  @Test(enabled = true, groups = {"smoke"})
  public void testExceptionCreateAccount() {
    //Try to create an exist account
    Assert
        .assertFalse(PublicMethod.createAccount(account007Address, account007Address, account007Key,
            blockingStubFull));
  //Try to create an invalid account
    byte[] wrongAddress = "wrongAddress".getBytes();
    Assert.assertFalse(PublicMethod.createAccount(account007Address, wrongAddress, account007Key,
        blockingStubFull));
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }


}


