package stest.tron.wallet.dailybuild.manual;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.tron.api.GrpcAPI;
import org.tron.api.GrpcAPI.AccountResourceMessage;
import org.tron.api.WalletSolidityGrpc;
import org.tron.protos.Protocol;
import org.tron.protos.Protocol.TransactionInfo;
import stest.tron.wallet.common.client.Configuration;
import stest.tron.wallet.common.client.utils.ByteArray;
import stest.tron.wallet.common.client.utils.ECKey;
import stest.tron.wallet.common.client.utils.PublicMethod;
import stest.tron.wallet.common.client.utils.Utils;
import stest.tron.wallet.common.client.utils.TronBaseTest;

@Slf4j
public class WalletTestAccount013 extends TronBaseTest {  private final String testKey003 = Configuration.getByPath("testng.conf")
      .getString("foundationAccount.key2");
  private final byte[] toAddress = PublicMethod.getFinalAddress(testKey003);
  Optional<TransactionInfo> infoById = null;
  long account013BeforeBalance;
  long freezeAmount = 10000000L;
  long freezeDuration = 0;
  byte[] account013Address;
  String testKeyForAccount013;
  byte[] receiverDelegateAddress;
  String receiverDelegateKey;
  byte[] emptyAddress;
  String emptyKey;
  byte[] account4DelegatedResourceAddress;
  String account4DelegatedResourceKey;
  byte[] account5DelegatedResourceAddress;
  String account5DelegatedResourceKey;
  byte[] accountForDeployAddress;
  String accountForDeployKey;
  byte[] accountForAssetIssueAddress;
  String accountForAssetIssueKey;
  private ManagedChannel channelSoliInFull = null;
  private WalletSolidityGrpc.WalletSolidityBlockingStub blockingStubSoliInFull = null;
  private String soliditynode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(0);
  private String soliInFullnode = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(1);
  private String soliInPbft = Configuration.getByPath("testng.conf")
      .getStringList("solidityNode.ip.list").get(2);

  /**
   * constructor.
   */
  @BeforeClass(enabled = true)
  public void beforeClass() {    channelSolidity = ManagedChannelBuilder.forTarget(soliditynode)
        .usePlaintext()
        .build();
    blockingStubSolidity = WalletSolidityGrpc.newBlockingStub(channelSolidity);

    channelSoliInFull = ManagedChannelBuilder.forTarget(soliInFullnode)
        .usePlaintext()
        .build();
    blockingStubSoliInFull = WalletSolidityGrpc.newBlockingStub(channelSoliInFull);

    channelPbft = ManagedChannelBuilder.forTarget(soliInPbft)
        .usePlaintext()
        .build();
    blockingStubPbft = WalletSolidityGrpc.newBlockingStub(channelPbft);

    if(PublicMethod.freezeV2ProposalIsOpen(blockingStubFull)) {
      throw new SkipException("Skipping delegate resource v1 test case");
    }
  }

