package stest.tron.wallet.account;

import com.google.protobuf.ByteString;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
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
public class WalletTestAccount006 extends TronBaseTest {

  private static final long now = System.currentTimeMillis();
  private static final long totalSupply = now;
  private static final long sendAmount = 20000000000L;
  private static final long FREENETLIMIT = 5000L;
  private static final long BASELINE = 4800L;
  private static String name = "AssetIssue012_" + Long.toString(now);
  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  //get account
  ECKey ecKey = new ECKey(Utils.getRandom());
  byte[] account006Address = ecKey.getAddress();
  String account006Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());

  /**
   * constructor.
   */

  @BeforeClass(enabled = true)
  public void beforeClass() {
    PublicMethod.printAddress(account006Key);  }

  @Test(enabled = true, groups = {"smoke"})
  public void test1GetAccountNet() {
    ecKey = new ECKey(Utils.getRandom());
    account006Address = ecKey.getAddress();
    account006Key = ByteArray.toHexString(ecKey.getPrivKeyBytes());
  //Sendcoin to this account
    ByteString addressBS1 = ByteString.copyFrom(account006Address);
    Account request1 = Account.newBuilder().setAddress(addressBS1).build();
    GrpcAPI.AssetIssueList assetIssueList1 = blockingStubFull
        .getAssetIssueByAccount(request1);
    Optional<GrpcAPI.AssetIssueList> queryAssetByAccount = Optional.ofNullable(assetIssueList1);
    Assert.assertTrue(PublicMethod.freezeBalance(foundationAddress, 100000000, 3, foundationKey,
        blockingStubFull));
    Assert.assertTrue(PublicMethod
        .sendcoin(account006Address, sendAmount, foundationAddress, foundationKey, blockingStubFull));
  //Get new account net information.
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
    logger.info(Long.toString(accountNetMessage.getNetLimit()));
    logger.info(Long.toString(accountNetMessage.getNetUsed()));
    logger.info(Long.toString(accountNetMessage.getFreeNetLimit()));
    logger.info(Long.toString(accountNetMessage.getFreeNetUsed()));
    logger.info(Long.toString(accountNetMessage.getTotalNetLimit()));
    logger.info(Long.toString(accountNetMessage.getTotalNetWeight()));
    Assert.assertTrue(accountNetMessage.getNetLimit() == 0);
    Assert.assertTrue(accountNetMessage.getNetUsed() == 0);
    Assert.assertTrue(accountNetMessage.getFreeNetLimit() == FREENETLIMIT);
    Assert.assertTrue(accountNetMessage.getFreeNetUsed() == 0);
    Assert.assertTrue(accountNetMessage.getTotalNetLimit() > 0);
    Assert.assertTrue(accountNetMessage.getTotalNetWeight() > 0);
    logger.info("testGetAccountNet");

  }

  @Test(enabled = true, groups = {"smoke"})
  public void test2UseFreeNet() {

    //Transfer some TRX to other to test free net cost.
    Assert.assertTrue(PublicMethod.sendcoin(foundationAddress, 1L, account006Address,
        account006Key, blockingStubFull));
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
  //Every transaction may cost 200 net.
    Assert.assertTrue(accountNetMessage.getFreeNetUsed() > 0 && accountNetMessage
        .getFreeNetUsed() < 300);
    logger.info("testUseFreeNet");
  }

  @Test(enabled = true, groups = {"smoke"})
  public void test3UseMoneyToDoTransaction() {
    Assert.assertTrue(PublicMethod.sendcoin(account006Address, 1000000L, foundationAddress,
        foundationKey, blockingStubFull));
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
  //Use out the free net
    Integer times = 0;
    while (accountNetMessage.getFreeNetUsed() < BASELINE && times++ < 30) {
      PublicMethod.sendcoin(foundationAddress, 1L, account006Address, account006Key,
          blockingStubFull);
      accountNetMessage = blockingStubFull.getAccountNet(request);
    }

    Account queryAccount = PublicMethod.queryAccount(account006Key, blockingStubFull);
  Long beforeSendBalance = queryAccount.getBalance();
    Assert.assertTrue(PublicMethod.sendcoin(foundationAddress, 1L, account006Address, account006Key,
        blockingStubFull));
    queryAccount = PublicMethod.queryAccount(account006Key, blockingStubFull);
  Long afterSendBalance = queryAccount.getBalance();
  //when the free net is not enough and no balance freeze, use money to do the transaction.
    Assert.assertTrue(beforeSendBalance - afterSendBalance > 1);
    logger.info("testUseMoneyToDoTransaction");
  }

  @Test(enabled = true, groups = {"smoke"})
  public void test4UseNet() {
    //Freeze balance to own net.
    Assert.assertTrue(PublicMethod.freezeBalance(account006Address, 10000000L,
        3, account006Key, blockingStubFull));
    Assert.assertTrue(PublicMethod.sendcoin(toAddress, 1L, account006Address,
        account006Key, blockingStubFull));
    ByteString addressBs = ByteString.copyFrom(account006Address);
    Account request = Account.newBuilder().setAddress(addressBs).build();
    AccountNetMessage accountNetMessage = blockingStubFull.getAccountNet(request);
    Assert.assertTrue(accountNetMessage.getNetLimit() > 0);
    Assert.assertTrue(accountNetMessage.getNetUsed() > 150);

    Account queryAccount = PublicMethod.queryAccount(account006Key, blockingStubFull);
  Long beforeSendBalance = queryAccount.getBalance();
    Assert.assertTrue(PublicMethod.sendcoin(foundationAddress, 1L, account006Address,
        account006Key, blockingStubFull));
    queryAccount = PublicMethod.queryAccount(account006Key, blockingStubFull);
  Long afterSendBalance = queryAccount.getBalance();
  //when you freeze balance and has net,you didn't cost money.
    logger.info("before is " + Long.toString(beforeSendBalance) + " and after is "
        + Long.toString(afterSendBalance));
    Assert.assertTrue(beforeSendBalance - afterSendBalance == 1);
    addressBs = ByteString.copyFrom(account006Address);
    request = Account.newBuilder().setAddress(addressBs).build();
    accountNetMessage = blockingStubFull.getAccountNet(request);
  //when you freeze balance and has net,you cost net.
    logger.info(Long.toString(accountNetMessage.getNetUsed()));
    Assert.assertTrue(accountNetMessage.getNetUsed() > 350);
  }

  /**
   * constructor.
   */

  @AfterClass(enabled = true)
  public void shutdown() throws InterruptedException {  }
}