  @Test(enabled = true, description = "Delegate resource for bandwidth and energy", groups = {"daily"})
  public void test1DelegateResourceForBandwidthAndEnergy() {
    //Create account013
    ECKey ecKey1 = new ECKey(Utils.getRandom());
    account013Address = ecKey1.getAddress();
    testKeyForAccount013 = ByteArray.toHexString(ecKey1.getPrivKeyBytes());
    PublicMethod.printAddress(testKeyForAccount013);
  //Create receiver
    ECKey ecKey2 = new ECKey(Utils.getRandom());
    receiverDelegateAddress = ecKey2.getAddress();
    receiverDelegateKey = ByteArray.toHexString(ecKey2.getPrivKeyBytes());
    PublicMethod.printAddress(receiverDelegateKey);
  //Create Empty account
    ECKey ecKey3 = new ECKey(Utils.getRandom());
    emptyAddress = ecKey3.getAddress();
    emptyKey = ByteArray.toHexString(ecKey3.getPrivKeyBytes());
  //sendcoin to Account013
    Assert.assertTrue(PublicMethod
        .sendcoin(account013Address, 10000000000L, foundationAddress, foundationKey, blockingStubFull));
  //sendcoin to receiver
    Assert.assertTrue(PublicMethod
        .sendcoin(receiverDelegateAddress, 10000000000L, toAddress, testKey003, blockingStubFull));
  //getAccountResource account013
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    AccountResourceMessage account013Resource = PublicMethod
        .getAccountResource(account013Address, blockingStubFull);
    logger.info("013 energy limit is " + account013Resource.getEnergyLimit());
    logger.info("013 net limit is " + account013Resource.getNetLimit());
  //getAccountResource receiver
    AccountResourceMessage receiverResource = PublicMethod
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    logger.info("receiver energy limit is " + receiverResource.getEnergyLimit());
    logger.info("receiver net limit is " + receiverResource.getNetLimit());
    Protocol.Account account013infoBefore = PublicMethod
        .queryAccount(account013Address, blockingStubFull);
  //get resources of account013 before DelegateResource
    account013BeforeBalance = account013infoBefore.getBalance();
    AccountResourceMessage account013ResBefore = PublicMethod
        .getAccountResource(account013Address, blockingStubFull);
  final long account013BeforeBandWidth = account013ResBefore.getNetLimit();
    AccountResourceMessage receiverResourceBefore = PublicMethod
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
    long receiverBeforeBandWidth = receiverResourceBefore.getNetLimit();
  //Account013 DelegateResource for BandWidth to receiver
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(account013Address, freezeAmount, freezeDuration, 0,
            ByteString.copyFrom(receiverDelegateAddress), testKeyForAccount013, blockingStubFull));
    Protocol.Account account013infoAfter = PublicMethod
        .queryAccount(account013Address, blockingStubFull);
  //get balance of account013 after DelegateResource
    long account013AfterBalance = account013infoAfter.getBalance();
    AccountResourceMessage account013ResAfter = PublicMethod
        .getAccountResource(account013Address, blockingStubFull);
  //get BandWidth of account013 after DelegateResource
    long account013AfterBandWidth = account013ResAfter.getNetLimit();
    AccountResourceMessage receiverResourceAfter = PublicMethod
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
  //Bandwidth of receiver after DelegateResource
    long receiverAfterBandWidth = receiverResourceAfter.getNetLimit();
  //Balance of Account013 reduced amount same as DelegateResource
    Assert.assertTrue(account013BeforeBalance == account013AfterBalance + freezeAmount);
  //Bandwidth of account013 is equally before and after DelegateResource
    Assert.assertTrue(account013AfterBandWidth == account013BeforeBandWidth);
  //Bandwidth of receiver after DelegateResource is greater than before
    Assert.assertTrue(receiverAfterBandWidth > receiverBeforeBandWidth);
    Protocol.Account account013Before1 = PublicMethod
        .queryAccount(account013Address, blockingStubFull);
  //balance of account013 before DelegateResource
    long account013BeforeBalance1 = account013Before1.getBalance();
    AccountResourceMessage account013ResBefore1 = PublicMethod
        .getAccountResource(account013Address, blockingStubFull);
  //Energy of account013 before DelegateResource
    long account013BeforeEnergy = account013ResBefore1.getEnergyLimit();
    AccountResourceMessage receiverResourceBefore1 = PublicMethod
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
  //Energy of receiver before DelegateResource
    long receiverBeforeEnergy = receiverResourceBefore1.getEnergyLimit();
  //Account013 DelegateResource Energy to receiver
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(account013Address, freezeAmount, freezeDuration, 1,
            ByteString.copyFrom(receiverDelegateAddress), testKeyForAccount013, blockingStubFull));
    Protocol.Account account013infoAfter1 = PublicMethod
        .queryAccount(account013Address, blockingStubFull);
  //balance of account013 after DelegateResource
    long account013AfterBalance1 = account013infoAfter1.getBalance();
    AccountResourceMessage account013ResAfter1 = PublicMethod
        .getAccountResource(account013Address, blockingStubFull);
    long account013AfterEnergy = account013ResAfter1.getEnergyLimit();
  //Energy of account013 after DelegateResource
    AccountResourceMessage receiverResourceAfter1 = PublicMethod
        .getAccountResource(receiverDelegateAddress, blockingStubFull);
  //Energy of receiver after DelegateResource
    long receiverAfterEnergy = receiverResourceAfter1.getEnergyLimit();
  //Balance of Account013 reduced amount same as DelegateResource
    Assert.assertTrue(account013BeforeBalance1 == account013AfterBalance1 + freezeAmount);
  //Bandwidth of account013 is equally before and after DelegateResource
    Assert.assertTrue(account013AfterEnergy == account013BeforeEnergy);
  //Bandwidth of receiver after DelegateResource is greater than before
    Assert.assertTrue(receiverAfterEnergy > receiverBeforeEnergy);
  //account013 DelegateResource to Empty failed
    Assert.assertFalse(PublicMethod
        .freezeBalanceForReceiver(account013Address, freezeAmount, freezeDuration, 0,
            ByteString.copyFrom(emptyAddress), testKeyForAccount013, blockingStubFull));
  //account013 DelegateResource to account013 failed
    Assert.assertFalse(PublicMethod
        .freezeBalanceForReceiver(account013Address, freezeAmount, freezeDuration, 0,
            ByteString.copyFrom(account013Address), testKeyForAccount013, blockingStubFull));
    account013Resource = PublicMethod.getAccountResource(account013Address, blockingStubFull);
    logger.info("After 013 energy limit is " + account013Resource.getEnergyLimit());
    logger.info("After 013 net limit is " + account013Resource.getNetLimit());

    receiverResource = PublicMethod.getAccountResource(receiverDelegateAddress, blockingStubFull);
    logger.info("After receiver energy limit is " + receiverResource.getEnergyLimit());
    logger.info("After receiver net limit is " + receiverResource.getNetLimit());
  }

  @Test(enabled = true, description = "Get delegate resource", groups = {"daily"})
  public void test2getDelegatedResource() {
    //Create Account4
    ECKey ecKey4 = new ECKey(Utils.getRandom());
    account4DelegatedResourceAddress = ecKey4.getAddress();
    account4DelegatedResourceKey = ByteArray.toHexString(ecKey4.getPrivKeyBytes());
  //Create Account5
    ECKey ecKey5 = new ECKey(Utils.getRandom());
    account5DelegatedResourceAddress = ecKey5.getAddress();
    account5DelegatedResourceKey = ByteArray.toHexString(ecKey5.getPrivKeyBytes());
  //sendcoin to Account4
    Assert.assertTrue(PublicMethod
        .sendcoin(account4DelegatedResourceAddress, 10000000000L, foundationAddress, foundationKey,
            blockingStubFull));
  //sendcoin to Account5
    Assert.assertTrue(PublicMethod
        .sendcoin(account5DelegatedResourceAddress, 20000000000L, toAddress, testKey003,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);

    Protocol.Account account4infoBefore = PublicMethod
        .queryAccount(account4DelegatedResourceAddress, blockingStubFull);
  //Balance of Account4 before DelegateResource
    final long account4BeforeBalance = account4infoBefore.getBalance();
  //account013 DelegateResource of bandwidth to Account4
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(account013Address, freezeAmount, freezeDuration, 0,
            ByteString.copyFrom(account4DelegatedResourceAddress), testKeyForAccount013,
            blockingStubFull));
  //Account4 DelegateResource of energy to Account5
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(account4DelegatedResourceAddress, freezeAmount, freezeDuration, 1,
            ByteString.copyFrom(account5DelegatedResourceAddress), account4DelegatedResourceKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //check DelegatedResourceList，from:account013 to:Account4
    Optional<GrpcAPI.DelegatedResourceList> delegatedResourceResult1 = PublicMethod
        .getDelegatedResource(account013Address, account4DelegatedResourceAddress,
            blockingStubFull);
    long afterFreezeBandwidth = delegatedResourceResult1.get().getDelegatedResource(0)
        .getFrozenBalanceForBandwidth();
  //check DelegatedResourceList，from:Account4 to:Account5
    Optional<GrpcAPI.DelegatedResourceList> delegatedResourceResult2 = PublicMethod
        .getDelegatedResource(account4DelegatedResourceAddress, account5DelegatedResourceAddress,
            blockingStubFull);
    long afterFreezeEnergy = delegatedResourceResult2.get().getDelegatedResource(0)
        .getFrozenBalanceForEnergy();
  //FrozenBalanceForBandwidth > 0
    Assert.assertTrue(afterFreezeBandwidth > 0);
  //FrozenBalanceForEnergy > 0
    Assert.assertTrue(afterFreezeEnergy > 0);
  //check DelegatedResourceAccountIndex for Account4
    Optional<Protocol.DelegatedResourceAccountIndex> delegatedResourceIndexResult1 = PublicMethod
        .getDelegatedResourceAccountIndex(account4DelegatedResourceAddress, blockingStubFull);
  //result of From list, first Address is same as account013 address
    Assert.assertTrue(new String(account013Address)
        .equals(new String(delegatedResourceIndexResult1.get().getFromAccounts(0).toByteArray())));
  //result of To list, first Address is same as Account5 address
    Assert.assertTrue(new String(account5DelegatedResourceAddress)
        .equals(new String(delegatedResourceIndexResult1.get().getToAccounts(0).toByteArray())));
  //unfreezebalance of bandwidth from Account013 to Account4
    Assert.assertTrue(PublicMethod.unFreezeBalance(account013Address, testKeyForAccount013, 0,
        account4DelegatedResourceAddress, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //check DelegatedResourceAccountIndex of Account4
    Optional<Protocol.DelegatedResourceAccountIndex> delegatedResourceIndexResult1AfterUnfreeze =
        PublicMethod
            .getDelegatedResourceAccountIndex(account4DelegatedResourceAddress, blockingStubFull);
  //result of From list is empty
    Assert.assertTrue(
        delegatedResourceIndexResult1AfterUnfreeze.get().getFromAccountsList().isEmpty());
    Assert.assertFalse(
        delegatedResourceIndexResult1AfterUnfreeze.get().getToAccountsList().isEmpty());
  //Balance of Account013 after unfreezeBalance
    // (013 -> receiver(bandwidth), 013 -> receiver(Energy), 013 -> Account4(bandwidth))
    Assert.assertTrue(PublicMethod.queryAccount(account013Address, blockingStubFull).getBalance()
        == account013BeforeBalance - 2 * freezeAmount);
  //bandwidth from Account013 to  Account4 gone
    Assert.assertTrue(
        PublicMethod.getAccountResource(account4DelegatedResourceAddress, blockingStubFull)
            .getNetLimit() == 0);
  //unfreezebalance of Energy from Account4 to Account5
    Assert.assertTrue(PublicMethod
        .unFreezeBalance(account4DelegatedResourceAddress, account4DelegatedResourceKey, 1,
            account5DelegatedResourceAddress, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    Protocol.Account account4infoAfterUnfreezeEnergy = PublicMethod
        .queryAccount(account4DelegatedResourceAddress, blockingStubFull);
  //balance of Account4 after unfreezebalance
    long account4BalanceAfterUnfreezeEnergy = account4infoAfterUnfreezeEnergy.getBalance();
  //balance of Account4 is same as before
    Assert.assertTrue(account4BeforeBalance == account4BalanceAfterUnfreezeEnergy);
  //Energy from Account4 to  Account5 gone
    Assert.assertTrue(
        PublicMethod.getAccountResource(account5DelegatedResourceAddress, blockingStubFull)
            .getEnergyLimit() == 0);
  //Unfreezebalance of Bandwidth from Account4 to Account5 fail
    Assert.assertFalse(PublicMethod
        .unFreezeBalance(account4DelegatedResourceAddress, account4DelegatedResourceKey, 0,
            account5DelegatedResourceAddress, blockingStubFull));
  }

  @Test(enabled = true, description = "Prepare delegate resource token", groups = {"daily"})
  public void test3PrepareToken() {
    //Create Account7
    ECKey ecKey7 = new ECKey(Utils.getRandom());
    accountForAssetIssueAddress = ecKey7.getAddress();
    accountForAssetIssueKey = ByteArray.toHexString(ecKey7.getPrivKeyBytes());
  //sendcoin to Account7
    Assert.assertTrue(PublicMethod
        .sendcoin(accountForAssetIssueAddress, 10000000000L, toAddress, testKey003,
            blockingStubFull));
  //account013 DelegateResource of bandwidth to accountForAssetIssue
    Assert.assertTrue(PublicMethod
        .freezeBalanceForReceiver(account013Address, 1000000000L, freezeDuration, 0,
            ByteString.copyFrom(accountForAssetIssueAddress), testKeyForAccount013,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //accountForAssetIssue AssetIssue
    long now = System.currentTimeMillis();
  String name = "testAccount013_" + Long.toString(now);
    long totalSupply = 100000000000L;
  String description = "zfbnb";
  String url = "aaa.com";
    Assert.assertTrue(PublicMethod
        .createAssetIssue(accountForAssetIssueAddress, name, totalSupply, 1, 1,
            System.currentTimeMillis() + 2000, System.currentTimeMillis() + 1000000000, 1,
            description, url, 2000L, 2000L, 500L, 1L, accountForAssetIssueKey, blockingStubFull));

  }

  @Test(enabled = true, description = "Delegate resource about transfer asset", groups = {"daily"})
  public void test4DelegateResourceAboutTransferAsset() {
    //Wait for 3s
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //get AssetIssue Id
    Protocol.Account getAssetIdFromThisAccount;
    getAssetIdFromThisAccount = PublicMethod
        .queryAccount(accountForAssetIssueAddress, blockingStubFull);
    ByteString assetAccountId = getAssetIdFromThisAccount.getAssetIssuedID();
  //Account5 Participate AssetIssue
    Assert.assertTrue(PublicMethod
        .participateAssetIssue(accountForAssetIssueAddress, assetAccountId.toByteArray(), 1000000,
            account5DelegatedResourceAddress, account5DelegatedResourceKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //get account013，accountForAssetIssue，Account5 account resources before transferAssets
    final long account013CurrentBandwidth = PublicMethod
        .getAccountResource(account013Address, blockingStubFull).getNetUsed();
    long accountForAssetIssueCurrentBandwidth = PublicMethod
        .getAccountResource(accountForAssetIssueAddress, blockingStubFull).getNetUsed();
  final long account5CurrentBandwidth = PublicMethod
        .getAccountResource(account5DelegatedResourceAddress, blockingStubFull).getNetUsed();
  //Account5 transfer Assets receiver
    Assert.assertTrue(PublicMethod
        .transferAsset(receiverDelegateAddress, assetAccountId.toByteArray(), 100000,
            account5DelegatedResourceAddress, account5DelegatedResourceKey, blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
    PublicMethod.printAddress(accountForAssetIssueKey);
    PublicMethod.printAddress(account5DelegatedResourceKey);
  //get account013，accountForAssetIssue，Account5 resource after transferAsset
    final long account013CurrentBandwidthAfterTrans = PublicMethod
        .getAccountResource(account013Address, blockingStubFull).getNetUsed();
  final long accountForAssetIssueCurrentBandwidthAfterTrans = PublicMethod
        .getAccountResource(accountForAssetIssueAddress, blockingStubFull).getFreeNetUsed();
  final long account5CurrentBandwidthAfterTrans = PublicMethod
        .getAccountResource(account5DelegatedResourceAddress, blockingStubFull).getNetUsed();
    AccountResourceMessage account5ResourceAfterTrans = PublicMethod
        .getAccountResource(account5DelegatedResourceAddress, blockingStubFull);
  String result = "";
    if (account5ResourceAfterTrans.getAssetNetLimitCount() > 0) {
      logger.info("getAssetNetLimitCount > 0 ");
      for (String name1 : account5ResourceAfterTrans.getAssetNetLimitMap().keySet()) {
        logger.info(name1);
        result += account5ResourceAfterTrans.getAssetNetUsedMap().get(name1);

      }
    }
    logger.info(result);
    PublicMethod.printAddress(receiverDelegateKey);
    PublicMethod.printAddress(account5DelegatedResourceKey);
    long account5FreeAssetNetUsed = accountForAssetIssueCurrentBandwidthAfterTrans;
  //check resource diff
    Assert.assertTrue(Long.parseLong(result) > 0);
    Assert.assertTrue(account013CurrentBandwidth == account013CurrentBandwidthAfterTrans);
    Assert.assertTrue(account5CurrentBandwidth == account5CurrentBandwidthAfterTrans);
  }

  @Test(enabled = true, description = "Can't delegate resource for contract", groups = {"daily"})
  public void test5CanNotDelegateResourceToContract() {
    //Create Account6
    ECKey ecKey6 = new ECKey(Utils.getRandom());
    accountForDeployAddress = ecKey6.getAddress();
    accountForDeployKey = ByteArray.toHexString(ecKey6.getPrivKeyBytes());
  //PublicMethod.printAddress(accountForDeployKey);
  //sendcoin to Account6
    Assert.assertTrue(PublicMethod
        .sendcoin(accountForDeployAddress, 10000000000L, foundationAddress, foundationKey,
            blockingStubFull));
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //deploy contract under Account6
    Integer consumeUserResourcePercent = 0;
  String contractName = "TestSStore";
  String code = Configuration.getByPath("testng.conf")
        .getString("code.code_WalletTestAccount013");
  String abi = Configuration.getByPath("testng.conf").getString("abi.abi_WalletTestAccount013");

    logger.info("TestSStore");
  final byte[] contractAddress = PublicMethod
        .deployContract(contractName, abi, code, "", maxFeeLimit, 0L, consumeUserResourcePercent,
            null, accountForDeployKey, accountForDeployAddress, blockingStubFull);
    PublicMethod.waitProduceNextBlock(blockingStubFull);
  //Account4 DelegatedResource of Energy to Contract
    //After 3.6 can not delegate resource to contract
    Assert.assertFalse(PublicMethod
        .freezeBalanceForReceiver(account4DelegatedResourceAddress, freezeAmount, freezeDuration, 1,
            ByteString.copyFrom(contractAddress), account4DelegatedResourceKey, blockingStubFull));
  }


  @Test(enabled = true, description = "Get delegate resource from solidity", groups = {"daily"})
  public void test6GetDelegateResourceFromSolidity() {
    Optional<GrpcAPI.DelegatedResourceList> delegateResource = PublicMethod
        .getDelegatedResourceFromSolidity(account013Address, receiverDelegateAddress,
            blockingStubSolidity);
    Assert.assertTrue(delegateResource.get().getDelegatedResource(0)
        .getFrozenBalanceForEnergy() == 10000000);
    Assert.assertTrue(delegateResource.get().getDelegatedResource(0)
        .getFrozenBalanceForBandwidth() == 10000000);
  }

  @Test(enabled = true, description = "Get delegate resource from PBFT", groups = {"daily"})
  public void test7GetDelegateResourceFromPbft() {
    Optional<GrpcAPI.DelegatedResourceList> delegateResource = PublicMethod
        .getDelegatedResourceFromSolidity(account013Address, receiverDelegateAddress,
            blockingStubPbft);
    Assert.assertTrue(delegateResource.get().getDelegatedResource(0)
        .getFrozenBalanceForEnergy() == 10000000);
    Assert.assertTrue(delegateResource.get().getDelegatedResource(0)
        .getFrozenBalanceForBandwidth() == 10000000);
  }

  @Test(enabled = true, description = "Get delegate resource index from solidity", groups = {"daily"})
  public void test8GetDelegateResourceIndexFromSolidity() {
    Optional<Protocol.DelegatedResourceAccountIndex> delegateResourceIndex = PublicMethod
        .getDelegatedResourceAccountIndexFromSolidity(account013Address,
            blockingStubSolidity);
    Assert.assertTrue(delegateResourceIndex.get().getToAccountsCount() == 2);
  }

  @Test(enabled = true, description = "Get delegate resource index from PBFT", groups = {"daily"})
  public void test9GetDelegateResourceIndexFromPbft() {
    Optional<Protocol.DelegatedResourceAccountIndex> delegateResourceIndex = PublicMethod
        .getDelegatedResourceAccountIndexFromSolidity(account013Address,
            blockingStubSolidity);
    Assert.assertTrue(delegateResourceIndex.get().getToAccountsCount() == 2);
  }


  /**
   * constructor.
   */
  @AfterClass
  public void shutdown() throws InterruptedException {
    PublicMethod.freeResource(account013Address, testKeyForAccount013,
        foundationAddress, blockingStubFull);
    PublicMethod.freeResource(receiverDelegateAddress, receiverDelegateKey, foundationAddress,
        blockingStubFull);
    PublicMethod.freeResource(account4DelegatedResourceAddress, account4DelegatedResourceKey,
        foundationAddress, blockingStubFull);
    PublicMethod.freeResource(account5DelegatedResourceAddress, account5DelegatedResourceKey,
        foundationAddress, blockingStubFull);    if (channelSoliInFull != null) {
      channelSoliInFull.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }
  }
}